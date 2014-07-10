/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thordev.webapi.utility;

import java.io.IOException;

/**
 *
 * @author nuo.qin
 */
public class JsonConfig {

	protected final Class<?> type;
	protected Object configInstance = null;
	protected final String configFile;

	private static String modifyFileName(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			fileName = "web";
		}
		if (!fileName.toLowerCase().endsWith(".config")) {
			fileName += ".config";
		}
		return fileName;
	}

	public JsonConfig(String configFile, Class<?> type) throws IOException {
		this.configFile = configFile;
		this.type = type;
		load(configFile);
	}
	
	public JsonConfig(String configFile, boolean fromResource, Class<?> type) throws IOException {
		this.type = type;
		if (fromResource) {
			this.configFile =  JsonConfig.class.getClassLoader().getResource(configFile).toString();
			loadResource(configFile);
		} else {
			this.configFile = configFile;
			load(configFile);
		}
	}

	public JsonConfig(Class<?> type) throws InstantiationException, IllegalAccessException {
		this.configFile = null;
		this.type = type;
		configInstance = type.newInstance();
	}

	public final void load(String configFile) throws IOException {
		configFile = modifyFileName(configFile);
		configInstance = Serializer.loadJsonFile(configFile, type);
	}
	
	public final void loadResource(String configFile) throws IOException {
		configFile = modifyFileName(configFile);
		configInstance = Serializer.loadJsonResource(configFile, type);
	}

	public Object get() {
		return configInstance;
	}

	public void set(Object config) {
		configInstance = config;
	}

	public static void save(Object config, String configFile) {
		configFile = modifyFileName(configFile);
		try {
			Serializer.saveJsonFile(config, configFile, true);
		} catch (Exception ex) {
		}
	}

	public void save() {
		save(configInstance, configFile);
	}

	public void save(String configFile) {
		save(configInstance, configFile);
	}
}
