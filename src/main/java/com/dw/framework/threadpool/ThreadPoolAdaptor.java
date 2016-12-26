package com.dw.framework.threadpool;

import java.util.concurrent.Future;

/**
 * Description: 实现任务的代理
 * @author caohui
 */
public class ThreadPoolAdaptor implements IAsynchronousHandler{

	private IAsynchronousHandler handler;
	private Future<Object> future;
	private final long executeTime;
	
	public ThreadPoolAdaptor(IAsynchronousHandler handler, long time) {
		this.handler = handler;
		executeTime = System.currentTimeMillis();
	}
	
	@Override
	public Object call() throws Exception {
		return handler.call();
	}

	@Override
	public void executeAfter(Throwable t) {
 
		handler.executeAfter(t);
	}

	@Override
	public void executeBefore(Thread t) {
		handler.executeBefore(t);
	}

	public IAsynchronousHandler getHandler() {
		return handler;
	}

	public void setHandler(IAsynchronousHandler handler) {
		this.handler = handler;
	}

	public Future<Object> getFuture() {
		return future;
	}

	public void setFuture(Future<Object> future) {
		this.future = future;
	}

	public long getExecuteTime() {
		return executeTime;
	}
}
