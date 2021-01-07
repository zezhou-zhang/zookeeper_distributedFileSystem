import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import configuration.Servers;
import zookeeper.FileHashMap;
import zookeeper.FileWatcher;
import zookeeper.FilesRegistry;
import zookeeper.LockRegistry;
import zookeeper.ServiceDiscovery;
import zookeeper.ServiceRegistry;
import zookeeper.TcpComm;
import zookeeper.ZookeeperConnector;

public class Application extends FileHashMap{
	private static String serverID;
	private static final int DEFAULT_PORT = 8003;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		int portNum = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
		//Get the ip address from current server and print it out
		InetAddress tcpServerAddress = getMyServerAddress();
	    System.out.println("The local IP address is: " + tcpServerAddress.getHostAddress());
	    serverID = tcpServerAddress.getHostAddress() + ":" + String.valueOf(portNum);
	    //Establish the server
	  	ServerSocket tcpServerSocket = new ServerSocket(portNum,50,tcpServerAddress);
	  	System.out.println(serverID + " is lisenting on port " + portNum);
	  	
	  	/* For static server configurations
	  	Servers servers = new Servers();
	  	List<String> serverList = servers.getServerList();
	  	for (String server: serverList){
	  		System.out.println(server);
	  	}
	  	*/
	  	ZookeeperConnector zookeeperConnector = new ZookeeperConnector(tcpServerAddress.getHostAddress(),portNum);
	  	try {
			zookeeperConnector.runZookeeper();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	  	FilesRegistry filesRegistry = zookeeperConnector.runFilesRegistry();
	    new FileWatcher(filesRegistry).start();
	    
	    ServiceRegistry serviceRegistry = zookeeperConnector.runServiceRegistry();
	    new ServiceDiscovery(serviceRegistry, tcpServerAddress.getHostAddress(), portNum).start();
	    
	    TcpComm tcpComm = zookeeperConnector.runTcpComm();
	    
	    LockRegistry lockRegistry = zookeeperConnector.runLockRegistry();
	    
	  	while(true) {
	  		Socket socket = tcpServerSocket.accept();
			new OperatingCommand(filesRegistry,lockRegistry,serverID, socket, tcpComm).start();
	  	}
	}
	
	
	
	
	
	private static InetAddress getMyServerAddress() {
		InetAddress myServerAddress = null;
		try {
			myServerAddress = InetAddress.getLocalHost();
			
		} catch (UnknownHostException e) {
			System.out.println("Exception: Cannot find localhost address.");
		}
		return myServerAddress;
	}
	
}
