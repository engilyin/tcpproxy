/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package asl.tcpproxy.tunnels;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import asl.tcpproxy.filters.WhitelistFilter;
import asl.tcpproxy.handlers.ClientToProxyIoHandler;

public class Tunnel implements Closeable {

	private static Logger log = LoggerFactory.getLogger(Tunnel.class);

	int remotePort;
	String endpoint;
	int proxyPort;
	NioSocketAcceptor acceptor;
	NioSocketConnector connector;
	InetAddress[] clientWhiteList;
	int connectTimeout;
	String statusMessage;
	boolean failed;

	public Tunnel(int remotePort, String endpoint, int proxyPort, int connectTimeout, InetAddress[] clientWhiteList) {
		this.remotePort = remotePort;
		this.endpoint = endpoint;
		this.proxyPort = proxyPort;
		this.connectTimeout = connectTimeout;
		this.clientWhiteList = clientWhiteList;
	}

	public Tunnel(String tunnelDesc, int connectTimeout, InetAddress[] clientWhiteList) {
		Pattern regex = Pattern.compile("^(\\d*):(.*):(\\d*)$");
		Matcher matcher = regex.matcher(tunnelDesc);
		if (matcher.matches()) {
			this.proxyPort = Integer.parseInt(matcher.group(1));
			this.endpoint = matcher.group(2);
			this.remotePort = Integer.parseInt(matcher.group(3));
			this.clientWhiteList = clientWhiteList;
			this.connectTimeout = connectTimeout;
			failed = false;
		} else {
			failed = true;
			statusMessage = String.format("Wrong tunnel description: %s", tunnelDesc);
		}
	}
	
	@Override
	public String toString() {
		return statusMessage;
	}
	
	public String descriptor() {
		return String.format("%d:%s:%d", proxyPort, endpoint, remotePort);
	}

	public void init() {
		if (!failed) {
			try {
				acceptor = new NioSocketAcceptor();

				acceptor.getSessionConfig().setReadBufferSize(1000000);

				acceptor.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 10);

				acceptor.getSessionConfig().setKeepAlive(true);

				connector = new NioSocketConnector();

				DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();

				if (clientWhiteList != null) {
					WhitelistFilter whitelistFilter = new WhitelistFilter();
					whitelistFilter.setBlacklist(clientWhiteList);
					chain.addLast("whitelistFilter", whitelistFilter);
				}

				connector.setConnectTimeoutMillis(connectTimeout);

				ClientToProxyIoHandler handler = new ClientToProxyIoHandler(connector,
						new InetSocketAddress(endpoint, remotePort));

				// Start the proxy.
				acceptor.setHandler(handler);

				acceptor.bind(new InetSocketAddress(proxyPort));

				failed = false;
				statusMessage = String
						.format("TCP Proxy to %s port %d is listening on port %d...", endpoint, remotePort, proxyPort);

				if (log.isInfoEnabled()) {
					log.info(statusMessage);
				}

			} catch (Throwable ex) {
				failed = true;
				statusMessage = String.format("TCP Proxy to %s port %d on port %d is FAILED with message: %s!",
						endpoint,
						remotePort,
						proxyPort,
						ex.getMessage());
				log.error(statusMessage, ex);
			}
		}
	}

	@Override
	public void close() {
		try {
			if (!failed) {
				connector.dispose();
				acceptor.unbind();
				if (log.isInfoEnabled()) {
					log.info(String.format("The proxy on the port %d to %s port %d is closed",
							proxyPort,
							endpoint,
							remotePort));
				}
			}

		} catch (Throwable ex) {
			failed = true;
			statusMessage = String.format("Unable to close TCP Proxy to %s port %d on port %d with message: %s!",
					endpoint,
					remotePort,
					proxyPort,
					ex.getMessage());
			log.error(statusMessage, ex);
		}
	}
}
