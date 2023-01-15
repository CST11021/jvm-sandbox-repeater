package com.alibaba.jvm.sandbox.repeater.module.util;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * {@link SPILoader} 加载spi
 * <p>
 *
 * @author zhaoyb1990
 */
public class SPILoader {

    private final static Logger log = LoggerFactory.getLogger(SPILoader.class);

    /**
     * API（Application Programming Interface）：用户程序接口，提供一些具体的实现的api，用户基于这些实现编写功能
     * SPI（Service Provider Interface）：服务提供者接口，SPI是一种回调的思想，提供一些回调接口让用户去自定义实现
     *
     * @param spiType
     * @param classLoader
     * @return
     * @param <T>
     */
    public static <T> List<T> loadSPI(Class<T> spiType, ClassLoader classLoader) {
        ServiceLoader<T> loaded = ServiceLoader.load(spiType, classLoader);
        Iterator<T> spiIterator = loaded.iterator();

        List<T> target = Lists.newArrayList();
        while (spiIterator.hasNext()) {
            try {
                target.add(spiIterator.next());
            } catch (Throwable e) {
                log.error("Error load spi {} >>> ", spiType.getCanonicalName(), e);
            }
        }
        return target;
    }
}
