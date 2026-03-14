package com.kbank.kbaseball.config.featuretoggle;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
}
