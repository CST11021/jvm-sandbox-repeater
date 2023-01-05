package com.alibaba.repeater.console.start.controller.page;

import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.repeater.console.common.domain.ReplayBO;
import com.alibaba.repeater.console.common.params.ReplayParams;
import com.alibaba.repeater.console.service.ReplayService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * {@link ReplayController}
 * <p>
 *
 * @author zhaoyb1990
 */
@Controller
@RequestMapping("/replay")
public class ReplayController {

    @Resource
    private ReplayService replayService;

    /**
     * 查看录制回放详情
     *
     * @param params
     * @param model
     * @return
     */
    @RequestMapping("detail.htm")
    public String detail(@ModelAttribute("requestParams") ReplayParams params, Model model) {
        RepeaterResult<ReplayBO> result = replayService.query(params);
        if (!result.isSuccess()) {
            return "/error/404";
        }
        model.addAttribute("replay", result.getData());
        model.addAttribute("record", result.getData().getRecord());
        return "replay/detail";
    }

    /**
     * 执行回放功能
     *
     * @param params
     * @return
     */
    @RequestMapping("execute.json")
    @ResponseBody
    public RepeaterResult<String> replay(@ModelAttribute("requestParams") ReplayParams params) {
        return replayService.replay(params);
    }
}
