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
public class AccessToken {
	public String accessToken;
	public String tokenType;
	public int expiresIn;
	public String refreshToken;
	public String scope;
}
