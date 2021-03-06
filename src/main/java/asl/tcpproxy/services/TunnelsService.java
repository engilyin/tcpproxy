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
 */
package asl.tcpproxy.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import asl.tcpproxy.AppConfig;
import asl.tcpproxy.tunnels.Tunnel;

@Service
public class TunnelsService {

	AppConfig config;

	List<Tunnel> tunnels;

	public TunnelsService(AppConfig config) {
        this.config = config;
    }

    @PostConstruct
	private void init() {
		tunnels = config.getTunnels().parallelStream().map(tunnelDesc -> createTunnel(tunnelDesc)).collect(Collectors.toList());
	}
    
    @PreDestroy
    private void close() {
        tunnels.forEach(tunnel -> tunnel.close());
    }
    
    public Map<String, String> status() {
        return tunnels.stream().collect(Collectors.toMap(t -> t.descriptor(), t -> t.toString()));
    }

	private Tunnel createTunnel(String tunnelDesc) {
		Tunnel tunnel = new Tunnel(tunnelDesc, config.getConnectTimeout(), config.getClientWhiteListAddresses());

		tunnel.open();

		return tunnel;
	}
}
