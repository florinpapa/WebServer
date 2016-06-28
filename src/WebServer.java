import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

/*
 * Main class that implements the HTTP server.
 * This class will listen for incoming connections
 * and will pass the work to a thread pool.
 */

public class WebServer {
	private ServerSocket servSock;
	private ArrayList<Thread> webThreads;
	private ConcurrentLinkedQueue<WorkItem> work;
	private Semaphore availableWork;
	private int threadNo;
	private boolean done = false;
	private int SO_TIMEOUT = 2000;
	private String host;
	private int port;
	
	public WebServer(String host, int port, int threadNo) {
		try {
			servSock = new ServerSocket(port);
			webThreads = new ArrayList<>();
			work = new ConcurrentLinkedQueue<>();
			availableWork = new Semaphore(0); // no thread can enter until
											  // there is work to do
			this.threadNo = threadNo;
			servSock.setSoTimeout(SO_TIMEOUT); // make sure the server does
											   // not block on accept(), so
											   // that it can be stopped
			this.host = host;
			this.port = port;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void run() {
		for (int i = 0; i < threadNo; i++) {
			WebThread wt = new WebThread(availableWork, work, host, port);
			wt.start();
			webThreads.add(wt);
		}

		while (!done) {
			try {
				Socket s = servSock.accept();
				System.out.println("Got new connection");
				WorkItem w = new WorkItem(false, s);
				
				work.offer(w);
				availableWork.release();
			} catch (SocketTimeoutException e) {
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		stopServer();
	}
	

	void stopServer() {
		try {
			System.out.println("Stopping server");
			setDone(true);
			for (int i = 0; i < threadNo; i++) {
				WorkItem w = new WorkItem(true, null);
				work.offer(w);
				availableWork.release();
			}
			for (int i = 0; i < threadNo; i++) {
				webThreads.get(i).join();
			}
			servSock.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Stopped server");
	}
	
	void setDone(boolean done) {
		this.done = done;
	}
	
	public static void main(String args[]) {
		WebServer ws = new WebServer("localhost", 8080, 2);
		System.out.println("Starting web server on port 8080");
		
		/* start server */
		Thread t = new Thread(new Runnable() {
			public void run() {
				ws.run();
			}
		});
		t.start();
	}
}