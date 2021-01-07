import java.io.PrintWriter;

public class LoadTest {
	private PrintWriter pr;
	public LoadTest(PrintWriter pr) {
		this.pr = pr;
	}
	public void performLoadTest() {
		pr.println("received");
	}
}
