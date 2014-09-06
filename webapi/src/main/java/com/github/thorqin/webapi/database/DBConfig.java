package com.github.thorqin.webapi.database;

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.database.DBConfig.DBSetting.DBSettingItem;
import com.github.thorqin.webapi.utility.JsonConfig;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class DBConfig extends JsonConfig {
	public static class DBSetting {
		public static class DBSettingItem {
			public boolean trace = false;
			public String driver;
			public String uri;
			public String user;
			public String password;
			public int minConnectionsPerPartition = 5;
			public int maxConnectionsPerPartition = 20;
			public int partitionCount = 1;
			public boolean defaultAutoCommit = true;
		}
		public Map<String, DBSettingItem> db;
	}
	
	private final String configName;
	
	public DBConfig(WebApplication application, String configName) throws IOException, RuntimeException, URISyntaxException {
		super(application, "web.config", DBSetting.class);
		if (configName == null || configName.isEmpty())
			configName = "default";
		DBSetting setting = (DBSetting)this.configInstance;
		if (setting == null || setting.db == null || !setting.db.containsKey(configName))
			throw new RuntimeException("Database config not exists!");
		this.configName = configName;
	}
	
	private DBSettingItem getInstance() {
		DBSetting setting = (DBSetting)this.configInstance;
		return setting.db.get(configName);
	}
	
	public boolean enableTrace() {
		return getInstance().trace;
	}
	
	public String getDBDriver() {
		return getInstance().driver;
	}
	public String getDBUri() {
		return getInstance().uri;
	}
	public String getDBUser() {
		return getInstance().user;
	}
	public String getDBPassword() {
		return getInstance().password;
	}
	public int getMinConnectionsPerPartition() {
		return getInstance().minConnectionsPerPartition;
	}
	public int getMaxConnectionsPerPartition() {
		return getInstance().maxConnectionsPerPartition;
	}
	public int getPartitionCount() {
		return getInstance().partitionCount;
	}
	public boolean getDefaultAutoCommit() {
		return getInstance().defaultAutoCommit;
	}	
	
}