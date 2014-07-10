/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.oauth2;

/**
 *
 * @author nuo.qin
 */
public class OAuthException extends Exception {
	private String error = null;
	private String errorDescription = null;
	private String errorUri = null;

	public OAuthException(String error) {
		super(error);
		this.error = error;
	}

	public OAuthException(String error, String errorDescription) {
		super(error);
		this.error = error;
		this.errorDescription = errorDescription;
	}

	public OAuthException(String error, String errorDescription, String errorUri) {
		super(error);
		this.error = error;
		this.errorDescription = errorDescription;
		this.errorUri = errorUri;
	}

	public String getError() {
		return error;
	}

	public String getErrorDescription() {
		return errorDescription;
	}

	public String getErrorUri() {
		return errorUri;
	}
	
}
