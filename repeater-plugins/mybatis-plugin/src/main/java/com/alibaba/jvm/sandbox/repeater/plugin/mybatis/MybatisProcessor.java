package com.alibaba.jvm.sandbox.repeater.plugin.mybatis;

import com.alibaba.jvm.sandbox.api.event.BeforeEvent;
import com.alibaba.jvm.sandbox.repeater.plugin.core.impl.api.DefaultInvocationProcessor;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Identity;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.Invocation;
import com.alibaba.jvm.sandbox.repeater.plugin.domain.InvokeType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * InvocationProcessor负责组装请求入参、调用唯一标志Identity等系统操作，是每个插件需要实现的核心接口，也提供了基础的默认实现
 * DefaultInvocationProcessor，满足大部分的拦截场景，插件只需要继承它即可：
 *
 * @author zhaoyb1990
 */
class MybatisProcessor extends DefaultInvocationProcessor {

    MybatisProcessor(InvokeType type) {
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
        // 拦截的目标对象
        Object mapperMethod = event.target;
        // 这里field表示的是执行的SQL命令, 对应: SqlCommand = org.apache.ibatis.binding.MapperMethod#command
        Field field = FieldUtils.getDeclaredField(mapperMethod.getClass(), "command", true);
        if (field == null) {
            return new Identity(InvokeType.MYBATIS.name(), "Unknown", "Unknown", new HashMap<String, String>(1));
        }

        try {
            // 获取org.apache.ibatis.binding.MapperMethod.SqlCommand对象
            Object command = field.get(mapperMethod);
            // 表示Mybatis中Mapper接口的方法名
            Object name = MethodUtils.invokeMethod(command, "getName");
            // 表示Mapper接口方法映射的SQL的操作类型，比如：insert/select/insert/delete等
            Object type = MethodUtils.invokeMethod(command, "getType");
            return new Identity(InvokeType.MYBATIS.name(), type.toString(), name.toString(), new HashMap<String, String>(1));
        } catch (Exception e) {
            return new Identity(InvokeType.MYBATIS.name(), "Unknown", "Unknown", new HashMap<String, String>(1));
        }
    }

    /**
     * 放回拦截的切点的入参
     *
     * @param event before事件
     * @return
     */
    @Override
    public Object[] assembleRequest(BeforeEvent event) {
        // MapperMethod#execute(SqlSession sqlSession, Object[] args)
        // args可能存在不可序序列化异常（例如使用tk.mybatis)
        return new Object[]{event.argumentArray[1]};
    }

    /**
     * 是否及时序列化请求参数，因为请求参数在后续的调用过程中，可能会被篡改，因此在录制的过程中，默认会在before事件时候直接序列化；
     * 但有一些特殊场景例如：Mybatis的insert开启自动生成ID，会把入参里面ID补全，及时序列化录不到ID，会导致后续流程出错，这样的场景可以在return时去序列化
     *
     * @param invocation 调用
     * @param event before事件
     * @return true / false
     */
    @Override
    public boolean inTimeSerializeRequest(Invocation invocation, BeforeEvent event) {
        return false;
    }

}
