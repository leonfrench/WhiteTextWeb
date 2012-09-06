package chibi.whitetextweb.data;

import gate.Gate;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.util.FileTools;
import ubic.pubmedgate.GateInterface;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.interactions.SLOutputReader;
import ubic.pubmedgate.resolve.MakeLexiconRDFModel;
import ubic.pubmedgate.resolve.RDFResolvers.BagOfStemsRDFMatcher;
import ubic.pubmedgate.resolve.RDFResolvers.RDFResolver;
import ubic.pubmedgate.resolve.mentionEditors.BracketRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.CytoPrefixMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.DirectionSplittingMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.HemisphereStripMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.NucleusOfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.OfTheRemoverMentionEditor;
import ubic.pubmedgate.resolve.mentionEditors.RegionSuffixRemover;
import chibi.whitetextweb.server.Config;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Creates the RDF data model that is loaded into the server.
 * 
 * This takes in data from several sources - GATE, Airola XML, SL classifier and
 * NIFSTD.
 * 
 * @author leon
 * 
 */
public class CreateRDFData {

	// TODO
	// load in pair evaluations from 2000 and not im BAMS?
	// is it using the resolution evaluations?

	protected static Log log = LogFactory.getLog(CreateRDFData.class);
	RDFResolver resolver;
	WhiteTextRDFModel resolutionModel;

	public CreateRDFData() throws Exception {
		MakeLexiconRDFModel lexiconModel = new MakeLexiconRDFModel();
		lexiconModel.addNIFSTDNodes();

		boolean reason = true;
		resolutionModel = new WhiteTextRDFModel(lexiconModel.getModel(), reason);

		resolutionModel.loadManualMatches();
		boolean createMentions = true;
		resolutionModel.loadManualEvaluations(createMentions);

		resolutionModel.getStats();

		resolver = new BagOfStemsRDFMatcher(resolutionModel.getTerms());
		resolver.addMentionEditor(new DirectionSplittingMentionEditor());
		resolver.addMentionEditor(new HemisphereStripMentionEditor());
		resolver.addMentionEditor(new BracketRemoverMentionEditor());
		resolver.addMentionEditor(new OfTheRemoverMentionEditor());
		resolver.addMentionEditor(new CytoPrefixMentionEditor());
		resolver.addMentionEditor(new RegionSuffixRemover());
		resolver.addMentionEditor(new DirectionRemoverMentionEditor());
		resolver.addMentionEditor(new NucleusOfTheRemoverMentionEditor());
		resolver.addMentionEditor(new DirectionRemoverMentionEditor());
	}

	// cleanup
	// NIFSTD that are not linked
	// mention terms that are not linked

	public void run(AirolaXMLReader reader, List<String> pairs, SLOutputReader SLReader) throws Exception {
		Map<String, Set<String>> pairToEntities = reader.getPairIDToEntities();

		// only add the entities we use
		Set<String> usedEntities = new HashSet<String>();
		for (String pair : pairs) {
			usedEntities.addAll(pairToEntities.get(pair));
		}

		int count = 0;
		for (String entityID : usedEntities) {
			count++;
			if (count % 1000 == 0)
				log.info(count + " of " + usedEntities.size());

			String entityText = reader.getEntityText(entityID);

			Resource mentionNode = resolutionModel.makeEntityNode(entityText, entityID);
		}

		resolutionModel.runResolver(resolver, resolutionModel.getMentions());
		resolutionModel.getStats();

		// pairs
		resolutionModel.addPairs(pairs, reader.getPairIDToEntities(), SLReader.getScores());

		// sentences
		resolutionModel.addSentences(reader);

		resolutionModel.addSpeciesToModel(pairs, reader);

		resolutionModel.getStats();

		resolutionModel.writeOut();

	}

	/**
	 * Get all relation annotation tags with count higher than one
	 * 
	 * @param p2g
	 * @throws Exception
	 */
	public void createPredicateList(GateInterface p2g) throws Exception {
		String filename = Config.config.getString("whitetextweb.predicateList");

		Set<String> annotators = new HashSet<String>();
		annotators.add("Suzanne");
		annotators.add("Lydia");
		Set<String> filtered = new HashSet<String>();

		for (String annotator : annotators) {
			CountingMap<String> connectionPreds = p2g.getAnnotationCountedMap(p2g.getDocuments(), annotator,
					"ConnectionPredicate", true);

			for (String connectionPred : connectionPreds.keySet()) {
				if (connectionPreds.get(connectionPred) > 1) {
					filtered.add(connectionPred);
				}
			}
		}
		FileTools.stringsToFile(filtered, filename);
	}

	public static void addSmall() throws Exception {
		GateInterface p2g = new GateInterface();
		p2g.setUnSeenCorpNull();
		p2g.setNamedCorpNull("PubMedUnseenJNChem");
		p2g.setNamedCorpNull("PubMedUnseenJCN");
		p2g.setNamedCorpNull("PubMedUnseenMScan1");

		CreateRDFData instance = new CreateRDFData();

		// create list of predicates found in abstracts
		instance.createPredicateList(p2g);

		String annotationSet = "Suzanne";
		String name = "WhiteTextNegFixFull";
		instance.addCVCorpus(annotationSet, p2g, name);

		log.info("DONE");
	}

	/**
	 * @param args
	 */
	public void addCVCorpus(String annotationSet, GateInterface p2g, String name) throws Exception {

		String airolaXML = Config.config.getString("whitetextweb.airolaXML") + name + ".xml";
		System.setProperty(Gate.BUILTIN_CREOLE_DIR_PROPERTY_NAME, "file:" + System.getProperty("user.dir") + "/");

		String SLFolder = Config.config.getString("whitetextweb.SLResults") + "CV/" + name + "/predict/" + name;

		SLOutputReader SLReader = new SLOutputReader(new File(SLFolder));

		AirolaXMLReader XMLReader = new AirolaXMLReader(airolaXML, p2g, annotationSet);

		run(XMLReader, SLReader.getPositivePredictions(), SLReader);
	}

	public static void addFull() throws Exception {
		String trainingSet = "WhiteTextNegFixFull";

		// modify this for the XML file being used

		GateInterface p2g = new GateInterface();
		p2g.setUnSeenCorpNull();
		p2g.setNamedCorpNull("PubMedUnseenJNChem");
		// p2g.setNamedCorpNull("PubMedUnseenJCN");
		p2g.setNamedCorpNull("PubMedUnseenMScan1");

		CreateRDFData instance = new CreateRDFData();

		// add in the base data
		String annotationSet = "Suzanne";
		String name = "WhiteTextNegFixFull";
		instance.addCVCorpus(annotationSet, p2g, name);

		String SLName = "NegFixFullOnUnseen";
		String testSet = "WhiteTextUnseen";
		instance.addCCCorpus(testSet, trainingSet, p2g, SLName);

		// testSet = "WhiteTextUnseenMScan";
		// SLName = "NegFixFullOnWhiteTextUnseenMScan";
		// instance.addCCCorpus(testSet, trainingSet, p2g, SLName);
		//
		// testSet = "WhiteTextUnseenMScan2";
		// SLName = "NegFixFullOnWhiteTextUnseenMScan2";
		// instance.addCCCorpus(testSet, trainingSet, p2g, SLName);

		log.info("DONE");

	}

	public void addCCCorpus(String testSet, String trainingSet, GateInterface p2g, String SLName) throws Exception {
		String annotationSet = "Mallet";

		String airolaXML = Config.config.getString("whitetextweb.airolaXML") + testSet + ".xml";
		System.setProperty(Gate.BUILTIN_CREOLE_DIR_PROPERTY_NAME, "file:" + System.getProperty("user.dir") + "/");

		String SLFolder = Config.config.getString("whitetextweb.SLResults") + "CC/" + SLName + "/";

		SLOutputReader SLReader = new SLOutputReader(trainingSet, testSet, SLFolder);

		log.info(SLName);
		log.info("  Pos predictions:" + SLReader.getPositivePredictions().size());
		log.info("  Neg predictions:" + SLReader.getNegativePredictions().size());

		AirolaXMLReader XMLReader = new AirolaXMLReader(airolaXML, p2g, annotationSet);
		run(XMLReader, SLReader.getPositivePredictions(), SLReader);
	}

	public static void main(String[] args) throws Exception {
		addFull();
	}
}
