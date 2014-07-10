/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thordev.webapi.mail;

import java.io.IOException;
import java.util.Map;
import org.thordev.webapi.mail.MailConfig.MailSetting.MailSettingItem;
import org.thordev.webapi.utility.JsonConfig;

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
	
	public MailConfig(String configName) throws IOException {
		super("web.config", true, MailSetting.class);
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
}
