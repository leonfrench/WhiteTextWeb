package chibi.whitetextweb.client;

import java.util.List;
import java.util.Set;

import chibi.whitetextweb.shared.DataGridRow;
import chibi.whitetextweb.shared.DataStatistics;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * The client side stub for the RPC service.
 */
@RemoteServiceRelativePath("greet")
public interface GreetingService extends RemoteService {
	List<DataGridRow> greetServer(String name) throws IllegalArgumentException;

	DataStatistics getDataStatistics() throws IllegalArgumentException;

	void writeFlaggedColumn(String data) throws IllegalArgumentException;
	
	String getConnectionPredRegex() throws IllegalArgumentException;

	Set<String> getOracle() throws IllegalArgumentException;
}
