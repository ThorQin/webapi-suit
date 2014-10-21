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

package com.github.thorqin.webapi;

import com.github.thorqin.webapi.amq.AMQ;
import com.github.thorqin.webapi.amq.AMQProxy;
import com.github.thorqin.webapi.database.DBProxy;
import com.github.thorqin.webapi.database.DBService;
import com.github.thorqin.webapi.database.DBTranscationFactory;
import com.github.thorqin.webapi.mail.MailService;
import com.github.thorqin.webapi.monitor.MonitorService;
import com.github.thorqin.webapi.security.WebSecurityManager;
import com.github.thorqin.webapi.utility.JsonConfig;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.jms.JMSException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import org.apache.activemq.ConfigurationException;

/**
 *
 * @author nuo.qin
 */
public abstract class WebApplication implements ServletContextListener {

	private ServletContext servletContext;
	private String dataDirectory = null;
	private Dispatcher dispatcher = null;
	private SecurityFilter securityFilter = null;
	private final Map<String, DBService> dbMapping = new HashMap<>();
	private final Map<String, Object> dbProxyMapping = new HashMap<>();
	private final Map<String, AMQ> amqMapping = new HashMap<>();
	private final Map<String, Object> amqProxyMapping = new HashMap<>();
	private final Map<String, MailService> mailServiceMapping = new HashMap<>();

	/**
	 * If 
	 * @param servletContext
	 * @throws ServletException 
	 */
	final void onStartup(ServletContext servletContext) throws ServletException {
		this.servletContext = servletContext;
		this.onStartup();
	}
	
	@Override
	public final void contextInitialized(ServletContextEvent sce) {
	}

	@Override
	public final void contextDestroyed(ServletContextEvent sce) {
		try {
			for (Map.Entry<String, DBService> db: dbMapping.entrySet()) {
				db.getValue().close();
			}
			for (Map.Entry<String, AMQ> amq: amqMapping.entrySet()) {
				amq.getValue().stop();
			}
			for (Map.Entry<String, MailService> mail: mailServiceMapping.entrySet()) {
				mail.getValue().stop();
			}
		} catch (Exception ex) {
		}
		this.onShutdown();
	}

	
	public abstract void onStartup();
	
	public abstract void onShutdown();
	
	public final Dispatcher getDispatcher() {
		if (dispatcher == null)
			dispatcher = new Dispatcher(this);
		return dispatcher;
	}
	final SecurityFilter getSecurityFilter() {
		if (securityFilter == null)
			securityFilter = new SecurityFilter(this);
		return securityFilter;
	}
	
	public final WebSecurityManager getSecurityManager() {
		return WebSecurityManager.getInstance(this);
	}
	
	public final String getDataPath() throws MalformedURLException, URISyntaxException {
		if (dataDirectory != null) {
			return dataDirectory;
		} else {
			File dataPath = new File(servletContext.getResource("/WEB-INF/").toURI());
			
			dataPath = new File(dataPath.getAbsolutePath() + "/data");
			dataPath.mkdirs();
			return dataPath.getAbsolutePath();
		}
	}
	
	public final String getDataPath(String subDir) throws MalformedURLException, URISyntaxException {
		String baseDir = getDataPath();
		if (subDir.startsWith("/") || subDir.startsWith("\\")) {
			if (baseDir.endsWith("/") || baseDir.endsWith("\\"))
				return baseDir + subDir.substring(1);
			else
				return baseDir + subDir;
		} else {
			if (baseDir.endsWith("/") || baseDir.endsWith("\\"))
				return baseDir + subDir;
			else
				return baseDir + "/" + subDir;
		}
	}
	
	public final File getValidConfigFile(String configFile) throws URISyntaxException {
		try {
			String path = getDataPath(configFile);
			File config = new File(path);
			if (config.exists() && config.canRead()) {
				return config;
			} else {
				return new File(WebApplication.class.getClassLoader().getResource(configFile).toURI());
			}
		} catch (Exception ex) {
			return new File(WebApplication.class.getClassLoader().getResource(configFile).toURI());
		}
	}
	
	public final <T> T getConfig(String configFile, String key, Class<T> type) throws IOException, URISyntaxException {
		JsonConfig config = new JsonConfig(this, configFile, Map.class);
		Map<String, Object> map = (Map<String, Object>)config.get();
		return (T)map.get(key);
	}
	
	public final synchronized void setDataPath(String directory) {
		dataDirectory = directory;
	}
	
	public final synchronized MailService getMailService(String configName) {
		if (mailServiceMapping.containsKey(configName))
			return mailServiceMapping.get(configName);
		else {
			MailService service = new MailService(this, configName);
			service.start();
			mailServiceMapping.put(configName, service);
			return service;
		}
	}

	public final synchronized DBService getDBService(String configName) {
		if (dbMapping.containsKey(configName))
			return dbMapping.get(configName);
		else {
			DBService db = new DBService(this, configName);
			dbMapping.put(configName, db);
			return db;
		}
	}
	
	public final MonitorService getMonitorService() {
		return MonitorService.getInstance();
	}
	
	public final synchronized AMQ getAMQ(String configName) throws JMSException, IOException, ConfigurationException, URISyntaxException {
		if (amqMapping.containsKey(configName))
			return amqMapping.get(configName);
		else {
			AMQ amq = new AMQ(this, configName);
			amqMapping.put(configName, amq);
			return amq;
		}
	}
	
	@SuppressWarnings("unchecked")
	public final synchronized <T> T getAMQProxy(String configName, Class<T> interfaceType) throws JMSException, IOException, ConfigurationException, URISyntaxException {
		String key = configName + ":" + interfaceType.getName();
		if (amqProxyMapping.containsKey(key)) {
			return (T)amqProxyMapping.get(key);
		} else {
			Object instance = Proxy.newProxyInstance(
					Dispatcher.class.getClassLoader(),
					new Class<?>[] { interfaceType }, 
					new AMQProxy(getAMQ(configName)) );
			amqProxyMapping.put(key, instance);
			return (T)instance;
		}
	}
	
	@SuppressWarnings("unchecked")
	public final synchronized <T> T getDBProxy(String configName, Class<T> interfaceType) throws SQLException, IOException, RuntimeException, URISyntaxException {
		String key = configName + ":" + interfaceType.getName();
		if (dbProxyMapping.containsKey(key)) {
			return (T)dbProxyMapping.get(key);
		} else {
			Object instance = Proxy.newProxyInstance(
					WebApplication.class.getClassLoader(),
					new Class<?>[] { interfaceType, DBTranscationFactory.class }, 
					new DBProxy(getDBService(configName)) );
			dbProxyMapping.put(key, instance);
			return (T)instance;
		}
	}	
}
