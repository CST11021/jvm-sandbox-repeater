package com.alibaba.jvm.sandbox.repeater.plugin.java;

import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Behavior;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.RepeaterConfig;
import com.alibaba.jvm.sandbox.repeater.plugin.exception.PluginLifeCycleException;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;
import org.kohsuke.MetaInfServices;

import java.util.List;

/**
 * Java入口插件
 * <p>
 *
 * @author zhaoyb1990
 */
@MetaInfServices(InvokePlugin.class)
public class JavaEntrancePlugin extends AbstractInvokePluginAdapter {

    private RepeaterConfig config;

    /**
     * 返回需要拦截的切面集合，这里从RepeaterConfig配置里获取
     *
     * @return
     */
    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        if (config == null || CollectionUtils.isEmpty(config.getJavaEntranceBehaviors())) {
            return null;
        }

        List<EnhanceModel> ems = Lists.newArrayList();
        for (Behavior behavior : config.getJavaEntranceBehaviors()) {
            ems.add(EnhanceModel.convert(behavior));
        }
        return ems;
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new JavaInvocationProcessor(getType());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.JAVA;
    }

    /**
     * 插件标识
     *
     * @return
     */
    @Override
    public String identity() {
        return "java-entrance";
    }

    /**
     * 是否是流量入口插件
     *
     * @return
     */
    @Override
    public boolean isEntrance() {
        return true;
    }

    @Override
    public boolean enable(RepeaterConfig config) {
        this.config = config;
        return super.enable(config);
    }

    /**
     * 监听repeater配置的变更情况
     *
     * @param config 配置文件
     * @throws PluginLifeCycleException
     */
    @Override
    public void onConfigChange(RepeaterConfig config) throws PluginLifeCycleException {
        // configTemporary为null说明是插件第一次加载
        if (configTemporary == null) {
            super.onConfigChange(config);
        } else {
            this.config = config;
            super.onConfigChange(config);
            List<Behavior> current = config.getJavaEntranceBehaviors();
            List<Behavior> latest = configTemporary.getJavaEntranceBehaviors();
            if (JavaPluginUtils.hasDifference(current, latest)) {
                reWatch0();
            }
        }
    }
}
