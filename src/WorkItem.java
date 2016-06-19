import java.net.Socket;

public class WorkItem {
	boolean done;
	Socket sock;
	
	WorkItem(boolean done, Socket s) {
		this.done = done;
		this.sock = s;
	}
}
