package com.alibaba.jvm.sandbox.repeater.plugin.dubbo;

import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.listener.EventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import org.kohsuke.MetaInfServices;

import java.util.List;

/**
 * {@link DubboConsumerPlugin} Apache dubbo consumer 插件
 * <p>
 * 拦截ConsumerContextFilter$ConsumerContextListenerr#onResponse行录制
 * 拦截ConsumerContextFilter#invoke进行MOCK
 * </p>
 *
 * @author zhaoyb1990
 */
@MetaInfServices(InvokePlugin.class)
public class DubboConsumerPlugin extends AbstractInvokePluginAdapter {

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        // ConsumerContextFilter ConsumerContextListener
        // dubbo 2.7版本中的隐式参数传递：内部类ConsumerContextListener负责将传回来的隐性参数添加到当前线程的LOCAL实例对应的map中。
        EnhanceModel onResponse = EnhanceModel.builder()
                .classPattern("org.apache.dubbo.rpc.filter.ConsumerContextFilter$ConsumerContextListener")
                .methodPatterns(EnhanceModel.MethodPattern.transform("onResponse"))
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();

        EnhanceModel invoke = EnhanceModel.builder()
                .classPattern("org.apache.dubbo.rpc.filter.ConsumerContextFilter")
                .methodPatterns(EnhanceModel.MethodPattern.transform("invoke"))
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Lists.newArrayList(onResponse, invoke);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new DubboConsumerInvocationProcessor(getType());
    }

    @Override
    public InvokeType getType() {
        return InvokeType.DUBBO;
    }

    @Override
    public String identity() {
        return "dubbo-consumer";
    }

    /**
     * 1、是否是入口流量插件，dubbo consumer是去调用服务接口，所以不是流量入口插件
     * 2、流量入口插件有：
     *  com.alibaba.jvm.sandbox.repater.plugin.http.HttpPlugin
     *  com.alibaba.jvm.sandbox.repeater.plugin.java.JavaEntrancePlugin
     *  com.alibaba.jvm.sandbox.repeater.plugin.dubbo.DubboProviderPlugin
     *
     * @return true/false
     */
    @Override
    public boolean isEntrance() {
        return false;
    }

    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new DubboConsumerEventListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }
}
