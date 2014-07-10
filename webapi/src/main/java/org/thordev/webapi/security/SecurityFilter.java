/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.security;

import java.io.IOException;
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

	private Security security;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		boolean remoteSync = Boolean.getBoolean(filterConfig.getInitParameter("remoteSync"));
		try {
			security = Security.getInstance(remoteSync);
		} catch (Exception ex) {
			throw new ServletException("Initialize 'SecurityFilter' failed, cannot instance 'Sercurity' object.", ex);
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (security.checkPermission((HttpServletRequest)request, (HttpServletResponse)response))
			chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		security.close();
	}
	
}
