package com.tdh.urlInterceptor.interfaces;

import com.tdh.urlInterceptor.interceptor.UrlInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * url 拦截器接口，其子类实现，用于获取根据需求拦截
 * 如根据用户的 ip 进行限制， 或者根据用的 user 限制
 * 需要注意加入spring bean 管理
 */
public abstract class InterceptorConfig {
    private static final Logger logger = LoggerFactory.getLogger(InterceptorConfig.class);

    /**
     * 子类实现，具体根据哪一种方式拦截， 返回一个用户判断唯一用户的标志， 作为唯一 key 校验 redis 计数器
     * 如果该类没有实现类， 将默认使用 sessionId 作为唯一标识符
     * @param request
     * @return
     */
    public abstract String limitBy(HttpServletRequest request);

    /**
     * url 地址配置了根据用户拦截， 需要实现该接口，判断当前用户是否有权限访问
     * @param request
     * @param currentCount  当前访问的次数
     * @param maxTimes 拦截器配置的最大访问次数
     * @return 返回 true 表示限制， 返回 false 表示不限制
     */
    public abstract boolean isPermissionAccess(HttpServletRequest request, Long currentCount, Integer maxTimes);

    /**
     *  获取IP地址
     * @param request
     * @return
     * @throws IOException
     */

    protected String getIpAddress(HttpServletRequest request) throws IOException {
        // 获取请求主机IP地址,如果通过代理进来，则透过防火墙获取真实IP地址

        String ip = request.getHeader("X-Forwarded-For");
        if (logger.isInfoEnabled()) {
            logger.info("getIpAddress(HttpServletRequest) - X-Forwarded-For - String ip=" + ip);
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
                if (logger.isInfoEnabled()) {
                    logger.info("getIpAddress(HttpServletRequest) - Proxy-Client-IP - String ip=" + ip);
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
                if (logger.isInfoEnabled()) {
                    logger.info("getIpAddress(HttpServletRequest) - WL-Proxy-Client-IP - String ip=" + ip);
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
                if (logger.isInfoEnabled()) {
                    logger.info("getIpAddress(HttpServletRequest) - HTTP_CLIENT_IP - String ip=" + ip);
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
                if (logger.isInfoEnabled()) {
                    logger.info("getIpAddress(HttpServletRequest) - HTTP_X_FORWARDED_FOR - String ip=" + ip);
                }
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
                if (logger.isInfoEnabled()) {
                    logger.info("getIpAddress(HttpServletRequest) - getRemoteAddr - String ip=" + ip);
                }
            }
        } else if (ip.length() > 15) {
            String[] ips = ip.split(",");
            for (int index = 0; index < ips.length; index++) {
                String strIp = (String) ips[index];
                if (!("unknown".equalsIgnoreCase(strIp))) {
                    ip = strIp;
                    break;
                }
            }
        }
        return ip;
    }
}
