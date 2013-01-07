#!/bin/bash -x
REMOTE=lfrench@krusty
REMOTEWEBSERVER=kent
REMOTEDEST=/home/lfrench/apache-tomcat-7.0.29/webapps/whitetext/

echo "Shutdown"
ssh $REMOTE "ssh $REMOTEWEBSERVER /home/lfrench/apache-tomcat-7.0.29/bin/shutdown.sh"
echo


echo 
echo "Source:"
echo $REMOTE$REMOTEDEST
echo 

#make contents of dest like src and dest anything that should not be there
rsync --delete -avr /home/leon/git/WhiteTextWeb/WhiteTextWeb/war/.  $REMOTE:$REMOTEDEST

#copy config files
ssh $REMOTE "cp $REMOTEDEST/../../webapp-config/whitetext/*.properties $REMOTEDEST/WEB-INF/classes/"

echo 
echo "Startup"
ssh $REMOTE "ssh $REMOTEWEBSERVER /home/lfrench/apache-tomcat-7.0.29/bin/startup.sh"

