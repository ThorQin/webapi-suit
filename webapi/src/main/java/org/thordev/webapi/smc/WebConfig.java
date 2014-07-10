/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.smc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.thordev.webapi.amq.AMQConfig;
import org.thordev.webapi.database.DBConfig;
import org.thordev.webapi.mail.MailConfig;
import org.thordev.webapi.security.SecuritySetting;
import org.thordev.webapi.utility.JsonConfig;

/**
 *
 * @author nuo.qin
 */
public class WebConfig extends JsonConfig {

	public static class WebSetting {
		public Map<String, DBConfig.DBSetting.DBSettingItem> db = new HashMap<>();
		public Map<String, AMQConfig.AMQSetting.AMQSettingItem> amq = new HashMap<>();
		public Map<String, MailConfig.MailSetting.MailSettingItem> mail = new HashMap<>();
		public SecuritySetting security = new SecuritySetting();
	}
	
	public WebConfig(String configFile) throws IOException {
		super(configFile, WebSetting.class);
	}
	
	public WebConfig() throws InstantiationException, IllegalAccessException {
		super(WebSetting.class);
	}
	
	public SecuritySetting getSecurity() {
		return ((WebSetting)this.configInstance).security;
	}
	
	public Map<String, DBConfig.DBSetting.DBSettingItem> getDB() {
		return ((WebSetting)this.configInstance).db;
	}
	
	public Map<String, AMQConfig.AMQSetting.AMQSettingItem> getAmq() {
		return ((WebSetting)this.configInstance).amq;
	}
	
	public Map<String, MailConfig.MailSetting.MailSettingItem> getMail() {
		return ((WebSetting)this.configInstance).mail;
	}
}
