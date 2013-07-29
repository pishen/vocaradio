package info.pishen.radio;

import java.util.concurrent.TimeoutException;

public class ProcessMonitor {
	
	public static int waitProcessWithTimeout(final Process process, long timeout) throws TimeoutException{
		Thread waiter = new Thread(){
			@Override
			public void run() {
				try {
					process.waitFor();
				} catch (InterruptedException e) {
					//interrupted and gone
				}
			}
		};
		
		waiter.start();
		try {
			waiter.join(timeout);
			int exit = process.exitValue();
			return exit;
		} catch (InterruptedException e) {
			throw new TimeoutException();
		} catch (IllegalThreadStateException e) {
			throw new TimeoutException();
		}
	}
}
