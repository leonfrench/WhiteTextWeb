package chibi.whitetextweb.data;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.mortbay.log.Log;

import ubic.basecode.util.FileTools;
import chibi.whitetextweb.server.Config;

public class ConnectionPredicateRegexMaker {

	public static String getRegex() throws Exception {
		List<String> preds = FileTools.getLines(Config.config.getString("whitetextweb.predicateList"));
		Collections.sort(preds, new Comparator<String>() {
			public int compare(String a, String b) {
				return b.length() - a.length();
			}
		});

		StringBuffer stringBuf = new StringBuffer();
		stringBuf.append("(");
		for (String pred : preds) {
			stringBuf.append("\\b");
			stringBuf.append(pred);
			stringBuf.append("\\b");
			stringBuf.append("|");
		}
		stringBuf.deleteCharAt(stringBuf.length() - 1);
		stringBuf.append(")");
		return stringBuf.toString();
	}

	public static void main(String[] args) throws Exception {

		System.out.println(getRegex());
		String test = "Many of the medullary neurons with projections to the lateral tegmental field and the lumbar cord were located dorsal and lateral to those neurons with projections to the intermediolateral cell column(IML) .";
		test = test.replaceAll(getRegex(), "<span STYLE=\"font-weight: bold; color: #007A00\">($1)</span>");

		String x = "alskdjf jim alskjf";
		x= x.replaceAll("(jim)", "<span STYLE=\\\"font-weight: bold; color: #007A00\">$1<b>");
		System.out.println();
		System.out.println(x);

		System.out.println();
		System.out.println(test);

	}

}
