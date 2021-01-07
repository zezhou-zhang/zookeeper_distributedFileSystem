package zookeeper;

import org.apache.zookeeper.KeeperException;

public class FileWatcher extends Thread {
	private FilesRegistry filesRegistry;
	
	public FileWatcher(FilesRegistry filesRegistry) {
		this.filesRegistry = filesRegistry;
	}
	
	public void watchFilesChildren() throws KeeperException, InterruptedException {
		filesRegistry.registerForUpdates();
	}
	
	
	public void start() {
		Thread thread = new Thread(this);
		thread.setName("fileWatcher");
		thread.start();
	}
	
	public void run() {
		try {
			watchFilesChildren();
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
