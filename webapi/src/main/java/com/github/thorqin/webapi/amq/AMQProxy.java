/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.amq;

import com.github.thorqin.webapi.amq.annotation.AMQMethod;
import com.github.thorqin.webapi.monitor.MonitorService;
import com.github.thorqin.webapi.monitor.RMIInfo;
import com.github.thorqin.webapi.utility.Serializer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 *
 * @author nuo.qin
 */
public class AMQProxy implements InvocationHandler {
	private final AMQ amq;
	private final boolean enableTrace;
	public AMQProxy(AMQ amq) {
		this.amq = amq;
		enableTrace = amq.enableTrace();
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		long beginTime = System.currentTimeMillis();
		Class<?> classInterface = proxy.getClass().getInterfaces()[0];
		String address = "rmi:" + classInterface.getName();
		AMQMethod amqMethod = method.getAnnotation(AMQMethod.class);
		boolean waitResponse = true;
		if (amqMethod != null) {
			waitResponse = amqMethod.waitResponse();
		}
		AMQ.Sender sender = amq.createSender(address);
		try {
			if (waitResponse) {
				AMQ.IncomingMessage incoming = sender.sendAndWaitForReply(method.getName(), args);
				if (incoming.getReplyCode() == -1)
					throw new RuntimeException((String)incoming.getBody());
				if (method.getReturnType().equals(void.class) || method.getReturnType().equals(Void.class)) {
					return null;
				} else {
					Object result = Serializer.fromKryo(incoming.getBodyBytes());
					return result;
				}
			} else {
				sender.send(method.getName(), args);
				return null;
			}
		} finally {
			if (enableTrace) {
				RMIInfo info = new RMIInfo();
				info.method = method.getDeclaringClass().getName() + "::" + method.getName();
				info.address = address;
				info.startTime = beginTime;
				info.runningTime = System.currentTimeMillis() - beginTime;
				MonitorService.record(info);
			}
		}
	}
}
