package chibi.whitetextweb.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.BAMSandAllen.JenaUtil;
import chibi.whitetextweb.server.Config;

import com.hp.hpl.jena.ontology.AllValuesFromRestriction;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.Restriction;
import com.hp.hpl.jena.ontology.SomeValuesFromRestriction;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;

public class NIFSTDTreeModel {
	protected static Log log = LogFactory.getLog(NIFSTDTreeModel.class);
	Model model;

	public NIFSTDTreeModel() throws Exception {
		model = ModelFactory.createDefaultModel();
		model.read(new FileInputStream(Config.config.getString("whitetextweb.NIFSTDTreeModel")), null);
	}

	public Map<Resource, Set<Resource>> getPartMap(Set<Resource> usedNIFConcepts) {
		Map<Resource, Set<Resource>> regionToChildren = new HashMap<Resource, Set<Resource>>();
		for (Resource usedNIFConcept : usedNIFConcepts) {
			Set<Resource> subClasses = getSubClasses(usedNIFConcept);
			// we only care about the ones we use
			subClasses.retainAll(usedNIFConcepts);
			regionToChildren.put(usedNIFConcept, subClasses);
		}
		return regionToChildren;
	}

	public Set<Resource> getSubClasses(String uri) {
		Set<Resource> visited = new HashSet<Resource>();
		Resource r = model.createResource(uri);
		// log.info(JenaUtil.getLabel(r));
		return getSubClasses(r, 0, visited);
	}

	public Set<Resource> getSubClasses(Resource NIFTerm) {
		Set<Resource> visited = new HashSet<Resource>();
		return getSubClasses(NIFTerm, 0, visited);
	}

	public Set<Resource> getAddedRegions(Set<Resource> usedNIFRegions) {
		// iterate all in tree
		// if subregions contains a used NIF region then keep it

		Set<Resource> result = new HashSet<Resource>();
		Set<Resource> regions = JenaUtil.getSubjects(model.listStatements(null, RDFS.label, (String) null));
		for (Resource region : regions) {
			if (usedNIFRegions.contains(region))
				continue;
			Set<Resource> subClasses = getSubClasses(region);
			subClasses.retainAll(usedNIFRegions);
			if (!subClasses.isEmpty())
				result.add(region);
		}
		return result;
	}

	public StmtIterator getLabels() {
		return model.listStatements(null, RDFS.label, (String) null);
	}

	public Set<Resource> getSubClasses(Resource NIFTerm, int depth, Set<Resource> visited) {
		// StmtIterator iterator = NIFTerm.listProperties(RDFS.subClassOf);
		StmtIterator iterator = model.listStatements(null, RDFS.subClassOf, NIFTerm);
		Set<Resource> subClasses = JenaUtil.getSubjects(iterator);
		subClasses.removeAll(visited);
		Set<Resource> result = new HashSet<Resource>();
		if (subClasses.isEmpty()) {
			return new HashSet<Resource>();
		} else {
			for (Resource subClass : subClasses) {
				// log.info(JenaUtil.getLabel(NIFTerm) +
				// " has sub region named " + JenaUtil.getLabel(subClass) + " "
				// + subClass.getURI() + " " + depth);
				result.add(subClass);
				visited.add(subClass);
				result.addAll(getSubClasses(subClass, depth + 1, visited));
			}
		}
		return result;
	}

	/**
	 * @param args
	 */

	public static void createTree() throws Exception {
		Model NIFTreeModel = ModelFactory.createDefaultModel();

		OntModel model = ModelFactory.createOntologyModel();
		String filename = "/home/leon/temp/NIF-GrossAnatomy.owl.1";
		FileReader rdfFile = new FileReader(new File(filename));
		model.getDocumentManager().setProcessImports(false);
		model.read(rdfFile, null);
		rdfFile.close();

		Set<Resource> classes = JenaUtil.getSubjects(model.listStatements(null, RDF.type, (RDFNode) null));

		int count = 0;
		for (Resource region : classes) {
			if (region.isAnon())
				continue;
			String label = JenaUtil.getLabel(region);
			log.info(label + " " + region.getURI() + " " + count++ + " of " + classes.size());
			// listSubClasses seems broken so listing based on properties
			// ExtendedIterator<OntClass> subClassIterator =
			// region.listSubClasses();

			Set<Resource> subclasses = JenaUtil.getObjects(model
					.listStatements(region, RDFS.subClassOf, (RDFNode) null));
			for (Resource subClassR : subclasses) {
				OntClass subclass;
				try {
					subclass = subClassR.as(OntClass.class);
				} catch (Exception e) {
					continue;
				}

				if (subclass.isRestriction()) {
					Restriction restriction = subclass.asRestriction();

					OntProperty onProperty = restriction.getOnProperty();
					if (onProperty.getURI().equals("http://www.obofoundry.org/ro/ro.owl#proper_part_of")) {
						if (restriction.isSomeValuesFromRestriction()) {
							SomeValuesFromRestriction some = restriction.asSomeValuesFromRestriction();
							Resource r = some.getSomeValuesFrom();
							log.info(" Some from:" + JenaUtil.getLabel(r) + " " + onProperty.getURI() + " "
									+ r.getURI());
							NIFTreeModel.add(region, RDFS.subClassOf, r);
							NIFTreeModel.add(region.getProperty(RDFS.label));
						}
						if (restriction.isAllValuesFromRestriction()) {
							AllValuesFromRestriction allValues = restriction.asAllValuesFromRestriction();
							Resource r = allValues.getAllValuesFrom();
							log.info(" All from:" + JenaUtil.getLabel(r) + " " + onProperty.getURI());
						}
					}
				}
			}
		}

		NIFTreeModel.write(new FileOutputStream(Config.config.getString("whitetextweb.NIFSTDTreeModel")), null);
	}

	public static void main(String[] args) throws Exception {

		// run createTree to make a new tree structure (shouldn't need that much updating)
		// createTree();

		NIFSTDTreeModel tree = new NIFSTDTreeModel();
		log.info(tree.getSubClasses(
				"http://ontology.neuinfo.org/NIF/BiomaterialEntities/NIF-GrossAnatomy.owl#birnlex_721").size());
	}
}
