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

/**
 * Represents an HTTP Response in a simplified way
 * <ul>
 * <li>Status code (200, 400, etc.)</li>
 * <li>Header (Content-type: application/json, etc.)</li>
 * <li>Body</li>
 * </ul>
 */
public class HttpResponse {

	private int statusCode;
	private StringBuilder body;
	private StringBuilder header;

	/**
	 * Create a new HTTP Response
	 */
	public HttpResponse() {
		statusCode = 0;
		header = new StringBuilder();
		body = new StringBuilder();
	}

	/**
	 * @return HTTP status code (200, 300, etc.)
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Sets the HTTP status code
	 *
	 * @param code HTTP status code (200, 302, etc.)
	 */
	public void setStatusCode(int code) {
		statusCode = code;
	}

	/**
	 * Get the HTTP header as a single string like below:
	 * <pre>
	 * Content-type: text/html
	 * Content-length: 34094
	 * </pre>
	 * @return the HTTP header
	 */
	public String getHeader() {
		return header.toString();
	}

	/**
	 * Add one header value
	 * @param name Header name (e.g. "Content-type")
	 * @param value Header value (e.g. "text/html")
	 */
	public void appendHeader(String name, String value) {
		if (name != null && value != null && !name.isEmpty() && !value.isEmpty()) {
			header.append(name).append(": ").append(value).append("\n");
		}
	}

	/**
	 * @return the body of the HTTP response
	 */
	public String getBody() {
		return body.toString();
	}

	/**
	 * Append content to the body of the HTTP response
	 *
	 * @param data Data to append
	 */
	public void appendBody(String data) {
		body.append(data);
	}

	/**
	 * @return the entire HTTP response, header and body
	 */
	@Override
	public String toString() {
		return new StringBuilder().append(header).append("\n").append(body).toString();
	}
}
