import java.io.IOException;
import java.io.PrintWriter;

public class LoadTest {
	private PrintWriter pr;

	public LoadTest(PrintWriter pr) {
		this.pr = pr;
	}
	public void performLoadTest() throws IOException {
		pr.println("received");
		pr.close();
	}
	
	public void perfromStressTest(int threads, double load, long duration) {
		for (int thread = 0; thread < threads; thread++) {
			new BusyThread("Thread" + thread, load, duration).start();
	    }
	        System.out.println("This thread will run infinite loop until reach duration time.");
	        /*
		 * Wait for all threads finished and then plot for performance comparison
		 */
		for(int i = 0; i<threads; i++) {
			Thread t = getThreadByName("Thread"+i);
			//if return null, it means that thread already finished before its turn in original order
			if (t != null) {
				try {
					System.out.println("The thread name is : " + t.getName());
					System.out.println("The thread alive status is: " + t.isAlive());
					if(t.isAlive())
						t.join();
				} catch (InterruptedException e) {}
			}
		}
		pr.println("Stress Test Finished!");
	}
	
	private Thread getThreadByName(String threadName) {
		for (Thread t : Thread.getAllStackTraces().keySet()) {
	        if (t.getName().equals(threadName)) return t;
	    }
	    return null;
	}
}
