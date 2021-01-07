package zookeeper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class FileHashMap {
	private static ConcurrentHashMap<String, List<String>> fileHashMap; 
	private static List<String> lockedFileList;
	
	public static List<String> getLockedFileList() {
		return lockedFileList;
	}
	
	public static void setLockedFileList() {
		FileHashMap.lockedFileList = new ArrayList<String>();
	}
	
	public static void addLockedFileList(String fileName) {
		FileHashMap.lockedFileList.add(fileName);
	}
	
	public static void removeFromLockedFileList(String fileName) {
		FileHashMap.lockedFileList.remove(fileName);
	}
	
	
	public static void putFileHashMap(String fileName, String serverName) {
		if(fileHashMap.get(fileName)==null){
			fileHashMap.put(fileName, new ArrayList<String>());
		} 
		if (!fileHashMap.get(fileName).contains(serverName)){
			fileHashMap.get(fileName).add(serverName);
		}
	}
	public static void setFileHashMap(){
		fileHashMap = new ConcurrentHashMap<String, List<String>>();
	}
	
	public static ConcurrentHashMap<String, List<String>> getFileHashMap(){
		return fileHashMap;
	}
	
}
