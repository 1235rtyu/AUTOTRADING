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

    @Value("${kis.forceReal:false}")
    private boolean forceReal;

    @Value("${kis.overseas.trIdBuy:TTTT1002U}")
    private String overseasTrIdBuy;

    @Value("${kis.overseas.trIdSell:TTTT1006U}")
    private String overseasTrIdSell;

    @Value("${kis.overseas.demoTrIdBuy:VTTT1002U}")
    private String overseasDemoTrIdBuy;

    @Value("${kis.overseas.demoTrIdSell:VTTT1001U}")
    private String overseasDemoTrIdSell;

    @Value("${kis.domestic.ordDvsnLimit:00}")
    private String domesticOrdDvsnLimit;

    @Value("${kis.domestic.ordDvsnMarket:01}")
    private String domesticOrdDvsnMarket;

    @Value("${kis.overseas.ordDvsnLimit:00}")
    private String overseasOrdDvsnLimit;

    @Value("${kis.overseas.ordDvsnMarket:31}")
    private String overseasOrdDvsnMarket;

    @Value("${kis.overseas.usSellFallbackDiscount:0.002}")
    private double overseasUsSellFallbackDiscount;

    @Value("${kis.overseas.ordDvsnUsLimit:00}")
    private String overseasOrdDvsnUsLimit;

    @Value("${kis.overseas.ordDvsnUsSellMoo:31}")
    private String overseasOrdDvsnUsSellMoo;

    @Value("${kis.overseas.ordDvsnUsSellMoc:33}")
    private String overseasOrdDvsnUsSellMoc;

    public String getEnv() {
        return env;
    }

    public boolean isDemo() {
        if (forceReal) {
            return false;
        }
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

    public synchronized void setAccountNo(String accountNo) {
        this.accountNo = StringUtils.hasText(accountNo) ? accountNo.trim() : "";
    }

    public synchronized void setAccountProductCode(String accountProductCode) {
        this.accountProductCode = StringUtils.hasText(accountProductCode) ? accountProductCode.trim() : "";
    }

    public synchronized void updateAccountInfo(String accountNo, String accountProductCode) {
        setAccountNo(accountNo);
        if (StringUtils.hasText(accountProductCode)) {
            setAccountProductCode(accountProductCode);
        }
    }

    public String getCustomerType() {
        return customerType;
    }

    public boolean isRequireHashKey() {
        return requireHashKey;
    }

    public String getOverseasTrIdBuy() {
        return overseasTrIdBuy;
    }

    public String getOverseasTrIdSell() {
        return overseasTrIdSell;
    }

    public String getOverseasDemoTrIdBuy() {
        return overseasDemoTrIdBuy;
    }

    public String getOverseasDemoTrIdSell() {
        return overseasDemoTrIdSell;
    }

    public String getDomesticOrdDvsnLimit() {
        return domesticOrdDvsnLimit;
    }

    public String getDomesticOrdDvsnMarket() {
        return domesticOrdDvsnMarket;
    }

    public String getOverseasOrdDvsnLimit() {
        return overseasOrdDvsnLimit;
    }

    public String getOverseasOrdDvsnMarket() {
        return overseasOrdDvsnMarket;
    }

    public double getOverseasUsSellFallbackDiscount() {
        return overseasUsSellFallbackDiscount;
    }

    public String getOverseasOrdDvsnUsLimit() {
        return overseasOrdDvsnUsLimit;
    }

    public String getOverseasOrdDvsnUsSellMoo() {
        return overseasOrdDvsnUsSellMoo;
    }

    public String getOverseasOrdDvsnUsSellMoc() {
        return overseasOrdDvsnUsSellMoc;
    }

    public boolean isConfigured() {
        return StringUtils.hasText(appKey) && StringUtils.hasText(appSecret);
    }
}
