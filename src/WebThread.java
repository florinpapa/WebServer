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
	
	WebThread(Semaphore availableWork, ConcurrentLinkedQueue<WorkItem> work) {
		this.availableWork = availableWork;
		this.work = work;
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
					http = new HTTPParser("localhost");
					
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
