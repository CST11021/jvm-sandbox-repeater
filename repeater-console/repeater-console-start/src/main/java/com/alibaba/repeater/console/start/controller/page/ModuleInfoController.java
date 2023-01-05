package com.alibaba.repeater.console.start.controller.page;

import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.repeater.console.common.domain.ModuleInfoBO;
import com.alibaba.repeater.console.common.domain.PageResult;
import com.alibaba.repeater.console.common.params.ModuleInfoParams;
import com.alibaba.repeater.console.service.ModuleInfoService;
import com.alibaba.repeater.console.start.controller.vo.PagerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.List;

/**
 * {@link ModuleInfoController}
 * <p>
 * 在线模块页面
 *
 * @author zhaoyb1990
 */
@Slf4j
@RequestMapping("/module")
@Controller
public class ModuleInfoController {

    @Resource
    private ModuleInfoService moduleInfoService;

    /**
     * 模块列表
     *
     * @param params
     * @param model
     * @return
     */
    @RequestMapping("list.htm")
    public String list(@ModelAttribute("requestParams") ModuleInfoParams params, Model model) {
        PageResult<ModuleInfoBO> result = moduleInfoService.query(params);
        PagerAdapter.transform0(result, model);
        return "module/list";
    }

    /**
     * 按应用名查询模块
     *
     * @param appName
     * @return
     */
    @ResponseBody
    @RequestMapping("/byName.json")
    public RepeaterResult<List<ModuleInfoBO>> list(@RequestParam("appName") String appName) {
        return moduleInfoService.query(appName);
    }

    /**
     * 激活模块
     *
     * @param params
     * @return
     */
    @ResponseBody
    @RequestMapping("/active.json")
    public RepeaterResult<ModuleInfoBO> active(@ModelAttribute("requestParams") ModuleInfoParams params) {
        return moduleInfoService.active(params);
    }

    /**
     * 冻结模块
     *
     * @param params
     * @return
     */
    @ResponseBody
    @RequestMapping("/frozen.json")
    public RepeaterResult<ModuleInfoBO> frozen(@ModelAttribute("requestParams") ModuleInfoParams params) {
        return moduleInfoService.frozen(params);
    }

    /**
     * 安装模块
     *
     * @param params
     * @return
     */
    @ResponseBody
    @RequestMapping("/install.json")
    public RepeaterResult<String> install(@ModelAttribute("requestParams") ModuleInfoParams params) {
        return moduleInfoService.install(params);
    }

    /**
     * 心跳上报（定时任务触发）
     *
     * @param params
     * @return
     */
    @ResponseBody
    @RequestMapping("/report.json")
    public RepeaterResult<ModuleInfoBO> list(@ModelAttribute("requestParams") ModuleInfoBO params) {
        // log.info("心跳上报: {}", JSONObject.toJSONString(params));
        return moduleInfoService.report(params);
    }

    /**
     * 模块刷新
     *
     * @param params
     * @return
     */
    @ResponseBody
    @RequestMapping("/reload.json")
    public RepeaterResult<String> reload(@ModelAttribute("requestParams") ModuleInfoParams params) {
        return moduleInfoService.reload(params);
    }
}
