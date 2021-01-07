package zookeeper;

import java.io.IOException;
import java.net.InetAddress;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

public class ZookeeperConnector implements Watcher {
	private static String ZOOKEEPER_ADDRESS;
    private static final int SESSION_TIMEOUT = 3000;
    private static int portNum;
    private static String serverAddress;
    
    private ZooKeeper zooKeeper;
    
    public ZookeeperConnector(String serverAddress, int portNum) {
    	this.serverAddress = serverAddress;
    	this.portNum = portNum;
    }
    
    public void connectToZookeeper() throws IOException {
    	ZOOKEEPER_ADDRESS = InetAddress.getLocalHost().getHostAddress() +":2181";
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
    }
    
    public void runZookeeper() throws IOException, InterruptedException {
    	connectToZookeeper();
    }
    
    public FilesRegistry runFilesRegistry() {
    	FilesRegistry filesRegistry = new FilesRegistry(zooKeeper);
    	return filesRegistry;
    }
    
    public ServiceRegistry runServiceRegistry() {
    	ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);
    	return serviceRegistry;
    }
    
    public TcpComm runTcpComm() {
    	TcpComm tcpComm = new TcpComm(zooKeeper, serverAddress, portNum);
    	return tcpComm;
    }
    
    public LockRegistry runLockRegistry() throws IOException, InterruptedException {
    	LockRegistry lockRegistry = new LockRegistry(zooKeeper);
    	return lockRegistry;
    }
   
    
    
    
    private void run() throws InterruptedException {
        synchronized (zooKeeper) {
            zooKeeper.wait();
        }
    }

    private void close() throws InterruptedException {
        this.zooKeeper.close();
    }

    @Override
    public void process(WatchedEvent event) {
        switch (event.getType()) {
            case None:
                if (event.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to Zookeeper");
                } else {
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
        }
    }
}
