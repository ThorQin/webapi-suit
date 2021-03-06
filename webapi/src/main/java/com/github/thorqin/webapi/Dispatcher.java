package com.github.thorqin.webapi;

import com.github.thorqin.webapi.amq.AMQ;
import com.github.thorqin.webapi.amq.AMQService;
import com.github.thorqin.webapi.amq.annotation.AMQInstance;
import com.github.thorqin.webapi.annotation.App;
import com.github.thorqin.webapi.annotation.Entity;
import static com.github.thorqin.webapi.annotation.Entity.ParseEncoding.EITHER;
import static com.github.thorqin.webapi.annotation.Entity.ParseEncoding.HTTP_FORM;
import static com.github.thorqin.webapi.annotation.Entity.ParseEncoding.JSON;
import com.github.thorqin.webapi.annotation.Entity.SourceType;
import com.github.thorqin.webapi.annotation.UrlParam;
import com.github.thorqin.webapi.annotation.WebCleanup;
import com.github.thorqin.webapi.annotation.WebEntry;
import com.github.thorqin.webapi.annotation.WebEntry.HttpMethod;
import com.github.thorqin.webapi.annotation.WebModule;
import com.github.thorqin.webapi.annotation.WebStartup;
import com.github.thorqin.webapi.database.DBService;
import com.github.thorqin.webapi.database.annotation.DBInstance;
import com.github.thorqin.webapi.database.annotation.DBInterface;
import com.github.thorqin.webapi.mail.MailService;
import com.github.thorqin.webapi.mail.annotation.MailInstance;
import com.github.thorqin.webapi.monitor.MonitorService;
import com.github.thorqin.webapi.monitor.RequestInfo;
import com.github.thorqin.webapi.security.ClientSession;
import com.github.thorqin.webapi.security.WebSecurityManager;
import com.github.thorqin.webapi.utility.JsonConfig;
import com.github.thorqin.webapi.utility.RuleMatcher;
import com.github.thorqin.webapi.utility.Serializer;
import com.github.thorqin.webapi.validation.ValidateException;
import com.github.thorqin.webapi.validation.Validator;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public final class Dispatcher extends HttpServlet {
	

	private static final long serialVersionUID = -4658671798328062327L;
	private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());
	private DispatcherSetting.RouterInfo routerInfo;
	
	protected class MappingInfo {
		public Object instance;
		public Method method;
		public Map<String, Integer> parameters;
		public Pattern pattern;
	}
	
	private WebApplication application = null;
	private RuleMatcher<MappingInfo> mapping = null;
	private List<MappingInfo> startups = null;
	private List<MappingInfo> cleanups = null;
	//private Publisher publisher = null;
	//private static final AtomicInteger referenceCount = new AtomicInteger(0);
	
	Dispatcher(WebApplication application) {
		super();
		this.application = application;
	}
	
	public WebApplication getApplication() {
		return this.application;
	}
	
	/**
	 * Get the path of related to the current class path
	 * @param relativePath
	 * @return URI of the path
	 * @throws java.net.URISyntaxException
	 */
	public static URI getClassPath(String relativePath) throws URISyntaxException {
		return Dispatcher.class.getResource(relativePath).toURI();
	}
	

	
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		try {
			routerInfo = ((DispatcherSetting)(
					new JsonConfig(application, 
							"web.config", DispatcherSetting.class).get())).router;
		} catch (IOException | URISyntaxException ex) {
			routerInfo = new DispatcherSetting.RouterInfo();
		}
		MonitorService.addRef();
		if (mapping == null) {
			try {
				makeApiMapping(config.getServletContext());
			} catch (Exception ex) {
				throw new ServletException("Initialize dispatcher servlet error.", ex);
			}
		}
		if (startups != null && startups.size() > 0) {
			for (MappingInfo info : startups) {
				try {
					info.method.invoke(info.instance);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
					logger.log(Level.SEVERE, "Invoke startup failed", ex);
				}
			}
			startups.clear();
		}
		
		//referenceCount.incrementAndGet();
	}

	@Override
	public void destroy() {
//		int refCount = referenceCount.decrementAndGet();
//		if (refCount <= 0) {
//			
//		}
		// Servlet destroy
		if (cleanups != null && cleanups.size() > 0) {
			for (MappingInfo info : cleanups) {
				try {
					info.method.invoke(info.instance);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
					logger.log(Level.SEVERE, "Invoke cleanup failed", ex);
				}
			}
			cleanups.clear();
		}
		MonitorService.release();
		super.destroy();
	}

	public void crossSiteOptions(HttpServletRequest request,
			HttpServletResponse response) {
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Credentials", "true");
		response.setHeader("Access-Control-Allow-Methods",
				"GET,POST,PUT,DELETE,HEAD,OPTIONS");
		response.setHeader("Access-Control-Allow-Headers",
				"Content-Type,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control");
	}
	
	private Object addStartup(Class<?> clazz, Method method, Object inst) {
		if (method.getParameterTypes().length == 0) {
			try {
				if (inst == null) {
					inst = clazz.newInstance();
				}
				MappingInfo info = new MappingInfo();
				info.instance = inst;
				info.method = method;
				method.setAccessible(true);
				startups.add(info);
			} catch (IllegalAccessException | InstantiationException | SecurityException ex) {
				logger.log(Level.SEVERE,
						"New instance failed: " + clazz.getName() + "." + method.getName(), ex);
			}
		} else {
			logger.log(Level.WARNING, 
					"Startup method ''{0}.{1}'' parameters more then 0, ignored.",
					new Object[]{clazz.getName(), method.getName()});
		}
		return inst;
	}
	
	private Object addCleanup(Class<?> clazz, Method method, Object inst) {
		if (method.getParameterTypes().length == 0) {
			try {
				if (inst == null) {
					inst = clazz.newInstance();
				}
				MappingInfo info = new MappingInfo();
				info.instance = inst;
				info.method = method;
				method.setAccessible(true);
				cleanups.add(info);
			} catch (IllegalAccessException | InstantiationException | SecurityException ex) {
				logger.log(Level.SEVERE,
						"New instance failed: " + clazz.getName() + method.getName(), ex);
			}
		} else {
			logger.log(Level.WARNING, 
					"cleanup method ''{0}.{1}'' parameters more then 0, ignored.",
					new Object[]{clazz.getName(), method.getName()});
		}
		return inst;
	}
	
	private WebEntry checkMethodParametersAndAnnotation(Class<?> clazz, Method method) {
		if (!method.isAnnotationPresent(WebEntry.class))
			return null;
		WebEntry entry = method.getAnnotation(WebEntry.class);
		if (entry.method().length <=0 )
			return null;
		Class<?>[] parameterTypes = method.getParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		PARAMETER_CHECK:
		for (int i = 0; i < parameterTypes.length; i++) {
			Class<?> paramType = parameterTypes[i];
			if (paramType.equals(HttpServletRequest.class) ||
					paramType.equals(HttpServletResponse.class) ||
					paramType.equals(HttpSession.class) ||
					paramType.equals(ClientSession.class)) {
				continue;
			}
			Annotation[] annotations = parameterAnnotations[i];
			if (annotations != null) {
				for (Annotation annotation : annotations) {
					if (annotation instanceof Entity ||
							annotation instanceof UrlParam) {
						continue PARAMETER_CHECK;
					}
				}
			}
			logger.log(Level.WARNING,
				"Method ''{0}.{1}'' has unknow parameter type: {2}, method ignored.",
				new Object[]{clazz.getName(), method.getName(), paramType.getName()});
			return null;
		}
		return entry;
	}
	
	

	private void analyzeClass(Class<?> clazz) throws Exception {
		if (!clazz.isAnnotationPresent(WebModule.class)) {
			return;
		}
		WebModule classAnno = clazz.getAnnotation(WebModule.class);
		boolean crossSite = classAnno.crossSite();
		String path = clazz.getAnnotation(WebModule.class).path();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (!path.endsWith("/")) {
			path += "/";
		}
		Object inst;
		inst = clazz.newInstance();
		
		for (Field field : clazz.getDeclaredFields()) {
			Class<?> fieldType = field.getType();
			DBInstance db = field.getAnnotation(DBInstance.class);
			AMQInstance amq = field.getAnnotation(AMQInstance.class);
			MailInstance mail = field.getAnnotation(MailInstance.class);
			App app = field.getAnnotation(App.class);
			if (app != null) {
				if (WebApplication.class.isAssignableFrom(fieldType)) {
					field.setAccessible(true);
					field.set(inst, application);
				}
			} else if (db != null) {
				if (fieldType.equals(DBService.class)) {
					field.setAccessible(true);
					field.set(inst, application.getDBService(db.value()));
					continue;
				}
				if (!fieldType.isInterface())
					continue;
				if (!fieldType.isAnnotationPresent(DBInterface.class))
					continue;
				field.setAccessible(true);
				Object proxyInstance = application.getDBProxy(db.value(), fieldType);
				field.set(inst, proxyInstance);
			} else if (amq != null) { 
				if (fieldType.equals(AMQ.class)) {
					field.setAccessible(true);
					field.set(inst, application.getAMQ(amq.value()));
				} else if (fieldType.equals(AMQService.class)) {
					field.setAccessible(true);
					field.set(inst, new AMQService(application.getAMQ(amq.value())));
				} else {
					if (!fieldType.isInterface())
						continue;
					field.setAccessible(true);
					Object proxyInstance = application.getAMQProxy(amq.value(), fieldType);
					field.set(inst, proxyInstance);
				}
			} else if (mail != null) {
				if (fieldType.equals(MailService.class)) {
					field.setAccessible(true);
					field.set(inst, application.getMailService(mail.value()));
				}
			}
		}
		
		for (Method method : clazz.getDeclaredMethods()) {
			if (startups != null && method.isAnnotationPresent(WebStartup.class)) {
				addStartup(clazz, method, inst);
			} else if (cleanups != null && method.isAnnotationPresent(WebCleanup.class)) {
				addCleanup(clazz, method, inst);
			}
			if (mapping == null) {
				continue;
			}
			WebEntry entry = checkMethodParametersAndAnnotation(clazz, method);
			if (entry == null)
				continue;
			String name = entry.name();
			if (name.isEmpty()) {
				name = method.getName();
			} else if (name.equals("/")) {
				name = "";
			} else if (name.startsWith("/")) {
				name = name.substring(1);
			}
			Map<String, Integer> paramInfo = new HashMap<>();
			String fullPath = RuleMatcher.formatUrlRule(path + name, paramInfo);
			if (crossSite || entry.crossSite()) {
				String key = "^" + HttpMethod.OPTIONS + ":" + fullPath.substring(1);
				System.out.println("Add Mapping: " + key);
				MappingInfo info = new MappingInfo();
				info.instance = this;
				info.method = this.getClass().getMethod("crossSiteOptions",
						HttpServletRequest.class, HttpServletResponse.class);
				info.method.setAccessible(true);
				info.parameters = paramInfo;
				info.pattern = Pattern.compile(key);
				mapping.addRule(key, info);
			}
			String methodPrefix = "";
			for (HttpMethod httpMethod : entry.method()) {
				if (!methodPrefix.isEmpty())
					methodPrefix += "|";
				methodPrefix += httpMethod;
			}
			methodPrefix = "^(" + methodPrefix + ")";
			String key = methodPrefix + ":" + fullPath.substring(1);
			System.out.println("Add Mapping: " + key);
			MappingInfo info = new MappingInfo();
			info.instance = inst;
			info.method = method;
			info.parameters = paramInfo;
			method.setAccessible(true);
			info.pattern = Pattern.compile(key);
			mapping.addRule(key, info);
		}
	}

	private void scanClasses(File path) throws Exception {
		if (path == null) {
			return;
		}
		if (path.isDirectory()) {
			for (File item : path.listFiles()) {
				scanClasses(item);
			}
			return;
		}
		else if (!path.isFile() || !path.getName().endsWith(".class")) {
			return;
		}
		try (DataInputStream fstream = new DataInputStream(new FileInputStream(path.getPath()))){
			ClassFile cf = new ClassFile(fstream);
			String className = cf.getName();
			AnnotationsAttribute visible = (AnnotationsAttribute) cf.getAttribute(
					AnnotationsAttribute.visibleTag);
			if (visible == null) {
				return;
			}
			for (javassist.bytecode.annotation.Annotation ann : visible.getAnnotations()) {
				if (ann.getTypeName().equals(WebModule.class.getName())) {
					Class<?> clazz = Class.forName(className);
					if (clazz == null) {
						continue;
					}
					analyzeClass(clazz);
				}
			}
		}
	}

	private synchronized void makeApiMapping(ServletContext context) throws Exception {
		if (mapping != null) {
			return;
		}
		mapping = new RuleMatcher<>();
		startups = new LinkedList<>();
		cleanups = new LinkedList<>();
		File file = new File(getClassPath("/"));
		scanClasses(file);
		mapping.build();
	}

	private String readHttpBody(HttpServletRequest request) {
		try {
			InputStream is = request.getInputStream();
			if (is != null) {
				Writer writer = new StringWriter();
				char[] buffer = new char[1024];
				try {
					String encoding = request.getCharacterEncoding();
					if (encoding == null) {
						encoding = "UTF-8";
					}
					Reader reader = new BufferedReader(
							new InputStreamReader(is, encoding));
					int n;
					while ((n = reader.read(buffer)) != -1) {
						writer.write(buffer, 0, n);
					}
				} catch (IOException ex) {
					logger.log(Level.WARNING,
							"Read http body failed: ", ex);
				}
				return writer.toString();
			} else {
				return "";
			}
		} catch (IOException e) {
			return "";
		}
	}
	
	private Object parseFromBody(Class<?> paramType, Entity annoEntity, MethodRuntimeInfo mInfo) {
		try {
			if ((annoEntity.encoding() == JSON || annoEntity.encoding() == EITHER) && 
					mInfo.postType == RequestPostType.JSON) {
				return Serializer.fromJson(mInfo.httpBody, paramType);
			} else if ((annoEntity.encoding() == HTTP_FORM || annoEntity.encoding() == EITHER) && 
					mInfo.postType == RequestPostType.HTTP_FORM) {
				return Serializer.fromUrlEncoding(mInfo.httpBody, paramType);
			} else {
				logger.log(Level.WARNING, 
					"WARNING: Cannot deserialize class ''{0}'' from HTTP body: Unsupported post encoding.", paramType.getName());
				return null;
			}
		} catch (IOException | ClassCastException | IllegalAccessException | InstantiationException ex) {
			logger.log(Level.WARNING, 
					"WARNING: Cannot deserialize class ''{0}'' from HTTP body.", paramType.getName());
			return null;
		}
	}
	
	private Object parseFromQueryString(Class<?> paramType, Entity annoEntity, MethodRuntimeInfo mInfo) {
		try {
			return Serializer.fromUrlEncoding(mInfo.request.getQueryString(), paramType);
		} catch (UnsupportedEncodingException | IllegalAccessException | InstantiationException ex) {
			logger.log(Level.WARNING, 
					"Warnning: Cannot deserialize class ''{0}'' from QueryString.", paramType.getName());
			return null;
		}
	}
	
	private Object convertUrlParam(String val, Class<?> paramType, String paramName) throws ValidateException {
		if (paramType.equals(String.class))
			return val;
		else if (paramType.equals(Integer.class) || paramType.equals(int.class)) {
			try {
				return Integer.valueOf(val);
			} catch (NumberFormatException err) {
				throw new ValidateException("Invalid URL parameter '" + paramName + "': Need an integer value");
			}
		} else if (paramType.equals(Long.class) || paramType.equals(long.class)) {
			try {
				return Long.valueOf(val);
			} catch (NumberFormatException err) {
				throw new ValidateException("Invalid URL parameter '" + paramName + "': Need an long integer value");
			}
		} else if (paramType.equals(Short.class) || paramType.equals(short.class)) {
			try {
				return Short.valueOf(val);
			} catch (NumberFormatException err) {
				throw new ValidateException("Invalid URL parameter '" + paramName + "': Need an short integer value");
			}
		} else if (paramType.equals(Byte.class) || paramType.equals(byte.class)) {
			try {
				return Byte.valueOf(val);
			} catch (NumberFormatException err) {
				throw new ValidateException("Invalid URL parameter '" + paramName + "': Need an byte value");
			}
		} else if (paramType.equals(Float.class) || paramType.equals(float.class)) {
			try {
				return Float.valueOf(val);
			} catch (NumberFormatException err) {
				throw new ValidateException("Invalid URL parameter '" + paramName + "': Need an float value");
			}
		} else if (paramType.equals(Double.class) || paramType.equals(double.class)) {
			try {
				return Double.valueOf(val);
			} catch (NumberFormatException err) {
				throw new ValidateException("Invalid URL parameter '" + paramName + "': Need an double value");
			}
		} else if (paramType.equals(Boolean.class) || paramType.equals(boolean.class)) {
			try {
				return Boolean.valueOf(val);
			} catch (Exception err) {
				throw new ValidateException("Invalid URL parameter '" + paramName + "': Need an boolean value");
			}
		} else
			throw new ValidateException("Invalid URL parameter '" + paramName + "': Cannot translate to specified parameter type!");
	}

	private Object makeParam(
			Class<?> paramType, 
			Annotation[] annos,
			MethodRuntimeInfo mInfo) throws ValidateException {
		if (paramType.equals(HttpServletRequest.class)) {
			return mInfo.request;
		} else if (paramType.equals(HttpServletResponse.class)) {
			return mInfo.response;
		} else if (paramType.equals(HttpSession.class)) {
			return mInfo.request.getSession();
		} else if (paramType.equals(ClientSession.class)) {
			if (mInfo.clientSession == null)
				mInfo.clientSession = ClientSession.newSession(mInfo.request, mInfo.response);
			return mInfo.clientSession;
		} else {
			for (Annotation ann : annos) {
				if (ann instanceof UrlParam) {
					UrlParam annParam = (UrlParam)ann;
					String paramName = annParam.value();
					if (mInfo.urlParams.containsKey(paramName)) {
						String val = mInfo.urlParams.get(paramName);
						Object obj = convertUrlParam(val, paramType, paramName);
						Validator validator = new Validator();
						validator.validate(obj, paramType, annos);
						return obj;
					} else
						return null;
				} else if (ann instanceof Entity) {
					Entity annoEntity = (Entity)ann;
					Object param = null;
					if (annoEntity.source() == SourceType.HTTP_BODY ) {
						param = parseFromBody(paramType, annoEntity, mInfo);
					} else if (annoEntity.source() == SourceType.QUERY_STRING) {
						param = parseFromQueryString(paramType, annoEntity, mInfo);
					} else if (annoEntity.source() == SourceType.EITHER) {
						param = parseFromBody(paramType, annoEntity, mInfo);
						if (param == null)
							param = parseFromQueryString(paramType, annoEntity, mInfo);
					}
					Validator validator = new Validator();
					validator.validate(param, paramType, annos);
					return param;
				}
			}
			return null;
		}
	}
	
	private static enum RequestPostType {
		JSON,
		HTTP_FORM,
		UNKNOW
	}
	
	private static class MethodRuntimeInfo {
		public RequestPostType postType;
		public String httpBody;
		public HttpServletRequest request;
		public HttpServletResponse response;
		public ClientSession clientSession;
		public Map<String, String> urlParams = new HashMap<>();
	}

	private boolean dispatch(HttpServletRequest request,
			HttpServletResponse response)
			throws ServletException, IOException {
		String httpMethod = request.getMethod().toUpperCase();
		String requestPath = request.getServletPath();
		if (request.getPathInfo() != null) {
			requestPath += request.getPathInfo();
		}
		String key = httpMethod + ":" + requestPath;
		MappingInfo info = mapping.match(key);
		if (info == null) {
			return false;
		}
		// Handler has been found then route the input request to appropriate routine to do a further processing.
		long beginTime = System.currentTimeMillis();
		try {
			MethodRuntimeInfo mInfo = new MethodRuntimeInfo();
			mInfo.request = request;
			mInfo.response = response;
			// Extract URL's parameters which like '/{user}/{id}' form into a hash map.
			mInfo.urlParams = new HashMap<>();
			if (info.parameters.size() > 0) {
				Matcher matcher = info.pattern.matcher(key);
				if (!matcher.find())
					return false;
				int gCount = matcher.groupCount();
				for (String k : info.parameters.keySet()) {
					int idx = info.parameters.get(k);
					if (idx <= gCount) {
						mInfo.urlParams.put(k, matcher.group(idx));
					}
				}
			}
			
			boolean postJson = (request.getContentType() != null && 
					request.getContentType().split(";")[0].equalsIgnoreCase("application/json") || 
					request.getContentType() == null && 
					request.getMethod().equalsIgnoreCase("POST"));
			boolean postForm = (request.getContentType() != null
				&& request.getContentType().split(";")[0].equalsIgnoreCase("application/x-www-form-urlencoded"));
			if (postJson)
				mInfo.postType = RequestPostType.JSON;
			else if (postForm)
				mInfo.postType = RequestPostType.HTTP_FORM;
			else
				mInfo.postType = RequestPostType.UNKNOW;
			if (mInfo.postType != RequestPostType.UNKNOW)
				mInfo.httpBody = readHttpBody(request);
			// Obtain Client Session From Cookie
			mInfo.clientSession = ClientSession.fromCookie(request, response);
			if (mInfo.clientSession != null) {
				if (mInfo.clientSession.isExpired()) {
					mInfo.clientSession = null;
				} else
					mInfo.clientSession.touch();
			}
			WebEntry entryAnno = info.method.getAnnotation(WebEntry.class);
			if (entryAnno != null && entryAnno.crossSite()) {
				response.setHeader("Access-Control-Allow-Origin", "*");
				response.setHeader("Access-Control-Allow-Credentials", "true");
				response.setHeader("Access-Control-Allow-Methods",
						"GET,POST,OPTIONS");
				response.setHeader("Access-Control-Allow-Headers",
						"Content-Type,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control");
			}
			Object inst = info.instance;
			Class<?>[] params = info.method.getParameterTypes();
			Annotation[][] annos = info.method.getParameterAnnotations();
			List<Object> realParameters = new ArrayList<>(params.length);
			for (int i = 0; i < params.length; i++) {
				realParameters.add(makeParam(params[i], annos[i], mInfo));
			}
			Object result = info.method.invoke(inst, realParameters.toArray());
			ClientSession s = ClientSession.existSession(request);
			if (s != null && !s.isSaved())
				s.save();
			if (!info.method.getReturnType().equals(Void.class) && 
					!info.method.getReturnType().equals(void.class) && 
					(entryAnno != null && entryAnno.toJson() || result != null)) {
				sendJson(response, result);
			}
			return true;
		} catch (ValidateException ex) {
			send(response, HttpServletResponse.SC_BAD_REQUEST, "Bad request: invalid parameters!");
			logger.log(Level.WARNING, "Bad request: {0}", ex.getMessage());
			return true;
		} catch (HttpException ex) {
			if (ex.getMessage() != null) {
				if (ex.getJsonObject() != null)
					sendJson(response, ex.getHttpStatus(), ex.getJsonObject());
				else if (ex.isJsonString())
					sendJsonString(response, ex.getHttpStatus(), ex.getMessage());
				else
					send(response, ex.getHttpStatus(), ex.getMessage());
			} else
				send(response, ex.getHttpStatus());
			if (ex.getCause() != null)
				logger.log(Level.WARNING, ex.getMessage(), ex.getCause());
			else
				logger.log(Level.WARNING, ex.getMessage());
			return true;
		} catch (InvocationTargetException ex) {
			Throwable realEx = ex.getTargetException();
			if (HttpException.class.isInstance(realEx)) {
				HttpException httpEx = (HttpException)realEx;
				if (httpEx.getMessage() != null) {
					if (httpEx.getJsonObject() != null)
						sendJson(response, httpEx.getHttpStatus(), httpEx.getJsonObject());
					else if (httpEx.isJsonString())
						sendJsonString(response, httpEx.getHttpStatus(), httpEx.getMessage());
					else
						send(response, httpEx.getHttpStatus(), httpEx.getMessage());
				} else
					send(response, httpEx.getHttpStatus());
				if (httpEx.getCause() != null)
					logger.log(Level.WARNING, httpEx.getMessage(), httpEx.getCause());
				else
					logger.log(Level.WARNING, httpEx.getMessage());
			} else {
				send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error!");
				logger.log(Level.SEVERE, "Error processing", ex);
			}
			return true;
		} catch (Exception ex) {
			send(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Server error!");
			logger.log(Level.SEVERE, "Error processing", ex);
			return true;
		} finally {
			if (routerInfo.trace) {
				WebSecurityManager.LoginInfo loginInfo =  application.getSecurityManager()
						.getLoginInfo((HttpServletRequest)request, (HttpServletResponse)response);
				RequestInfo reqInfo = MonitorService.buildRequestInfo(
						request, response, loginInfo, "Dispatcher", beginTime);
				MonitorService.record(reqInfo);
			}
		}
	}

	@Override
	protected void doHead(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!dispatch(request, response)) {
			super.doHead(request, response);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!dispatch(request, response)) {
			super.doGet(request, response);
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!dispatch(request, response)) {
			super.doPost(request, response);
		}
	}

	@Override
	protected void doDelete(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!dispatch(request, response)) {
			super.doDelete(request, response);
		}
	}

	@Override
	protected void doOptions(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!dispatch(request, response)) {
			super.doOptions(request, response);
		}
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!dispatch(request, response)) {
			super.doPut(request, response);
		}
	}

	@Override
	protected void doTrace(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		if (!dispatch(request, response)) {
			super.doTrace(request, response);
		}
	}

	public static void httpException(Exception ex, Integer status) throws HttpException {
		Throwable e = ex;
		Throwable cause;
		while ((cause = e.getCause()) != null)
			e = cause;
		throw new HttpException(status, e.getMessage(), ex);
	}
	
	public static void httpException(Exception ex) throws HttpException {
		httpException(ex, 400);
	}
	
	public static void send(HttpServletResponse response, Integer status, String message) {
		response.setStatus(status);
		send(response, message);
	}

	public static void send(HttpServletResponse response, String message) {
		response.setContentType("text/plain");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-store");
		response.setDateHeader("Expires", 0);
		try (Writer w = response.getWriter();) {
			if (message != null)
				w.write(message);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Send message to client failed!", ex);
		}
	}
	
	public static void sendJsonString(HttpServletResponse response, Integer status, String jsonString) {
		response.setStatus(status);
		sendJsonString(response, jsonString);
	}
	
	public static void sendJsonString(HttpServletResponse response, String jsonString) {
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-store");
		response.setDateHeader("Expires", 0);
		try (Writer w = response.getWriter();) {
			w.write(jsonString);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Send message to client failed!", ex);
		}
	}

	public static void send(HttpServletResponse response, Integer status) {
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-store");
		response.setDateHeader("Expires", 0);
		response.setStatus(status);
	}

	public static void sendJson(HttpServletResponse response, Integer status, Object obj) {
		response.setStatus(status);
		sendJson(response, obj);
	}

	public static void sendJson(HttpServletResponse response, Object obj) {
		response.setContentType("application/json");
		response.setCharacterEncoding("utf-8");
		response.setHeader("Pragma", "no-cache");
		response.setHeader("Cache-Control", "no-store");
		response.setDateHeader("Expires", 0);
		try (Writer w = response.getWriter();){
			Serializer.toJson(obj, w);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Send message to client failed!", ex);
		}
	}
}
