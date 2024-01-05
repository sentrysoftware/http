package org.sentrysoftware.http;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * HTTP Java Client
 * ჻჻჻჻჻჻
 * Copyright (C) 2023 - 2024 Sentry Software
 * ჻჻჻჻჻჻
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱
 */

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The ThreadSafeNoCacheAuthenticator class is used to provide the specified username/password for the
 * Web site itself, or for the proxy.
 * <p>
 * Additionally, it works around the incredibly annoying limitation of Java's UrlConnection
 * class and its underlying authentication mechanism, which is... gasp... not Thread-safe!
 * </p>
 * <p>
 * Also, this class works around the fact that Java's HTTP authentication mechanism uses a
 * cache for all credentials that worked once! Very annoying... again!
 * </p>
 * <p>
 * It must be known that authentication in Java's HTTP client (the HttpUrlConnection class)
 * is based on the Authenticator class, whose only method to specify credentials stores
 * the credentials in a static property.
 * </p>
 * <p>
 * <code>Authenticator.setDefault(Authenticator a)</code> is a joke... an annoying joke!
 * </p>
 * <p>
 * Anyway, we will store the credentials in a ConcurrentHashMap where the key is the current Thread.
 * It's the developer's responsibility to remove the credentials that he doesn't want to keep
 * in memory.
 * </p>
 * <p>
 * And we override the non-API AuthCache class to disable the caching mechanism, which was causing
 * unwanted behavior (like getting a successful HTTP response when specifying a wrong password,
 * simply because you provided the proper password once before...)
 * </p>
 *
 * To use this class, you will need to do the following:
 * <br>
 * <code>
 * Authenticator.setDefault(ThreadSafeAuthenticator.getInstance()); <br>
 * <br>
 * ThreadSafeAuthenticator.setCredentials(username, password, proxyUsername, proxyPassword); <br>
 * <br>
 * // Then go ahead with your HttpUrlConnection request <br>
 * </code>
 *
 * @author bertrand
 */
@SuppressWarnings("restriction")
class ThreadSafeNoCacheAuthenticator extends Authenticator {

	/**
	 * One set of credentials that will be stored in the ConcurrentHashMap,
	 * associated to the Thread making the request
	 *
	 * @author bertrand
	 */
	private static class CredEntry {

		public String username;
		public char[] password;
		public String proxyUsername;
		public char[] proxyPassword;

		public CredEntry(String pUsername, char[] pPassword, String pProxyUsername, char[] pProxyPassword) {
			username = pUsername;
			password = pPassword;
			proxyUsername = pProxyUsername;
			proxyPassword = pProxyPassword;
		}

	}

	/**
	 * Only this class can instantiate itself, as only one instance will ever be required
	 */
	private ThreadSafeNoCacheAuthenticator() { /* Nothing do to, it's just ro declare the Constructor as private */ }

	/**
	 * There will be only one instance of this class, because there is no need
	 * for other instances, as everything is managed in static properties
	 */
	private static ThreadSafeNoCacheAuthenticator oneSingleAuthenticator;
	static {
		// Create the one instance required, as store it statically
		oneSingleAuthenticator = new ThreadSafeNoCacheAuthenticator();
	}

	/**
	 * @return the one single instance of ThreadSafeAuthenticator, to be used in
	 * <code>Authenticator.setDefault(ThreadSafeAuthenticator.getInstance()</code>
	 */
	public static ThreadSafeNoCacheAuthenticator getInstance() {
		return oneSingleAuthenticator;
	}

	/**
	 * The ConcurrentHashMap that stores the credentials to be used for each Thread.
	 * Each Thread needs to call <code>setCredentials(...)</code> with the credentials that will be
	 * used for upcoming HttpUrlConnection requests.
	 */
	private static ConcurrentHashMap<Thread,CredEntry> credList = new ConcurrentHashMap<Thread,CredEntry>();

	/**
	 * Sets the credentials that will be used in HttpUrlConnection requests made by this Thread.
	 * <p>
	 * <b>Warning!</b> Each Thread needs to set the credentials it needs to use with HttpUrlConnection.
	 * </p>
	 * 
	 * @param pUsername The username to be used to authenticate with the HTTP server
	 * @param pPassword The associated password
	 * @param pProxyUsername The user name to be used to authenticate with the proxy server
	 * @param pProxyPassword The associated password
	 */
	public static void setCredentials(String pUsername, char[] pPassword, String pProxyUsername, char[] pProxyPassword) {
		credList.put(Thread.currentThread(), new CredEntry(pUsername, pPassword, pProxyUsername, pProxyPassword));
	}

	/**
	 * Sets the credentials that will be used in HttpUrlConnection requests made by this Thread.
	 * <p>
	 * <b>Warning!</b> Each Thread needs to set the credentials it needs to use with HttpUrlConnection.
	 * </p>
	 *
	 * @param pUsername The username to be used to authenticate with the HTTP server
	 * @param pPassword The associated password
	 */
	public static void setCredentials(String pUsername, char[] pPassword) {
		credList.put(Thread.currentThread(), new CredEntry(pUsername, pPassword, null, null));
	}

	/**
	 * Removes the credentials associated to this Thread.
	 * <p>
	 * <b>IMPORTANT!</b> Make sure to clear the credentials once finished with the HttpUrlConnection,
	 * both for security reasons and to avoid memory leaks!
	 * </p>
	 */
	public static void clearCredentials() {
		credList.remove(Thread.currentThread());
	}

	public PasswordAuthentication getPasswordAuthentication() {

		// Get the credentials of the current Thread
		CredEntry credEntry = credList.get(Thread.currentThread());
		if (credEntry == null) {
			// No credentials? Return null
			return null;
		}

		// Return the specified username and password for the server, if it's the server requesting it
		if (getRequestorType() == RequestorType.SERVER && credEntry.username != null && credEntry.password != null) {
			return (new PasswordAuthentication(credEntry.username, credEntry.password));
		}

		// Or send the username and password for the proxy if it's the proxy requesting it
		else if (getRequestorType() == RequestorType.PROXY && credEntry.proxyUsername != null && credEntry.proxyPassword != null) {
			return (new PasswordAuthentication(credEntry.proxyUsername, credEntry.proxyPassword));
		}

		// Return null if we don't have the necessary credentials (which is the default implementation)
		return null;
	}

	/**
	 * This class ensures that no cache is used for credentials to authenticate with an HTTP server
	 * <br>
	 * Courtesy of: https://stackoverflow.com/a/6049879/8494773
	 * @author so_mv
	 *
	 */
	static class DisabledCache implements sun.net.www.protocol.http.AuthCache {
		 public void put(String pkey, sun.net.www.protocol.http.AuthCacheValue value) {}
		 public sun.net.www.protocol.http.AuthCacheValue get(String pkey, String skey) { return null; }
		 public void remove(String pkey, sun.net.www.protocol.http.AuthCacheValue entry) {}
	}
	static{
		sun.net.www.protocol.http.AuthCacheValue.setAuthCache(new DisabledCache());

	}
}


