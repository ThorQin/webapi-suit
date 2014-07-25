package org.thordev.webapi.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;
import org.apache.commons.codec.binary.Base64;
import org.thordev.webapi.utility.Serializer;

/**
 * This tool class can let developer easy to save session data to client by cookie
 * because client cookie have 4k size store limit, so we shouldn't save must data to the session.
 * @author nuo.qin
 */
public class ClientSession implements HttpSession {
	private final static String sessionName = "webapiSession";
	private final static String keyCode = "kyj1JEkLQ/5To0AF81vlmA==";
	private final static Logger logger = Logger.getLogger(ClientSession.class.getName());
	private static Encryptor enc = null;
	
	private HttpServletRequest request = null;
	private HttpServletResponse response = null;
	private Map<String, Object> values = null;
	private boolean isSaved = false;
	private boolean isNew = true;
			
	static {
		try {
			enc = new Encryptor(importKey(), "AES");
		} catch (Exception ex) {
			enc = null;
			logger.log(Level.SEVERE, "Initialize ClientSession failed!", ex);
		}
	}
	
	private static byte[] importKey() {
		String path = ClientSession.class.getClassLoader().getResource("/").getFile();
		path += "server.key";
		try {
			return Files.readAllBytes(new File(path).toPath());
		} catch (IOException ex) {
			logger.log(Level.WARNING, "server.key does not exists in classes folder, use default key instead.");
			return Base64.decodeBase64(keyCode);
		}
	}
	
	public static ClientSession newSession(HttpServletRequest request, HttpServletResponse response) {
		ClientSession inst = new ClientSession(request, response);
		request.setAttribute("org.thordev.webapi.security.ClientSession", inst);
		return inst;
	}
	
	public static ClientSession existSession(HttpServletRequest request) {
		Object obj = request.getAttribute("org.thordev.webapi.security.ClientSession");
		if (obj != null)
			return (ClientSession)obj;
		else
			return null;
	}
	
	public static ClientSession fromCookie(HttpServletRequest request, HttpServletResponse response) {
		Object obj = request.getAttribute("org.thordev.webapi.security.ClientSession");
		if (obj != null)
			return (ClientSession)obj;
		Cookie[] cookies = request.getCookies();
		if (cookies == null)
			return null;
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(sessionName)) {
				try {
					ClientSession inst = new ClientSession(request, response, cookie.getValue());
					request.setAttribute("org.thordev.webapi.security.ClientSession", inst);
					return inst;
				} catch (Exception ex) {
					logger.log(Level.SEVERE, "Session verify failed", ex);
				}
			}
		}
		return null;
	}
	
	private ClientSession(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
		this.isSaved = false;
		values = new HashMap<>();
		values.put("sid", java.util.UUID.randomUUID().toString().replace("-", ""));
		long now = new Date().getTime();
		values.put("timestamp", now);
		values.put("creationTime", now);
		this.isNew = true;
	}
	
	private ClientSession(HttpServletRequest request, HttpServletResponse response, String data) throws Exception {
		if (enc == null)
			throw new Exception("ClientSession not ready!");
		this.request = request;
		this.response = response;
		this.isSaved = false;
		values = Serializer.fromKryo(enc.decrypt(Base64.decodeBase64(data)));
		this.isNew = false;
	}
	
	@Override
	public String getId() {
		return (String)getAttribute("sid");
	}
	

	
	/**
	 * Set session item
	 * @param key
	 * @param value 
	 */
	@Override
	public void setAttribute(String key, Object value) {
		if (key.equals("timestamp") && !(value instanceof Long))
			return;
		if (key.equals("sid"))
			return;
		if (key.equals("creationTime"))
			return;
		values.put(key, value);
		touch();
	}
	
	public String getString(String key) {
		return (String)getAttribute(key);
	}
	
	public Integer getInteger(String key) {
		return (Integer)getAttribute(key);
	}

	@Override
	public Object getAttribute(String key) {
		Object value = values.get(key);
		return value;
	}

	@Override
	public void removeAttribute(String key) {
		if (key.equals("sid") || key.equals("timestamp") || key.equals("creationTime"))
			return;
		values.remove(key);
		touch();
	}
	/**
	 * Update session timestamp to now
	 */
	public void touch() {
		values.put("timestamp", new Date().getTime());
		this.isSaved = false;
	}
	
	public boolean isSaved() {
		return this.isSaved;
	}
	
	/**
	 * Export session data to base64 string
	 * @return Base64 encoded string
	 */
	@Override
	public String toString() {
		try {
			return Base64.encodeBase64String(enc.encrypt(Serializer.toKryo(values)));
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Export session failed", ex);
			return null;
		}
	}
	
	public void save(String path, String domain, Integer maxAge, boolean httpOnly, boolean secureOnly) {
		try {
			touch();
			String sessionContent = toString();
			if (sessionContent == null)
				return;
			Cookie cookie = new Cookie(sessionName, sessionContent);
			if (maxAge != null)
				cookie.setMaxAge(maxAge);
			if (domain != null && !domain.isEmpty())
				cookie.setDomain(domain);
			if (path != null && !path.isEmpty())
				cookie.setPath(path);
			cookie.setHttpOnly(httpOnly);
			cookie.setSecure(secureOnly);
			cookie.setVersion(1);
			if (response != null)
				response.addCookie(cookie);
			this.isSaved = true;
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Save client session failed!", ex);
		}
	}
	
	public void save(String path, String domain) {
		save(path, domain, null, false, false);
	}
	
	public void save(String path) {
		save(path, null, null, false, false);
	}
	
	private static String getRootPath(HttpServletRequest req) {
		String path = req.getContextPath();
		if (path == null || path.isEmpty())
			return "/";
		else
			return path;
	}
	
	public void save() {
		save(getRootPath(request), null, null, false, false);
	}
	
	public void delete(String path, String domain) {
		save(path, domain, 0, false, false);
	}
	public void delete(String path) {
		save(path, null, 0, false, false);
	}
	/**
	 * Remove session cookie from client browser
	 * (same with call 'save' function and pass 0 to maxAge parameter)
	 */
	public void delete() {
		invalidate();
		save(getRootPath(request), null, 0, false, false);
	}

	@Override
	public long getCreationTime() {
		return (Long)getAttribute("creationTime");
	}

	@Override
	public long getLastAccessedTime() {
		return (Long)getAttribute("timestamp");
	}

	@Override
	public ServletContext getServletContext() {
		return this.request.getServletContext();
	}

	@Override
	public void setMaxInactiveInterval(int interval) {
		setAttribute("maxInactiveInterval", (Integer)interval);
	}

	@Override
	public int getMaxInactiveInterval() {
		return (Integer)getAttribute("maxInactiveInterval");
	}

	@Override
	public HttpSessionContext getSessionContext() {
		return null;
	}

	@Override
	public Object getValue(String name) {
		return getAttribute(name);
	}
	
	public static class IteratorEnumeration<E> implements Enumeration<E> {
		private final Iterator<E> iterator;
		public IteratorEnumeration(Iterator<E> iterator){
			this.iterator = iterator;
		}
		@Override
		public E nextElement() {
			return iterator.next();
		}
		@Override
		public boolean hasMoreElements() {
			return iterator.hasNext();
		}
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return new IteratorEnumeration(values.keySet().iterator());
	}

	@Override
	public String[] getValueNames() {
		return (String[])values.keySet().toArray();
	}

	@Override
	public void putValue(String name, Object value) {
		setAttribute(name, value);
	}

	@Override
	public void removeValue(String name) {
		removeAttribute(name);
	}

	@Override
	public void invalidate() {
		List<String> deleteKeys = new LinkedList<>();
		for (String key : values.keySet()) {
			if (!key.equals("sid"))
				deleteKeys.add(key);
		}
		for (String key : deleteKeys) {
			values.remove(key);
		}
		touch();
		this.isSaved = false;
	}

	@Override
	public boolean isNew() {
		return this.isNew;
	}
}
