/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.amq;

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.amq.AMQConfig.AMQSetting.AMQSettingItem;
import com.github.thorqin.webapi.utility.JsonConfig;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

public class AMQConfig extends JsonConfig {
	public static class AMQSetting {
		public static class AMQSettingItem {
			public boolean trace = false;
			public String uri;
			public String user;
			public String password;
			public String address = "default";
			public boolean broadcast = false;
		}
		public Map<String, AMQSettingItem> amq;
	}
	private final String configName;
	public AMQConfig(WebApplication application, String configName) throws IOException, URISyntaxException {
		super(application, "web.config", AMQSetting.class);
		if (configName == null || configName.isEmpty())
			configName = "default";
		AMQSetting setting = (AMQSetting)this.configInstance;
		if (setting == null || setting.amq == null || !setting.amq.containsKey(configName))
			throw new RuntimeException("ActiveMQ config not exists!");
		this.configName = configName;
	}
	private AMQSettingItem getInstance() {
		AMQSetting setting = (AMQSetting)this.configInstance;
		return setting.amq.get(configName);
	}
	
	public boolean enableTrace() {
		return getInstance().trace;
	}
	
	public String getActiveMQUri() {
		return getInstance().uri;
	}
	public String getActiveMQUser() {
		return getInstance().user;
	}
	public String getActiveMQPassword() {
		return getInstance().password;
	}
	public String getDefaultAddress() {
		return getInstance().address;
	}
	public boolean getDefaultBroadcast() {
		return getInstance().broadcast;
	}	
}