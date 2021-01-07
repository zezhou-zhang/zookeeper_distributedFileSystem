package zookeeper;
import java.io.IOException;
import java.util.List;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

public class TcpComm extends ServiceRegistry {
	
	private List<String> allServerAddresses;
	private static int portNum;
	private static String serverAddress;
	
	public TcpComm(ZooKeeper zooKeeper, String serverAddress, int portNum) {
		super(zooKeeper);
		this.serverAddress = serverAddress;
		this.portNum = portNum;
	}

	public void synchronizedToReplicas(String threadName, String fileName, String data) throws KeeperException, InterruptedException, IOException {
		 allServerAddresses = this.getAllServerAddresses();
		 if (allServerAddresses.contains(threadName)){
			 return;
		 }
		 for (String server: allServerAddresses) {
			 if (server.equals(serverAddress+":"+portNum)) {
				 continue;
			 }
			 System.out.println("Ready to synchrnoize writing to " + server);
			 String ip = server.split(":")[0];
			 int port = Integer.valueOf(server.split(":")[1]);
			 new BroadCastComm(serverAddress+":"+portNum, fileName, data, ip, port).start();
		 }	
	}
}


	
	
	

