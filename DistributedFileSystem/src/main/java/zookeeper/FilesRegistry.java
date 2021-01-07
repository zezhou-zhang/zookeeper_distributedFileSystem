package zookeeper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;



public class FilesRegistry implements Watcher {
	 private static final String FILE_ZNODE = "/files";
	 private final ZooKeeper zooKeeper;
	 private String fileZnode = null;
	 
	 public FilesRegistry(ZooKeeper zooKeeper) {
		 this.zooKeeper = zooKeeper;
		 FileHashMap.setLockedFileList();
	     FileHashMap.setFileHashMap();
		 // Every node calls the FilesRegistry will see if files znode exists
		 createFilesRegistryZnode();
	 }
	 public void registerToFiles(String serverName, String fileName) {
		 try {
			if (zooKeeper.exists(FILE_ZNODE + "/" + fileName, false)==null) {
				this.fileZnode = zooKeeper.create(FILE_ZNODE + "/" + fileName, serverName.getBytes(), 
						 ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			}
			System.out.println(fileName + " has registered to the files registry.");
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	 }
	 
	 public void unregisterToFiles(String fileName) throws InterruptedException, KeeperException {
		 if (zooKeeper.exists(FILE_ZNODE + "/" + fileName, false)!=null) {
			 zooKeeper.delete(FILE_ZNODE + "/" + fileName, -1);
		 }
	 }
	 
	 public void registerForUpdates() throws KeeperException, InterruptedException {
		 updateFileHashMap();
	 }
	 
	 public ConcurrentHashMap<String, List<String>> getAllFilesHashMap() throws KeeperException, InterruptedException {
		 // caller forgot to register for update so file hash map can be null
		 if (FileHashMap.getFileHashMap() == null) {
			 updateFileHashMap();
		 }
		 return FileHashMap.getFileHashMap();
	 }
	 
	 private void createFilesRegistryZnode() {
		 
		 //at this time znode does not exist
		 try {
			if (zooKeeper.exists(FILE_ZNODE, false) == null) {
					zooKeeper.create(FILE_ZNODE, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			 }
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	 }
	 
	// Get update from files registry about file creation and deletion
	private synchronized void updateFileHashMap() throws KeeperException, InterruptedException {
		List<String> fileZnodes = zooKeeper.getChildren(FILE_ZNODE, this);
		FileHashMap.setFileHashMap();
		for (String fileZnode: fileZnodes) {
			String fileZnodeFullPath = FILE_ZNODE + "/" + fileZnode;
			Stat stat = zooKeeper.exists(fileZnodeFullPath, false);
			// (Race Condition)
			// If znode deleted between get znode and exists method, continue to next znode
			if (stat == null) {
				continue;
			}
			byte [] serverNameBytes = zooKeeper.getData(fileZnodeFullPath, false, stat);
			String serverName = new String(serverNameBytes);
			FileHashMap.putFileHashMap(fileZnode, serverName);
		}
		System.out.println("The file hash map is: " + FileHashMap.getFileHashMap());
	
	}
	 
	@Override
	public void process(WatchedEvent event) {
		List<String> fileZnodes;
		switch (event.getType()) {
			case NodeChildrenChanged:
				System.out.println("Detect Children Nodes Changed in files registry.");
				System.out.println("Current file hash map: " + FileHashMap.getFileHashMap());
			try {
				fileZnodes = zooKeeper.getChildren(FILE_ZNODE, false);
				ConcurrentHashMap<String, List<String>> fileHashMap = getAllFilesHashMap();
				if (fileZnodes.size() < fileHashMap.size()) {
					System.out.println("Detect Node Deleted in files registry.");
					List<String> deletedFiles = new ArrayList<>(fileHashMap.keySet());
					deletedFiles.removeAll(fileZnodes);
					for (String deleteFile: deletedFiles) {
						System.out.println("Try to delete local file: " + deleteFile);
						FileOperation.deleteLocalFile(deleteFile);
					}
				}
				if (fileZnodes.size() > fileHashMap.size()) {
					System.out.println("Detect Node created in files registry.");
					List<String> createdFiles = new ArrayList<>(fileZnodes);
					createdFiles.removeAll(fileHashMap.keySet());
					for (String createdFile: createdFiles) {
						System.out.println("Try to create local file: " + createdFile);
						FileOperation.createLocalFile(createdFile);
					}
				}
			} catch (KeeperException | InterruptedException | IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
				
				break;
//			case NodeDeleted:

//			case NodeCreated:

		}
		
		// update file hash map and register for future update
		try {
			updateFileHashMap();
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	 
}
