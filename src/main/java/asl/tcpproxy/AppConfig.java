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
package asl.tcpproxy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties
@ConfigurationProperties
public class AppConfig {

	List<String> clientWhiteList = new ArrayList<String>();

	List<String> tunnels = new ArrayList<String>();

	int connectTimeout;

	public InetAddress[] getClientWhiteListAddresses() {
		try {

			List<InetAddress> addressList = new ArrayList<>();
			for (String addressString : getClientWhiteList()) {
				if ("*".equals(addressString.trim())) {
					return null;
				} else {

					addressList.add(InetAddress.getByName(addressString));

				}
			}
			if (addressList.size() > 0) {
				return addressList.toArray(new InetAddress[addressList.size()]);
			} else {
				return null;
			}

		} catch (UnknownHostException e) {
			throw new IllegalStateException(
					String.format("Unable to proceed. Wrong white list address: %s", e.toString()),
					e);
		}
	}

	public List<String> getClientWhiteList() {
		return clientWhiteList;
	}

	public void setClientWhiteList(List<String> clientWhiteList) {
		this.clientWhiteList = clientWhiteList;
	}

	public List<String> getTunnels() {
		return tunnels;
	}

	public void setTunnels(List<String> tunnels) {
		this.tunnels = tunnels;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

}
