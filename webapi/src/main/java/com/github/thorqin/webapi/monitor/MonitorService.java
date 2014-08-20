/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.monitor;

import com.github.thorqin.webapi.security.Security;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author nuo.qin
 */
public class MonitorService {
	
	private boolean alive = false;
	private Thread sendThread = null;
	private static final Logger logger = Logger.getLogger(MonitorService.class.getName());
	private final LinkedBlockingQueue<RequestInfo> monitorQueue = new LinkedBlockingQueue<>();
	private final RequestInfo stopSignal = new RequestInfo();
	private static final MonitorService inst = new MonitorService();
	private long refcount = 0;
	private final List<MonitorHandler> handlers = new LinkedList<>();
	
	public static MonitorService instance() {
		return inst;
	}
	
	private void start() {
		if (alive)
			return;
		alive = true;
		sendThread = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Monitor service started!");
				while (alive) {
					RequestInfo reqInfo = null;
					try {
						reqInfo = monitorQueue.take();
					} catch (InterruptedException e) {
					}
					try {
						if (reqInfo != stopSignal)
							doNotify(reqInfo);
						else if (!alive)
							break;
					} catch (Exception ex) {
						logger.log(Level.SEVERE, null, ex);
					}
				}
			}
		});
		// After server shutdown keep the thread running until all task is finished.
		// sendThread.setDaemon(false);
		sendThread.start();
	}
	private void stop() {
		if (!alive)
			return;
		alive = false;
		monitorQueue.offer(stopSignal);
		try {
			sendThread.join(30000);
		} catch (InterruptedException e) {
		}
		System.out.println("Monitor service stopped!!");
	}
	
	private void doNotify(RequestInfo reqInfo) {
		for (MonitorHandler h: handlers) {
			h.requestProcessed(reqInfo);
		}
	}
	
	public void record(RequestInfo reqInfo) {
		if (monitorQueue != null && alive)
			monitorQueue.offer(reqInfo);
	}
	
	public static RequestInfo buildRequestInfo(HttpServletRequest request, 
			HttpServletResponse response, String recorder, long startTime) {
		RequestInfo reqInfo = new RequestInfo();
		reqInfo.recorder = recorder;
		reqInfo.startTime = startTime;
		long endTime = System.currentTimeMillis();
		reqInfo.runningTime = endTime - startTime;
		reqInfo.clientIP = request.getRemoteAddr();
		reqInfo.method = request.getMethod().toUpperCase();
		String q = request.getQueryString();
		reqInfo.url = request.getRequestURL() + (q == null ? "" : "?" + q);
		reqInfo.referrerUrl = request.getHeader("Referer");
		Security.LoginInfo loginInfo = Security.getLoginInfo(request, response);
		reqInfo.role = loginInfo.role;
		reqInfo.user = loginInfo.user;
		reqInfo.serverName = request.getLocalName();
		reqInfo.serverIP = request.getLocalAddr();
		reqInfo.serverPort = request.getLocalPort();
		reqInfo.userAgent = request.getHeader("User-Agent");
		return reqInfo;
	}
	
	public void addHandler(MonitorHandler handler) {
		for (MonitorHandler h : handlers) {
			if (h.equals(handler))
				return;
		}
		handlers.add(handler);
	}
	
	public void removeHandler(MonitorHandler handler) {
		handlers.remove(handler);
	}

	private synchronized void internalIncreaseRef() {
		refcount++;
		if (refcount > 0)
			inst.start();
	}
	
	private synchronized void internalDecreaseRef() {
		refcount--;
		if (refcount <= 0)
			inst.stop();
	}
	
	public static void addRef() {
		instance().internalIncreaseRef();
	}
	
	public static void release() {
		instance().internalDecreaseRef();
	}
}
