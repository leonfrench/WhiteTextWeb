package chibi.whitetextweb.shared;

import java.io.Serializable;
import java.util.Set;

import org.mortbay.log.Log;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DataGridRow implements IsSerializable, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public String pairURI;
	public String sentence;
	public String entityOne;
	public String entityTwo;
	public String speciesLabel;
	public boolean flagged;
	public String pmid;
	public double score;

	public DataGridRow() {
		flagged = false;
		sentence = "";
	}

	public boolean isFlagged() {
		return flagged;
	}

	public void setChecked(boolean checked) {
		this.flagged = checked;
	}

	public DataGridRow(String pairURI, String sentence, String entityOne, String entityTwo, String speciesLabel, String pmid, double score) {
		super();
		this.pairURI = pairURI;
		this.sentence = sentence;
		this.entityOne = entityOne;
		this.entityTwo = entityTwo;
		this.speciesLabel = speciesLabel;
		this.flagged = flagged;
		this.pmid = pmid;
		this.score = score;
	}

	public DataGridRow(String sentence, String entityOne, String entityTwo, String speciesLabel, String pmid, double score) {
		super();
		this.sentence = sentence;
		this.entityOne = entityOne;
		this.entityTwo = entityTwo;
		this.speciesLabel = speciesLabel;
		this.pmid = pmid;
		this.score = score;
	}

	public DataGridRow(String sentence, String entityOne, String entityTwo, String pmid, double score) {
		super();
		this.sentence = sentence;
		this.entityOne = entityOne;
		this.entityTwo = entityTwo;
		this.pmid = pmid;
		this.score = score;
	}

	public DataGridRow(String sentence, String entityOne, String entityTwo, String pmid) {
		super();
		this.sentence = sentence;
		this.entityOne = entityOne;
		this.entityTwo = entityTwo;
		this.pmid = pmid;
	}

	public DataGridRow(String sentence, String entityOne, String entityTwo) {
		super();
		this.sentence = sentence;
		this.entityOne = entityOne;
		this.entityTwo = entityTwo;
	}

	// public Set<String> species;
	// public double score;

	// needed for serialization

	public DataGridRow(String sentence) {
		this.sentence = sentence;
	}

	public static String getTSVHeader() {
		String TAB = "\t";
		return "PMID" + TAB + "Sentence Text" + TAB + "Query Region" + TAB + "Connected region" + TAB + "Species co-mentioned" + TAB + "Score" + TAB + "PairURI" + TAB
				+ "Pubmed URL";
	}
	public String getAsTSVRow() {
		String TAB = "\t";
		return pmid + TAB + sentence + TAB + entityOne + TAB + entityTwo + TAB + speciesLabel + TAB + score + TAB + pairURI + TAB
				+ "http://www.ncbi.nlm.nih.gov/pubmed/" + pmid;
	}

	@Override
	public boolean equals(Object o) {
		DataGridRow compare = (DataGridRow) o;
		if (compare.pairURI.equals(pairURI) && compare.speciesLabel.equals(speciesLabel)) {
			return true;
		}
		return false;
	}
}
