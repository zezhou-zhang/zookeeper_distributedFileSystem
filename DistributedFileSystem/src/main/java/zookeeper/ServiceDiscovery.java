package zookeeper;

import org.apache.zookeeper.KeeperException;

public class ServiceDiscovery extends Thread{
	
	private final ServiceRegistry serviceRegistry;
	private final int port;
	private final String serverAddress;
	public ServiceDiscovery(ServiceRegistry serviceRegistry, String serverAddress, int port) {
		this.serviceRegistry = serviceRegistry;
		this.serverAddress = serverAddress;
		this.port = port;
	}
	
	public void registerService() {
		String currentServerAddress = String.format("%s:%d", serverAddress, port);
		try {
			serviceRegistry.registerToCluster(currentServerAddress);
			serviceRegistry.registerForUpdates();
		} catch (KeeperException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		Thread thread = new Thread(this);
		thread.setName("serviceDiscovery");
		thread.start();
	}
	
	public void run() {
		registerService();
	}
}
