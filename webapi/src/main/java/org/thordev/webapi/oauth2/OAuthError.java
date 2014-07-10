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
public enum OAuthError {
	INVALID_REQUEST, 
	UNAUTHORIZED_CLIENT, 
	ACCESS_DENIED, 
	UNSUPPORTED_RESPONSE_TYPE, 
	INVALID_SCOPE, 
	SERVER_ERROR, 
	TEMPORARILY_UNAVAILABLE
}
