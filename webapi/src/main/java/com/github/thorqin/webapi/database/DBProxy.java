/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.database;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import com.github.thorqin.webapi.database.DBService.DBOut;
import com.github.thorqin.webapi.database.DBService.DBRef;
import com.github.thorqin.webapi.database.DBService.DBSession;
import com.github.thorqin.webapi.database.annotation.DBInterface;
import com.github.thorqin.webapi.database.annotation.DBProcedure;
import com.github.thorqin.webapi.utility.StringUtil;

/**
 *
 * @author nuo.qin
 */
public class DBProxy implements InvocationHandler {
	private final DBService store;
	public DBProxy(DBService store) {
		this.store = store;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class<?> classInterface = proxy.getClass().getInterfaces()[0];
		DBInterface dbInterface = classInterface.getAnnotation(DBInterface.class);
		String prefix = "";
		if (dbInterface != null) {
			prefix = dbInterface.procPrefix();
		}
		String procName;
		DBProcedure dbProc = method.getAnnotation(DBProcedure.class);
		if (dbProc != null) {
			procName = dbProc.value();
		} else {
			procName = (prefix.isEmpty() ? "" : prefix + "_") + StringUtil.toDBName(method.getName());
		}
		Class<?>[] paramTypes = method.getParameterTypes();
		try (DBSession session = store.getSession()) {
			session.setAutoCommit(false);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					Class<?> superClass = paramTypes[i].getSuperclass();
					if (args[i] == null && superClass != null && superClass.equals(DBOut.class)) {
						args[i] = paramTypes[i].newInstance();
					} else if ( args[i] == null && superClass != null && superClass.equals(DBRef.class)) {
						args[i] = paramTypes[i].newInstance();
					}
				}
			}
			if (method.getReturnType().equals(void.class) || method.getReturnType().equals(Void.class)) {
				session.perform(procName, args);
				session.commit();
				return null;
			} else {
				Object result = session.invoke(procName, method.getReturnType(), args);
				session.commit();
				return result;
			}
		}
	}
	
}
