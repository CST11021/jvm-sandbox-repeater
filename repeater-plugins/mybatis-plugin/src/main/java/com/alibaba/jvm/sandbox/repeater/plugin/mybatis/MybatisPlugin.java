package com.alibaba.jvm.sandbox.repeater.plugin.mybatis;

import com.alibaba.jvm.sandbox.api.event.Event.Type;
import com.alibaba.jvm.sandbox.repeater.plugin.api.InvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.AbstractInvokePluginAdapter;
import com.alibaba.jvm.sandbox.repeater.plugin.core.model.EnhanceModel;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import com.alibaba.jvm.sandbox.repeater.plugin.spi.InvokePlugin;
import com.google.common.collect.Lists;
import org.kohsuke.MetaInfServices;

import java.util.List;

/**
 * repeater-module通过SPI方式加载插件，所有插件需要实现InvokePlugin并按标准SPI协议配置，框架抽象了基础插件信息，
 * 可以直接继承AbstractInvokePluginAdapter即可，插件代码如下：
 *
 * @author zhaoyb1990
 */
@MetaInfServices(InvokePlugin.class)
public class MybatisPlugin extends AbstractInvokePluginAdapter {

    /**
     * 返回需要拦截的切面集合
     *
     * @return
     */
    @Override
    protected List<EnhanceModel> getEnhanceModels() {
        // 这里面比较难的点在EnhanceModel的构建，需要开发者自己找到对应框架最合适的埋点，Demo中mybatis框架通过拦截
        // org.apache.ibatis.binding.MapperMethod#execute来录制和mock回放
        EnhanceModel em = EnhanceModel.builder()
                .classPattern("org.apache.ibatis.binding.MapperMethod")
                .methodPatterns(EnhanceModel.MethodPattern.transform("execute"))
                .watchTypes(Type.BEFORE, Type.RETURN, Type.THROWS)
                .build();
        return Lists.newArrayList(em);
    }

    @Override
    protected InvocationProcessor getInvocationProcessor() {
        return new MybatisProcessor(getType());
    }

    /**
     * 协议类型
     *
     * @return
     */
    @Override
    public InvokeType getType() {
        return InvokeType.MYBATIS;
    }

    /**
     * 插件的唯一标识
     *
     * @return
     */
    @Override
    public String identity() {
        return "mybatis";
    }

    /**
     * 是否是入口流量插件，mybatis的拦截器不是流量入口，像dubbo的服务提供者这种拦截器可以是流量入口
     *
     * @return
     */
    @Override
    public boolean isEntrance() {
        return false;
    }

}
