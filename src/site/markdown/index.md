# HTTP Java Client

The HTTP Java Client is a simple Java library that facilitates the execution of HTTP requests. Built on Java's HttpURLConnection, it requires no external dependencies and supports various HTTP methods (GET, POST, PUT, DELETE).

## Features

* **Easy Integration**: No external dependencies, making it easy to integrate into your Java projects.
* **HTTPS Support**: Handles HTTPS connections with customizable TLS protocols.
* **Proxy Configuration**: Easily configure proxy settings if needed.
* **Authentication**: Supports basic authentication for both the target URL and proxy.
* **Customizable Headers**: Add additional headers to your HTTP requests.
* **User-Agent Configuration**: Set your own User-Agent or use the default one.
* **Download Support**: Download content directly to a file with optional file path customization.
* **Timeout Handling**: Set timeout limits for connection and read operations.

## Usage

Simply add HTTP in the list of dependencies in your [Maven **pom.xml**](https://maven.apache.org/pom.html):

```xml
<dependencies>
	<!-- [...] -->
	<dependency>
		<groupId>${project.groupId}</groupId>
		<artifactId>${project.artifactId}</artifactId>
		<version>${project.version}</version>
	</dependency>
</dependencies>
```

Then, invoke the HTTP Client as follows:

```java
package org.sentrysoftware.http;

import java.io.IOException;
import java.util.Map;

public class Main {

	public static void main(String[] args) throws IOException {

		final String url = "https://httpbin.org/anything";
		final String method = "GET";
		final String[] specifiedSslProtocols = null;
		final String username = null;
		final char[] password = null;
		final String proxyServer = null;
		final int proxyPort = -1;
		final String proxyUsername = null;
		final char[] proxyPassword = null;
		final String userAgent = null;
		final Map<String, String> headers = Map.of("accept", "application/json");
		final String body = null;
		final int timeout = 10;
		final String downloadToPath = null;

		final HttpResponse response = HttpClient.sendRequest(
			url,
			method,
			specifiedSslProtocols,
			username,
			password,
			proxyServer,
			proxyPort,
			proxyUsername,
			proxyPassword,
			userAgent,
			headers,
			body,
			timeout,
			downloadToPath
		);

		System.out.format("HTTP Response Status: %d%n", response.getStatusCode());
		System.out.format("HTTP Response Headers:%n%s", response.getHeader());
		System.out.format("HTTP Response Body:%n%s%n", response.getBody());
	}
}
```

> Note: When using JRE 21, encountering the error message `java.lang.NoSuchMethodError: 'void sun.net.www.protocol.http.AuthCacheValue.setAuthCache(sun.net.www.protocol.http.AuthCache)` indicates that your application is not configured to leverage multi-release JARs. The *HTTP Java Client* supports Java 21 and beyond by defining an updated version of the `ThreadSafeNoCacheAuthenticator` that is located in `META-INF/versions/21`. Depending on how your application works, it may be necessary to set `Multi-Release` to true in the JAR manifest.
