/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.monitor;

import com.github.thorqin.webapi.security.WebSecurityManager;
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
	private final LinkedBlockingQueue<Object> monitorQueue = new LinkedBlockingQueue<>();
	private final RequestInfo stopSignal = new RequestInfo();
	private static final MonitorService inst = new MonitorService();
	private long refcount = 0;
	private final List<Monitor> monitors = new LinkedList<>();
	
	public static MonitorService getInstance() {
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
					Object info = null;
					try {
						info = monitorQueue.take();
					} catch (InterruptedException e) {
					}
					try {
						if (info != stopSignal)
							doNotify(info);
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
	
	private void doNotify(Object info) {
		if (info == null)
			return;
		if (info.getClass().equals(RequestInfo.class)) {
			for (Monitor h: monitors) {
				if (h instanceof RequestMonitor)
					((RequestMonitor)h).requestProcessed((RequestInfo)info);
			}
		} else if (info.getClass().equals(MailInfo.class)) {
			for (Monitor h: monitors) {
				if (h instanceof MailMonitor)
					((MailMonitor)h).mailSent((MailInfo)info);
			}
		} else if (info.getClass().equals(StatementInfo.class)) {
			for (Monitor h: monitors) {
				if (h instanceof DBMonitor)
					((DBMonitor)h).statementExecuted((StatementInfo)info);
			}
		} else if (info.getClass().equals(RMIInfo.class)) {
			for (Monitor h: monitors) {
				if (h instanceof RMIMonitor)
					((RMIMonitor)h).methodInvoked((RMIInfo)info);
			}
		}
	}
	
	public static void record(RequestInfo reqInfo) {
		if (inst.monitorQueue != null && inst.alive)
			inst.monitorQueue.offer(reqInfo);
	}
	
	public static void record(MailInfo mailInfo) {
		if (inst.monitorQueue != null && inst.alive)
			inst.monitorQueue.offer(mailInfo);
	}
	
	public static void record(StatementInfo sqlInfo) {
		if (inst.monitorQueue != null && inst.alive)
			inst.monitorQueue.offer(sqlInfo);
	}
	
	public static void record(RMIInfo rmiInfo) {
		if (inst.monitorQueue != null && inst.alive)
			inst.monitorQueue.offer(rmiInfo);
	}
	
	public static RequestInfo buildRequestInfo(HttpServletRequest request, 
			HttpServletResponse response, WebSecurityManager.LoginInfo loginInfo, String recorder, long startTime) {
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
		reqInfo.role = loginInfo.role;
		reqInfo.user = loginInfo.user;
		reqInfo.serverName = request.getLocalName();
		reqInfo.serverIP = request.getLocalAddr();
		reqInfo.serverPort = request.getLocalPort();
		reqInfo.userAgent = request.getHeader("User-Agent");
		return reqInfo;
	}
	
	public void addMonitor(Monitor monitor) {
		for (Monitor h : monitors) {
			if (h.equals(monitor))
				return;
		}
		monitors.add(monitor);
	}
	
	public void removeMonitor(Monitor monitor) {
		monitors.remove(monitor);
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
		inst.internalIncreaseRef();
	}
	
	public static void release() {
		inst.internalDecreaseRef();
	}
}
