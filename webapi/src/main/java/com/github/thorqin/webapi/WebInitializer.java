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

import com.github.thorqin.webapi.annotation.Order;
import com.github.thorqin.webapi.annotation.UseDispatcher;
import com.github.thorqin.webapi.annotation.UseSecurity;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javassist.Modifier;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;


class OrderComparetor<T> implements Comparator<T> {

	@Override
	public int compare(T o1, T o2) {
		int i1, i2;
		Order order1 = o1.getClass().getAnnotation(Order.class);
		if (order1 == null)
			i1 = 2147483647;
		else
			i1 = order1.value();
			
		Order order2 = o2.getClass().getAnnotation(Order.class);
		if (order2 == null)
			i2 = 2147483647;
		else
			i2 = order2.value();
		return i1 - i2;
	}
	
}

/**
 *
 * @author nuo.qin
 */
@HandlesTypes(WebApplication.class)
public class WebInitializer implements ServletContainerInitializer {
	@Override
	public void onStartup(Set<Class<?>> initializerClasses, 
			ServletContext ctx) throws ServletException {
		List<WebApplication> applications = new LinkedList<>();
		if (initializerClasses != null) {
			for (Class<?> waiClass : initializerClasses) {
				if (!waiClass.isInterface()
						&& !Modifier.isAbstract(waiClass.getModifiers())
						&& WebApplication.class.isAssignableFrom(waiClass)) {
					try {
						applications.add((WebApplication) waiClass.newInstance());
					} catch (IllegalAccessException | InstantiationException ex) {
						throw new ServletException(
								"Failed to instantiate WebApplication class", ex);
					}
				}
			}
		}

		if (applications.isEmpty()) {
			ctx.log("No WebApplication types detected on classpath");
		} else {
			Collections.sort(applications, new OrderComparetor<WebApplication>());
			ctx.log("WebApplication detected on classpath: " + applications);
			int i = 0;
			for (WebApplication application : applications) {
				application.onStartup(ctx);
				// Adding dispatcher ....
				UseDispatcher dispatcherAnno = 
						application.getClass().getAnnotation(UseDispatcher.class);
				if (dispatcherAnno != null) {
					String[] pathList = dispatcherAnno.value();
					ServletRegistration.Dynamic servletRegistion = ctx.addServlet(
							"WebApiDispatcher" + i, application.getDispatcher());
					servletRegistion.setLoadOnStartup(0);
					servletRegistion.setAsyncSupported(true);
					int count = 0;
					for (String path : pathList) {
						if (path.trim().length() != 0) {
							servletRegistion.addMapping(path.trim());
							count++;
						}
					}
					if (count <= 0)
						servletRegistion.addMapping("/*");
				}
				// Adding security filter ...
				UseSecurity securityAnno = 
						application.getClass().getAnnotation(UseSecurity.class);
				if (securityAnno != null) {
					String[] pathList = securityAnno.value();
					FilterRegistration filterRegistion = ctx.addFilter(
							"WebApiSecurityFilter" + i, application.getSecurityFilter());
					List<String> validPath = new LinkedList<>();
					for (String path : pathList) {
						if (path.trim().length() != 0) {
							validPath.add(path.trim());
						}
					}
					pathList = validPath.toArray(pathList);
					filterRegistion.addMappingForUrlPatterns(null, true, pathList);
					if (pathList.length <= 0)
						filterRegistion.addMappingForUrlPatterns(null, true, "/*");
				}
				
				ctx.addListener(application);
			}
		}
	}

}
