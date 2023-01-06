package com.alibaba.jvm.sandbox.repeater.plugin.dubbo2x.consumer;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationListener;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultEventListener;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.DubboInvocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.util.List;
import java.util.Map;

/**
 * {@link DubboConsumerEventListener}
 * <p>
 *
 * @author zhaoyb1990
 */
public class DubboConsumerEventListener extends DefaultEventListener {

    public DubboConsumerEventListener(InvokeType invokeType, boolean entrance, InvocationListener listener, InvocationProcessor processor) {
        super(invokeType, entrance, listener, processor);
    }

    @Override
    protected Invocation initInvocation(BeforeEvent event) {
        DubboInvocation dubboInvocation = new DubboInvocation();
        // Result invoke(Invoker<?> invoker, Invocation invocation) {}
        Object invoker = event.argumentArray[0];
        Object invocation = event.argumentArray[1];
        try {
            Object url = MethodUtils.invokeMethod(invoker, "getUrl");
            @SuppressWarnings("unchecked")
            Map<String, String> parameters = (Map<String, String>) MethodUtils.invokeMethod(url, "getParameters");
            String protocol =  (String)MethodUtils.invokeMethod(url, "getProtocol");
            // methodName
            String methodName = (String) MethodUtils.invokeMethod(invocation, "getMethodName");
            Class<?>[] parameterTypes = (Class<?>[]) MethodUtils.invokeMethod(invocation, "getParameterTypes");
            // interfaceName
            String interfaceName = ((Class) MethodUtils.invokeMethod(invoker, "getInterface")).getCanonicalName();
            dubboInvocation.setProtocol(protocol);
            dubboInvocation.setInterfaceName(interfaceName);
            dubboInvocation.setMethodName(methodName);
            dubboInvocation.setParameters(parameters);
            dubboInvocation.setVersion(parameters.get("version"));
            dubboInvocation.setParameterTypes(transformClass(parameterTypes));
            // todo find a right way to get address and group
        } catch (Exception e) {
            LogUtil.error("error occurred when init dubbo invocation", e);
        }
        return dubboInvocation;
    }


    private String[] transformClass(Class<?>[] parameterTypes) {
        List<String> paramTypes = Lists.newArrayList();
        if (ArrayUtils.isNotEmpty(parameterTypes)) {
            for (Class<?> clazz : parameterTypes) {
                paramTypes.add(clazz.getCanonicalName());
            }
        }
        return paramTypes.toArray(new String[0]);
    }
}
