package com.alibaba.jvm.sandbox.repeater.module;

import com.alibaba.jvm.sandbox.api.ModuleException;
import com.alibaba.jvm.sandbox.api.resource.ConfigInfo;
import com.alibaba.jvm.sandbox.api.resource.ModuleManager;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.ApplicationModel;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.HttpUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.PropertyUtil;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.jvm.sandbox.repeater.plugin.Constants.REPEAT_HEARTBEAT_URL;

/**
 * {@link HeartbeatHandler}
 * <p>
 *
 * @author zhaoyb1990
 */
public class HeartbeatHandler {

    /** 十秒执行一次 */
    private static final long FREQUENCY = 10;

    /** 上报健康检查信息的http url请求路径 */
    private final static String HEARTBEAT_DOMAIN = PropertyUtil.getPropertyOrDefault(REPEAT_HEARTBEAT_URL, "");

    /** 用于控制心跳任务是否开始进行 */
    private AtomicBoolean initialize = new AtomicBoolean(false);

    /** 单线程的心跳健康检查定时任务 */
    private static ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
            new BasicThreadFactory.Builder().namingPattern("heartbeat-pool-%d").daemon(true).build());

    /** 沙箱配置 */
    private final ConfigInfo configInfo;
    /** sandbox模块管理器 */
    private final ModuleManager moduleManager;


    public HeartbeatHandler(ConfigInfo configInfo, ModuleManager moduleManager) {
        this.configInfo = configInfo;
        this.moduleManager = moduleManager;
    }

    public synchronized void start() {
        if (initialize.compareAndSet(false, true)) {
            executorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        innerReport();
                    } catch (Exception e) {
                        LogUtil.error("error occurred when report heartbeat", e);
                    }
                }
            }, 0, FREQUENCY, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (initialize.compareAndSet(true, false)) {
            executorService.shutdown();
        }
    }

    /**
     * 心跳上报
     * 发送http请求，上报目标java进程的健康状态和repeater模块的激活状态
     */
    private void innerReport() {
        Map<String, String> params = new HashMap<String, String>(8);
        params.put("appName", ApplicationModel.instance().getAppName());
        params.put("ip", ApplicationModel.instance().getHost());
        params.put("environment", ApplicationModel.instance().getEnvironment());
        params.put("port", configInfo.getServerAddress().getPort() + "");
        params.put("version", Constants.VERSION);
        try {
            params.put("status", moduleManager.isActivated(Constants.MODULE_ID) ? "ACTIVE" : "FROZEN");
        } catch (ModuleException e) {
            // ignore
        }
        HttpUtil.doGet(HEARTBEAT_DOMAIN, params);
    }
}
