package cn.keepbx.jpom.common.commander.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSpliter;
import cn.hutool.core.util.StrUtil;
import cn.keepbx.jpom.common.commander.AbstractProjectCommander;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.system.NetstatModel;
import cn.keepbx.util.CommandUtil;
import cn.keepbx.util.JvmUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * windows 版
 *
 * @author Administrator
 */
public class WindowsProjectCommander extends AbstractProjectCommander {

    @Override
    public String buildCommand(ProjectInfoModel projectInfoModel) {
        String classPath = ProjectInfoModel.getClassPathLib(projectInfoModel);
        if (StrUtil.isBlank(classPath)) {
            return null;
        }
        // 拼接命令
        String jvm = projectInfoModel.getJvm();
        String tag = projectInfoModel.getId();
        String mainClass = projectInfoModel.getMainClass();
        String args = projectInfoModel.getArgs();
        return String.format("javaw %s -%s=%s -DJpom.basedir=%s " +
                        "%s  %s  %s >> %s &",
                jvm, JvmUtil.POM_PID_TAG, tag, projectInfoModel.getAbsoluteLib(),
                classPath, mainClass, args, projectInfoModel.getAbsoluteLog());
    }

    @Override
    public String stop(ProjectInfoModel projectInfoModel) throws Exception {
        String result = super.stop(projectInfoModel);
        String tag = projectInfoModel.getId();
        // 查询状态，如果正在运行，则执行杀进程命令
        int pid = parsePid(result);
        if (pid > 0) {
            String cmd = String.format("taskkill /F /PID %s", pid);
            CommandUtil.asyncExeLocalCommand(FileUtil.file(projectInfoModel.getLib()), cmd);
            loopCheckRun(projectInfoModel.getId(), false);
            result = status(tag);
        }
        return result;
    }

    @Override
    public List<NetstatModel> listNetstat(int pId, boolean listening) {
        String cmd;
        if (listening) {
            cmd = "netstat -nao -p tcp | findstr \"LISTENING\" | findstr " + pId;
        } else {
            cmd = "netstat -nao -p tcp | findstr /V \"CLOSE_WAIT\" | findstr " + pId;
        }
        String result = CommandUtil.execSystemCommand(cmd);
        List<String> netList = StrSpliter.splitTrim(result, StrUtil.LF, true);
        if (netList == null || netList.size() <= 0) {
            return null;
        }
        List<NetstatModel> array = new ArrayList<>();
        for (String str : netList) {
            List<String> list = StrSpliter.splitTrim(str, " ", true);
            if (list.size() < 5) {
                continue;
            }
            NetstatModel netstatModel = new NetstatModel();
            netstatModel.setProtocol(list.get(0));
            netstatModel.setLocal(list.get(1));
            netstatModel.setForeign(list.get(2));
            netstatModel.setStatus(list.get(3));
            netstatModel.setName(list.get(4));
            array.add(netstatModel);
        }
        return array;
    }
}
