package com.lapstore.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật @Scheduled (áp dụng giá hẹn ngày; sau này thêm job hết hạn hold, tổng hợp
 * báo cáo).
 */
@Configuration
@EnableScheduling
class SchedulingConfig {
}
