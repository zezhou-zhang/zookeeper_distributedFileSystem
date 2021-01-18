package zookeeper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import org.apache.zookeeper.KeeperException;
public class FileOperation{
	
	private static String serverID;
	private static String threadName;
	private static TcpComm tcpComm;
	private static FilesRegistry filesRegistry;
	private static LockRegistry lockRegistry;
	
	
	public FileOperation(FilesRegistry filesRegistry, LockRegistry lockRegistry, String serverID, String threadName, TcpComm tcpComm){
		this.filesRegistry = filesRegistry;
		this.lockRegistry = lockRegistry;
		this.serverID = serverID;
		this.threadName = threadName;
		this.tcpComm = tcpComm;
	}
	
	/**
	 * File creation method needs to register itself to files znode
	 * so that other servers can be notified
	 * @param fileName
	 * @return
	 * @throws IOException
	 */
	public static String createFile(String fileName) throws IOException {
		String feedback = null;
		File file = new File(fileName);
		if (!file.exists()) {
			if (file.createNewFile()) {
				feedback = "File created: "+ file.getName();
				filesRegistry.registerToFiles(serverID, fileName);
			}
		}else {
			feedback = "File already exists.";
		}
		System.out.println(feedback);
		return feedback;
	}
	
	public static void createLocalFile(String fileName) throws IOException {
		String feedback = null;
		File file = new File(fileName);
		if (!file.exists()) {
			if (file.createNewFile()) {
				feedback = "File created: "+ file.getName();
			}
		}else {
			feedback = "File already exists.";
		}
		System.out.println(feedback);
	}
	
	/**
	 * File deletion operation requires write lock
	 * Step:
	 * 		1. register write lock
	 * 		2. delete local file
	 * 		3. unregister from files znode
	 * 		4. unregister the write lock
	 * @param fileName
	 * @return
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public static String deleteFile(String fileName) throws InterruptedException, KeeperException {
		String feedback = null;
		if (lockRegistry.registerToWriteLock(serverID, fileName, null) == false)
			return fileName + " has been locked right now...waiting...";
		File file = new File(fileName);
		if(file.delete()) {
			feedback = "File: " + fileName + " Deleted.";
			filesRegistry.unregisterToFiles(fileName);
		}else {
			feedback = "File deletion failed or file already been deleted.";
		}
		lockRegistry.unregisterToWriteLock(fileName);
		System.out.println(feedback);
		return feedback;
		
	}
	public static void deleteLocalFile(String fileName) throws InterruptedException, KeeperException {
		String feedback = null;
		File file = new File(fileName);
		if(file.delete()) {
			feedback = "File: " + fileName + " Deleted.";
		}else {
			feedback = "File deletion failed or file already been deleted.";
		}
		System.out.println(feedback);
		
	}
	
	/**
	 * Read file method requires read lock
	 * Step:
	 * 		1. register read lock
	 * 			a. if read lock already exists, continue read file
	 * 			b. if write lock exists, return and wait for callback
	 * 		2. read file by using scanner
	 * 		3. close file scanner
	 * 		4. unregister read lock
	 * @param fileName
	 * @return
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public static String readFile(String fileName) throws InterruptedException, KeeperException {
		String feedback = "";
		if (FileHashMap.getFileHashMap().containsKey(fileName)) {
			if (lockRegistry.registerToReadLock(serverID, fileName)== false)
				return fileName + " has been locked right now...waiting...";
			File file = new File(fileName);
			Scanner scanner;
			try {
				scanner = new Scanner(file);
				while (scanner.hasNextLine()) {
					String data = scanner.nextLine();
					feedback = feedback + data;
				}
				scanner.close();
				lockRegistry.unregisterToReadLock(fileName);
			} catch (FileNotFoundException e) {feedback = "File does not exist.";}
		}else {
			feedback = "File does not exist.";
		}
		lockRegistry.unregisterToReadLock(fileName);
		System.out.println(feedback);
		return feedback;
		
	}
	
	/**
	 * Write file method requires write lock
	 * Step:
	 * 		1. register write lock
	 * 			a. if write or read lock already exists, wait for call back
	 * 			b. if not, continue writing
	 * 		2. write to file using file writer
	 * 		3. close file writer
	 * 		4. unregister the write lock
	 * 
	 * @param fileName
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws KeeperException
	 */
	public static String writeFile(String fileName, String data) throws IOException, InterruptedException, KeeperException {
		int min = 5;
	    int max = 100;
		int bytes = (int)(Math.random() * (max - min + 1) + min);
		String feedback = "";
		if (FileHashMap.getFileHashMap().containsKey(fileName)) {
			if (lockRegistry.registerToWriteLock(serverID, fileName, data) == false)
				return fileName + " has been locked right now...waiting...";
			try {
				FileWriter fileWriter = new FileWriter(fileName);
				if (data == null)
					data = generateRandomString(bytes);
				fileWriter.write(data);
				fileWriter.close();
				feedback = "Successfully wrote to the file.";
				System.out.println("The total number of bytes written: " + bytes);
				lockRegistry.unregisterToWriteLock(fileName);
				tcpComm.synchronizedToReplicas(threadName,fileName,data);
			}catch (FileNotFoundException e) {feedback = "File does not exist.";}
		}else {
			feedback = "File does not exist.";
			lockRegistry.unregisterToWriteLock(fileName);
		}
		System.out.println(feedback);
		return feedback;
	}
	
	public static String generateRandomString(int size){
    	String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < size) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }
	
}
