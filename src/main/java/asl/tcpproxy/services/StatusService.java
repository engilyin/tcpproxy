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
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import asl.tcpproxy.AppConfig;

@Service
public class StatusService {

    AppConfig config;

    TunnelsService tunnelsService;

    public StatusService(AppConfig config, TunnelsService tunnelsService) {
        this.config = config;
        this.tunnelsService = tunnelsService;
    }

    public String status() {
        return "TCP Proxy up and running";
    }
    
    public int timeout() {
        return config.getConnectTimeout();
    }

    public List<String> clientWhiteList() {
        return config.getClientWhiteList();
    }
    
    public Map<String, String> tunnels() {
        return tunnelsService.status();
    }
    
    public String appVersion() {
        return config.appVersion();
    }

    public Map<String, Object> configurationInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("status", status());
        info.put("Client White List", clientWhiteList());
        info.put("Connect timeout", timeout());

        info.put("tunnels: ", tunnelsService.status());

        return info;
    }
}
