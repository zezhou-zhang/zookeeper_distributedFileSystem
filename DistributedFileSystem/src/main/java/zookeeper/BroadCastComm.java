package zookeeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class BroadCastComm extends Thread{
	private String myServerAddress;
	private String data;
	private String ip;
	private int port;
	private String fileName;
	
	public BroadCastComm(String myServerAddress, String fileName, String data, String ip, int port) {
		this.myServerAddress = myServerAddress;
		this.data = data;
		this.ip = ip;
		this.port = port;
		this.fileName = fileName;
	}
	
	public void start() {
		Thread thread = new Thread(this);
		thread.setName(ip+":"+String.valueOf(port));
		thread.start();
	}
	
	public void run() {
		Socket socketServer = createSocketServer(ip, port);
		if (socketServer!=null) {
			PrintWriter pw;
			try {
				pw = new PrintWriter(socketServer.getOutputStream(),true);
				BufferedReader bf = new BufferedReader(new InputStreamReader(socketServer.getInputStream()));
				pw.println(myServerAddress);
				pw.println(String.format("write %s %s", fileName, data));
				String feedback = bf.readLine();
				System.out.println(ip + ": " + feedback);
				socketServer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public Socket createSocketServer(String serverAddress, int portNum) {
		Socket socketServer = null;
		try {
			socketServer = new Socket(serverAddress, portNum);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return socketServer;
	}
}