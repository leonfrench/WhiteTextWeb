# WhiteTextWeb

An instance of this tool is online at https://whitetext.msl.ubc.ca/

The key setup point is WhitetextWeb.properties. The other property files are needed to create the rdf input from GATE corpii.

The property file needs:

whitetextweb.RDFModel=<path>/WhiteTextWeb/WhiteTextWeb/war/data/model.rdf
whitetextweb.NIFSTDTreeModel=<path>/WhiteTextWeb/WhiteTextWeb/war/data/NIFSTDTree.rdf
whitetextweb.predicateList=<path>/git/WhiteTextWeb/WhiteTextWeb/war/data/predicateList.txt
whitetextweb.flaggedLog=<path>/git/WhiteTextWeb/WhiteTextWeb/war/data/flaggedLog.txt

whitetextweb.threadsPerUser=1


The model.rdf file can be retrieved from:

http://www.chibi.ubc.ca/faculty/paul-pavlidis/pavlidis-lab/data-and-supplementary-information/the-whitetext-project/whitetext-automated-text-mining-for-neuroanatomy/

WhiteTextWeb.model.plus.mscanner.rdf is currently used for the website but a smaller version is recomended for testing.
