/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.github.thorqin.webapi.monitor;

/**
 *
 * @author nuo.qin
 */
public class RequestInfo {
	public String recorder;
	public String url;
	public String method;
	public String referrerUrl;
	public String serverName;
	public String serverIP;
	public int serverPort;
	public String clientIP;
	public String userAgent;
	public String user;
	public String role;
	public long startTime;
	public long runningTime;
}
