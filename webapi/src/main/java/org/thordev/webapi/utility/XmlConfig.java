/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.utility;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;

/**
 *
 * @author nuo.qin
 */
public class XmlConfig {
	private Document doc = null;
	private ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
	public XmlConfig(String configFile) {
		if (configFile == null || configFile.isEmpty())
			configFile = "config";
		if (!configFile.toLowerCase().endsWith(".xml"))
			configFile += ".xml";
		try (InputStream in = XmlConfig.class.getClassLoader().getResourceAsStream(configFile)){
			if (in != null) {
				try {
					DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					doc = builder.parse(in);
				} catch (Exception e) {
				}
			}
		} catch (Exception ex) {
		}
	}
	public String get(String key) {
		if (doc == null)
			return null;
		else {
			key = "/" + key;
			if (map.containsKey(key))
				return map.get(key);
			String value = null;
			try {
				XPath xPath = XPathFactory.newInstance().newXPath();
				value = xPath.evaluate(key, doc.getDocumentElement());
			} catch (XPathExpressionException e) {
				System.err.print("Get config field error.");
			}
			map.put(key, value);
			if (value == null || value.trim().isEmpty())
				return null;
			else
				return value.trim();
		}
	}
	
	public String get(String key, String defValue) {
		String val = get(key);
		if (val == null)
			return defValue;
		else
			return val;
	}
	
	public boolean getBoolean(String key, boolean defValue) {
		String val = get(key);
		if (val == null)
			return defValue;
		else {
			try {
				return Boolean.parseBoolean(val);
			} catch (Exception ex) {
				return defValue;
			}
		}
	}
	
	public int getInteger(String key, int defValue) {
		String val = get(key);
		if (val == null)
			return defValue;
		else {
			try {
				return Integer.parseInt(val);
			} catch (Exception ex) {
				return defValue;
			}
		}
	}
			
	public long getLong(String key, long defValue) {
		String val = get(key);
		if (val == null)
			return defValue;
		else {
			try {
				return Long.parseLong(val);
			} catch (Exception ex) {
				return defValue;
			}
		}
	}
	
	public float getFloat(String key, float defValue) {
		String val = get(key);
		if (val == null)
			return defValue;
		else {
			try {
				return Float.parseFloat(val);
			} catch (Exception ex) {
				return defValue;
			}
		}
	}
	
	public double getDouble(String key, double defValue) {
		String val = get(key);
		if (val == null)
			return defValue;
		else {
			try {
				return Double.parseDouble(val);
			} catch (Exception ex) {
				return defValue;
			}
		}
	}
}
