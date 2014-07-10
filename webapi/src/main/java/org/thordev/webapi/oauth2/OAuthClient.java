/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.oauth2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.binary.Base64;
import org.thordev.webapi.utility.Serializer;

/**
 *
 * @author nuo.qin
 */
public class OAuthClient {
	
	/**
	 * Client Get Authorization Code
	 * @param authorityServerUri
	 * @param clientId
	 * @param redirectUri Authorization-Server will redirect user's browser to this URI with authorization-code. 
	 * (optional, null means to use client pre-configurated value, but if no this configuration existed or 
	 * have more than one configuration existing then must provide this parameter.)
	 * @param scope Resource scope (optional, null means to default scope)
	 * @param state Used to avoid cross-site request forgery (optional, can be null)
	 * @return An URI which to let user's browser navigate to authorization server to obtain authority-code
	 * @throws UnsupportedEncodingException 
	 */
	public static String makeAuthorizationUri(String authorityServerUri, 
			String clientId, String redirectUri, String scope, String state) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder(authorityServerUri);
		if (authorityServerUri.contains("?"))
			result.append("&response_type=code");
		else
			result.append("?response_type=code");
		result.append("&client_id=").append(URLEncoder.encode(clientId, "utf-8"));
		if (redirectUri != null)
			result.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "utf-8"));
		if (scope != null)
			result.append("&scope=").append(URLEncoder.encode(scope, "utf-8"));
		if (state != null)
			result.append("&state=").append(URLEncoder.encode(state, "utf-8"));
		return result.toString();
	}
	
	/**
	 * Redirect user's browser to authority server to obtain authorization code
	 * @throws java.io.UnsupportedEncodingException
	 * @see #makeAuthorizationUri
	 * @see OAuthServer#getAuthorizationCodeRequest
	 * @param authorityServerUri
	 * @param response
	 * @param clientId
	 * @param redirectUri
	 * @param scope
	 * @param state 
	 */
	public static void redirectAuthorization(HttpServletResponse response, String authorityServerUri,
			String clientId, String redirectUri, String scope, String state) throws IOException {
		response.sendRedirect(makeAuthorizationUri(authorityServerUri, clientId, redirectUri, scope, state));
	}
	
	public static String makeObtainAccessTokenByCode( 
			String clientId, String clientSecret, String code, String redirectUri, 
			boolean useBasicAuthentication) throws UnsupportedEncodingException {
		StringBuilder result = new StringBuilder("grant_type=authorization_code");
		result.append("&client_id=").append(URLEncoder.encode(clientId, "utf-8"));
		result.append("&code=").append(URLEncoder.encode(code, "utf-8"));
		if (redirectUri != null)
			result.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "utf-8"));
		if (!useBasicAuthentication && clientSecret != null && !clientSecret.isEmpty())
			result.append("&client_secret=").append(URLEncoder.encode(clientSecret, "utf-8"));
		return result.toString();
	}
	
	public static class AccessTokenResponse extends AccessToken {
		public String error;
		public String errorDescription;
		public String errorUri;
	}
	
	public static AccessToken obtainAccessTokenByCode(String authorityServerUri, 
			String clientId, String clientSecret, String code, String redirectUri, 
			boolean useBasicAuthentication) throws IOException, OAuthException {
		String rawData = makeObtainAccessTokenByCode(
				clientId, clientSecret, code, redirectUri, useBasicAuthentication);
		String type = "application/x-www-form-urlencoded";
		URL u = new URL(authorityServerUri);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setDoOutput(true);
		if (useBasicAuthentication) {
			String basicAuthentication = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
			conn.setRequestProperty( "Authorization", "Basic " + basicAuthentication);
		}
		conn.setRequestMethod("POST");
		conn.setRequestProperty( "Content-Type", type );
		conn.setRequestProperty( "Content-Length", String.valueOf(rawData.length()));
		try (OutputStream os = conn.getOutputStream()) {
			os.write(rawData.getBytes());
		}
		try (InputStream is = conn.getInputStream(); 
				InputStreamReader reader = new InputStreamReader(is)) {
			AccessTokenResponse token = Serializer.fromJson(reader, AccessTokenResponse.class);
			if (token.error != null) {
				throw new OAuthException(token.error, token.errorDescription, token.errorUri);
			}
			return token;
		}
	}
	
	private static String getHeadSubParameter(String content, String key) {
		Pattern pattern = Pattern.compile(key + "=\"(.*)\"");
		Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			return matcher.group(1);
		} else
			return null;
	}

	public static <T> T obtainResource(String authorityServerUri, String accessToken, 
			Class<T> type) throws IOException, OAuthException, ClassCastException {
		URL u = new URL(authorityServerUri);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setRequestProperty( "Authorization", "Bearer " + accessToken);
		if (conn.getResponseCode() == 401) {
			String wwwAuth = conn.getHeaderField("WWW-Authenticate");
			if (wwwAuth == null) {
				throw new OAuthException("unauthorized_client");
			} else {
				throw new OAuthException(getHeadSubParameter(wwwAuth, "error"),
					getHeadSubParameter(wwwAuth, "error_description"),
					getHeadSubParameter(wwwAuth, "error_uri"));
			}
		} else if (conn.getResponseCode() == 400) {
			throw new OAuthException("invalid_request");
		} else if (conn.getResponseCode() > 401 && conn.getResponseCode() < 500) {
			throw new OAuthException("access_denied");
		} else if (conn.getResponseCode() >= 500) {
			throw new OAuthException("server_error");
		} else if (conn.getResponseCode() != 200)
			throw new OAuthException("unsupported_response_type");
		try (InputStream is = conn.getInputStream(); 
				InputStreamReader reader = new InputStreamReader(is)) {
			T resource = Serializer.fromJson(reader, type);
			return resource;
		}
	}
	
	public AccessToken refreshAccessToken(
			String authorityServerUri, String clientId, String clientSecret, String refreshToken, String scope) 
			throws IOException, OAuthException {
		String content = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, "utf-8");
		if (scope != null && !scope.trim().isEmpty())
			content += "&scope=" + URLEncoder.encode(scope, "utf-8");
		String type = "application/x-www-form-urlencoded";
		URL u = new URL(authorityServerUri);
		HttpURLConnection conn = (HttpURLConnection) u.openConnection();
		conn.setDoOutput(true);
		String basicAuthentication = Base64.encodeBase64String((clientId + ":" + clientSecret).getBytes());
		conn.setRequestProperty( "Authorization", "Basic " + basicAuthentication);
		conn.setRequestMethod("POST");
		conn.setRequestProperty( "Content-Type", type );
		conn.setRequestProperty( "Content-Length", String.valueOf(content.length()));
		try (OutputStream os = conn.getOutputStream()) {
			os.write(content.getBytes());
		}
		try (InputStream is = conn.getInputStream(); 
				InputStreamReader reader = new InputStreamReader(is)) {
			AccessTokenResponse token = Serializer.fromJson(reader, AccessTokenResponse.class);
			if (token.error != null) {
				throw new OAuthException(token.error, token.errorDescription, token.errorUri);
			}
			return token;
		}
	}
}
