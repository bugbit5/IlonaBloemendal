package cn.keepbx.build;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.jiangzeyin.common.DefaultSystemLog;
import cn.jiangzeyin.common.spring.SpringUtil;
import cn.keepbx.jpom.model.data.BuildModel;
import cn.keepbx.jpom.service.build.BuildService;

import java.io.File;

/**
 * 构建的基础类
 *
 * @author bwcx_jzy
 * @date 2019/7/19
 */
public abstract class BaseBuild {

    private File logFile;
    String buildModelId;

    BaseBuild(File logFile, String buildModelId) {
        this.logFile = logFile;
        this.buildModelId = buildModelId;
    }

    protected void log(String title, Throwable throwable, BuildModel.Status status) {
        DefaultSystemLog.ERROR().error(title, throwable);
        FileUtil.appendLines(CollectionUtil.toList(title), this.logFile, CharsetUtil.CHARSET_UTF_8);
        String s = ExceptionUtil.stacktraceToString(throwable);
        FileUtil.appendLines(CollectionUtil.toList(s), this.logFile, CharsetUtil.CHARSET_UTF_8);
        updateStatus(status);
    }

    protected void log(String info) {
        FileUtil.appendLines(CollectionUtil.toList(info), this.logFile, CharsetUtil.CHARSET_UTF_8);
    }

    protected boolean updateStatus(BuildModel.Status status) {
        BuildService buildService = SpringUtil.getBean(BuildService.class);
        BuildModel item = buildService.getItem(this.buildModelId);
        item.setStatus(status.getCode());
        buildService.updateItem(item);
        return true;
    }
}
