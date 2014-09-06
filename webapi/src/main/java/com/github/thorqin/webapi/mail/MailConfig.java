/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.thorqin.webapi.mail;

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.mail.MailConfig.MailSetting.MailSettingItem;
import com.github.thorqin.webapi.utility.JsonConfig;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

/**
 *
 * @author nuo.qin
 */
public class MailConfig extends JsonConfig {

	public static String SECURE_STARTTLS = "starttls";
	public static String SECURE_SSL = "ssl";
	public static String SECURE_NO = "no";

	public static class MailSetting {
		public static class MailSettingItem {
			public boolean trace = false;
			public boolean auth = true;
			public String host;
			public int port = 25;
			public String user;
			public String password;
			public String secure = "no";
			public String from;
		}
		public Map<String, MailSettingItem> mail;
	}
	
	private final String configName;
	
	public MailConfig(WebApplication application, String configName) throws IOException, URISyntaxException {
		super(application, "web.config", MailSetting.class);
		if (configName == null || configName.isEmpty())
			configName = "default";
		MailSetting setting = (MailSetting)this.configInstance;
		if (setting == null || setting.mail == null || !setting.mail.containsKey(configName))
			throw new RuntimeException("Mail config not exists!");
		this.configName = configName;
	}
	
	private MailSettingItem getInstance() {
		MailSetting setting = (MailSetting)this.configInstance;
		return setting.mail.get(configName);
	}
	
	public boolean enableTrace() {
		return getInstance().trace;
	}
	
	public String getHost() {
		return getInstance().host;
	}

	public Integer getPort() {
		return getInstance().port;
	}

	public String getUsername() {
		return getInstance().user;
	}

	public String getPassword() {
		return getInstance().password;
	}

	public String getFrom() {
		return getInstance().from;
	}

	public String getSecure() {
		return getInstance().secure;
	}
	
	public boolean useAuthentication() {
		return getInstance().auth;
	}
}
