/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.amq;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import com.github.thorqin.webapi.utility.Serializer;
import com.github.thorqin.webapi.amq.annotation.AMQMethod;

/**
 *
 * @author nuo.qin
 */
public class AMQProxy implements InvocationHandler {
	private final AMQ amq;
	public AMQProxy(AMQ amq) {
		this.amq = amq;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class<?> classInterface = proxy.getClass().getInterfaces()[0];
		String address = "rmi:" + classInterface.getName();
		AMQMethod amqMethod = method.getAnnotation(AMQMethod.class);
		boolean waitResponse = true;
		if (amqMethod != null) {
			waitResponse = amqMethod.waitResponse();
		}
		AMQ.Sender sender = amq.createSender(address);
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
	}
}
