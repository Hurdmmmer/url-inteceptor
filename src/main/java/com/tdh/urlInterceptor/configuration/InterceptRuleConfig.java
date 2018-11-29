package com.tdh.urlInterceptor.configuration;

import com.google.gson.*;
import com.tdh.urlInterceptor.enums.LimitEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 拦截器配置
 */
@Configuration
public class InterceptRuleConfig {
    private static Logger logger = LoggerFactory.getLogger(InterceptRuleConfig.class);
    private Map<String, Rule> ruleMap = new HashMap<String, Rule>();
    private String errorPage ;
    private String errorMsg = "{\"code\": 500,\"msg\": \"The system is busy, please try again later.\"}";
    @PostConstruct
    public void init() {
        InputStream in = null;
        try {
            logger.info("Load interceptor configuration...");
            in = this.getClass().getClassLoader().getResourceAsStream("config/interceptor.json");
            if (in == null) {
                logger.warn("The current environment does not have an interceptor configuration file configured, " +
                        " Unable to find interceptor.json file under classpath:/config path.");
                return;
            }
            JsonParser jsonParser = new JsonParser();
            JsonObject parse = (JsonObject) jsonParser.parse(new InputStreamReader(in));

            JsonArray urls = parse.getAsJsonArray("urls");
            for (JsonElement url : urls) {
                Set<Map.Entry<String, JsonElement>> entries = ((JsonObject) url).entrySet();
                for (Map.Entry<String, JsonElement> entry : entries) {
                    String key = entry.getKey();
                    JsonObject value = (JsonObject) entry.getValue();
                    JsonElement max_times = value.get("max_times");
                    JsonElement time_frequency = value.get("time_frequency");
                    JsonElement distinguish_users = value.get("distinguish_users");
                    Rule rule = new Rule();
                    rule.setDistinguishUsers(distinguish_users == null ? LimitEnum.DO_NOT_DISTINGUISH__USERS.getValue() : distinguish_users.getAsInt());
                    rule.setMaxTimes(max_times == null ? -1 : max_times.getAsInt());
                    rule.setTimeFrequency(time_frequency == null ? -1 : time_frequency.getAsLong());
                    ruleMap.put(key, rule);
                }
            }
            JsonElement error_page = parse.get("error_page");
            if (error_page != null) {
                this.errorPage = error_page.getAsString();
            }
            JsonElement error_msg = parse.get("error_msg");
            if (error_msg != null) {
                this.errorMsg = errorMsg.toString();
            }

            logger.info("Interceptor configuration loading is complete.");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                in = null;
            }
        }

    }

    public  Map<String, Rule> getRuleMap() {
        if (ruleMap.size() == 0) {
            return null;
        }
        return ruleMap;
    }

    public String getErrorPage() {
        return errorPage;
    }

    public void setErrorPage(String errorPage) {
        this.errorPage = errorPage;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }
}
