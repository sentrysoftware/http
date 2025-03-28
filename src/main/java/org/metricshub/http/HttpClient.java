package org.metricshub.http;

/*-
 * ╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲╱╲
 * HTTP Java Client
 * ჻჻჻჻჻჻
 * Copyright (C) 2023 MetricsHub
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Simple HTTP Client implementation for Java's HttpURLConnection.<br>
 * It has no external dependencies and facilitates the execution of HTTP requests.
 */
public class HttpClient {
	static {
		// Fix JDK-8208526 issue
		System.setProperty("jdk.tls.acknowledgeCloseNotify", "true");
	}

	/**
	 * The default User-Agent to use if the <code>userAgent</code> value is not provided to the HttpClient.
	 */
	public static final String DEFAULT_USER_AGENT =
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36 Edge/14.14393 org.metricshub.http";
	private static final int MAX_CONTENT_LENGTH = 50 * 1024 * 1024; // 50 MB max
	private static final int BUFFER_SIZE = 64 * 1024; // 64 KB chunks
	private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
	private static final Pattern CHARSET_REGEX = Pattern.compile("charset=\\s*\"?([^; \"]+)", Pattern.CASE_INSENSITIVE);

	/**
	 * Hostname verifier that doesn't verify sh*t
	 */
	private static final HostnameVerifier LOUSY_HOSTNAME_VERIFIER = (String urlHostName, SSLSession session) -> true;

	/**
	 * Trust manager that welcomes any certificate from anywhere
	 */
	private static final TrustManager[] LOUSY_TRUST_MANAGER = new TrustManager[] {
		new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}

			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
		}
	};

	/**
	 * The lousy SSL Socket Factory, that accepts any certificate
	 */
	private static final SSLSocketFactory BASE_SOCKET_FACTORY;

	/**
	 * The default SSL protocols available in this JRE
	 */
	private static final String[] DEFAULT_SSL_PROTOCOLS;

	static {
		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, LOUSY_TRUST_MANAGER, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException | KeyManagementException e) {}
		BASE_SOCKET_FACTORY = sc.getSocketFactory();
		DEFAULT_SSL_PROTOCOLS = sc.getDefaultSSLParameters().getProtocols();
	}

	/**
	 * Returns the InputStream that will be properly decoded, according to the
	 * content encoding of the HTTP response.
	 *
	 * @param httpURL HttpURLConnection instance
	 * @return the input stream to read from
	 */
	private static InputStream getDecodedStream(HttpURLConnection httpURL) {
		String contentEncoding = httpURL.getContentEncoding();

		try {
			// In case of a GZIP-encoded content, well, unzip it!
			if ("gzip".equalsIgnoreCase(contentEncoding)) {
				return new GZIPInputStream(httpURL.getInputStream());
			} else if ("deflate".equalsIgnoreCase(contentEncoding)) {
				return new InflaterInputStream(httpURL.getInputStream());
			} else {
				return httpURL.getInputStream();
			}
		} catch (IOException e) {
			// If getInputStream() failed, then use the error stream, if available
			return httpURL.getErrorStream();
		}
	}

	/**
	 * @param url The URL to be requested (e.g. https://w3.test.org/site/list.jsp)
	 * @param method GET|POST|PUT|DELETE or whatever HTTP verb is supported
	 * @param specifiedSslProtocolArray Array of string of the SSL protocols to use (e.g.: "SSLv3", "TLSv1", etc.)
	 * @param username Username to access the specified URL
	 * @param password Password associated to username
	 * @param proxyServer Host name of IP address of the proxy. Leave empty or null if no proxy is required.
	 * @param proxyPort Port of the proxy (e.g. 3128)
	 * @param proxyUsername Username to connect to the proxy (if any)
	 * @param proxyPassword Password associated to the proxy username
	 * @param userAgent String of the user agent to specify in the request (if null, will use a default one)
	 * @param addHeaderMap Additional headers to be added to the HTTP request (pairs of key and value)
	 * @param body Body of the HTTP request to be sent
	 * @param timeout Timeout in seconds before the operation is canceled
	 * @param downloadToPath A path where to download the content of the HTTP response to
	 * @return an HttpResponse, which itself contains the HTTP status code, the headers and the body of the response
	 * @throws MalformedURLException when the specified URL is invalid
	 * @throws IOException when anything wrong happens during the connection and while downloading information from the Web server
	 * @throws FileNotFoundException when the specified downloadToPath is not correct (not a file, not accessible, etc.)
	 */
	public static HttpResponse sendRequest(
		String url,
		String method,
		String[] specifiedSslProtocolArray,
		String username,
		char[] password,
		String proxyServer,
		int proxyPort,
		String proxyUsername,
		char[] proxyPassword,
		String userAgent,
		Map<String, String> addHeaderMap,
		String body,
		int timeout,
		String downloadToPath
	) throws IOException {
		// Connect through a proxy?
		boolean useProxy = proxyServer != null && !proxyServer.isEmpty();

		// Connect directly (no proxy)
		HttpURLConnection httpURL;
		if (!useProxy) {
			httpURL = (HttpURLConnection) new URL(url).openConnection();
		} else {
			Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServer, proxyPort));
			httpURL = (HttpURLConnection) new URL(url).openConnection(proxy);
		}

		// Download to a file?
		// Perform some verifications on the specified downloadToPath
		File downloadToFile = null;
		if (downloadToPath != null && !downloadToPath.isEmpty()) {
			// So, the user specified a file path to download to
			downloadToFile = new File(downloadToPath);

			// Check whether the user specified a path whose path doesn't exist
			File parentDirectory = downloadToFile.getParentFile();
			if (parentDirectory != null && !parentDirectory.exists()) {
				// So, the parent directory of the specified filed path does not exist
				try {
					// We therefore need to create the directory (and maybe the grand-parent directories, and so on...)
					parentDirectory.mkdirs();
				} catch (SecurityException e) {
					throw new IOException("Couldn't create the necessary directories for " + downloadToPath);
				}
			}
		}

		/////////////////////////////////////////
		//      H T T P S   C a s e            //
		/////////////////////////////////////////

		// For HTTPS connections, we need to setup more things
		if (httpURL instanceof HttpsURLConnection) {
			// In order to accept to connect to all invalid HTTPS servers, we need to setup
			// our own lousy -- very untight -- verifiers
			((HttpsURLConnection) httpURL).setHostnameVerifier(LOUSY_HOSTNAME_VERIFIER);

			// If no protocols were specified (as normal), use the default ones
			if (specifiedSslProtocolArray == null || specifiedSslProtocolArray.length == 0) {
				// So, simply use the base socket factory
				((HttpsURLConnection) httpURL).setSSLSocketFactory(BASE_SOCKET_FACTORY);
			} else {
				// Clean-up the list of specified protocols (remove non supported ones, incl. SSLv2Hello)
				String[] protocolsToEnable = Arrays
					.stream(specifiedSslProtocolArray)
					.filter(p -> p != null && !"SSLv2Hello".equalsIgnoreCase(p))
					.filter(p -> Arrays.stream(DEFAULT_SSL_PROTOCOLS).anyMatch(d -> d.equalsIgnoreCase(p)))
					.toArray(String[]::new);

				// Create a new SSL socket factory with these settings
				SSLSocketFactory overridenSocketFactory = new ProtocolOverridingSSLSocketFactory(
					BASE_SOCKET_FACTORY,
					protocolsToEnable
				);
				((HttpsURLConnection) httpURL).setSSLSocketFactory(overridenSocketFactory);
			}
		}

		// Setup the HTTP connection
		httpURL.setRequestMethod(method);
		httpURL.setDefaultUseCaches(false);
		httpURL.setDoOutput(true);
		httpURL.setDoInput(true);
		httpURL.setConnectTimeout(timeout * 1000);
		httpURL.setReadTimeout(timeout * 1000);
		httpURL.setAllowUserInteraction(false);
		httpURL.setInstanceFollowRedirects(true);

		// User agent
		if (userAgent == null || userAgent.isEmpty()) {
			userAgent = DEFAULT_USER_AGENT;
		}
		httpURL.addRequestProperty("User-Agent", userAgent);

		// Add the additional specified headers
		if (addHeaderMap != null) {
			addHeaderMap.forEach((header, value) -> {
				if (header != null && value != null && !header.isEmpty() && !value.isEmpty()) {
					httpURL.addRequestProperty(header, value);
				}
			});
		}

		// Authentication
		ThreadSafeNoCacheAuthenticator.setCredentials(username, password, proxyUsername, proxyPassword);
		Authenticator.setDefault(ThreadSafeNoCacheAuthenticator.getInstance());

		// Go!
		try {
			httpURL.connect();

			// Send our request
			if (body != null && !body.isEmpty()) {
				try (OutputStream os = httpURL.getOutputStream()) {
					os.write(body.getBytes(UTF8_CHARSET));
				}
			}

			// New HttpResponse
			HttpResponse response = new HttpResponse();

			// Get the HTTP response code
			// Note: this may fail and trigger an IOException with JRE1.6 on some 401 (Unauthorized) responses
			response.setStatusCode(httpURL.getResponseCode());

			// Read the response headers
			httpURL
				.getHeaderFields()
				.forEach((header, valueList) -> valueList.forEach(value -> response.appendHeader(header, value)));

			// Do we have a file path to write to?
			if (downloadToFile != null) {
				// If the specified downloadToPath is a directory, we will have to make up a file name
				// by retrieving the name of the resource that we're downloading, basically
				// We can do this only at the very last second because the user may have specified a
				// URL which doesn't not specify a file (like .../download.php?file=Avatar1080p.mkv)
				// which will be then redirected to the real URL (..../A09230E9FB58A0459284/Avatar1080p.mkv)
				// So, only now we can retrieve the URL of the httpURL connection, which should be the
				// latest one that we've queried after we've been redirected
				if (downloadToFile.isDirectory()) {
					String tempFilename = httpURL.getURL().getPath();
					String filename = tempFilename.substring(tempFilename.lastIndexOf('/'));
					downloadToPath = new File(downloadToFile, filename).getPath();
				}

				// Download the content directly to the file
				try (
					FileOutputStream fileStream = new FileOutputStream(downloadToPath);
					InputStream httpStream = getDecodedStream(httpURL)
				) {
					byte[] tempBuf = new byte[BUFFER_SIZE];
					int readBytes;
					while ((readBytes = httpStream.read(tempBuf)) != -1) {
						fileStream.write(tempBuf, 0, readBytes);
					}
				}

				// As we have successfully created the file, we will put in the returned "body" of the
				// HTTP response the path to the file that we just created, so that the client can reference it
				response.appendBody(downloadToPath);

				// Return
				return response;
			}

			// Read the content (expecting a text string, as it's going to be returned as a String, and not a byte[])

			// First, what is the content length?
			int contentLength = httpURL.getContentLength();

			// If content is too large, then discard it
			if (contentLength > MAX_CONTENT_LENGTH) {
				throw new IOException("Content is too large (" + contentLength + " bytes > " + MAX_CONTENT_LENGTH + " bytes)");
			}

			// What is the encoding (so we can build the String accordingly)
			Charset charset = UTF8_CHARSET;
			String contentType = httpURL.getContentType();
			if (contentType != null) {
				Matcher charsetMatcher = CHARSET_REGEX.matcher(contentType);
				if (charsetMatcher.find()) {
					charset = Charset.forName(charsetMatcher.group(1));
				}
			}

			// Read body by chunks
			ByteArrayOutputStream bodyBytes = contentLength > 0
				? new ByteArrayOutputStream(contentLength)
				: new ByteArrayOutputStream();

			byte[] buffer = new byte[BUFFER_SIZE];
			int totalBytesCount = 0;
			int bytesCount;

			try (InputStream httpStream = getDecodedStream(httpURL)) {
				while (httpStream != null && (bytesCount = httpStream.read(buffer)) != -1) {
					bodyBytes.write(buffer, 0, bytesCount);
					totalBytesCount += bytesCount;
					if (totalBytesCount > MAX_CONTENT_LENGTH) {
						throw new IOException("Content is too large (maximum " + MAX_CONTENT_LENGTH + " bytes)");
					}
				}
			}
			response.appendBody(new String(bodyBytes.toByteArray(), charset));

			// Return
			return response;
		} finally {
			// Disconnect
			httpURL.disconnect();

			// Clear the credentials
			ThreadSafeNoCacheAuthenticator.clearCredentials();
		}
	}
}
