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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

/**
 * A {@link SSLSocketFactory} which uses an existing {@link SSLSocketFactory} to delegate its operations to and overrides the
 * {@link javax.net.ssl.SSLSocket#getEnabledProtocols() enabled protocols} to the protocols that were passed to its
 * {@link #ProtocolOverridingSSLSocketFactory(javax.net.ssl.SSLSocketFactory, String[]) constructor}
 *
 */
public class ProtocolOverridingSSLSocketFactory extends SSLSocketFactory {

	private final SSLSocketFactory underlyingSSLSocketFactory;
	private final String[] enabledProtocols;

	/**
	 * Constructs a {@code ProtocolOverridingSSLSocketFactory} with the given
	 * delegate {@link SSLSocketFactory} and array of enabled protocols.
	 *
	 * @param delegate         The underlying {@link SSLSocketFactory} to delegate operations to.
	 * @param enabledProtocols The array of protocols to be set as enabled protocols.
	 */
	public ProtocolOverridingSSLSocketFactory(final SSLSocketFactory delegate, final String[] enabledProtocols) {
		this.underlyingSSLSocketFactory = delegate;
		this.enabledProtocols = enabledProtocols;
	}

	@Override
	public String[] getDefaultCipherSuites() {
		return underlyingSSLSocketFactory.getDefaultCipherSuites();
	}

	@Override
	public String[] getSupportedCipherSuites() {
		return underlyingSSLSocketFactory.getSupportedCipherSuites();
	}

	@Override
	public Socket createSocket(final Socket socket, final String host, final int port, final boolean autoClose)
		throws IOException {
		Socket underlyingSocket = underlyingSSLSocketFactory.createSocket(socket, host, port, autoClose);
		return overrideProtocol(underlyingSocket);
	}

	@Override
	public Socket createSocket(final String host, final int port) throws IOException {
		Socket underlyingSocket = underlyingSSLSocketFactory.createSocket(host, port);
		return overrideProtocol(underlyingSocket);
	}

	@Override
	public Socket createSocket(final String host, final int port, final InetAddress localAddress, final int localPort)
		throws IOException {
		Socket underlyingSocket = underlyingSSLSocketFactory.createSocket(host, port, localAddress, localPort);
		return overrideProtocol(underlyingSocket);
	}

	@Override
	public Socket createSocket(final InetAddress host, final int port) throws IOException {
		Socket underlyingSocket = underlyingSSLSocketFactory.createSocket(host, port);
		return overrideProtocol(underlyingSocket);
	}

	@Override
	public Socket createSocket(
		final InetAddress host,
		final int port,
		final InetAddress localAddress,
		final int localPort
	) throws IOException {
		Socket underlyingSocket = underlyingSSLSocketFactory.createSocket(host, port, localAddress, localPort);
		return overrideProtocol(underlyingSocket);
	}

	/**
	 * Set the {@link javax.net.ssl.SSLSocket#getEnabledProtocols() enabled protocols} to {@link #enabledProtocols} if the <code>socket</code> is a
	 * {@link SSLSocket}
	 *
	 * @param socket The Socket
	 * @return the amended socket
	 **/
	private Socket overrideProtocol(final Socket socket) {
		if (socket instanceof SSLSocket && enabledProtocols != null && enabledProtocols.length > 0) {
			((SSLSocket) socket).setEnabledProtocols(enabledProtocols);
		}
		return socket;
	}
}
