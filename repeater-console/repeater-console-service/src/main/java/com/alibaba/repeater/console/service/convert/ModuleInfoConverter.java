package com.alibaba.repeater.console.service.convert;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.repeater.console.common.domain.ModuleInfoBO;
import com.alibaba.repeater.console.common.domain.ModuleStatus;
import com.alibaba.repeater.console.dal.model.ModuleInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;


/**
 * {@link ModuleInfoConverter}
 * <p>
 *
 * @author zhaoyb1990
 */
@Component("moduleInfoConverter")
public class ModuleInfoConverter implements ModelConverter<ModuleInfo, ModuleInfoBO> {

    @Override
    public ModuleInfoBO convert(ModuleInfo source) {
        ModuleInfoBO moduleInfo = new ModuleInfoBO();
        BeanUtils.copyProperties(source, moduleInfo);
        moduleInfo.setStatus(ModuleStatus.of(source.getStatus()));
        String extJson = source.getExt();
        if (StringUtils.isNotBlank(extJson)) {
            ModuleInfo.Ext ext = JSONObject.parseObject(extJson, ModuleInfo.Ext.class);
            moduleInfo.setPid(ext.getPid());
        }

        return moduleInfo;
    }

    @Override
    public ModuleInfo reconvert(ModuleInfoBO target) {
        ModuleInfo moduleInfo = new ModuleInfo();
        BeanUtils.copyProperties(target, moduleInfo);
        moduleInfo.setStatus(target.getStatus().name());

        ModuleInfo.Ext ext = new ModuleInfo.Ext();
        ext.setPid(target.getPid());
        moduleInfo.setExt(JSONObject.toJSONString(ext));
        return moduleInfo;
    }
}
