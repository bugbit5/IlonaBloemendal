/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 码之科技工作室
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
package io.jpom.controller.system;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Tuple;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jpom.build.BuildUtil;
import io.jpom.common.BaseServerController;
import io.jpom.common.Const;
import io.jpom.common.interceptor.OptLog;
import io.jpom.model.AfterOpt;
import io.jpom.model.BaseEnum;
import io.jpom.model.data.*;
import io.jpom.model.enums.BuildReleaseMethod;
import io.jpom.model.log.UserOperateLogV1;
import io.jpom.plugin.ClassFeature;
import io.jpom.plugin.Feature;
import io.jpom.plugin.MethodFeature;
import io.jpom.service.dblog.BackupInfoService;
import io.jpom.service.dblog.BuildInfoService;
import io.jpom.service.dblog.DbBuildHistoryLogService;
import io.jpom.service.dblog.RepositoryService;
import io.jpom.service.node.ssh.SshService;
import io.jpom.system.ServerExtConfigBean;
import io.jpom.util.CommandUtil;
import io.jpom.util.GitUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * 数据库备份 controller
 * @author Hotstrip
 * @date 2021-11-18
 */
@RestController
@Feature(cls = ClassFeature.SYSTEM)
public class BackupInfoController extends BaseServerController {

	@Resource
	private BackupInfoService backupInfoService;

	/**
	 * 分页加载备份列表数据
	 * @param limit    每页条数
	 * @param page     页码
	 * @param name     备份名称
	 * @param backupType 备份类型{0: 全量, 1: 部分}
	 * @return json
	 */
	@PostMapping(value = "/system/backup/list")
	@Feature(method = MethodFeature.LOG)
	public Object loadBackupList(@ValidatorConfig(value = {@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")}, defaultVal = "10") int limit,
									 @ValidatorConfig(value = {@ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")}, defaultVal = "1") int page,
									 String name, Integer backupType) {
		Page pageObj = new Page(page, limit);
		pageObj.addOrder(new Order("modifyTimeMillis", Direction.DESC));

		// 设置查询参数
		Entity entity = Entity.create();
		entity.setIgnoreNull("name", name);
		entity.setIgnoreNull("backupType", backupType);

		// 查询数据库
		PageResult<BackupInfoModel> pageResult = backupInfoService.listPage(entity, pageObj);

		JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", pageResult);
		jsonObject.put("total", pageResult.getTotal());
		return jsonObject;
	}

	// 导入备份数据

	/**
	 * 删除备份数据
	 * @param id id
	 * @return
	 */
	@PostMapping(value = "/system/backup/delete")
	@Feature(method = MethodFeature.DEL)
	public Object deleteBackup(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "数据 id 不能为空") String id) {
		// 根据 id 查询备份信息
		BackupInfoModel backupInfoModel = backupInfoService.getByKey(id);
		Objects.requireNonNull(backupInfoModel, "备份数据不存在");

		// 删除对应的文件
		FileUtil.del(backupInfoModel.getFilePath());

		// 删除备份信息
		backupInfoService.delByKey(id);
		return JsonMessage.toJson(200, "获取成功");
	}

	// 还原备份数据

	// 创建备份任务

}
