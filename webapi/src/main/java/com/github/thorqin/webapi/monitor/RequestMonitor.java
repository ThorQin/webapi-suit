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
public interface RequestMonitor extends Monitor {
	public void requestProcessed(RequestInfo info);
}
