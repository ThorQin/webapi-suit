/*
 * The MIT License
 *
 * Copyright 2014 nuo.qin.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.thordev.webapi.security;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.thordev.webapi.Dispatcher;
import org.thordev.webapi.amq.AMQ;
import org.thordev.webapi.database.annotation.DBInterface;
import static org.thordev.webapi.security.SecuritySetting.RuleAction.ALLOW;
import static org.thordev.webapi.security.SecuritySetting.RuleAction.DENY;
import org.thordev.webapi.utility.Serializer;


/**
 *
 * @author nuo.qin
 */
public final class Security {
	private static final Logger logger = Logger.getLogger(Security.class.getName());
	private static Security instance = null;
	
	private final SecurityConfig config;
	private final boolean syncFromRemote;
	
	private AMQ amq;
	private AMQ.AsyncReceiver receiver;
	private final AMQ.MessageHandler handler = new AMQ.MessageHandler () {
		@Override
		public void onMessage(AMQ.IncomingMessage message) {
			try {
				updateSetting((SecuritySetting)message.getBody(SecuritySetting.class));
			} catch (Exception ex) {
				logger.log(Level.SEVERE, "Receive dynamic configuration failed!.", ex);
			}
		}
	};
	
	private synchronized void updateSetting(SecuritySetting setting) {
		config.set(setting);
		config.buildMatcher();
		config.save();
	}
	
	
	@DBInterface(procPrefix="fn_webapi")
	public static interface SecurityAPI {
		// Save setting to database
		public void saveSetting(String setting);
		// Load setting from database
		public String loadSetting();
		// Perform a security check return whether can perform specified action on that resource
		public boolean checkPermission(
				String resType, String resId, String operation, 
				String user, String role, String scenario);
	}
	
	public static String join(Set<String> src) {
		if (src == null)
			return "";
		StringBuilder sb = new StringBuilder();
		boolean isEmpty = true;
		for (String v: src) {
			if (!isEmpty)
				sb.append(",");
			sb.append(v.trim());
			isEmpty = false;
		}
		return sb.toString();
	}
	public static Set<String> split(String src) {
		return split(src, false);
	}
	public static Set<String> split(String src, boolean toLowerCase) {
		if (src.trim().isEmpty())
			return null;
		Set<String> result = new HashSet<>();
		for (String v: src.split(",")) {
			if (toLowerCase)
				result.add(v.trim().toLowerCase());
			else
				result.add(v.trim());
		}
		return result;
	}

	private Security(boolean syncFromRemote) throws IOException, SQLException, JMSException {
		this.syncFromRemote = syncFromRemote;
		config = new SecurityConfig();
		load();
	}
	
	public void load() throws IOException, SQLException, JMSException {
		SecuritySetting setting = config.get();
		if (setting.dbConfig != null && !setting.dbConfig.isEmpty()) {
			try {
				SecurityAPI api = Dispatcher.getDBProxy(setting.dbConfig, SecurityAPI.class);
				SecurityConfig aSetting = Serializer.fromJson(api.loadSetting(), SecurityConfig.class);
				config.set(aSetting);
				config.save();
			} catch (Exception ex) {
				logger.log(Level.WARNING, "Load database security setting failed.", ex);
			}
		}
		config.buildMatcher();
		createReceiver();
	}
	
	public static synchronized Security getInstance(boolean syncFromRemote)  throws IOException, SQLException, JMSException {
		if (instance != null)
			return instance;
		else {
			Security inst = new Security(syncFromRemote);
			instance = inst;
			return inst;
		}
	}
	
	private void createReceiver() throws IOException {
		if (receiver != null) {
			receiver.close();
		}
		if (!syncFromRemote)
			return;
		SecuritySetting setting = config.get();
		if (setting.amqConfig != null && !setting.amqConfig.isEmpty()) {
			try {
				amq = Dispatcher.getAMQ(setting.amqConfig);
				receiver = amq.createAsyncReceiver(handler);
			} catch (JMSException ex) {
				logger.log(Level.SEVERE, "Create AMQ receiver failed.", ex);
			}
		}
	}
	
//	public static void saveSetting(SecurityConfig setting, String configFile) {
//		try {
//			if (!configFile.toLowerCase().endsWith(".security"))
//				configFile += ".security";
//			Serializer.saveJsonFile(setting, configFile, true);
//		} catch (Exception ex) {
//			setting = new SecurityConfig();
//		}
//		SecurityConfig aSetting = Serializer.copy(setting);
//		if (setting.dbConfig != null && !setting.dbConfig.isEmpty()) {
//			try {
//				SecurityAPI api = Dispatcher.getDBProxy(setting.dbConfig, SecurityAPI.class);
//				aSetting.dbConfig = null;
//				aSetting.amqConfig = null;
//				api.saveSetting(Serializer.toJsonString(aSetting));
//			} catch (Exception ex) {
//				logger.log(Level.SEVERE, "Save database security setting failed.", ex);
//			}
//		}
//		if (setting.amqConfig != null && !setting.amqConfig.isEmpty()) {
//			try  {
//				AMQ amq = new AMQ(setting.amqConfig);
//				AMQ.Sender sender = amq.createSender();
//				sender.send("changed", aSetting);
//			} catch (Exception ex) {
//				logger.log(Level.SEVERE, "Save database security setting failed.", ex);
//			}
//		}
//	}
	
	
	public void close() {
		if (receiver != null) {
			receiver.close();
			receiver = null;
		}
	}
	
	public static class CheckPermissionResult {
		public boolean allow;
		public String ruleName;
		public CheckPermissionResult(boolean allow) {
			this.allow = allow;
			ruleName = null;
		}
		public CheckPermissionResult(boolean allow, String rule) {
			this.allow = allow;
			ruleName = rule;
		}
	}
	
	public CheckPermissionResult checkPermission(
				String resType, String resId, String operation, 
				String user, String role, String scenario) {
		SecuritySetting setting = config.get();
		for (SecuritySetting.Rule rule: setting.rules) {
			if (rule.match(resType, resId, operation, user, role, scenario)) {
				if (rule.action == ALLOW)
					return new CheckPermissionResult(true, rule.name);
				else if (rule.action == DENY) {
					logger.log(Level.INFO, "Permission denied.");
					return new CheckPermissionResult(false, rule.name);
				} else if (setting.dbConfig != null && !setting.dbConfig.trim().isEmpty()) {
					try {
						SecurityAPI api = Dispatcher.getDBProxy(setting.dbConfig, SecurityAPI.class);
						boolean result = api.checkPermission(resType, resId, operation, user, role, scenario);
						return new CheckPermissionResult(result, rule.name);
					} catch (Exception ex) {
						logger.log(Level.SEVERE, "Check permission raise a DB exception, access denied!", ex);
						return new CheckPermissionResult(false, rule.name);
					}
				}
			}
		}
		return new CheckPermissionResult(setting.defaultAllow);
	}
	
	private CheckPermissionResult checkPermission(MappingInfo info, String user, String role) {
		return checkPermission(info.resType, info.resId, info.operation, 
				user, role, info.scenario);
	}
	
	private boolean checkPermission(HttpServletRequest request, HttpServletResponse response, 
			String user, String role, HttpSession session) {
		String pathInfo = request.getPathInfo();
		String servletPath = request.getServletPath();
		String queryString = request.getQueryString();
		String path = servletPath + (pathInfo == null ? "" : pathInfo) +
				(queryString == null ? "" : "?" + queryString);
		System.out.println("Check permission: " + path);
		MappingInfo info = mappingURL(path, 
				request.getScheme().toLowerCase(), request.getServerName().toLowerCase(), 
				request.getMethod().toLowerCase(), request.getServerPort(), session);
		CheckPermissionResult result = checkPermission(info, user, role);
		if (result.allow) {
			return true;
		} else {
			try {
				if (info.redirection != null) {
					String redirectUrl = info.redirection.get(result.ruleName);
					if (redirectUrl != null)
						response.sendRedirect(redirectUrl);
					else {
						redirectUrl = info.redirection.get("");
						if (redirectUrl != null)
							response.sendRedirect(redirectUrl);
						else
							response.sendError(HttpServletResponse.SC_FORBIDDEN);
					}
				} else {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
			} catch (IOException ex) {
				logger.log(Level.SEVERE, "Send response failed.", ex);
			}
			return false;
		}
	}
	
	public boolean checkPermission(HttpServletRequest request, HttpServletResponse response) {
		String user = null;
		String role = null;
		SecuritySetting setting = config.get();
		HttpSession session;
		if (setting.clientSession) {
			session = ClientSession.fromCookie(request, response);
			if (session != null && (setting.sessionTimeout <= 0 || 
					(new Date().getTime() - session.getLastAccessedTime()) < 
					setting.sessionTimeout * 1000)) {
				user = (String)session.getAttribute("user");
				role = (String)session.getAttribute("role");
			}
		} else {
			session = request.getSession();
			user = (String)session.getAttribute("user");
			role = (String)session.getAttribute("role");
		}
		return checkPermission(request, response, user, role, session);
	}
	
	public static class MappingInfo {
		public Map<String, String> redirection;
		public String resType;
		public String resId;
		public String operation;
		public String scenario;
	}
	
	public synchronized MappingInfo mappingURL(String url, String schema,
			String domain, String method, int port, HttpSession session) {
		SecuritySetting setting = config.get();
		Map<String, String> parameters = new HashMap<>();
		MappingInfo info = new MappingInfo();
		for (SecuritySetting.URLMatcher matcher : setting.matchers) {
			if (matcher.match(url, schema, domain, method, port, session, parameters)) {
				info.redirection = matcher.redirection;
				info.resType = matcher.resType;
				info.resId = matcher.resId;
				info.operation = matcher.operation;
				info.scenario = matcher.scenario;
				if (parameters.containsKey("res"))
					info.resType = parameters.get("res");
				if (parameters.containsKey("resid"))
					info.resId = parameters.get("resid");
				if (parameters.containsKey("operation"))
					info.operation = parameters.get("operation");
				return info;
			}
		}
		return info;
	}
	
	public static void login(HttpServletRequest request, HttpServletResponse response, String user, String role) {
		if (instance == null)
			return;
		SecuritySetting setting = instance.config.get();
		if (setting.clientSession) {
			ClientSession session = ClientSession.fromCookie(request, response);
			session.setAttribute("user", user);
			session.setAttribute("role", role);
			session.save();
		} else {
			HttpSession session = request.getSession();
			session.setMaxInactiveInterval(setting.sessionTimeout);
			session.setAttribute("user", user);
			session.setAttribute("role", role);
		}
	}
	
	public static void logout(HttpServletRequest request, HttpServletResponse response) {
		if (instance == null)
			return;
		try {
			SecuritySetting setting = instance.config.get();
			if (setting.clientSession) {
				ClientSession session = ClientSession.fromCookie(request, response);
				session.delete();
			} else {
				HttpSession session = request.getSession();
				session.invalidate();
			}
		} catch (Exception ex) {
			logger.log(Level.WARNING, "Logout exception!.", ex);
		}
	}
}
