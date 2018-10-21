package io.pivotal.gemfire.server.Executor;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialExecutor implements Executor {
	private final Queue<Runnable> tasks = new ArrayDeque<Runnable>();
	private final ExecutorService executor;
	private Runnable active;
	private static SerialExecutor single;
	private final Logger log = LoggerFactory.getLogger(SerialExecutor.class);

	public void shutdown() {
		while (!tasks.isEmpty()) {
			try {
				Thread.sleep(1000);
				log.debug("SerialExecutor waiting to shutdown");
			} catch (InterruptedException e) {
				log.debug("SerialExecutor waiting to shutdown exception "
						+ e.getStackTrace());
			}
		}
		executor.shutdown();
		log.debug("SerialExecutor shutdown");
	}

	public synchronized static SerialExecutor getInstance() {
		if (single == null) {
			single = new SerialExecutor();
		}
		return single;
	}

	private SerialExecutor() {
		executor = Executors.newCachedThreadPool();
	}

	public synchronized void execute(final Runnable r) {
		tasks.offer(new Runnable() {
			public void run() {
				try {
					r.run();
				} finally {
					scheduleNext();
				}
			}
		});
		if (active == null) {
			scheduleNext();
		}
	}

	protected synchronized void scheduleNext() {
		if ((active = tasks.poll()) != null) {
			executor.execute((Runnable) active);
		}
	}
}
