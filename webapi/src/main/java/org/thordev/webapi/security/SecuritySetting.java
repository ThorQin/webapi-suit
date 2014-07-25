/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.thordev.webapi.security;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpSession;
import org.thordev.webapi.utility.RuleMatcher;

/**
 *
 * @author nuo.qin
 */
public class SecuritySetting {
	public enum RuleAction {
		allow,
		deny,
		check_db
	}
	
	public static class URLMatcher {
		public String name;
		public String description;
		public String url;
		public Set<String> scheme;
		public Set<String> domain;
		public Set<String> method;
		public Set<String> port;
		public Map<String, Set<String>> sessionVariables = new HashMap<>();
		// Mapping to 
		public String resType;
		public String resId;
		public String operation;
		public String scenario;
		// Actions
		public Map<String, String> redirection = new HashMap<>();
		
		// For internal use, created when after call build() method.
		private Pattern pattern;
		private Map<String, Integer> parameters;

		public void build() {
			parameters = new HashMap<>();
			String urlRegexp = RuleMatcher.formatUrlRule(url, parameters);
			pattern = Pattern.compile(urlRegexp);
		}

		public boolean match(String url, String scheme, String domain, String method, int port, HttpSession session, Map<String, String> urlParams) {
			if (!(this.scheme == null || this.scheme.isEmpty() || this.scheme.contains(scheme))) {
				return false;
			}
			if (!(this.domain == null || this.domain.isEmpty() || this.domain.contains(domain))) {
				return false;
			}
			if (!(this.method == null || this.method.isEmpty() || this.method.contains(method))) {
				return false;
			}
			if (!(this.port == null || this.port.isEmpty() || this.port.contains(String.valueOf(port)))) {
				return false;
			}
			for (String key : sessionVariables.keySet()) {
				Set<String> values = sessionVariables.get(key);
				String value = session.getAttribute(key).toString();
				if (value == null)
					return false;
				if (!values.contains(value))
					return false;
			}
			Matcher matcher = pattern.matcher(url);
			if (!matcher.find()) {
				return false;
			}
			if (urlParams != null) {
				urlParams.clear();
				int gCount = matcher.groupCount();
				for (String k : parameters.keySet()) {
					int idx = parameters.get(k);
					if (idx <= gCount) {
						urlParams.put(k, matcher.group(idx));
					}
				}
			}
			return true;
		}
	}
	public List<URLMatcher> matchers = new LinkedList<>();

	public static class Rule {

		public String name;
		public String description;
		public String resType;
		public Set<String> resId;
		public Set<String> operation;
		public Set<String> user;
		public Set<String> role;
		public Set<String> scenario;
		
		public RuleAction action;

		public boolean match(String resType, String resId, String operation, String user, String role, String scenario) {
			if (this.resType == null)
				return false;
			if (!this.resType.equals("*")) {
				if (!(resType.equalsIgnoreCase(this.resType)))
					return false;
				if (!(this.resId == null || this.resId.isEmpty() || this.resId.contains(resId)))
					return false;
			}
			if (!(this.operation == null || this.operation.isEmpty() || this.operation.contains(operation)))
				return false;
			if (!(this.scenario == null || this.scenario.isEmpty() || this.scenario.contains(scenario)))
				return false;
			if (this.user != null && (this.user.contains("*") || this.user.contains(user))) {
				return true;
			} else 
				return this.role != null && (this.role.contains("*") || this.role.contains(role));
		}
	}
	public List<Rule> rules = new LinkedList<>();
	/**
	 * In seconds ...
	 */
	public int sessionTimeout = 180;
	public boolean defaultAllow = false;
	public boolean clientSession = true;
	public String amqConfig;
	public String dbConfig;
}
