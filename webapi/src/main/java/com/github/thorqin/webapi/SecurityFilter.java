/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi;

import com.github.thorqin.webapi.monitor.MonitorService;
import com.github.thorqin.webapi.monitor.RequestInfo;
import com.github.thorqin.webapi.security.WebSecurityManager;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.SQLException;
import javax.jms.JMSException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author nuo.qin
 */
public class SecurityFilter implements Filter {

	private final WebApplication application;
	private WebSecurityManager security;
	
	SecurityFilter(WebApplication application) {
		this.application = application;
	}
	
	public WebApplication getApplication() {
		return application;
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		MonitorService.addRef();
		security = WebSecurityManager.getInstance(application);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		long beginTime = System.currentTimeMillis();
		if (security.checkPermission((HttpServletRequest)request, (HttpServletResponse)response))
			chain.doFilter(request, response);
		if (security.getSetting().trace) {
			WebSecurityManager.LoginInfo loginInfo = security.getLoginInfo((HttpServletRequest)request, 
					(HttpServletResponse)response);
			RequestInfo reqInfo = MonitorService.buildRequestInfo((HttpServletRequest)request, 
					(HttpServletResponse)response, loginInfo, "Security Manager", beginTime);
			MonitorService.record(reqInfo);
		}
	}

	@Override
	public void destroy() {
		security.close();
		MonitorService.release();
	}
	
}
