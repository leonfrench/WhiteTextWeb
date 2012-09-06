package chibi.whitetextweb.server;

import java.io.FileInputStream;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public class Config {

	static String filename = "WhiteTextWeb.properties";

	// just grab the config and call get to get the parameter value
	public static Configuration config;

	static {
		try {
			config = new PropertiesConfiguration(filename);
		} catch (Exception e) {
			System.out.println("Could not load " + filename);
			System.exit(1);
		}
	}

	public static void main(String argsp[]) throws Exception {
		FileInputStream fis = new FileInputStream( "gate.properties" );
		
		Iterator i = config.getKeys();

		while (i.hasNext()) {
			String key = (String) i.next();
			System.out.println(key + ":" + config.getString(key));
		}
	}

}
