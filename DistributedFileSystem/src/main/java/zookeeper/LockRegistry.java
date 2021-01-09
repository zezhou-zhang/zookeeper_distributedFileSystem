package zookeeper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class LockRegistry implements Watcher {
	
	private static final String LOCK_REGISTRY = "/lock_registry";
	private static final String READ_FLAG = "_LockforRead";
	private static final String WRITE_FLAG = "_LockforWrite";
	private final ZooKeeper zooKeeper;
	private String data;
	public LockRegistry(ZooKeeper zooKeeper) {
		 this.zooKeeper = zooKeeper;
		 // Every node calls the FilesRegistry will see if files znode exists
		 createLockRegistryZnode();
	 }
	
	public void createLockRegistryZnode() {
		//at this time znode does not exist
		 try {
			if (zooKeeper.exists(LOCK_REGISTRY, false) == null) {
					zooKeeper.create(LOCK_REGISTRY, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			 }
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	 public boolean registerToReadLock(String serverName, String fileName) {
		 boolean lockAquired = false;
		 try {
			if (zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, false)==null) {
				if (zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + READ_FLAG, false)==null) {
					zooKeeper.create(LOCK_REGISTRY + "/" + fileName + READ_FLAG, serverName.getBytes(), 
						 ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
				}
				if (!FileHashMap.getLockedFileList().contains(fileName + READ_FLAG)) {
					FileHashMap.addLockedFileList(fileName + READ_FLAG);
				}
				lockAquired = true;
				System.out.println(fileName + " has successfully acuqired read lock.");
			}else {
				lockAquired = false;
				System.out.println(fileName + " faild to aquire read lock.");
				registerForWriteLockUpdates(fileName);
			}
			
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		return lockAquired;
	 }
	 
	 public void unregisterToReadLock(String fileName) throws InterruptedException, KeeperException {
		 if (zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + READ_FLAG, false)!=null) {
			 zooKeeper.delete(LOCK_REGISTRY + "/" + fileName + READ_FLAG, -1);
			 if (FileHashMap.getLockedFileList().contains(fileName + READ_FLAG)) {
				 FileHashMap.removeFromLockedFileList(fileName + READ_FLAG);
			 }
			 System.out.println("Successfully unregistered the read lock");
		 }
	 }
	 
	 public void unregisterToWriteLock(String fileName) throws InterruptedException, KeeperException {
		 if (zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, false)!=null) {
			 zooKeeper.delete(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, -1);
			 if (FileHashMap.getLockedFileList().contains(fileName + WRITE_FLAG)) {
				 FileHashMap.removeFromLockedFileList(fileName + WRITE_FLAG);
			 }
			 System.out.println("Successfully unregistered the write lock");
		 }
	 }
	 
	 public boolean registerToWriteLock(String serverName, String fileName, String data) throws InterruptedException {
		 this.data = data;
		 boolean lockAcquired = false;
		 while(!lockAcquired) {
			 try {
					if (zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, false)==null &&
							zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + READ_FLAG, false)==null) {
						zooKeeper.create(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, serverName.getBytes(), 
								 ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
						if (!FileHashMap.getLockedFileList().contains(fileName + WRITE_FLAG)) {
							FileHashMap.addLockedFileList(fileName + WRITE_FLAG);
						}
						lockAcquired = true;
						System.out.println(fileName + " has successfully acuqired write lock.");
					}else {
						lockAcquired = false;
						System.out.println(fileName + " faild to aquire write lock.");
						registerForWriteLockUpdates(fileName);
					}
					
				} catch (KeeperException | InterruptedException e) {
					System.out.println(" Acuqiring write lock may have failed. Retry to acquire write lock...");
					Thread.sleep(500);
				}
		 }
		 return lockAcquired;
		 /* Shared Lock Solution (future implementation)
		 this.data = data;
		 boolean lockAcquired = false;
		 try {
			String znodeFullPath = zooKeeper.create(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, serverName.getBytes(), 
					 ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
			String myZnodeName = znodeFullPath.replace(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, "");
			List<String> childrenList = zooKeeper.getChildren(LOCK_REGISTRY, false);
			for (int i = 0; i< childrenList.size();i++) {
				String child = childrenList.get(i).replace(fileName + WRITE_FLAG, "");
				childrenList.set(i, child);
			}
			Collections.sort(childrenList);
			String smallestChild = childrenList.get(0);
			if (myZnodeName == smallestChild) {
				lockAcquired = true;
			}else {
				Stat stat = zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG+smallestChild, this);
				if (stat == null) {
					registerToWriteLock(serverName, fileName,  data);
				}else {
					lockAcquired = false;
				}
			}
		} catch (KeeperException | InterruptedException e) {
			System.out.println(" Acuqiring write lock may have failed. Retry to acquire write lock...");
			Thread.sleep(500);
		}
		 
		return lockAcquired;
		*/
	 }
	 
	 public synchronized void registerForWriteLockUpdates(String fileName) throws KeeperException, InterruptedException {
		 if (zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, false)!=null) {
			 zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + WRITE_FLAG, this);
		 }
		 else if (zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + READ_FLAG, false)!=null) {
			 zooKeeper.exists(LOCK_REGISTRY + "/" + fileName + READ_FLAG, this);
		 }
					
		 
	 }

	@Override
	public void process(WatchedEvent event) {
		// TODO Auto-generated method stub
		switch (event.getType()) {
			case NodeDeleted:
				System.out.println("Detect Node Deleted.");
				List<String> lockFileZnodes;
				try {
					lockFileZnodes = zooKeeper.getChildren(LOCK_REGISTRY, false);
					List<String> lockedFileList = FileHashMap.getLockedFileList();
					for (String lockedFile : lockedFileList) {
						if (!lockFileZnodes.contains(lockedFile)) {
							lockedFileList.remove(lockedFile);
							if (lockedFile.contains(WRITE_FLAG)){
								System.out.println("Trying to re-write the file.");
								FileOperation.writeFile(lockedFile.replace(WRITE_FLAG,""),data);
								// fileCallBack.onWrite()
							}
							if (lockedFile.contains(READ_FLAG)){
								System.out.println("Trying to re-read the file.");
								FileOperation.readFile(lockedFile.replace(READ_FLAG, ""));
								// fileCallBack.onRead()
							}
						}
					}
				} catch (KeeperException | InterruptedException | IOException e) {
					e.printStackTrace();
				}
				break;

		}
	}
	
}
