package com.yiling.stocktake.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** Bridges Spring configuration to the small header-based access adapter. */
@Component
public class AccessContextInitializer {
    @Value("${stocktake.auth.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void initialize() {
        System.setProperty(RequestAccessContext.AUTH_ENABLED, Boolean.toString(enabled));
    }
}
