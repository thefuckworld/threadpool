package com.dw.framework.threadpool;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:
 * @author caohui
 */
public final class CommonThreadPool {

	public static final String LONG_FORMAT = "yyyy-MM-dd HH:mm:ss";
	
	private static ExecutorService execute = init();
	
	private static final long EXECUTETIME = 10000L;
	
    private static class ThreadPoolConfig {
    	public final static String COREPOOLSIZE = "corePoolSize";
    	public final static String MAXIMUMPOOLSIZE = "maximumPoolSize";
    	public final static String INITIALCAPACITY = "initialCapacity";
    	public final static String KEEPALIVETIME = "keepAliveTime";
    	public final static String THREADNAME = "threadName";
    }
	
	private CommonThreadPool() {
		
	}
	
	public static Object executeForResult(IAsynchronousHandler command) {
		ThreadPoolAdaptor handler = new ThreadPoolAdaptor(command, EXECUTETIME);
		Future<Object> future = execute.submit(handler);
		Object o = null;
		try {
			 o = future.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return o;
	}
	
	public static Future<Object> executeAsync(IAsynchronousHandler command) {
		ThreadPoolAdaptor handler = new ThreadPoolAdaptor(command, EXECUTETIME);
		Future<Object> future = execute.submit(handler);
		return future;
	}
	
	public static ThreadPoolExecutorExtend getThreadPool(ThreadPoolParameterVO vo) {
		int corePoolSize = vo.getCorePoolSize();
		int maximumPoolSize = vo.getMaximumPoolSize();
		int initialCapacity = vo.getInitialCapacity();
		long keepAliveTime = vo.getKeepAliveTime();
		String threadName = vo.getThreadName();
		
		// 增加构造队列容量参数
		TaskQueue taskQueue = new TaskQueue(initialCapacity, vo.isDiscard());
		ThreadPoolExecutorExtend executeNew = new ThreadPoolExecutorExtend(corePoolSize, maximumPoolSize,
				keepAliveTime, TimeUnit.SECONDS,
				taskQueue, new TaskThreadFactory(threadName), new ThreadPoolRejectedExecutionHandler(vo.isDiscard()));
		taskQueue.setParent(executeNew);
		return executeNew;
	}
	
	
	private static ExecutorService init() {
		Properties ps = getThreadPoolConfig();
		if(ps == null) {
			throw new NullPointerException("classpath can't find the theadpool's config named 'threadPoolConfig.properties'");
		}
		
		int corePoolSize = Integer.parseInt(ps.getProperty(ThreadPoolConfig.COREPOOLSIZE, "5"));
		int maximumPoolSize = Integer.parseInt(ps.getProperty(ThreadPoolConfig.MAXIMUMPOOLSIZE, "120"));
		int initialCapacity = Integer.parseInt(ps.getProperty(ThreadPoolConfig.INITIALCAPACITY, "20000"));
		long keepAliveTime = Long.parseLong(ps.getProperty(ThreadPoolConfig.KEEPALIVETIME, "120"));
		String threadName = ps.getProperty(ThreadPoolConfig.THREADNAME, "base-framework-threadPool-");
		
	    ThreadPoolParameterVO vo = new ThreadPoolParameterVO();
	    vo.setCorePoolSize(corePoolSize);
	    vo.setMaximumPoolSize(maximumPoolSize);
	    vo.setInitialCapacity(initialCapacity);
	    vo.setKeepAliveTime(keepAliveTime);
	    vo.setMaximumPoolSize(maximumPoolSize);
	    vo.setThreadName(threadName);
	    vo.setDiscard(false);
	    return getThreadPool(vo);
		
	}
	
	private static Properties getThreadPoolConfig() {
		Properties ps = new Properties();
		InputStream in = CommonThreadPool.class.getResourceAsStream("/threadPoolConfig.properties");
		if(in == null) {
			return null;
		}
		
		try {
			ps.load(in);
		}catch(IOException e) {
			throw new RuntimeException(e);
		}
		
		return ps;
	}
	
	public static boolean isMemoryThreshold() {
		long size = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		long thresholdSize = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax();
		if(size > thresholdSize) {
			return true;
		}
		
		return false;
	}
	/**
	 * Description: 线程工厂
	 * @author caohui
	 */
	static class TaskThreadFactory implements ThreadFactory {

		final ThreadGroup group;
		final AtomicInteger threadNumber = new AtomicInteger();
		final String namePrefix;
		
		TaskThreadFactory(String namePrefix) {
			SecurityManager s = System.getSecurityManager();
			group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			this.namePrefix = namePrefix;
		}
		
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
			t.setDaemon(true);
			if(t.getPriority() != Thread.NORM_PRIORITY) {
				t.setPriority(Thread.NORM_PRIORITY);
			}
			return t;
		}
		
	}
	
	/**
	 * Description: 自定义线程创建方法
	 * @author caohui
	 */
	static class TaskQueue extends LinkedBlockingQueue<Runnable> {
		private static final long serialVersionUID = -5048782803591461560L;
		
		ThreadPoolExecutorExtend parent = null;
		boolean isDiscard = true;
		
		public TaskQueue() {
			super();
		}
		
		public TaskQueue(int initialCapacity) {
			super(initialCapacity);
		}
		
		public TaskQueue(int initialCapacity, boolean isDiscard) {
			super(initialCapacity);
			this.isDiscard = isDiscard;
		}
		
		public TaskQueue(Collection<? extends Runnable> c) {
			super(c);
		}
		
		public void setParent(ThreadPoolExecutorExtend tp) {
			parent = tp;
		}

		public boolean force(Runnable r) {
			if(parent.isShutdown()) {
				throw new RejectedExecutionException("Executor not running, can't force a command into the queue.");
			}
			return super.offer(r);
		}
		
		@Override
		public boolean offer(Runnable r) {
			if(parent == null) {
				return super.offer(r);
			}
			
			// 内存限制
			if(this.isDiscard && isMemoryThreshold()) {
				return false;
			}

			// We are maxed out on threads, simply queue the object.
			if(parent.getPoolSize() == parent.getMaximumPoolSize()) {
				return super.offer(r);
			}
			
			// We have idle threads, just add it to the queue
			// note that we don't use getActiveCount()
			AtomicInteger submittedTasksCountNew = parent.getSubmittedTasksCount();
			if(submittedTasksCountNew != null && submittedTasksCountNew.get() < parent.getPoolSize()) {
				return super.offer(r);
			}
			
			// if we have less threads than maximum force creation of a new thread
			if(parent.getPoolSize() < parent.getMaximumPoolSize()) {
				return false;
			}
			
			// if we reached here, we need to add it to the queue.
			return super.offer(r);
		}
	}
	
	/**
	 * Description: 自定义线程池拒绝策略
	 * @author caohui
	 */
	static class ThreadPoolRejectedExecutionHandler implements RejectedExecutionHandler {
		boolean isDiscard = true;
		public ThreadPoolRejectedExecutionHandler() {}
		
		public ThreadPoolRejectedExecutionHandler(boolean isDiscard) {
			this.isDiscard = isDiscard;
		}
		
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
			// 没有内存阈值，执行如下
			if(!this.isDiscard || (this.isDiscard && !isMemoryThreshold())) {
				// 判断是不是并发情况导致的失败
				try {
					boolean reAdd = executor.getQueue().offer(r, 3, TimeUnit.MILLISECONDS);
					if(reAdd) {
						return;
					}
				}catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			if(r instanceof CommonFutureTask) {
				IAsynchronousHandler handlerAdaptor =((CommonFutureTask) r).getR();
				if(handlerAdaptor == null) {
					System.out.println("CommonThreadPool 已达到队列容量上限：" + r.toString());
					throw new RejectedExecutionException();
				}
				
				// 获取真实的handler，记录日志
				IAsynchronousHandler handler = null;
				if(handlerAdaptor instanceof ThreadPoolAdaptor) {
					handler = ((ThreadPoolAdaptor) handlerAdaptor).getHandler();
					if(handler == null) {
						handler = handlerAdaptor;
					}
				}else {
					handler = handlerAdaptor;
				}
				
				StringBuilder sb = new StringBuilder();
				sb.append("任务名称:").append(handler.getClass());
				sb.append("。 happenTime=").append(formatDate());
				sb.append("。 toString=").append(handler.toString());
				System.out.println("CommonThreadPool 已达到队列容量上限:" + sb.toString());
			}else {
				StringBuilder sb = new StringBuilder();
				sb.append("任务名称:").append(formatDate());
				sb.append("。 happenTime=").append(formatDate());
				sb.append("。 toString=").append(r.toString());
				System.out.println("CommonThreadPool 已达到队列容量上限:" + sb.toString());
			}
			
			// 自定义线程池，执行
			if(executor instanceof ThreadPoolExecutorExtend) {
				((ThreadPoolExecutorExtend) executor).getSubmittedTasksCount().decrementAndGet();
			}
			
			throw new RejectedExecutionException();
		}
		
		private String formatDate() {
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat(LONG_FORMAT);
			String result = sdf.format(date);
			return result;
			
		}
	}
}
