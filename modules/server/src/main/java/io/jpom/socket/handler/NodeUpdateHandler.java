package io.jpom.socket.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.jpom.common.forward.NodeForward;
import io.jpom.common.forward.NodeUrl;
import io.jpom.model.AgentFileModel;
import io.jpom.model.WebSocketMessageModel;
import io.jpom.model.data.NodeModel;
import io.jpom.model.data.NodeVersionModel;
import io.jpom.model.data.UserModel;
import io.jpom.service.node.AgentFileService;
import io.jpom.service.node.NodeService;
import io.jpom.socket.BaseProxyHandler;
import io.jpom.socket.ConsoleCommandOp;
import io.jpom.socket.client.NodeClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * 节点管理控制器
 *
 * @author lf
 */
public class NodeUpdateHandler extends BaseProxyHandler {

	private final ConcurrentMap<String, NodeClient> clientMap = new ConcurrentHashMap<>();

	private AgentFileService agentFileService;
	private NodeService nodeService;

	public NodeUpdateHandler() {
		super(null);
	}

	private void init() {
		agentFileService = SpringUtil.getBean(AgentFileService.class);
		nodeService = SpringUtil.getBean(NodeService.class);
	}

	@Override
	protected boolean showHelloMsg() {
		return false;
	}

	@Override
	protected Object[] getParameters(Map<String, Object> attributes) {
		return new Object[]{};
	}

	private void pullNodeList(WebSocketSession session) {
		List<NodeModel> nodeModelList = nodeService.list();
		for (NodeModel model : nodeModelList) {
			if (clientMap.containsKey(model.getId())) {
				continue;
			}
			Map<String, Object> attributes = session.getAttributes();
			String url = NodeForward.getSocketUrl(model, NodeUrl.NodeUpdate, (UserModel) attributes.get("userInfo"));
			// 连接节点
			try {
				NodeClient client = new NodeClient(url, model, session);
				clientMap.put(model.getId(), client);
			} catch (Exception e) {
				DefaultSystemLog.getLog().error("创建插件端连接失败", e);
			}
		}
	}

	@Override
	public void destroy(WebSocketSession session) {
		for (String key : clientMap.keySet()) {
			NodeClient client = clientMap.get(key);
			if (client.isOpen()) {
				client.close();
			}
		}
		clientMap.clear();
		//
		super.destroy(session);
	}

	@Override
	protected void handleTextMessage(Map<String, Object> attributes, WebSocketSession session, JSONObject json, ConsoleCommandOp consoleCommandOp) throws IOException {
		WebSocketMessageModel model = WebSocketMessageModel.getInstance(json.toString());
		this.init();
		boolean pull = false;
		switch (model.getCommand()) {
			case "getNodeList":
				model.setData(getNodeList());
				pull = true;
				break;
			case "getAgentVersion":
				model.setData(getAgentVersion());
				break;
			case "updateNode":
				updateNode(model, session);
				break;
			default:
				break;
		}

		if (model.getData() != null) {
			this.sendMsg(model, session);
		}
		if (pull) {
			pullNodeList(session);
		}
	}

	/**
	 * 更新节点
	 *
	 * @param model 参数
	 */
	private void updateNode(WebSocketMessageModel model, WebSocketSession session) {
		JSONObject params = (JSONObject) model.getParams();
		JSONArray ids = params.getJSONArray("ids");
		if (CollUtil.isEmpty(ids)) {
			return;
		}
		try {
			AgentFileModel agentFileModel = agentFileService.getItem("agent");
			//
			if (agentFileModel == null || !FileUtil.exist(agentFileModel.getSavePath())) {
				WebSocketMessageModel error = new WebSocketMessageModel("onError", "");
				error.setData("Agent JAR包不存在");
				session.sendMessage(new TextMessage(error.toString()));
				return;
			}

			for (int i = 0; i < ids.size(); i++) {
				int finalI = i;
				ThreadUtil.execute(() -> {
					try {
						String id = ids.getString(finalI);
						NodeModel node = nodeService.getItem(id);
						if (node == null) {
							this.sendMsg(model.setData("没有对应的节点：" + id), session);
							return;
						}
						NodeClient client = clientMap.get(node.getId());
						if (client == null) {
							this.sendMsg(model.setData("对应的插件端还没有被初始化：" + id), session);
							return;
						}
						if (client.isOpen()) {
							// 发送文件信息
							WebSocketMessageModel webSocketMessageModel = new WebSocketMessageModel("upload", id);
							webSocketMessageModel.setNodeId(id);
							webSocketMessageModel.setParams(agentFileModel);
							client.send(webSocketMessageModel.toString());
							//
							try (FileInputStream fis = new FileInputStream(agentFileModel.getSavePath())) {
								// 发送文件内容
								int len;
								byte[] buffer = new byte[1024 * 1024];
								while ((len = fis.read(buffer)) > 0) {
									client.send(ByteBuffer.wrap(buffer, 0, len));
								}
							}
							WebSocketMessageModel restartMessage = new WebSocketMessageModel("restart", id);
							client.send(restartMessage.toString());
						} else {
							this.sendMsg(model.setData("节点连接丢失"), session);
						}
					} catch (Exception e) {
						DefaultSystemLog.getLog().error("升级失败:" + model, e);
						this.sendMsg(model.setData("节点升级失败：" + e.getMessage()), session);
					}
				});
			}
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("升级失败", e);
		}
	}

	private void sendMsg(WebSocketMessageModel model, WebSocketSession session) {
		try {
			synchronized (NodeUpdateHandler.class) {
				session.sendMessage(new TextMessage(model.toString()));
			}
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("发送消息失败", e);
		}
	}

	/**
	 * 获取当前系统缓存的Agent
	 *
	 * @return
	 */
	private String getAgentVersion() {
		AgentFileModel agentFileModel = agentFileService.getItem("agent");
		String version = "";
		if (agentFileModel == null) {
			return version;
		}
		try (JarFile jarFile = new JarFile(agentFileModel.getSavePath())) {
			Manifest manifest = jarFile.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
		} catch (Exception e) {
			DefaultSystemLog.getLog().error("解析 jar 异常", e);
		}
		return version;
	}

	/**
	 * 获取节点列表
	 *
	 * @return 节点列表
	 */
	private List<NodeVersionModel> getNodeList() {
		NodeService nodeService = SpringUtil.getBean(NodeService.class);
		List<NodeModel> nodeModels = nodeService.list();
		List<NodeVersionModel> result = new ArrayList<>();
		for (NodeModel node : nodeModels) {
			NodeVersionModel model = new NodeVersionModel();
			model.setId(node.getId());
			model.setName(node.getName());
			model.setGroup(node.getGroup());
			result.add(model);
		}
		return result;
	}
}
