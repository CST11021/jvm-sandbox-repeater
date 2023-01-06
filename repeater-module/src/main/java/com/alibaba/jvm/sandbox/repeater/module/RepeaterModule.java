package com.alibaba.jvm.sandbox.repeater.module;

import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.Information.Mode;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleException;
import com.alibaba.jvm.sandbox.api.ModuleLifecycle;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.resource.*;
import com.alibaba.jvm.sandbox.repeater.module.advice.SpringInstantiateAdvice;
import com.alibaba.jvm.sandbox.repeater.module.classloader.PluginClassLoader;
import com.alibaba.jvm.sandbox.repeater.module.classloader.PluginClassRouting;
import com.alibaba.jvm.sandbox.repeater.module.impl.JarFileLifeCycleManager;
import com.alibaba.jvm.sandbox.repeater.module.util.LogbackUtils;
import com.alibaba.jvm.sandbox.repeater.plugin.Constants;
import com.alibaba.jvm.sandbox.repeater.plugin.api.Broadcaster;
import com.alibaba.jvm.sandbox.repeater.plugin.api.ConfigManager;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.LifecycleManager;
import com.alibaba.jvm.sandbox.repeater.plugin.core.StandaloneSwitch;
import com.alibaba.jvm.sandbox.repeater.plugin.core.bridge.ClassloaderBridge;
import com.alibaba.jvm.sandbox.repeater.plugin.core.bridge.RepeaterBridge;
import com.alibaba.jvm.sandbox.repeater.plugin.core.eventbus.EventBusInner;
import com.alibaba.jvm.sandbox.repeater.plugin.core.eventbus.RepeatEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.ApplicationModel;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializeException;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.Serializer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.serialize.SerializerProvider;
import com.alibaba.jvm.sandbox.repeater.plugin.core.spring.SpringContextInnerContainer;
import com.alibaba.jvm.sandbox.repeater.plugin.core.trace.TtlConcurrentAdvice;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.ExecutorInner;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.PathUtils;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.PropertyUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.core.wrapper.SerializerWrapper;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeatMeta;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterResult;
import com.alibaba.jvm.sandbox.repeater.plugin.exception.PluginLifeCycleException;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.Repeater;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.SubscribeSupporter;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.alibaba.jvm.sandbox.repeater.plugin.Constants.REPEAT_SPRING_ADVICE_SWITCH;

/**
 * <p>
 *
 * @author zhaoyb1990
 */
@MetaInfServices(Module.class)
@Information(id = com.alibaba.jvm.sandbox.repeater.module.Constants.MODULE_ID, author = "zhaoyb1990", version = com.alibaba.jvm.sandbox.repeater.module.Constants.VERSION)
public class RepeaterModule implements Module, ModuleLifecycle {

    private final static Logger log = LoggerFactory.getLogger(RepeaterModule.class);

    // 以下这些组件jvm-sandbox框架会自动注入进来

    @Resource
    private ModuleEventWatcher eventWatcher;
    @Resource
    private ModuleController moduleController;
    @Resource
    private ConfigInfo configInfo;
    @Resource
    private ModuleManager moduleManager;
    @Resource
    private LoadedClassDataSource loadedClassDataSource;



    /** repeater插件配置管理器 */
    private ConfigManager configManager;
    /** 用于发送录制和回放的请求消息 */
    private Broadcaster broadcaster;
    /** InvokePlugin：所有的repeater插件都需要实现该接口，invokePlugins表示的是加载的插件集合 */
    private List<InvokePlugin> invokePlugins;

    private InvocationListener invocationListener;

    private LifecycleManager lifecycleManager;

    /** 用于标记是否已经初始化过 */
    private AtomicBoolean initialized = new AtomicBoolean(false);
    /** 用于监听心跳 */
    private HeartbeatHandler heartbeatHandler;

    /**
     * 模块加载，模块开始加载之前调用！
     * <p>
     * 模块加载是模块生命周期的开始，在模块生命中期中有且只会调用一次。
     * 这里抛出异常将会是阻止模块被加载的唯一方式，如果模块判定加载失败，将会释放掉所有预申请的资源，模块也不会被沙箱所感知
     * </p>
     *
     * @throws Throwable 加载模块失败
     */
    @Override
    public void onLoad() throws Throwable {
        // 初始化日志框架
        LogbackUtils.init(PathUtils.getConfigPath() + "/repeater-logback.xml");
        Mode mode = configInfo.getMode();
        log.info("module on loaded,id={},version={},mode={}", com.alibaba.jvm.sandbox.repeater.module.Constants.MODULE_ID, com.alibaba.jvm.sandbox.repeater.module.Constants.VERSION, mode);
        /* agent方式启动 */
        if (mode == Mode.AGENT && Boolean.valueOf(PropertyUtil.getPropertyOrDefault(REPEAT_SPRING_ADVICE_SWITCH, ""))) {
            log.info("agent launch mode,use Spring Instantiate Advice to register bean.");
            SpringContextInnerContainer.setAgentLaunch(true);
            SpringInstantiateAdvice.watcher(this.eventWatcher).watch();
            moduleController.active();
        }
    }

    /**
     * 模块卸载，模块开始卸载之前调用！
     * <p>
     * 模块卸载是模块生命周期的结束，在模块生命中期中有且只会调用一次。
     * 这里抛出异常将会是阻止模块被卸载的唯一方式，如果模块判定卸载失败，将不会造成任何资源的提前关闭与释放，模块将能继续正常工作
     * </p>
     *
     * @throws Throwable 卸载模块失败
     */
    @Override
    public void onUnload() throws Throwable {
        if (lifecycleManager != null) {
            lifecycleManager.release();
        }
        heartbeatHandler.stop();
    }

    /**
     * 模块激活
     * <p>
     * 模块被激活后，模块所增强的类将会被激活，所有{@link com.alibaba.jvm.sandbox.api.listener.EventListener}将开始收到对应的事件
     * </p>
     * <p>
     * 这里抛出异常将会是阻止模块被激活的唯一方式
     * </p>
     *
     * @throws Throwable 模块激活失败
     */
    @Override
    public void onActive() throws Throwable {
        log.info("onActive");
    }

    /**
     * 模块冻结
     * <p>
     * 模块被冻结后，模块所持有的所有{@link com.alibaba.jvm.sandbox.api.listener.EventListener}将被静默，无法收到对应的事件。
     * 需要注意的是，模块冻结后虽然不再收到相关事件，但沙箱给对应类织入的增强代码仍然还在。
     * </p>
     * <p>
     * 这里抛出异常将会是阻止模块被冻结的唯一方式
     * </p>
     *
     * @throws Throwable 模块冻结失败
     */
    @Override
    public void onFrozen() throws Throwable {
        log.info("onFrozen");
    }

    /**
     * 模块加载完成，模块完成加载后调用！
     * <p>
     * 模块完成加载是在模块完成所有资源加载、分配之后的回调，在模块生命中期中有且只会调用一次。
     * 这里抛出异常不会影响模块被加载成功的结果。
     * </p>
     * <p>
     * 模块加载完成之后，所有的基于模块的操作都可以在这个回调中进行
     * </p>
     */
    @Override
    public void loadCompleted() {
        ExecutorInner.execute(new Runnable() {
            @Override
            public void run() {
                configManager = StandaloneSwitch.instance().getConfigManager();
                broadcaster = StandaloneSwitch.instance().getBroadcaster();
                invocationListener = new DefaultInvocationListener(broadcaster);

                // 获取repeater的配置
                RepeaterResult<RepeaterConfig> pr = configManager.pullConfig();
                if (pr.isSuccess()) {
                    log.info("pull repeater config success,config={}", pr.getData());
                    // 缓存所有的类加载器
                    ClassloaderBridge.init(loadedClassDataSource);
                    initialize(pr.getData());
                }
            }
        });

        // 开始心跳健康检查
        heartbeatHandler = new HeartbeatHandler(configInfo, moduleManager);
        heartbeatHandler.start();
    }



    /**
     * 回放http接口
     *
     * @param req    请求参数
     * @param writer printWriter
     */
    @Command("repeat")
    public void repeat(final Map<String, String> req, final PrintWriter writer) {
        try {
            String data = req.get(Constants.DATA_TRANSPORT_IDENTIFY);
            if (StringUtils.isEmpty(data)) {
                writer.write("invalid request, cause parameter {" + Constants.DATA_TRANSPORT_IDENTIFY + "} is required");
                return;
            }
            RepeatEvent event = new RepeatEvent();
            Map<String, String> requestParams = new HashMap<String, String>(16);
            for (Map.Entry<String, String> entry : req.entrySet()) {
                requestParams.put(entry.getKey(), entry.getValue());
            }
            event.setRequestParams(requestParams);
            EventBusInner.post(event);
            writer.write("submit success");
        } catch (Throwable e) {
            writer.write(e.getMessage());
        }
    }

    /**
     * 重新加载插件
     *
     * @param req    请求参数
     * @param writer printWriter
     */
    @Command("reload")
    public void reload(final Map<String, String> req, final PrintWriter writer) {
        try {
            if (initialized.compareAndSet(true,false)) {
                reload();
                initialized.compareAndSet(false, true);
            }
        } catch (Throwable throwable) {
            writer.write(throwable.getMessage());
            initialized.compareAndSet(false, true);
        }
    }

    /**
     * 回放http接口(暴露JSON回放）
     *
     * @param req    请求参数
     * @param writer printWriter
     */
    @Command("repeatWithJson")
    public void repeatWithJson(final Map<String, String> req, final PrintWriter writer) {
        try {
            String data = req.get(Constants.DATA_TRANSPORT_IDENTIFY);
            if (StringUtils.isEmpty(data)) {
                writer.write("invalid request, cause parameter {" + Constants.DATA_TRANSPORT_IDENTIFY + "} is required");
                return;
            }
            RepeatMeta meta = SerializerProvider.instance().provide(Serializer.Type.JSON).deserialize(data, RepeatMeta.class);
            req.put(Constants.DATA_TRANSPORT_IDENTIFY, SerializerProvider.instance().provide(Serializer.Type.HESSIAN).serialize2String(meta));
            repeat(req, writer);
        } catch (Throwable e) {
            writer.write(e.getMessage());
        }
    }

    /**
     * 配置推送接口
     *
     * @param req    请求参数
     * @param writer printWriter
     */
    @Command("pushConfig")
    public void pushConfig(final Map<String, String> req, final PrintWriter writer) {
        String data = req.get(Constants.DATA_TRANSPORT_IDENTIFY);
        if (StringUtils.isEmpty(data)) {
            writer.write("invalid request, cause parameter {" + Constants.DATA_TRANSPORT_IDENTIFY + "} is required");
            return;
        }
        try {
            RepeaterConfig config = SerializerWrapper.hessianDeserialize(data, RepeaterConfig.class);
            ApplicationModel.instance().setConfig(config);
            noticeConfigChange(config);
            writer.write("config push success");
        } catch (SerializeException e) {
            writer.write("invalid request, cause deserialize config failed, reason = {" + e.getMessage() + "}");
        }
    }


    /**
     * 初始化插件
     *
     * @param config 配置文件
     */
    private synchronized void initialize(RepeaterConfig config) {
        if (initialized.compareAndSet(false, true)) {
            try {
                ApplicationModel.instance().setConfig(config);

                // 特殊路由表;
                PluginClassLoader.Routing[] routingArray = PluginClassRouting.wellKnownRouting(configInfo.getMode() == Mode.AGENT, 20L);

                // 获取插件jar包的绝对路径
                String pluginsPath;
                if (StringUtils.isEmpty(config.getPluginsPath())) {
                    pluginsPath = PathUtils.getPluginPath();
                } else {
                    pluginsPath = config.getPluginsPath();
                }

                lifecycleManager = new JarFileLifeCycleManager(pluginsPath, routingArray);

                // 1、装载插件
                invokePlugins = lifecycleManager.loadInvokePlugins();
                for (InvokePlugin invokePlugin : invokePlugins) {
                    try {
                        if (invokePlugin.enable(config)) {
                            log.info("enable plugin {} success", invokePlugin.identity());
                            invokePlugin.watch(eventWatcher, invocationListener);
                            invokePlugin.onConfigChange(config);
                        }
                    } catch (PluginLifeCycleException e) {
                        log.info("watch plugin occurred error", e);
                    }
                }

                // 2、装载回放器
                List<Repeater> repeaters = lifecycleManager.loadRepeaters();
                for (Repeater repeater : repeaters) {
                    if (repeater.enable(config)) {
                        repeater.setBroadcast(broadcaster);
                    }
                }
                RepeaterBridge.instance().build(repeaters);

                // 3、装载消息订阅器
                List<SubscribeSupporter> subscribes = lifecycleManager.loadSubscribes();
                for (SubscribeSupporter subscribe : subscribes) {
                    subscribe.register();
                }
                TtlConcurrentAdvice.watcher(eventWatcher).watch(config);
            } catch (Throwable throwable) {
                initialized.compareAndSet(true, false);
                log.error("error occurred when initialize module", throwable);
            }
        }
    }

    /**
     * 通知配置变更
     *
     * @param config 配置文件
     */
    private void noticeConfigChange(final RepeaterConfig config) {
        if (initialized.get()) {
            for (InvokePlugin invokePlugin : invokePlugins) {
                try {
                    if (invokePlugin.enable(config)) {
                        invokePlugin.onConfigChange(config);
                    }
                } catch (PluginLifeCycleException e) {
                    log.error("error occurred when notice config, plugin ={}", invokePlugin.getType().name(), e);
                }
            }
        }
    }

    private synchronized void reload() throws ModuleException {
        moduleController.frozen();
        // unwatch all plugin
        RepeaterResult<RepeaterConfig> result = configManager.pullConfig();
        if (!result.isSuccess()) {
            log.error("reload plugin failed, cause pull config not success");
            return;
        }
        for (InvokePlugin invokePlugin : invokePlugins) {
            if (invokePlugin.enable(result.getData())) {
                invokePlugin.unWatch(eventWatcher, invocationListener);
            }
        }
        // release classloader
        lifecycleManager.release();
        // reWatch
        initialize(result.getData());
        moduleController.active();
    }
}
