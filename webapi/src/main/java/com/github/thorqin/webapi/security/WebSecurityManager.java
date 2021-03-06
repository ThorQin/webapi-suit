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

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.amq.AMQ;
import com.github.thorqin.webapi.database.annotation.DBInterface;
import com.github.thorqin.webapi.security.WebSecurityManager.MappingInfo.RedirectionURL;
import static com.github.thorqin.webapi.security.SecuritySetting.RuleAction.allow;
import static com.github.thorqin.webapi.security.SecuritySetting.RuleAction.deny;
import com.github.thorqin.webapi.security.SecuritySetting.URLMatcher.RedirectionInfo;
import com.github.thorqin.webapi.utility.Serializer;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jms.JMSException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.activemq.ConfigurationException;


/**
 *
 * @author nuo.qin
 */
public final class WebSecurityManager {
	private static final Logger logger = Logger.getLogger(WebSecurityManager.class.getName());
	private static WebSecurityManager instance = null;
	private final SecurityConfig config;
	private final WebApplication application;
	private AMQ amq;
	private AMQ.AsyncReceiver receiver;
	
	private final AMQ.MessageHandler handler = new AMQ.MessageHandler () {
		@Override
		public void onMessage(AMQ.IncomingMessage message) {
			try {
				updateSetting((SecuritySetting)message.getBody(SecuritySetting.class));
			} catch (IOException ex) {
				logger.log(Level.SEVERE, "Receive dynamic configuration failed!.", ex);
			}
		}
	};
	
	private synchronized void updateSetting(SecuritySetting setting) throws IOException {
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

	private WebSecurityManager(WebApplication application) {
		this.application = application;
		try {
			config = new SecurityConfig(application);
			load();
		} catch (IOException | URISyntaxException | SQLException ex) {
			throw new ServiceConfigurationError("Initialize WebSecruityManager failed.", ex);
		}
	}
	
	public void load() throws RuntimeException, URISyntaxException, SQLException, IOException {
		SecuritySetting setting = config.get();
		if (setting.dbConfig != null && !setting.dbConfig.isEmpty()) {
			SecurityAPI api = application.getDBProxy(setting.dbConfig, SecurityAPI.class);
			SecurityConfig aSetting = Serializer.fromJson(api.loadSetting(), SecurityConfig.class);
			config.set(aSetting);
			config.save();
		}
		config.buildMatcher();
	}
	
	public static synchronized WebSecurityManager getInstance(WebApplication application) {
		if (instance != null)
			return instance;
		else {
			WebSecurityManager inst = new WebSecurityManager(application);
			instance = inst;
			return inst;
		}
	}
	
	public void createReceiver() throws IOException, ConfigurationException, URISyntaxException {
		if (receiver != null) {
			receiver.close();
		}
		SecuritySetting setting = config.get();
		if (setting.amqConfig != null && !setting.amqConfig.isEmpty()) {
			try {
				amq = application.getAMQ(setting.amqConfig);
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
	
	public SecuritySetting getSetting() {
		return config.get();
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
						SecurityAPI api = application.getDBProxy(setting.dbConfig, SecurityAPI.class);
						boolean result = api.checkPermission(resType, resId, operation, user, role, scenario);
						return new CheckPermissionResult(result, rule.name);
					} catch (SQLException | IOException | RuntimeException | URISyntaxException ex) {
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
		
		MappingInfo info = mappingURL(path, 
				request.getScheme().toLowerCase(), request.getServerName().toLowerCase(), 
				request.getMethod().toLowerCase(), request.getServerPort(), session);
		CheckPermissionResult result = checkPermission(info, user, role);
		if (result.allow) {
			return true;
		} else {
			try {
				if (info.redirection != null) {
					RedirectionURL redirectUrl = info.getRedirectionUrl(role, user);
					if (redirectUrl != null) {
						String url = redirectUrl.url;
						if (redirectUrl.setReference) {
							if (url.contains("?")) {
								url += "&ref=" + URLEncoder.encode(request.getContextPath() + path, "utf-8");
							} else {
								url += "?ref=" + URLEncoder.encode(request.getContextPath() + path, "utf-8");
							}
						}
						if (url.startsWith("$")) {
							url = request.getContextPath() + url.substring(1);
						}

						response.sendRedirect(url);
					} else {
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
					}
				} else {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
				}
			} catch (IOException ex) {
				logger.log(Level.SEVERE, "Send response failed.", ex);
				try {
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				} catch (Exception e) {
				}
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
	
	public static class LoginInfo {
		public String user = null;
		public String role = null;
	}
	
	public LoginInfo getLoginInfo(HttpServletRequest request, HttpServletResponse response) {
		LoginInfo info = new LoginInfo();
		SecuritySetting setting = config.get();
		HttpSession session;
		if (setting.clientSession) {
			session = ClientSession.fromCookie(request, response);
			if (session != null && !((ClientSession)session).isExpired()) {
				info.user = (String)session.getAttribute("user");
				info.role = (String)session.getAttribute("role");
			}
		} else {
			session = request.getSession();
			info.user = (String)session.getAttribute("user");
			info.role = (String)session.getAttribute("role");
		}
		return info;
	}
	
	public void login(HttpServletRequest request, HttpServletResponse response, String user, String role) {
		SecuritySetting setting = config.get();
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
	
	public void logout(HttpServletRequest request, HttpServletResponse response) {
		try {
			SecuritySetting setting = config.get();
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
