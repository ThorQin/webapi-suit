/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.database;

import com.github.thorqin.webapi.WebApplication;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import com.github.thorqin.webapi.database.DBService.DBOut;
import com.github.thorqin.webapi.database.DBService.DBRef;
import com.github.thorqin.webapi.database.DBService.DBSession;
import com.github.thorqin.webapi.database.annotation.DBInterface;
import com.github.thorqin.webapi.database.annotation.DBProcedure;
import com.github.thorqin.webapi.utility.StringUtil;
import java.lang.reflect.Proxy;
import java.sql.SQLException;

/**
 *
 * @author nuo.qin
 */
public class DBProxy implements InvocationHandler {
	private final DBService store;
	private DBSession transcation;
	public DBProxy(DBService store) {
		this.store = store;
	}
	public DBProxy(DBService store, DBSession transcation) throws SQLException {
		this.store = store;
		this.transcation = transcation;
		this.transcation.setAutoCommit(false);
	}
	
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		Class<?> classInterface = proxy.getClass().getInterfaces()[0];
		if (method.getDeclaringClass().equals(DBTranscationFactory.class)) {
			if (method.getName().equals("DBTranscationFactory")) {
				Object instance = Proxy.newProxyInstance(
					proxy.getClass().getClassLoader(),
					new Class<?>[] { classInterface, DBTranscationFactory.class, DBTranscation.class }, 
					new DBProxy(store, store.getSession()) );
				return instance;
			}
			return null;
		} else if (method.getDeclaringClass().equals(DBTranscation.class)) {
			switch (method.getName()) {
				case "commit":
					this.transcation.commit();
					return null;
				case "rollback":
					this.transcation.rollback();
					return null;
				case "getSession":
					return this.transcation;
			}
			return null;
		}

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
		if (this.transcation != null) {
			return invokeCall(args, paramTypes, method, transcation, procName);
		} else {
			try (DBSession session = store.getSession()) {
				session.setAutoCommit(false);
				return invokeCall(args, paramTypes, method, session, procName);
			}
		}
	}

	private Object invokeCall(Object[] args, Class<?>[] paramTypes, Method method, final DBSession session, String procName) throws SQLException, InstantiationException, IllegalAccessException {
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
