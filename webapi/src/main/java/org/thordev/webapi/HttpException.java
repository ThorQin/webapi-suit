/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi;

/**
 *
 * @author nuo.qin
 */
public class HttpException extends RuntimeException {
	private final int httpStatus;
	
	public HttpException(int httpStatus) {
		super("Http Code: " + httpStatus);
		this.httpStatus = httpStatus;
	}
	
	public HttpException(int httpStatus, String message) {
		super(message);
		this.httpStatus = httpStatus;
	}
	
	public int getHttpStatus() {
		return httpStatus;
	}
}
