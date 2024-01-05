package org.sentrysoftware.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HttpClientTest {

	/**
	 * By default, HTTPBIN_URL points to the public instance of httpbin:
	 * <code>http://httpbin.org</code>
	 * <p>
	 * When running tests, you can set the <code>HTTPBIN_URL</code> environment variable
	 * to specify another address where httpbin is running.
	 * </p>
	 * <p>
	 * Example:
	 * </p>
	 * <code>HTTPBIN_URL=http://httpbin.example.org:8080</code>
	 */
	private static final String HTTPBIN_URL;
	static {
		String url = System.getenv("HTTPBIN_URL");
		if (url == null || url.isEmpty()) {
			url = "http://httpbin.org";
		}
		HTTPBIN_URL = url;
	}

	/**
	 * By default, HTTPBIN_SSL_URL points to the public instance of httpbin:
	 * <code>https://httpbin.org</code>
	 * <p>
	 * When running tests, you can set the <code>HTTPBIN_SSL_URL</code> environment variable
	 * to specify another address where httpbin is running.
	 * </p>
	 * <p>
	 * Example:
	 * </p>
	 * <code>HTTPBIN_URL=https://httpbin.example.org:8082</code>
	 */
	private static final String HTTPBIN_SSL_URL;
	static {
		String url = System.getenv("HTTPBIN_SSL_URL");
		if (url == null || url.isEmpty()) {
			url = "https://httpbin.org";
		}
		HTTPBIN_SSL_URL = url;
	}

	@ParameterizedTest
	@ValueSource(ints = {
			200, 201, 202, 203, 204, 205, 206,
			300, 304, 306, 308,
			400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 418, 428, 429, 431, 451,
			500, 501, 502, 503, 504, 505, 511, 520, 522, 524
	})
	void statusCode(int status) throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/status/" + status,
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);

		assertEquals(status, r.getStatusCode(), "Must return status " + status);

	}

	@Test
	void https() throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_SSL_URL + "/status/200",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(200, r.getStatusCode(), "Default https must work");

		r = HttpClient.sendRequest(
				HTTPBIN_SSL_URL + "/status/200",
				"GET",
				new String[] { "TLSv1.2" },
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(200, r.getStatusCode(), "TLSv1.2 only must work");

		r = HttpClient.sendRequest(
				HTTPBIN_SSL_URL + "/status/200",
				"GET",
				new String[] { "SSLv2Hello" },
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(200, r.getStatusCode(), "Specifying SSLv2Hello must not break communication");

	}

	@ParameterizedTest
	@ValueSource(strings = { "GET", "DELETE", "POST", "PUT" } )
	void method(String method) throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/" + method.toLowerCase(),
				method,
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(200, r.getStatusCode(), "Method " + method + " must work");
		assertTrue(r.getBody().contains("args"));

	}

	@Test
	void basicAuth() throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/basic-auth/mypasswordis/password",
				"GET",
				null,
				"mypasswordis", "password".toCharArray(),
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(200, r.getStatusCode(), "Basic auth must work");
	}

	@Test
	void basicAuthFail() throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/basic-auth/mypasswordis/notpassword",
				"GET",
				null,
				"mypasswordis", "password".toCharArray(),
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(401, r.getStatusCode(), "Failed basic auth must return 401");
	}

	@Test
	void digestAuth() throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/digest-auth/auth/mypasswordis/password",
				"GET",
				null,
				"mypasswordis", "password".toCharArray(),
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(200, r.getStatusCode(), "Digest auth must work");
	}

	@Test
	void userAgent() throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/user-agent",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				"NCSA_Mosaic/2.0 (Windows 3.1)",
				null,
				null,
				30,
				null
		);
		assertTrue(r.getBody().contains("NCSA_Mosaic/2.0 (Windows 3.1)"), "User agent must be set properly");
	}

	@Test
	void userAgentDefault() throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/user-agent",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertTrue(r.getBody().contains(HttpClient.DEFAULT_USER_AGENT), "User agent must be set by default");
	}

	@Test
	void headers() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("first", "1");
		headers.put("second", "1 + 1");
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/headers",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				headers,
				null,
				30,
				null
		);
		assertTrue(r.getBody().contains("\"First\": \"1\""), "First header must be set in " + r.getBody());
		assertTrue(r.getBody().contains("\"Second\": \"1 + 1\""), "First header must be set" + r.getBody());
	}

	@Test
	void deflate() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept-Encoding", "deflate");
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/deflate",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				headers,
				null,
				30,
				null
		);
		assertTrue(r.getHeader().contains("Content-Encoding: deflate"), "Content encoding must be deflate in header: " + r.getHeader());
		assertTrue(r.getBody().contains("\"deflated\": true"), "Body must have been decoded properly");
	}

	@Test
	void gzip() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Accept-Encoding", "gzip");
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/gzip",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				headers,
				null,
				30,
				null
		);
		assertTrue(r.getHeader().contains("Content-Encoding: gzip"), "Content encoding must be gzip in header: " + r.getHeader());
		assertTrue(r.getBody().contains("\"gzipped\": true"), "Body must have been decoded properly");
	}

	@Test
	void postBody() throws Exception {
		Map<String, String> headers = new HashMap<String, String>();
		headers.put("Content-type", "text/plain");
		HttpResponse r = HttpClient.sendRequest(
				HTTPBIN_URL + "/anything",
				"POST",
				null,
				null, null,
				null, 0, null, null,
				null,
				headers,
				"abcdefjhijklmnopqrstuvwxyz",
				30,
				null
		);
		assertTrue(r.getBody().contains("\"data\": \"abcdefjhijklmnopqrstuvwxyz\""), "Body must have been sent properly");
	}

	@Test
	void timeoutException() throws Exception {
		assertThrows(IOException.class, () -> HttpClient.sendRequest(
				HTTPBIN_URL + "/delay/9",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				5,
				null
		), "Timeout must fire after 5 seconds");
	}

	@Test
	void timeout() throws Exception {
		assertTimeout(Duration.ofSeconds(5), () -> {
			try {
				HttpClient.sendRequest(
						HTTPBIN_URL + "/delay/9",
						"GET",
						null,
						null, null,
						null, 0, null, null,
						null,
						null,
						null,
						4,
						null
				);
			} catch (Exception e) {}
		}, "Request must complete at the specified timeout");
	}

	@Test
	void utf8() throws Exception {
		HttpResponse r = HttpClient.sendRequest(
				"https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-demo.txt",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				null
		);
		assertEquals(7625, r.getBody().trim().length(), "UTF-8 must be properly decoded (there are 7626 chars, incl. eol)");
	}

	@Test
	void downloadTo() throws Exception {
		Path tempPath = Files.createTempFile("test-download", ".txt");
		tempPath.toFile().deleteOnExit();
		HttpResponse r = HttpClient.sendRequest(
				"https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-demo.txt",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				tempPath.toString()
		);
		assertEquals(14058, Files.size(tempPath), "File must have been downloaded and size is good");
		assertEquals(tempPath.toString(), r.getBody(), "File path must be specified in response body");
		tempPath.toFile().delete();
	}

	@Test
	void downloadToDirectory() throws Exception {
		Path tempDirPath = Files.createTempDirectory("testDownloadToDirectory");
		tempDirPath.toFile().deleteOnExit();
		HttpResponse r = HttpClient.sendRequest(
				"https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-demo.txt",
				"GET",
				null,
				null, null,
				null, 0, null, null,
				null,
				null,
				null,
				30,
				tempDirPath.toString()
		);
		Path downloadedFilePath = new File(tempDirPath.toFile(), "UTF-8-demo.txt").toPath();
		assertEquals(14058, Files.size(downloadedFilePath), "File must have been downloaded and size is good");
		assertEquals(downloadedFilePath.toString(), r.getBody(), "File path must be specified in response body");
		tempDirPath.toFile().delete();
	}

}
