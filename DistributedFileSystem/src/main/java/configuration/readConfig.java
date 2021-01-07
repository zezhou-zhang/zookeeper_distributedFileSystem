package configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class readConfig {
	
	private static final String propertiesFileName = "server-config.properties";
	
	
	private List<String> serverList = new ArrayList<String>();
	private InputStream inputStream;
	public List<String> getPropertiesValue() throws FileNotFoundException {
		Properties prop = new Properties();
		
		inputStream = getClass().getClassLoader().getResourceAsStream(propertiesFileName);
		if (inputStream != null) {
			try {
				prop.load(inputStream);
			} catch (IOException e) {
				System.out.println("Exception: " + e);
			}
		}else {
			throw new FileNotFoundException("property file '" + propertiesFileName + "' not found in the classpath");
		}
		
		// get the property value
		String server1 = prop.getProperty("Server1");
		serverList.add(server1);
		String server2 = prop.getProperty("Server2");
		serverList.add(server2);
		String server3 = prop.getProperty("Server3");
		serverList.add(server3);
		
		return serverList;
	}
}
