package chibi.whitetextweb.client;

import java.util.List;
import java.util.Set;

import chibi.whitetextweb.shared.DataGridRow;
import chibi.whitetextweb.shared.DataStatistics;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface GreetingServiceAsync {
	void greetServer(String name, AsyncCallback<List<DataGridRow>> callback);

	void getOracle(AsyncCallback<Set<String>> callback);

	void getDataStatistics(AsyncCallback<DataStatistics> callback);

	void getConnectionPredRegex(AsyncCallback<String> callback);

	void writeFlaggedColumn(String data, AsyncCallback<Void> callback);

}
