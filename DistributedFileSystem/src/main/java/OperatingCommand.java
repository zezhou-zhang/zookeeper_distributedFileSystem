import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

import org.apache.zookeeper.KeeperException;

import zookeeper.FileOperation;
import zookeeper.FilesRegistry;
import zookeeper.LockRegistry;
import zookeeper.TcpComm;

public class OperatingCommand extends Thread {
	
	private String serverID;
	private PrintWriter writer;
	private BufferedReader reader;
	private Socket socket;
	private TcpComm tcpComm;
	private String threadName;
	private FilesRegistry filesRegistry;
	private LockRegistry lockRegistry;
	
	public OperatingCommand(FilesRegistry filesRegistry, LockRegistry lockRegistry, 
			String serverID, Socket socket, TcpComm tcpComm) throws IOException {
		this.filesRegistry = filesRegistry;
		this.lockRegistry = lockRegistry;
		this.serverID = serverID;
		this.socket = socket;
		this.tcpComm = tcpComm;
		this.writer = new PrintWriter(socket.getOutputStream(),true); 
		this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}
	
	public void start() {
		threadName = null;
		try {
			threadName = reader.readLine();
		} catch (IOException e) {
			System.out.println("Client name is not provided.");
		}
		System.out.println (threadName + " is connected to " + serverID);
		Thread thread = new Thread(this);
		thread.setName(threadName);
		thread.start();
	}
	
	public void run() {
		System.out.println(serverID + " is waiting for command.");
		while(true) {
			String command = null;
			try {
				command = reader.readLine();
			} catch (IOException e) {}
			if(command == null) {
				 System.out.println("Detect " + threadName + " disconnected.");
		         break;
			}
			try {
				this.operateCommand(command);
			} catch (IOException | InterruptedException | KeeperException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void operateCommand(String command) throws IOException, InterruptedException, KeeperException {
		FileOperation fileOperation = new FileOperation(filesRegistry, lockRegistry, serverID, threadName, tcpComm);
		System.out.println(threadName + ": " + command);
		String match = matchCommand(command);
		String fileName;
		String feedback;
		switch(match) {
			case "load test":
				LoadTest loadTest = new LoadTest(writer);
				loadTest.performLoadTest();
				break;
			case "stress test":
				String[] command_string = command.split("\\s+");
				int threads = command_string.length == 5 ? Integer.valueOf(command_string[2]) : 200;
				double load = command_string.length == 5 ? Double.valueOf(command_string[3]) : 1;
				long duration = command_string.length == 5 ? Long.valueOf(command_string[4]) : 100000;
				LoadTest stressTest = new LoadTest(writer);
				stressTest.perfromStressTest(threads, load, duration);
				break;
			case "create":
				fileName = command.split("\\s+")[1];
				feedback = fileOperation.createFile(fileName);
				writer.println(feedback);
				break;
			case "delete":
				fileName = command.split("\\s+")[1];
				feedback = fileOperation.deleteFile(fileName);
				writer.println(feedback);
				break;
			case "read":
				fileName = command.split("\\s+")[1];
				feedback = fileOperation.readFile(fileName);
				writer.println(feedback);
				break;
			case "write":
				String data = command.split("\\s+").length == 3 ? command.split("\\s+")[2] : null;
				fileName = command.split("\\s+")[1];
				feedback = fileOperation.writeFile(fileName, data);
				writer.println(feedback);
				break;
			case "not match":
				System.out.println("Invalid command.");
				break;
		}
	}
	
	private String matchCommand(String command) {
		if (command.contains("load test")){
			return "load test";
		}
		else if (command.contains("stress test")){
			return "stress test"; 
		}
		else if (command.contains("create")) {
			return "create";
		}
		else if (command.contains("delete")) {
			return "delete";
		}
		else if (command.contains("read")) {
			return "read";
		}
		else if (command.contains("write")) {
			return "write";
		}
		return "not match";
		
	}
	
}
