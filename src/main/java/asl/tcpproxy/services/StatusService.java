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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import asl.tcpproxy.AppConfig;

@Service
public class StatusService {

	@Autowired
	AppConfig config;

	@Autowired
	TunnelsService tunnelsService;

	public Map<String, Object> configurationInfo() {
		Map<String, Object> info = new HashMap<>();

		info.put("status", "TCP Proxy up and running");
		info.put("Client White List", config.getClientWhiteList());
		info.put("Connect timeout", config.getConnectTimeout());

		info.put("tunnels: ", tunnelsService.status());

		return info;
	}
}
