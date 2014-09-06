/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.thorqin.webapi.utility;

import com.github.thorqin.webapi.WebApplication;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 *
 * @author nuo.qin
 */
public class JsonConfig {

	protected final Class<?> type;
	protected Object configInstance = null;
	protected final File configFile;

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
		this.configFile = new File(modifyFileName(configFile));
		this.type = type;
		load(this.configFile);
	}
	
	public JsonConfig(WebApplication application, String configFile, Class<?> type)
			throws IOException, URISyntaxException {
		this.type = type;
		this.configFile = application.getValidConfigFile(modifyFileName(configFile));
		load(this.configFile);
	}

	public JsonConfig(Class<?> type) throws InstantiationException, IllegalAccessException {
		this.configFile = null;
		this.type = type;
		configInstance = type.newInstance();
	}

	private void load(File configFile) throws IOException {
		configInstance = Serializer.loadJsonFile(configFile, type);
	}

	public Object get() {
		return configInstance;
	}

	public void set(Object config) {
		configInstance = config;
	}

	public static void save(Object config, File configFile) throws IOException {
		Serializer.saveJsonFile(config, configFile, true);
	}

	public void save() throws IOException {
		save(configInstance, configFile);
	}

	public void save(String configFile) throws IOException {
		save(configInstance, new File(configFile));
	}
}
