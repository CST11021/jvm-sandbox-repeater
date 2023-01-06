package com.alibaba.jvm.sandbox.repeater.plugin.dubbo2x.provider;

import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.dubbo2x.consumer.DubboConsumerInvocationProcessor;

/**
 * {@link DubboProviderInvocationProcessor} dubbo服务端调用处理
 * <p>
 *
 * @author zhaoyb1990
 */
public class DubboProviderInvocationProcessor extends DubboConsumerInvocationProcessor {

    DubboProviderInvocationProcessor(InvokeType type) {
        super(type);
    }

}
