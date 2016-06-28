import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class WebThread extends Thread {
	private Semaphore availableWork;
	private ConcurrentLinkedQueue<WorkItem> work;
	private String host;
	private int port;
	
	WebThread(Semaphore availableWork, ConcurrentLinkedQueue<WorkItem> work,
			  String host, int port) {
		this.availableWork = availableWork;
		this.work = work;
		this.host = host;
		this.port = port;
	}
	
	public void run() {
		try {
			while(true) {
				WorkItem w;
				
				availableWork.acquire();
				w = work.poll();
				if (w == null) {
					continue;
				} else {
					Socket sock;
					BufferedReader in;
					OutputStream out;
				    HTTPParser http;
					
					/* if no more work to be done, stop this thread */
					if (w.done)
						break;
					
					sock = w.sock;
					in = new BufferedReader(
					            new InputStreamReader(sock.getInputStream()));
				    out = sock.getOutputStream();
					http = new HTTPParser(host, port);
					
					http.parseRequest(in, out);
				    
					sock.close();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}