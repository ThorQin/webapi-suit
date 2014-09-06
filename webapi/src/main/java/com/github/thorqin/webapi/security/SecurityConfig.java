/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.security;

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.utility.JsonConfig;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 *
 * @author nuo.qin
 */
public class SecurityConfig extends JsonConfig {
	public SecurityConfig(WebApplication application) throws IOException, URISyntaxException {
		super(application, "web.config", Config.class);
	}
	public static class Config {
		public SecuritySetting security;
		public Object db;
		public Object amq;
		public Object mail;
		public Object router;
	}

	@Override
	public SecuritySetting get() {
		return ((Config)configInstance).security;
	}
	
	public void set(SecuritySetting setting) {
		((Config)configInstance).security = setting;
	}
	
	public void buildMatcher() {
		SecuritySetting setting = get();
		for (SecuritySetting.URLMatcher urlMatcher : setting.matchers) {
			urlMatcher.build();
		}
	}
}
