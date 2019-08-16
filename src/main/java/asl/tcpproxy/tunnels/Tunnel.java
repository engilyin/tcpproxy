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
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.transport.socket.SocketSessionConfig;
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
    InetAddress[] clientWhiteList;
    int connectTimeout;
    String statusMessage;
    boolean active;

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
            succsessfulInit(connectTimeout, clientWhiteList, matcher);
        } else {
            failedInit(tunnelDesc);
        }
    }

    private void failedInit(String tunnelDesc) {
        statusMessage = String.format("Wrong tunnel description: %s", tunnelDesc);
    }

    private void succsessfulInit(int connectTimeout, InetAddress[] clientWhiteList, Matcher matcher) {
        this.proxyPort = Integer.parseInt(matcher.group(1));
        this.endpoint = matcher.group(2);
        this.remotePort = Integer.parseInt(matcher.group(3));
        this.clientWhiteList = clientWhiteList;
        this.connectTimeout = connectTimeout;
    }

    @Override
    public String toString() {
        return statusMessage;
    }

    public String descriptor() {
        return String.format("%d:%s:%d", proxyPort, endpoint, remotePort);
    }

    public void open() {
        if (isDown()) {
            try {
                this.acceptor = createAcceptor();
                startProxy(this.acceptor, createConnector());
                activate();
                
                log.info(statusMessage);

            } catch (Throwable ex) {
                disactivate(ex.getMessage());
                log.error(statusMessage, ex);
            }
        }
    }
    
    @Override
    public void close() {
        try {
            if (isActive()) {
                this.acceptor.dispose();
                this.acceptor = null;
                disactivate();
                log.info("The proxy on the port {} to {} port {} is closed", proxyPort, endpoint, remotePort);
            }

        } catch (Throwable ex) {
            disactivate("Unable to close because of " + ex.getMessage());
            log.error(statusMessage, ex);
        }
    }

    private void startProxy(NioSocketAcceptor acceptor, NioSocketConnector connector) throws IOException {
        ClientToProxyIoHandler handler = new ClientToProxyIoHandler(connector,
                new InetSocketAddress(endpoint, remotePort));

        acceptor.setHandler(handler);
        acceptor.bind(new InetSocketAddress(proxyPort));
    }

    private NioSocketConnector createConnector() {
        NioSocketConnector connector = new NioSocketConnector();
        connector.setConnectTimeoutMillis(connectTimeout);

        return connector;
    }

    private NioSocketAcceptor createAcceptor() {
        NioSocketAcceptor acceptor = new NioSocketAcceptor();

        initAcceptorSessionConfig(acceptor.getSessionConfig());
        initWhiteList(acceptor.getFilterChain());

        return acceptor;
    }

    private void initAcceptorSessionConfig(SocketSessionConfig sessionConfig) {
        sessionConfig.setReadBufferSize(1000000);
        sessionConfig.setIdleTime(IdleStatus.BOTH_IDLE, 10);
        sessionConfig.setKeepAlive(true);
    }

    private void initWhiteList(DefaultIoFilterChainBuilder chain) {
        if (clientWhiteList != null) {
            WhitelistFilter whitelistFilter = new WhitelistFilter();
            whitelistFilter.setBlacklist(clientWhiteList);
            chain.addLast("whitelistFilter", whitelistFilter);
        }
    }

    private void activate() {
        this.active = true;

        this.statusMessage = String
                .format("TCP Proxy to %s port %d is listening on port %d...", endpoint, remotePort, proxyPort);
    }

    private void disactivate(String errorDetails) {
        this.active = false;

        this.statusMessage = String.format("TCP Proxy to %s port %d on port %d is FAILED with message: %s!",
                endpoint,
                remotePort,
                proxyPort,
                errorDetails);

    }
    
    private void disactivate() {
        this.active = false;
        this.statusMessage = "Not active";
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDown() {
        return !active;
    }
}
