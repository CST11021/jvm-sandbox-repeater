package com.alibaba.repeater.console.start.controller;

import com.alibaba.repeater.console.dal.dao.ModuleInfoDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * TODO whz 这里不是很合理，启动后删除所有的模块会导致启动后短时间内无法进行录制，最好是进行心跳检测失败后再进行删除
 *
 * @Author 盖伦
 * @Date 2023/1/4
 */
@Slf4j
@Component
public class ModuleFlush {

    @Resource
    private ModuleInfoDao moduleInfoDao;

    @PostConstruct
    public void flush() {
        moduleInfoDao.deleteAll();
        log.info("控制台已启动, 删除所有模块");
    }
}
