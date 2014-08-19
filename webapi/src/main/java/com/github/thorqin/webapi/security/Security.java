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

package com.github.thorqin.webapi.security;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.github.thorqin.webapi.Dispatcher;
import com.github.thorqin.webapi.amq.AMQ;
import com.github.thorqin.webapi.database.annotation.DBInterface;
import com.github.thorqin.webapi.security.Security.MappingInfo.RedirectionURL;
import static com.github.thorqin.webapi.security.SecuritySetting.RuleAction.allow;
import static com.github.thorqin.webapi.security.SecuritySetting.RuleAction.deny;
import com.github.thorqin.webapi.security.SecuritySetting.URLMatcher.RedirectionInfo;
import com.github.thorqin.webapi.utility.Serializer;
import java.io.File;
import java.nio.file.Files;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;


/**
 *
 * @author nuo.qin
 */
public final class Security {
	private static final Logger logger = Logger.getLogger(Security.class.getName());
	private static final Logger tracerLogger;
	
	private static Security instance = null;
	
	private final SecurityConfig config;
	private final boolean syncFromRemote;
	
	private AMQ amq;
	private AMQ.AsyncReceiver receiver;
	
	static {
		tracerLogger = Logger.getLogger(Security.class.getName() + ".security");
		try {
			String logDir = Security.class.getResource("/").getPath() + "logs/";
			Files.createDirectories(new File(logDir).toPath());
			FileHandler handler = new FileHandler(logDir+"security%u.log", true);
			handler.setLevel(Level.INFO);
			handler.setEncoding("utf-8");
			handler.setFormatter(new SimpleFormatter());
			tracerLogger.setUseParentHandlers(false);
			tracerLogger.addHandler(new ConsoleHandler());
			tracerLogger.addHandler(handler);
		} catch (Exception ex) {
		}
	}
	
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
				if (rule.action == allow)
					return new CheckPermissionResult(true, rule.name);
				else if (rule.action == deny) {
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
		
		SecuritySetting setting = config.get();
		StringBuilder tracer = null;
		if (setting.trace) {
			tracer = new StringBuilder();
			tracer.append(request.getMethod().toUpperCase())
					.append(":").append(path).append("\n");
			tracer.append("USER: ").append(user).append("\n")
					.append("ROLE: ").append(role).append("\n");
		}
		try {
			MappingInfo info = mappingURL(path, 
					request.getScheme().toLowerCase(), request.getServerName().toLowerCase(), 
					request.getMethod().toLowerCase(), request.getServerPort(), session);
			CheckPermissionResult result = checkPermission(info, user, role);
			if (result.allow) {
				if (setting.trace) {
					tracer.append("ACTION: allowed!\n");
				}
				return true;
			} else {
				try {
					if (info.redirection != null) {
						RedirectionURL redirectUrl = info.getRedirectionUrl(role, user);
						if (redirectUrl != null) {
							String url = redirectUrl.url;
							if (redirectUrl.setReference) {
								if (url.contains("?")) {
									url += "&ref=" + URLEncoder.encode(path, "utf-8");
								} else {
									url += "?ref=" + URLEncoder.encode(path, "utf-8");
								}
							}
							if (url.startsWith("$")) {
								url = request.getContextPath() + url.substring(1);
							}
							if (setting.trace) {
								tracer.append("ACTION: redirect to: ").append(url).append("\n");
							}
							response.sendRedirect(url);
						} else {
							if (setting.trace) {
								tracer.append("ACTION: forbidden!!!").append("\n");
							}
							response.sendError(HttpServletResponse.SC_FORBIDDEN);
						}
					} else {
						if (setting.trace) {
							tracer.append("ACTION: forbidden!!!").append("\n");
						}
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
					}
				} catch (IOException ex) {
					if (setting.trace) {
						tracer.append("ACTION: check permission error!!!").append("\n");
					}
					logger.log(Level.SEVERE, "Send response failed.", ex);
					try {
						response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					} catch (Exception e) {
					}
				}
				return false;
			}
		} finally {
			if (setting.trace) {
				tracer.append("\n");
				tracerLogger.log(Level.INFO, tracer.toString()); 
			}
		}
	}
	
	public boolean checkPermission(HttpServletRequest request, HttpServletResponse response) {
		String user = null;
		String role = null;
		SecuritySetting setting = config.get();
		HttpSession session;
		if (setting.clientSession) {
			session = ClientSession.fromCookie(request, response);
			if (session != null && !((ClientSession)session).isExpired()) {
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
		public Map<String, RedirectionInfo> redirection;
		public String resType;
		public String resId;
		public String operation;
		public String scenario;
		public static class RedirectionURL {
			public RedirectionURL(String u, boolean k) {
				this.url = u;
				this.setReference = k;
			}
			public String url;
			public boolean setReference;
		}
		public RedirectionURL getRedirectionUrl(String role, String user) {
			for (Map.Entry<String,RedirectionInfo> r: redirection.entrySet()) {
				RedirectionInfo info = r.getValue();
				if (info.roles.contains("*") || 
						info.users.contains("*") ||
						(info.roles.contains("?") && role == null) ||
						(info.users.contains("?") && user == null) ||
						info.roles.contains(role) || 
						info.users.contains(user)) {
					return new RedirectionURL(r.getKey(), info.setReference);
				} 
			}
			return null;
		}
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
			ClientSession session = ClientSession.getSession(request, response);
			session.setMaxInactiveInterval(setting.sessionTimeout);
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
				if (session != null)
					session.delete();
			} else {
				HttpSession session = request.getSession();
				session.invalidate();
			}
		} catch (Exception ex) {
			logger.log(Level.WARNING, "Logout exception!.", ex);
		}
	}
	
	public static int getMaxSessionInactiveInterval() {
		if (instance == null)
			return 180;
		return instance.config.get().sessionTimeout;
	}
}
