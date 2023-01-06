package com.alibaba.jvm.sandbox.repeater.plugin.dubbo2x.consumer;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.api.event.Event;
import com.alibaba.jvm.sandbox.api.event.ReturnEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.util.LogUtil;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * {@link DubboConsumerInvocationProcessor}
 * <p>
 * dubbo consumer调用处理器，需要重写组装identity 和 组装request
 * </p>
 *
 * @author zhaoyb1990
 */
public class DubboConsumerInvocationProcessor extends DefaultInvocationProcessor {

    public DubboConsumerInvocationProcessor(InvokeType type) {
        super(type);
    }

    /**
     * 获取拦截的方法的标识
     *
     * @param event 事件
     * @return
     */
    @Override
    public Identity assembleIdentity(BeforeEvent event) {
        // invoke(Invoker<?> invoker, Invocation invocation)
        Object invoker = event.argumentArray[0];
        Object invocation = event.argumentArray[1];

        try {
            // methodName
            String methodName = (String) MethodUtils.invokeMethod(invocation, "getMethodName");
            Class<?>[] parameterTypes = (Class<?>[]) MethodUtils.invokeMethod(invocation, "getParameterTypes");

            // interfaceName
            String  interfaceName = ((Class)MethodUtils.invokeMethod(invoker, "getInterface")).getCanonicalName();
            return new Identity(InvokeType.DUBBO.name(), interfaceName, getMethodDesc(methodName, parameterTypes), getExtra());
        } catch (Exception e) {
            // ignore
            LogUtil.error("error occurred when assemble dubbo request", e);
        }
        return new Identity(InvokeType.DUBBO.name(), "unknown", "unknown", null);
    }

    /**
     * 获取调用方法的入参
     *
     * @param event before事件
     * @return
     */
    @Override
    public Object[] assembleRequest(BeforeEvent event) {
        // invoke(Invoker<?> invoker, Invocation invocation)
        Object invocation = event.argumentArray[1];
        try {
            return (Object[]) MethodUtils.invokeMethod(invocation, "getArguments");
        } catch (Exception e) {
            // ignore
            LogUtil.error("error occurred when assemble dubbo request", e);
        }
        return null;
    }

    /**
     * 获取方法调用的返回值
     *
     * @param event 事件
     * @return
     */
    @Override
    public Object assembleResponse(Event event) {
        if (event.type == Event.Type.RETURN) {
            // org.apache.dubbo.rpc.Result
            // Result invoke(Invoker<?> invoker, org.apache.dubbo.rpc.Invocation invocation)
             Object result = ((ReturnEvent) event).object;
            try {
                return MethodUtils.invokeMethod(result, "getValue");
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    @Override
    public Object assembleMockResponse(BeforeEvent event, Invocation invocation) {
        // Result invoke(Invoker<?> invoker, Invocation invocation)
        try {
            Object dubboInvocation = event.argumentArray[1];
            Object response = invocation.getResponse();
            Class<?> aClass = event.javaClassLoader.loadClass("org.apache.dubbo.rpc.AsyncRpcResult");
            // 调用AsyncRpcResult#newDefaultAsyncResult返回;
            return MethodUtils.invokeStaticMethod(aClass, "newDefaultAsyncResult",
                    new Object[]{response, dubboInvocation}, new Class[]{Object.class, dubboInvocation.getClass()});
        } catch (ClassNotFoundException e) {
            LogUtil.error("no valid AsyncRpcResult class fount in classloader {}", event.javaClassLoader, e);
            return null;
        } catch (Exception e) {
            LogUtil.error("error occurred when assemble dubbo mock response", e);
            return null;
        }
    }

}
