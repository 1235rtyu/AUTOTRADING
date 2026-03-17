package com.autotrading.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KisProperties {
    @Value("${kis.env:demo}")
    private String env;

    @Value("${kis.appKey:}")
    private String appKey;

    @Value("${kis.appSecret:}")
    private String appSecret;

    @Value("${kis.baseUrl:}")
    private String baseUrl;

    @Value("${kis.accountNo:}")
    private String accountNo;

    @Value("${kis.accountProductCode:}")
    private String accountProductCode;

    @Value("${kis.customerType:P}")
    private String customerType;

    @Value("${kis.requireHashKey:true}")
    private boolean requireHashKey;

    public String getEnv() {
        return env;
    }

    public boolean isDemo() {
        if (!StringUtils.hasText(env)) {
            return true;
        }
        String normalized = env.trim().toLowerCase();
        return normalized.equals("demo") || normalized.equals("vts") || normalized.equals("vps");
    }

    public String getAppKey() {
        return appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public String getBaseUrl() {
        if (StringUtils.hasText(baseUrl)) {
            return baseUrl.trim();
        }
        return isDemo() ? "https://openapivts.koreainvestment.com:29443"
                : "https://openapi.koreainvestment.com:9443";
    }

    public String getAccountNo() {
        return accountNo;
    }

    public String getAccountProductCode() {
        return accountProductCode;
    }

    public String getCustomerType() {
        return customerType;
    }

    public boolean isRequireHashKey() {
        return requireHashKey;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(appKey) && StringUtils.hasText(appSecret);
    }
}
