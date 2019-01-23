package com.company.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

public class EndpointsListener implements ApplicationListener {

    Logger logger = LoggerFactory.getLogger(EndpointsListener.class);

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ApplicationContext applicationContext = ((ContextRefreshedEvent) event).getApplicationContext();
            applicationContext.getBean(RequestMappingHandlerMapping.class).getHandlerMethods().forEach((s, e) -> {
                logger.info(e.toString());
            });
        }
    }
}