package cn.keepbx.jpom.controller.build;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import cn.hutool.db.sql.Direction;
import cn.hutool.db.sql.Order;
import cn.hutool.extra.servlet.ServletUtil;
import cn.jiangzeyin.common.JsonMessage;
import cn.jiangzeyin.common.validator.ValidatorConfig;
import cn.jiangzeyin.common.validator.ValidatorItem;
import cn.jiangzeyin.common.validator.ValidatorRule;
import cn.keepbx.build.BuildUtil;
import cn.keepbx.jpom.common.BaseServerController;
import cn.keepbx.jpom.model.BaseEnum;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.model.log.BuildHistoryLog;
import cn.keepbx.jpom.model.vo.BuildHistoryLogVo;
import cn.keepbx.jpom.service.build.BuildService;
import cn.keepbx.jpom.service.dblog.DbBuildHistoryLogService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author bwcx_jzy
 * @date 2019/7/17
 **/
@Controller
@RequestMapping(value = "/build")
public class BuildLogPageController extends BaseServerController {

    @Resource
    private BuildService buildService;
    @Resource
    private DbBuildHistoryLogService dbBuildHistoryLogService;

    @RequestMapping(value = "logPage.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String logPage() {
        return "build/logPage";
    }

    @RequestMapping(value = "history.html", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String logList() throws IOException {
        JSONArray jsonArray = BaseEnum.toJSONArray(BuildModel.Status.class);
        setAttribute("status", jsonArray);
        //
        List<BuildModel> list = buildService.list();
        setAttribute("buildS", list);
        //
        JSONArray releaseMethods = BaseEnum.toJSONArray(BuildModel.ReleaseMethod.class);
        setAttribute("releaseMethods", releaseMethods);
        return "build/history";
    }

    /**
     * 下载构建物
     *
     * @param logId 日志id
     * @throws SQLException e
     * @throws IOException  e
     */
    @RequestMapping(value = "download_file.html", method = RequestMethod.GET)
    @ResponseBody
    public void downloadFile(@ValidatorConfig(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "没有数据")) String logId) throws SQLException, IOException {
        BuildHistoryLog buildHistoryLog = dbBuildHistoryLogService.getByKey(logId);
        if (buildHistoryLog == null) {
            return;
        }
        BuildModel item = buildService.getItem(buildHistoryLog.getBuildDataId());
        if (item == null) {
            return;
        }
        File logFile = BuildUtil.getHistoryPackageFile(item.getId(), buildHistoryLog.getBuildNumberId(), buildHistoryLog.getResultDirFile());
        if (!logFile.exists()) {
            return;
        }
        if (logFile.isFile()) {
            ServletUtil.write(getResponse(), logFile);
        } else {
            File zipFile = BuildUtil.isDirPackage(logFile);
            assert zipFile != null;
            ServletUtil.write(getResponse(), zipFile);
        }
    }

    @RequestMapping(value = "history_list.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    @ResponseBody
    public String historyList(String time, String status,
                              @ValidatorConfig(value = {
                                      @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "limit error")
                              }, defaultVal = "10") int limit,
                              @ValidatorConfig(value = {
                                      @ValidatorItem(value = ValidatorRule.POSITIVE_INTEGER, msg = "page error")
                              }, defaultVal = "1") int page,
                              String buildDataId) {
        Page pageObj = new Page(page, limit);
        Entity entity = Entity.create();
        pageObj.addOrder(new Order("startTime", Direction.DESC));
        // 时间
        if (StrUtil.isNotEmpty(time)) {
            String[] val = StrUtil.split(time, "~");
            if (val.length == 2) {
                DateTime startDateTime = DateUtil.parse(val[0], DatePattern.NORM_DATETIME_FORMAT);
                entity.set("startTime", ">= " + startDateTime.getTime());

                DateTime endDateTime = DateUtil.parse(val[1], DatePattern.NORM_DATETIME_FORMAT);
                if (startDateTime.equals(endDateTime)) {
                    endDateTime = DateUtil.endOfDay(endDateTime);
                }
                entity.set("startTime ", "<= " + endDateTime.getTime());
            }
        }
        BaseEnum anEnum = null;
        if (StrUtil.isNotEmpty(status)) {
            Integer integer = Convert.toInt(status);
            if (integer != null) {
                anEnum = BaseEnum.getEnum(BuildModel.Status.class, integer);
            }
        }

        if (anEnum != null) {
            entity.set("status", anEnum.getCode());
        }
        if (StrUtil.isNotBlank(buildDataId)) {
            entity.set("buildDataId", buildDataId);
        }
        PageResult<BuildHistoryLog> pageResult = dbBuildHistoryLogService.listPage(entity, pageObj);
        List<BuildHistoryLogVo> buildHistoryLogVos = new ArrayList<>();
        pageResult.forEach(buildHistoryLog -> {
            BuildHistoryLogVo historyLogVo = new BuildHistoryLogVo();
            BeanUtil.copyProperties(buildHistoryLog, historyLogVo);
            String dataId = buildHistoryLog.getBuildDataId();
            try {
                BuildModel item = buildService.getItem(dataId);
                historyLogVo.setBuildName(item.getName());
            } catch (IOException ignored) {
            }
            buildHistoryLogVos.add(historyLogVo);
        });
        JSONObject jsonObject = JsonMessage.toJson(200, "获取成功", buildHistoryLogVos);
        jsonObject.put("total", pageResult.getTotal());
        return jsonObject.toString();
    }
}
