package chibi.whitetextweb.server;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.basecode.util.FileTools;
import chibi.whitetextweb.client.GreetingService;
import chibi.whitetextweb.data.ConnectionPredicateRegexMaker;
import chibi.whitetextweb.data.WhiteTextRDFModel;
import chibi.whitetextweb.shared.DataGridRow;
import chibi.whitetextweb.shared.DataStatistics;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class GreetingServiceImpl extends RemoteServiceServlet implements GreetingService {
	protected static Log log = LogFactory.getLog(GreetingServiceImpl.class);

	static WhiteTextRDFModel model;
	static Set<Resource> NIFTerms;
	static DataStatistics dataStatistics;
	static String regexForPredicates;
	static File flaggedFile;

	static {
		log.info("WhitetextWeb loading on startup");
		log.info("java version:" + System.getProperties().get("java.version"));

		flaggedFile = new File(Config.config.getString("whitetextweb.flaggedLog"));

		log.info(Config.config.getString("whitetextweb.RDFModel"));
		try {
			StopWatch s = new StopWatch();
			s.start();
			model = new WhiteTextRDFModel();
			model.getStats();
			log.info("Loading RDF file:" + s.toString());
			dataStatistics = model.getDataStatistics();
			NIFTerms = model.getUsedNIFConcepts();
			regexForPredicates = ConnectionPredicateRegexMaker.getRegex();
			log.info("Used NIF terms:" + NIFTerms.size());
			log.info("WhiteText Load time:" + s.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public DataStatistics getDataStatistics() {
		return dataStatistics;
	}

	public String getConnectionPredRegex() {
		return regexForPredicates;
	}

	public Set<String> getOracle() {
		Set<String> result = new HashSet<String>();
		for (Resource term : NIFTerms) {
			result.add(JenaUtil.getLabel(term));
		}
		log.info("Returning terms:" + result.size());
		return result;
	}

	public synchronized void writeFlaggedColumn(String data) {
		boolean append = true;
		String ip = getThreadLocalRequest().getRemoteAddr();
		String userAgent = getThreadLocalRequest().getHeader("User-Agent");
		data += ",IP=" + ip + ",AGENT=" + userAgent + ",TIME=" + (new Date()).toString() + "\n";
		try {
			FileTools.stringToFile(data, flaggedFile, append);
		} catch (Exception e) {
			log.warn("Flag file write is broken:" + e.getMessage());
		}
	}

	public List<DataGridRow> greetServer(String input) throws IllegalArgumentException {
		// Escape data from the client to avoid cross-site script
		// vulnerabilities.
		input = escapeHtml(input);
		log.info("Input received: " + input);
		return model.getRowsForRegion(input);
	}

	/**
	 * Escape an html string. Escaping data received from the client helps to
	 * prevent cross-site script vulnerabilities.
	 * 
	 * @param html
	 *            the html string to escape
	 * @return the escaped string
	 */
	private String escapeHtml(String html) {
		if (html == null) {
			return null;
		}
		return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
}
