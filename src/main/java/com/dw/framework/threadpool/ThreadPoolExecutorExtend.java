package com.dw.framework.threadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolExecutorExtend extends ThreadPoolExecutor{

	// 需要处理的任务数
	final AtomicInteger submittedTasksCount = new AtomicInteger();
	
	ThreadPoolExecutorExtend(int corePoolSize, int maximumPoolSize, 
			long keepAliveTime, TimeUnit unit, 
			BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory,
			RejectedExecutionHandler handler) {
		super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	
	@Override
	public void execute(Runnable command) {
		submittedTasksCount.incrementAndGet();
		super.execute(command);
	}
	
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		// 执行完毕减1
		submittedTasksCount.decrementAndGet();
		
		if(r instanceof CommonFutureTask) {
			IAsynchronousHandler handler = ((CommonFutureTask) r).getR();
			if(handler == null) {
				throw new NullPointerException("ThreadPool's parameter is null.");
			}
			handler.executeAfter(t);
		}
	}

	
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		if(r instanceof CommonFutureTask) {
			IAsynchronousHandler handler = ((CommonFutureTask) r).getR();
			if(handler == null) {
				throw new NullPointerException("ThreadPool's parameter is null.");
			}
			
			handler.executeBefore(t);
		}
	}


	@Override
	protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
		return new CommonFutureTask<T>(callable);
	}


	public AtomicInteger getSubmittedTasksCount() {
		return submittedTasksCount;
	}
}
