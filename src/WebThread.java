import java.io.IOException;
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
					/* if no more work to be done, stop this thread */
					if (w.done)
						break;
					
					System.out.println("Got work to do on port " + w.sock);
					w.sock.close();
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
