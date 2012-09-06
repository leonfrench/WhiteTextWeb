package chibi.whitetextweb.data;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import ubic.BAMSandAllen.Vocabulary;
import ubic.basecode.dataStructure.matrix.DenseDoubleMatrix;
import ubic.pubmedgate.ConnectionsDocument;
import ubic.pubmedgate.interactions.AirolaXMLReader;
import ubic.pubmedgate.organism.SpeciesLoader;
import ubic.pubmedgate.resolve.EvaluationRDFModel;
import chibi.whitetextweb.server.Config;
import chibi.whitetextweb.shared.DataGridRow;
import chibi.whitetextweb.shared.DataStatistics;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class WhiteTextRDFModel extends EvaluationRDFModel implements Serializable {
	private static final long serialVersionUID = -6288716658302455672L;

	protected static Log log = LogFactory.getLog(WhiteTextRDFModel.class);

	public final static String baseXMLURI = "http://www.purl.org/airolaXML/#";

	public final Resource entityType = model.createResource(baseXMLURI + "entityType");
	public final Resource pairType = model.createResource(baseXMLURI + "pairType");
	public final Resource sentenceType = model.createResource(baseXMLURI + "sentenceType");

	public final Property relation_partner = model.createProperty(baseXMLURI + "relation_partner");
	public final Property in_sentence = model.createProperty(baseXMLURI + "in_sentence");
	public final Property score = model.createProperty(baseXMLURI + "score");

	Map<String, Resource> labelToNIFURI;
	Set<Resource> usedNIFConcepts;
	NIFSTDTreeModel treeModel;
	Map<Resource, Set<Resource>> partMap;

	public WhiteTextRDFModel() throws Exception {
		super(true);
		Model modelFresh = ModelFactory.createDefaultModel();
		modelFresh.read(new FileInputStream(Config.config.getString("whitetextweb.RDFModel")), null);
		labelToNIFURI = new HashMap<String, Resource>();
		// super.modelLoad(modelFresh, true);
		this.model = modelFresh;

		reason();
		// getStats();

		Set<Resource> concepts = getNIFSTDConcepts();
		usedNIFConcepts = new HashSet<Resource>();

		// this has some blank terms still because the evaluation created some
		// mentions that are matched
		outer: for (Resource concept : concepts) {
			Set<Resource> terms = getTermsFromConcepts(concept);
			for (Resource term : terms) {
				if (model.contains(null, Vocabulary.match, term)) {
					usedNIFConcepts.add(concept);
					String label = JenaUtil.getLabel(concept);
					if (labelToNIFURI.containsKey(label)) {
						log.warn("Duplicate in NIF label hash:" + label);
					}
					labelToNIFURI.put(label, concept);
					continue outer;
				}
			}
		}

		treeModel = new NIFSTDTreeModel();

		// add labels
		model.add(treeModel.getLabels());

		// add regions that are not matched directly but subclasses are
		Set<Resource> newRegions = treeModel.getAddedRegions(usedNIFConcepts);
		log.info("Added NIF regions:");
		for (Resource newRegion : newRegions) {
			log.info(" " + JenaUtil.getLabel(newRegion));
			labelToNIFURI.put(JenaUtil.getLabel(newRegion), newRegion);
		}
		usedNIFConcepts.addAll(newRegions);

		partMap = treeModel.getPartMap(usedNIFConcepts);
	}

	public WhiteTextRDFModel(Model model, boolean reason) throws Exception {
		super(model, reason);
	}

	public void writeOut() throws Exception {
		model.write(new FileOutputStream(Config.config.getString("whitetextweb.RDFModel")));
	}

	public Resource makeEntityNode(String entityText, String entityID) {
		Resource mentionNode = makeMentionNode(entityText);

		Resource mainConcept = model.createResource(baseXMLURI + entityID);
		mainConcept.addProperty(Vocabulary.has_label_term, mentionNode);
		mainConcept.addProperty(RDFS.label, entityID);
		mainConcept.addProperty(RDF.type, entityType);

		return mentionNode;
	}

	public void addSpeciesToModel(Collection<String> pairs, AirolaXMLReader reader) throws Exception {
		SpeciesLoader filterLoader = new SpeciesLoader();
		for (String pair : pairs) {
			ConnectionsDocument doc = reader.getDocumentFromPairID(pair);
			filterLoader.addToModel(doc, model); // null
		}
	}

	public void addSentences(AirolaXMLReader reader) {
		Map<String, String> pairIDToSentenceElementID = reader.getPairIDToSentenceElementID();

		for (String pairID : pairIDToSentenceElementID.keySet()) {
			Resource pairConcept = model.createResource(baseXMLURI + pairID);

			String sentenceID = pairIDToSentenceElementID.get(pairID);

			Resource mainConcept = model.createResource(baseXMLURI + sentenceID);
			String sentenceText = reader.getSentenceText(sentenceID);
			mainConcept.addProperty(RDFS.label, sentenceText);
			mainConcept.addProperty(RDF.type, sentenceType);

			// add PMID
			String pmid = reader.getPairIDToPMID().get(pairID);
			Resource docResource = model.createResource(Vocabulary.getpubmedURIPrefix() + pmid);
			mainConcept.addProperty(Vocabulary.in_PMID, docResource);

			// link pair to sentence
			pairConcept.addProperty(in_sentence, mainConcept);
		}

	}

	public void getStats() throws Exception {
		super.getStats();
		log.info("Airola XML:");
		log.info("  entities:" + model.listStatements(null, RDF.type, entityType).toSet().size());
		log.info("  pairs:" + model.listStatements(null, RDF.type, pairType).toSet().size());
		log.info("  sentences:" + model.listStatements(null, RDF.type, sentenceType).toSet().size());
	}

	public void addPairs(List<String> pairs, Map<String, Set<String>> pairIDToEntities, Map<String, Double> scores) {
		// only use the pairs we get as input!
		// Set<String> pairs = pairIDToEntities.keySet();
		for (String pairID : pairs) {

			Resource mainConcept = model.createResource(baseXMLURI + pairID);
			mainConcept.addProperty(RDFS.label, pairID);
			mainConcept.addProperty(RDF.type, pairType);
			mainConcept.addLiteral(score, scores.get(pairID));
			Set<String> entities = pairIDToEntities.get(pairID);

			for (String entityID : entities) {
				Resource entityConcept = model.createResource(baseXMLURI + entityID);
				mainConcept.addProperty(relation_partner, entityConcept);
			}
		}
	}

	class GetRowThread implements Callable<List<DataGridRow>> {
		Resource NIFTerm;

		public GetRowThread(Resource NIFTerm) {
			this.NIFTerm = NIFTerm;
		}

		public List<DataGridRow> call() {
			return getSpecificRowsForRegion(NIFTerm);
		}
	}

	public List<DataGridRow> getRowsForRegion(String region) {
		StopWatch s = new StopWatch();
		s.start();

		Resource NIFConcept = getNIFfromLabel(region);
		Set<Resource> queryRegions = new HashSet<Resource>();
		queryRegions.add(NIFConcept);
		Set<Resource> subRegions = getSubRegions(NIFConcept);
		if (subRegions != null) {
			queryRegions.addAll(subRegions);
		}

		int threads = Config.config.getInt("whitetextweb.threadsPerUser");
		ExecutorService threadPool = Executors.newFixedThreadPool(threads);

		// create and submit threads
		Set<Future<List<DataGridRow>>> calledResults = new HashSet<Future<List<DataGridRow>>>();
		for (Resource NIFregion : queryRegions) {
			GetRowThread thread = new GetRowThread(NIFregion);
			Future<List<DataGridRow>> call = threadPool.submit(thread);
			calledResults.add(call);
		}

		threadPool.shutdown();
		try {
			threadPool.awaitTermination(20, TimeUnit.MINUTES);
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("Termination of RDF query threads complete (" + threads + " threads)");

		// gather up results
		List<DataGridRow> result = new LinkedList<DataGridRow>();
		for (Future<List<DataGridRow>> callResult : calledResults) {
			List<DataGridRow> dataResult;
			try {
				dataResult = callResult.get();
				if (dataResult != null) {
					result.addAll(dataResult);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// TODO check subterms for dupes!
		int beforeSize = result.size();

		LinkedList<DataGridRow> sorted = new LinkedList<DataGridRow>();
		for (DataGridRow row : result) {
			boolean dupe = false;
			for (DataGridRow compare : sorted) {
				if (row.equals(compare)) {
					dupe = true;
				}
			}
			if (!dupe && !row.speciesLabel.equals("rats"))
				sorted.add(row);
		}

		Collections.sort(sorted, new Comparator<DataGridRow>() {
			@Override
			public int compare(DataGridRow o1, DataGridRow o2) {
				// TODO Auto-generated method stub
				return (new Double(o2.score)).compareTo(o1.score);
			}
		});

		log.info("Get all rows on server took:" + s.toString());

		log.info("Duplicate rows:" + (beforeSize - sorted.size()) + " of " + beforeSize);

		return sorted;
	}

	public List<DataGridRow> getSpecificRowsForRegion(String region) {
		Resource NIFConcept = getNIFfromLabel(region);
		return getSpecificRowsForRegion(NIFConcept);
	}

	public List<DataGridRow> getSpecificRowsForRegion(Resource NIFConcept) {
		StopWatch s = new StopWatch();
		s.start();

		List<DataGridRow> result = new LinkedList<DataGridRow>();
		if (NIFConcept == null)
			return result;

		// query is also in /war/sparql/
		String queryString = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "PREFIX vocab: <http://www.chibi.ubc.ca/Gemma/ws/xml/neuroanatomyLinks.owl#>\n"
				+ "PREFIX airola:<http://www.purl.org/airolaXML/#>\n"
				+ "\n"
				+ "SELECT DISTINCT ?pair ?mention ?term ?sentence ?mentionText ?mentionText2 ?sentenceText ?pmid ?score ?speciesLabel      \n"
				+ "WHERE {                                                                                          \n"
				+ "	<"
				+ NIFConcept.getURI()
				+ "> ?zz ?term .                                      \n"
				+ "                          ?term rdf:type vocab:neuroterm .                                             \n"
				+ "                          ?mention vocab:match ?term .                                             \n"
				+ "	?entity vocab:has_label_term ?mention   .                                             \n"
				+ "	?mention rdfs:label ?mentionText .                                             \n"
				+ "	?pair airola:relation_partner ?entity                                              .\n"
				+ "	?pair airola:in_sentence ?sentence .                                                \n"
				+ " ?pair airola:score ?score 		.												\n"
				+ "	?sentence rdfs:label ?sentenceText .                                             \n"
				+ " ?sentence vocab:in_PMID ?pmid                                 .                 \n"
				+ "	OPTIONAL {                                                                                                                                        \n"
				+ " 	?pmid vocab:mentions_species ?species											.\n"
				+ " 	?species rdfs:label ?speciesLabel										.\n"
				+ "	}                                              \n"
				+ "	OPTIONAL {                                                                                                                                        \n"
				+ "		?pair airola:relation_partner ?entity2 .                                             \n"
				+ "		?entity2 vocab:has_label_term ?mention2   .                                             \n"
				+ "		?mention2 rdfs:label ?mentionText2 .                                             \n"
				+ "		FILTER (?entity != ?entity2)\n"
				+ "	}                                              \n"
				+ "}  ORDER BY DESC(?score)                                                                                         ";
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet RDFresults = qe.execSelect();

		while (RDFresults.hasNext()) {
			QuerySolution solution = RDFresults.next();
			String sentenceText = solution.getLiteral("sentenceText").getString();
			String mentionText1 = solution.getLiteral("mentionText").getString();
			String mentionText2 = solution.getLiteral("mentionText2").getString();
			String pairURI = solution.getResource("pair").getURI();
			String speciesLabel = "";
			Literal species = solution.getLiteral("speciesLabel");
			if (species != null) {
				speciesLabel = species.getString();
			} else {
				speciesLabel = "?";
			}
			String pmid = solution.getResource("pmid").getURI();
			pmid = pmid.substring(pmid.lastIndexOf(":") + 1);
			Double score = solution.getLiteral("score").getDouble();
			result.add(new DataGridRow(pairURI, sentenceText, mentionText1, mentionText2, speciesLabel, pmid, score));
		}

		// Important – free up resources used running the query
		qe.close();

		log.info(" Get specific rows on server took:" + s.toString() + " Size:" + result.size());
		return result;
	}

	public Resource getNIFfromLabel(String region) {
		return labelToNIFURI.get(region);
	}

	// this version is faster than the sparql method.
	@Deprecated
	public List<DataGridRow> getRowsForRegionOld(String region) {
		StopWatch s = new StopWatch();
		s.start();
		List<DataGridRow> result = new LinkedList<DataGridRow>();
		Resource NIFConcept = getNIFfromLabel(region);
		if (NIFConcept == null)
			return result;
		Set<Resource> terms = getTermsFromConcepts(NIFConcept);

		Set<Resource> mentions = getMentionsFromTerms(terms);

		// should be sparql query
		// mention <- entity <- pair -> sentence
		// ?entity vocab:has_label_term ?mention .

		Set<Resource> entities = new HashSet<Resource>();
		for (Resource r : mentions) {
			entities.addAll(JenaUtil.getSubjects(model.listStatements(null, Vocabulary.has_label_term, r)));
		}

		Set<Resource> pairs = new HashSet<Resource>();
		Set<String> entityStrings = new HashSet<String>();
		// Map<Resource, >
		for (Resource r : entities) {
			pairs.addAll(JenaUtil.getSubjects(model.listStatements(null, relation_partner, r)));
		}

		Set<Resource> sentences = new HashSet<Resource>();
		for (Resource r : pairs) {
			sentences.addAll(JenaUtil.getObjects(r.listProperties(in_sentence)));
		}

		for (Resource sentence : sentences) {
			String text = JenaUtil.getLabel(sentence);
			// log.info(text);
			result.add(new DataGridRow(text));
		}
		// get entities
		log.info("Get rows on server took:" + s.toString());
		return result;
	}

	public Set<Resource> getUsedNIFConcepts() {
		return usedNIFConcepts;
	}

	public DataStatistics getDataStatistics() {
		DataStatistics s = new DataStatistics();
		s.regionInstanceCount = model.listStatements(null, RDF.type, entityType).toSet().size();
		s.pairCount = model.listStatements(null, RDF.type, pairType).toSet().size();
		s.sentenceCount = model.listStatements(null, RDF.type, sentenceType).toSet().size();
		return s;
	}

	public Set<Resource> getSubRegions(Resource NIFTerm) {
		return partMap.get(NIFTerm);
	}

	public static void main(String[] args) throws Exception {
		log.info(String.format("%.2f", -2.2223d));
		log.info(System.getProperties().get("java.version"));
		// System.exit(1);

		StopWatch s = new StopWatch();
		s.start();
		WhiteTextRDFModel model = new WhiteTextRDFModel();
		model.getStats();
		log.info(s.toString());
		// log.info(s.toString());
		log.info("Used NIF terms:" + model.getUsedNIFConcepts().size());
		log.info(s.toString());

		String test = "Substantia nigra pars compacta";
		StopWatch ss = new StopWatch();
		ss.start();
		log.info("Rows:" + model.getRowsForRegion(test).size());
		log.info(ss.toString());

		log.info(model.getSubRegions(model.getNIFfromLabel("Hippocampus")).size());
		log.info(model.getSubRegions(model.getNIFfromLabel("Epithalamus")).size());
		// model.test();
	}
}
