package cn.keepbx.jpom.controller.node.manage;

import cn.keepbx.jpom.common.BaseNodeController;
import cn.keepbx.jpom.model.data.ProjectInfoModel;
import cn.keepbx.jpom.model.data.UserModel;
import cn.keepbx.jpom.service.manage.ProjectInfoService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.annotation.Resource;
import java.io.File;

/**
 * 控制台
 *
 * @author jiangzeyin
 * @date 2018/9/28
 */
@Controller
@RequestMapping(value = "/node/manage/")
public class ConsoleController extends BaseNodeController {

    @Resource
    private ProjectInfoService projectInfoService;


    /**
     * 管理项目
     *
     * @return page
     */
    @RequestMapping(value = "console", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
    public String console(String id) {
//        ProjectInfoModel projectInfoModel = projectInfoService.getItem(getNode(), id);
//        if (projectInfoModel != null) {
//            UserModel userName = getUser();
//            setAttribute("projectInfo", projectInfoModel);
//            String logSize = projectInfoService.getLogSize(id);
//            setAttribute("logSize", logSize);
//            setAttribute("manager", userName.isProject(id));
//
//            //获取日志备份路径
//            File logBack = projectInfoModel.getLogBack();
//            if (logBack.exists() && logBack.isDirectory()) {
//                setAttribute("logBack", true);
//            }
//        }
        return "node/manage/console";
    }


}
