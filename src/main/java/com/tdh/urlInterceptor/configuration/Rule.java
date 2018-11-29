package com.tdh.urlInterceptor.configuration;

/**
 * URL 拦截器规则类
 */
public class Rule {
    /** 限制次数 */
    private Integer maxTimes;
    /** 持续时间内 */
    private Long timeFrequency;
    /** 是否根据用户进行限制 */
    private Integer distinguishUsers;

    public Integer getMaxTimes() {
        return maxTimes;
    }

    public void setMaxTimes(Integer maxTimes) {
        this.maxTimes = maxTimes;
    }

    public Long getTimeFrequency() {
        return timeFrequency;
    }

    public void setTimeFrequency(Long timeFrequency) {
        this.timeFrequency = timeFrequency;
    }

    public Integer getDistinguishUsers() {
        return distinguishUsers;
    }

    public void setDistinguishUsers(Integer distinguishUsers) {
        this.distinguishUsers = distinguishUsers;
    }

}
