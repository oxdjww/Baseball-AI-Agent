package com.kbank.kbaseball.monitoring;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Map;

@Getter
public class MonitoringErrorEvent extends ApplicationEvent {

    private final String featureName;
    private final String errorMessage;
    private final String stackTrace;
    private final Map<String, String> contextData;
    private final String userId;

    public MonitoringErrorEvent(Object source,
                                String featureName,
                                String errorMessage,
                                String stackTrace,
                                Map<String, String> contextData,
                                String userId) {
        super(source);
        this.featureName = featureName;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.contextData = contextData;
        this.userId = userId;
    }
}
