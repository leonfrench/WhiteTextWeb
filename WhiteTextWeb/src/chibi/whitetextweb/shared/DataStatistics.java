package chibi.whitetextweb.shared;

import java.io.Serializable;

import com.google.gwt.user.client.rpc.IsSerializable;

public class DataStatistics implements Serializable , IsSerializable{
	private static final long serialVersionUID = 6726736614085634823L;
	public DataStatistics() {
		super();
	}
	public int regionInstanceCount;
	public int sentenceCount;
	public int pairCount;
}
