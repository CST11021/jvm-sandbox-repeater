package com.alibaba.jvm.sandbox.repeater.plugin.dubbo2x.consumer;

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
 *
 * {@link DubboConsumerPlugin} Apache dubbo2.x consumer 插件
 *
 * 低版本的dubbo没有ConsumerContextFilter$ConsumerContextListenerr#onResponse方法，无法进行录制，改为通过拦截ConsumerContextFilter#invoke进行录制和MOCK
 *
 * @author zhaoyb1990
 */
@MetaInfServices(InvokePlugin.class)
public class DubboConsumerPlugin extends AbstractInvokePluginAdapter {

    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        EnhanceModel invoke = EnhanceModel.builder()
                .classPattern("org.apache.dubbo.rpc.filter.ConsumerContextFilter")
                .methodPatterns(EnhanceModel.MethodPattern.transform("invoke"))
                .watchTypes(Event.Type.BEFORE, Event.Type.RETURN, Event.Type.THROWS)
                .build();
        return Lists.newArrayList(invoke);
    }

    @Override
    public InvokeType getType() {
        return InvokeType.DUBBO2X;
    }

    @Override
    public String identity() {
        return "dubbo2x-consumer";
    }

    @Override
    public boolean isEntrance() {
        return false;
    }

    @Override
    protected EventListener getEventListener(InvocationListener listener) {
        return new DubboConsumerEventListener(getType(), isEntrance(), listener, getInvocationProcessor());
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new DubboConsumerInvocationProcessor(getType());
    }
}
