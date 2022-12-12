package com.alibaba.jvm.sandbox.repeater.plugin.java;


import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;

/**
 * <p>
 *
 * @author zhaoyb1990
 */
class JavaInvocationProcessor extends DefaultInvocationProcessor {

    JavaInvocationProcessor(InvokeType type) {
        super(type);
    }

    /**
     * 返回执行切点前的拦截信息(这里抽象为Identity)，记录：类实例对象、方法、入参、入参类型等信息
     *
     * @param event 事件
     * @return
     */
    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        try {
            JavaInstanceCache.cacheInstance(event.target);
        } catch (Exception e) {
            // ignore
        }
        return super.assembleIdentity(event);
    }
}
