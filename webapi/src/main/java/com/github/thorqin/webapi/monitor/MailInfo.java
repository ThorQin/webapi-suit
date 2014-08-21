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
public class MailInfo {
	public String smtpServer;
	public String smtpUser;
	public String sender;
	public String[] recipients;
	public String subject;
	public long startTime;
	public long runningTime;
}
