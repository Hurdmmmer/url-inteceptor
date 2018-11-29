package com.tdh.urlInterceptor.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tdh.urlInterceptor.configuration.InterceptRuleConfig;
import com.tdh.urlInterceptor.configuration.Rule;
import com.tdh.urlInterceptor.enums.LimitEnum;
import com.tdh.urlInterceptor.interfaces.InterceptorConfig;
import com.tdh.urlInterceptor.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.*;

/**
 * url 拦截器
 */

public class UrlInterceptor extends HandlerInterceptorAdapter {

    private static final Logger logger = LoggerFactory.getLogger(UrlInterceptor.class);
    @Autowired
    private InterceptRuleConfig interceptRuleConfig;
    @Autowired
    private RedisService redisService;
    @Autowired(required = false)
    private InterceptorConfig interceptorConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.info("The url interceptor is working.");
        FutureTask<Boolean> task = null;
        ExecutorService executorService = null;
        try {
            final HttpServletRequest req = request;
            // 判断拦截任务是否超时
            executorService = Executors.newSingleThreadExecutor();
            task = new FutureTask<>(new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    return checkRequestIsRestricted(req);
                }
            });
            executorService.execute(task);
            boolean isLimit = task.get(3000, TimeUnit.MILLISECONDS);
            if (isLimit) {
                return executionInterception(req, response);
            }
        } catch (TimeoutException e) {
            // 取消定时任务
            logger.info("Execute interceptor task timeout, release directly.");
            task.cancel(true);
        } catch (Exception e){
            logger.error(e.getMessage(), e);
        } finally {
            if (executorService != null) {
                executorService.shutdown();
            }
        }
        return true;
    }

    /**
     * 执行拦截
     *
     * @param request
     * @param response
     * @return
     */
    private boolean executionInterception(HttpServletRequest request, HttpServletResponse response) {
        PrintWriter writer = null;
        try {
            logger.info("Trigger limit on access");
            String accept = request.getHeader("Accept");
            if (accept.startsWith("text/html")) {
                if (!StringUtils.isEmpty(this.interceptRuleConfig.getErrorPage())) {
                    String errorPage = this.interceptRuleConfig.getErrorPage();
                    if (!errorPage.startsWith("/")) {
                        errorPage = "/" + errorPage;
                    }

                    response.sendRedirect(request.getContextPath() + errorPage);
                    logger.info("Request forwarded to error page.");
                } else {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    writer = response.getWriter();
                    writer.write(interceptRuleConfig.getErrorMsg().toCharArray());
                    writer.flush();
                }
            } else {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                writer = response.getWriter();
                writer.write(interceptRuleConfig.getErrorMsg().toCharArray());
                writer.flush();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return false;
    }

    /**
     * 检查当前请求是否被限制
     * 如果没有配置拦截器属性或者拦截器根据那种依据拦截则返回 false
     * {@link InterceptorConfig}  {@link InterceptRuleConfig}
     */
    private boolean checkRequestIsRestricted(HttpServletRequest request) throws IOException {
        Map<String, Rule> ruleMap = interceptRuleConfig.getRuleMap();
        // 没有规则就直接放行
        if (ruleMap == null)
            return false;
        // 根据请求路径匹配规则，并查看是否限制
        return isLimit(getRule(request, ruleMap), request);
    }

    /**
     * 获取拦截规则， 使用缓存
     *
     * @param request
     * @param ruleMap
     * @return
     * @throws IOException
     */
    private Rule getRule(HttpServletRequest request, Map<String, Rule> ruleMap) throws IOException {
        Rule rule = redisService.get(request.getRequestURI());
        ObjectMapper mapper = new ObjectMapper();
        if (rule == null) {
            AntPathMatcher matcher = new AntPathMatcher();
            for (Map.Entry<String, Rule> stringRuleEntry : ruleMap.entrySet()) {
                boolean match = matcher.match(stringRuleEntry.getKey(), request.getRequestURI());
                if (match) {
                    Rule value = stringRuleEntry.getValue();
                    String json = mapper.writeValueAsString(value);
                    if (json == null) {
                        logger.info("No matching interception rules");
                        return null;
                    }
                    logger.info("Matching rules: " + json);
                    redisService.add(request.getRequestURI(), value);
                    return value;
                }
            }
        }
        logger.info("Matching rules: " + mapper.writeValueAsString(rule));
        return rule;
    }

    /**
     * 根据拦截器的规则进行校验拦截
     *
     * @return 返回 true 表示限制
     */
    private boolean isLimit(Rule rule, HttpServletRequest request) {
        // 没有匹配规则， 直接放行
        if (rule == null) return false;
        String key = null;
        if (this.interceptorConfig != null) {
            key = interceptorConfig.limitBy(request);
        } else {
            // 默认根据用户的sessionid
            key = request.getRequestedSessionId();
            if (null == key) {
                key = request.getSession().getId();
            }
        }
        Long currentCount = redisService.isLimit(key, rule.getTimeFrequency(), rule.getMaxTimes());
        logger.info("The currently accessed url: {}, Current access session id: {}, Current visits： {}",
                request.getRequestURI(), key, currentCount);
        if (rule.getDistinguishUsers() != null && rule.getDistinguishUsers().equals(LimitEnum.DISTINGUISH_USERS.getValue()) &&
                interceptorConfig != null) {
            // 根据用户拦截
            return interceptorConfig.isPermissionAccess(request, currentCount, rule.getMaxTimes());
        } else if (rule.getDistinguishUsers() != null && rule.getDistinguishUsers().equals(LimitEnum.DISTINGUISH_USERS.getValue()) &&
                interceptorConfig == null) {
            // url 拦截配置了根据用户拦截， 但是没有实现拦截器接口，那么将限制所有登录
            return true;
        }
        return currentCount == 0;
    }


}
