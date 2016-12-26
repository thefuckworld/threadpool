package com.dw.framework.threadpool;

import java.util.concurrent.Callable;

/**
 * Description:
 * @author caohui
 */
public interface IAsynchronousHandler extends Callable<Object> {

	/**
	 * Description: 在run执行体执行完成后执行executeAfter()
	 * All Rights Reserved.
	 *
	 * @param t
	 * @return void
	 * @version 1.0 2016年12月26日 上午11:08:17 created by caohui(1343965426@qq.com)
	 */
	void executeAfter(Throwable t);
	
	/**
	 * Description: 在run执行体执行前执行executeBefore()
	 * All Rights Reserved.
	 *
	 * @param t
	 * @return void
	 * @version 1.0 2016年12月26日 上午11:09:17 created by caohui(1343965426@qq.com)
	 */
	void executeBefore(Thread t);
	
}
