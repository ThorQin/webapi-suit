/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.oauth2;

import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.thordev.webapi.Dispatcher;

/**
 *
 * @author nuo.qin
 */
public class OAuthServer {
	

	public static class AuthorizationCodeRequest {
		public String clientId;
		public String redirectUri;
		public String scope;
		public String state;
	}
	
	public static class AccessTokenByCodeRequest {
		public String clientId;
		public String clientSecret;
		public String redirectUri;
		public String code;
	}
	
	public static class RefreshTokenRequest {
		public String clientId;
		public String clientSecret;
		public String refreshToken;
		public String scope;
	}
	
	public static String makeAuthorizationSuccessUri(
			String redirectionUri, String code, String state) throws IOException {
		StringBuilder result = new StringBuilder(redirectionUri);
		if (redirectionUri.contains("?"))
			result.append("&code=");
		else
			result.append("?code=");
		result.append(URLEncoder.encode(code, "utf-8"));
		if (state != null)
			result.append("&state=").append(URLEncoder.encode(state, "utf-8"));
		return result.toString();
	}
	
	public static void redirectAuthorizationSuccess(HttpServletResponse response, 
			String redirectionUri, String code, String state) throws IOException {
		response.sendRedirect(makeAuthorizationSuccessUri(redirectionUri, code, state));
	}
	
	public static String makeAuthorizationFailedUri(
			String redirectionUri, OAuthError error, String errorDescription, 
			String errorUri, String state) throws IOException {
		StringBuilder result = new StringBuilder(redirectionUri);
		if (redirectionUri.contains("?"))
			result.append("&error=");
		else
			result.append("?error=");
		result.append(error.toString().toLowerCase());
		if (errorDescription != null)
			result.append("&error_description=").append(URLEncoder.encode(errorDescription, "utf-8"));
		if (errorUri != null)
			result.append("&error_uri=").append(URLEncoder.encode(errorUri, "utf-8"));
		if (state != null)
			result.append("&state=").append(URLEncoder.encode(state, "utf-8"));
		return result.toString();
	}
	
	public static void redirectAuthorizationFailed(HttpServletResponse response, 
			String redirectionUri, OAuthError error, String errorDescription, 
			String errorUri, String state) throws IOException {
		response.sendRedirect(makeAuthorizationFailedUri(
				redirectionUri, error, errorDescription, errorUri, state));
	}
	
	public static String getResponseType(HttpServletRequest request) {
		return request.getParameter("response_type");
	}
	
	public static String getGrantType(HttpServletRequest request) {
		return request.getParameter("grant_type");
	}
	
	/**
	 * @see OAuthClient#redirectAuthorization
	 * @param request
	 * @return Authorization Code Request information
	 */
	public static AuthorizationCodeRequest getAuthorizationCodeRequest(HttpServletRequest request) {
		if (getResponseType(request).equalsIgnoreCase("code")) {
			AuthorizationCodeRequest codeRequest = new AuthorizationCodeRequest();
			codeRequest.clientId = request.getParameter("client_id");
			codeRequest.redirectUri = request.getParameter("redirect_uri");
			codeRequest.scope = request.getParameter("scope");
			codeRequest.state = request.getParameter("state");
			return codeRequest;
		} else
			return null;
	}
	
	private static String getBasicAuth(String authorization) {
		String[] parts = authorization.split("\\s+");
		if (parts.length < 2 || !parts[0].equalsIgnoreCase("Basic"))
			return null;
		else
			return parts[1];
	}
	
	
	public static AccessTokenByCodeRequest getAccessTokenByCodeRequest(HttpServletRequest request) {
		if (getGrantType(request).equalsIgnoreCase("authorization_code")) {
			AccessTokenByCodeRequest codeRequest = new AccessTokenByCodeRequest();
			codeRequest.clientId = request.getParameter("client_id");
			codeRequest.redirectUri = request.getParameter("redirect_uri");
			String auth = request.getHeader("Authorization");
			if (auth != null) {
				auth = getBasicAuth(auth);
				if (auth == null) {
					codeRequest.clientId = null;
					codeRequest.clientSecret = null;
				} else {
					auth =  new String(Base64.decodeBase64(auth));
					String[] parts = auth.split(":");
					if (parts.length < 2) {
						codeRequest.clientId = null;
						codeRequest.clientSecret = null;
					} else {
						codeRequest.clientId = parts[0];
						codeRequest.clientSecret = parts[1];
					}
				}
			} else {
				codeRequest.clientSecret = request.getParameter("client_secret");
			}
			codeRequest.code = request.getParameter("code");
			return codeRequest;
		} else
			return null;
	}
	
	public static RefreshTokenRequest getRefreshTokenRequest(HttpServletRequest request) {
		if (getGrantType(request).equalsIgnoreCase("authorization_code")) {
			RefreshTokenRequest codeRequest = new RefreshTokenRequest();
			codeRequest.clientId = request.getParameter("client_id");
			codeRequest.scope = request.getParameter("scope");
			String auth = request.getHeader("Authorization");
			if (auth != null) {
				auth = getBasicAuth(auth);
				if (auth == null) {
					codeRequest.clientId = null;
					codeRequest.clientSecret = null;
				} else {
					auth =  new String(Base64.decodeBase64(auth));
					String[] parts = auth.split(":");
					if (parts.length < 2) {
						codeRequest.clientId = null;
						codeRequest.clientSecret = null;
					} else {
						codeRequest.clientId = parts[0];
						codeRequest.clientSecret = parts[1];
					}
				}
			} else {
				codeRequest.clientSecret = request.getParameter("client_secret");
			}
			codeRequest.refreshToken = request.getParameter("refresh_token");
			return codeRequest;
		} else
			return null;
	}
	
	public static void responseAccessTokenByCodeSuccess(HttpServletResponse response, 
			String accessToken, int expiresIn, String refreshToken, String scope) {
		AccessToken token = new AccessToken();
		token.accessToken = accessToken;
		token.expiresIn = expiresIn;
		token.refreshToken = refreshToken;
		token.scope = scope;
		token.tokenType = "Bearer";
		Dispatcher.sendJson(response, HttpServletResponse.SC_OK, token);
	}
	
	public static void responseAccessTokenByCodeFailed(HttpServletResponse response, 
			OAuthError error, String errorDescription, String errorUri) {
		ErrorResponse authError = new ErrorResponse();
		authError.error = error.toString().toLowerCase();
		authError.errorDescription = errorDescription;
		authError.errorUri = errorUri;
		Dispatcher.sendJson(response, HttpServletResponse.SC_BAD_REQUEST, authError);
	}
	
	
	public static void responseGetResourceFailed(HttpServletResponse response,
			OAuthError error, String errorDescription, String errorUri) {
		String headContent = "Bearer ";
		headContent += "error=\"" + error.toString().toLowerCase() + "\"";
		if (errorDescription != null)
			headContent += "error_description=\"" + errorDescription + "\"";
		if (errorUri != null)
			headContent += "error_uri=\"" + errorUri + "\"";
		response.setHeader("WWW-Authenticate", headContent);
		
		switch (error) {
			case INVALID_REQUEST:
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				break;
			case UNAUTHORIZED_CLIENT:
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				break;
			case ACCESS_DENIED: 
			case UNSUPPORTED_RESPONSE_TYPE:
			case INVALID_SCOPE:
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				break;
			case SERVER_ERROR:
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				break;
			case TEMPORARILY_UNAVAILABLE:
				response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				break;
		}
	}
}
