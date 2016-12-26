package com.dw.framework.threadpool;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

public class CommonFutureTask<V> extends FutureTask<V>{

	private IAsynchronousHandler r;
	
	public CommonFutureTask(Callable<V> callable) {
		super(callable);
		
		if(callable instanceof IAsynchronousHandler) {
			this.r = (IAsynchronousHandler)callable;
		}
	}
	
	public CommonFutureTask(Runnable runnable, V result) {
		super(runnable, result);
	}

	public IAsynchronousHandler getR() {
		return r;
	}
}
