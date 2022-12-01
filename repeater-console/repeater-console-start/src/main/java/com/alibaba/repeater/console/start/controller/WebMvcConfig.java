package com.alibaba.repeater.console.start.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @Author 盖伦
 * @Date 2022/11/25
 */
@Component
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // 解决ERROR日志：
        // org.apache.velocity : ResourceManager : unable to find resource index.htm.vm in any resource loader.
        configurer.favorPathExtension(false);
    }
}
