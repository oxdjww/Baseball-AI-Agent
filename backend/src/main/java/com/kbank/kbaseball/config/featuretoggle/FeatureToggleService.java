package com.kbank.kbaseball.config.featuretoggle;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FeatureToggleService {

    public static final String AI_ANALYSIS        = "AI_ANALYSIS";
    public static final String REVERSAL_DETECTION = "REVERSAL_DETECTION";
    public static final String RAIN_ALERT         = "RAIN_ALERT";

    private final SystemSettingRepository systemSettingRepository;

    public boolean isEnabled(String featureKey) {
        return systemSettingRepository.findById(featureKey)
                .map(SystemSetting::isEnabled)
                .orElse(false);
    }

    @Transactional
    public void toggle(String featureKey) {
        SystemSetting setting = systemSettingRepository.findById(featureKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown feature key: " + featureKey));
        setting.setEnabled(!setting.isEnabled());
        setting.setUpdatedAt(Instant.now());
        systemSettingRepository.save(setting);
        log.info("[FeatureToggleService] {} toggled to {}", featureKey, setting.isEnabled());
    }

    public List<SystemSetting> findAll() {
        return systemSettingRepository.findAll();
    }
}
