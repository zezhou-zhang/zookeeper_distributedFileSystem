package zookeeper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ServiceRegistry implements Watcher {
	private static final String REGISTRY_ZNODE = "/service_registry";
	private final ZooKeeper zooKeeper;
	private String currentZnode = null;
	private List<String> allServerAddresses = null;
	
	public ServiceRegistry(ZooKeeper zooKeeper) {
		this.zooKeeper = zooKeeper;
		createServiceRegistryZnode();
	}
	
	public void registerToCluster(String metadata) throws KeeperException, InterruptedException {
		this.currentZnode = zooKeeper.create(REGISTRY_ZNODE + "/n_", metadata.getBytes(),
				ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		System.out.println(metadata + " registered to service registry.");
	}
	
	public void registerForUpdates() throws KeeperException, InterruptedException{
		updateAddresses();
	}
	
	public synchronized List<String> getAllServerAddresses() throws KeeperException, InterruptedException{
		if (allServerAddresses == null) {
			updateAddresses();
		}
		return allServerAddresses;
	}
	
	private void createServiceRegistryZnode() {
		try {
			if (zooKeeper.exists(REGISTRY_ZNODE, false) == null) {
					zooKeeper.create(REGISTRY_ZNODE, new byte[] {}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
			 }
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private synchronized void updateAddresses() throws KeeperException, InterruptedException {
		List<String> workerZnodes = zooKeeper.getChildren(REGISTRY_ZNODE, this);
		List<String> addresses = new ArrayList<>(workerZnodes.size());
		
		for (String workerZnode : workerZnodes) {
			String workerZnodeFullPath = REGISTRY_ZNODE + "/" + workerZnode;
			Stat stat = zooKeeper.exists(workerZnodeFullPath, false);
			if(stat == null) {
				continue;
			}
			byte [] addressBytes = zooKeeper.getData(workerZnodeFullPath, false, stat);
			String address = new String(addressBytes);
			addresses.add(address);
		}
		// read only access
		this.allServerAddresses = Collections.unmodifiableList(addresses);
		System.out.println("The cluster addresses are: " + this.allServerAddresses);
	}

	@Override
	public void process(WatchedEvent event) {
		// If detect change, update cluster list and register for future update
		try {
			updateAddresses();
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
}
