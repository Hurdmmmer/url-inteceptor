package com.tdh.urlInterceptor.interceptor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * 注册拦截器
 */
@Configuration
public class RegisterIntercept extends WebMvcConfigurerAdapter {
    @Bean
    public UrlInterceptor urlInterceptor() {
        return new UrlInterceptor();
    }
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
//        UrlInterceptor com.tdh.urlInterceptor = com.tdh.urlInterceptor();
        registry.addInterceptor(urlInterceptor()).addPathPatterns("/**");
    }
}
