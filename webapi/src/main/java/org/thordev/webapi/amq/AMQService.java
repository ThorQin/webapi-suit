/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.amq;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import org.thordev.webapi.amq.annotation.AMQInterface;
import org.thordev.webapi.amq.annotation.AMQMethod;

/**
 *
 * @author nuo.qin
 */
public class AMQService implements AMQ.MessageHandler {
	private final Map<String, AMQ.AsyncReceiver> receivers = new HashMap<>();
	private final Map<String, Class<?>> interfaceMapping = new HashMap<>();
	private Object serviceImpl;
	private final AMQ amq;
	private boolean isRunning = false;
	
	public AMQService(AMQ amq) throws JMSException {
		this.amq = amq;
	}
	
	public synchronized void start(Object serviceImplementation) throws JMSException {
		if (isRunning) {
			stop();
		}
		if (serviceImplementation == null)
			throw new RuntimeException("Service implementation cannot be null.");
		serviceImpl = serviceImplementation;
		receivers.clear();
		interfaceMapping.clear();
		Class<?>[] interfaces = serviceImplementation.getClass().getInterfaces();
		int registerCount = 0;
		for (Class<?> cls : interfaces) {
			if (cls.isAnnotationPresent(AMQInterface.class)) {
				registerCount++;
				receivers.put("rmi:" + cls.getName(), null);
				interfaceMapping.put("rmi:" + cls.getName(), cls);
			}
		}
		if (registerCount <= 0)
			throw new RuntimeException("Does not implements any AMQ interface, at least implements one please.");
		for (String address: receivers.keySet()) {
			receivers.put(address, amq.createAsyncReceiver(address, this));
		}
		isRunning = true;
	}
	
	public synchronized void stop() {
		if (!isRunning)
			return;
		for (String address: receivers.keySet()) {
			receivers.get(address).close();
		}
		isRunning = false;
	}

	public boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public void onMessage(AMQ.IncomingMessage message) {
		boolean waitResponse = true;
		try {
			Class<?> cls = interfaceMapping.get(message.getAddress());
			if (cls == null)
				throw new RuntimeException("Interface not found!");
			Object[] args = message.getBody();
			Class<?>[] argTypes = new Class<?>[args.length];
			for (int i = 0; i < args.length; i++) {
				argTypes[i] = args.getClass();
			}
			// TODO: getMethod() NEED TEST
			Method method = cls.getMethod(message.getSubject(), argTypes);
			if (method == null) {
				throw new RuntimeException("Method not found: " + message.getSubject());
			}
			AMQMethod methodAnno = method.getAnnotation(AMQMethod.class);
			if (methodAnno != null) {
				waitResponse = methodAnno.waitResponse();
			}
			Object result = method.invoke(serviceImpl, args);
			if (waitResponse) {
				try {
					message.reply(result);
				} catch (Exception ex) {
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(AMQService.class.getName()).log(Level.SEVERE, 
					"Perform AMQ service call failed!", ex);
			if (waitResponse) {
				try {
					message.reply(-1, ex.getMessage());
				} catch (Exception err) {
				}
			}
		}
	}
}
