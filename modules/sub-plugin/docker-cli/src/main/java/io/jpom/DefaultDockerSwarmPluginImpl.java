/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Code Technology Studio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.jpom;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.JoinSwarmCmd;
import com.github.dockerjava.api.model.Swarm;
import com.github.dockerjava.api.model.SwarmSpec;
import io.jpom.plugin.IDefaultPlugin;
import io.jpom.plugin.PluginConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * docker swarm
 *
 * @author bwcx_jzy
 * @since 2022/2/13
 */
@PluginConfig(name = "docker-cli:swarm")
@Slf4j
public class DefaultDockerSwarmPluginImpl implements IDefaultPlugin {

	@Override
	public Object execute(Object main, Map<String, Object> parameter) {
		String type = main.toString();
		switch (type) {
			case "inSpectSwarm":
				return this.inSpectSwarmCmd(parameter);
			case "tryInitializeSwarm":
				return this.tryInitializeSwarmCmd(parameter);
			case "joinSwarm":
				this.joinSwarmCmd(parameter);
				return null;
			default:
				break;
		}
		return null;
	}

	private void joinSwarmCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			String token = (String) parameter.get("token");
			String remoteAddrs = (String) parameter.get("remoteAddrs");
			JoinSwarmCmd joinSwarmCmd = dockerClient.joinSwarmCmd()
					.withRemoteAddrs(StrUtil.splitTrim(remoteAddrs, StrUtil.COMMA))
					.withJoinToken(token);
			joinSwarmCmd.exec();
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	private JSONObject tryInitializeSwarmCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			// 先尝试获取
			try {
				Swarm exec = dockerClient.inspectSwarmCmd().exec();
				JSONObject jsonObject = (JSONObject) JSONObject.toJSON(exec);
				if (jsonObject != null) {
					return jsonObject;
				}
			} catch (Exception ignored) {
				//
			}
			// 尝试初始化
			SwarmSpec swarmSpec = new SwarmSpec();
			swarmSpec.withName("default");
			dockerClient.initializeSwarmCmd(swarmSpec).exec();
			// 获取信息
			Swarm exec = dockerClient.inspectSwarmCmd().exec();
			return (JSONObject) JSONObject.toJSON(exec);
		} finally {
			IoUtil.close(dockerClient);
		}
	}

	private JSONObject inSpectSwarmCmd(Map<String, Object> parameter) {
		DockerClient dockerClient = DockerUtil.build(parameter);
		try {
			Swarm exec = dockerClient.inspectSwarmCmd().exec();
			return (JSONObject) JSONObject.toJSON(exec);
		} finally {
			IoUtil.close(dockerClient);
		}
	}
}
