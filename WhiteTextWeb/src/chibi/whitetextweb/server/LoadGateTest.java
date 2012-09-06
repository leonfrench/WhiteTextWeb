package chibi.whitetextweb.server;

import gate.Gate;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.NormalizeResult;
import ubic.pubmedgate.interactions.SLOutputReader;

public class LoadGateTest {
	private static final Logger log = Logger.getLogger(LoadGateTest.class
			.getName());
	// private static final Logger log4j =
	protected static Log log4j = LogFactory.getLog(AirolaXMLReader.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String testSet = "Annotated";
		String annotationSet = "Suzanne";
		NormalizeResult result = null;

		log4j.info("test");
		// System.exit(1);

		String airolaXML = Config.config.getString("whitetextweb.airolaXML");

		String SLFolder = Config.config.getString("whitetextweb.SLResults");

		// prevents loading of GATE GUI components
		System.setProperty(Gate.BUILTIN_CREOLE_DIR_PROPERTY_NAME, "file:"
				+ System.getProperty("user.dir") + "/");
		// System.exit(1);
		StopWatch s = new StopWatch();
		s.start();
		GateInterface p2g = new GateInterface();
		p2g.setUnSeenCorpNull();
		p2g.setNamedCorpNull("PubMedUnseenJNChem");
		p2g.setNamedCorpNull("PubMedUnseenJCN");
		p2g.setNamedCorpNull("PubMedUnseenMScan1");
		p2g.setNamedCorpNull("PubMedUnseenMScan2");

		log.info(Gate.getBuiltinCreoleDir().toString());
		Gate.removeAutoloadPlugin(new URL(
				"jar:file:/grp/java/.m2/repository/gate/gate/5.0/gate-5.0.jar!/gate/resources/creole/"));
		Gate.removeKnownPlugin(new URL(
				"jar:file:/grp/java/.m2/repository/gate/gate/5.0/gate-5.0.jar!/gate/resources/creole/"));
		log.info(Gate.getBuiltinCreoleDir().toString());

		AirolaXMLReader XMLReader = new AirolaXMLReader(airolaXML, p2g,
				annotationSet);

		log.info(XMLReader.getPairIDToPMID().size() + "");
		log.info(s.toString());
		s.reset();
		s.start();
		SLOutputReader SLReader = new SLOutputReader(new File(SLFolder));

		log.info(s.toString());

	}
}
