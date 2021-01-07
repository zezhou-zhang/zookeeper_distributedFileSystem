package configuration;

import java.io.FileNotFoundException;
import java.util.List;

public class Servers extends readConfig{
	
	public List<String> getServerList() {
		List<String> serverList = null; 
		try {
			serverList = getPropertiesValue();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return serverList;
		
	}
	
	public String getServerAddressByName(String serverName) {
		List<String> serverList = getServerList();
		int index = Integer.valueOf(serverName.substring(serverName.indexOf("server")+6));
		return serverList.get(index);
	}
}
