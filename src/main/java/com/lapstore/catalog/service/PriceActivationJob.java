package com.lapstore.catalog.service;

import java.time.Instant;

import com.lapstore.catalog.domain.PriceHistory;
import com.lapstore.catalog.repo.PriceHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Áp dụng giá đã duyệt nhưng hẹn ngày hiệu lực: tới hạn thì cập nhật product_variant.current_price. */
@Component
class PriceActivationJob {
    private static final Logger log = LoggerFactory.getLogger(PriceActivationJob.class);

    private final PriceHistoryRepository prices;

    PriceActivationJob(PriceHistoryRepository prices) { this.prices = prices; }

    @Scheduled(fixedDelayString = "${app.price.activation-interval-ms:60000}")
    @Transactional
    public void activateDue() {
        for (PriceHistory ph : prices.findDueForActivation(Instant.now())) {
            ph.getVariant().setCurrentPrice(ph.getPrice());
            log.info("Áp dụng giá mới {} cho {}", ph.getPrice(), ph.getVariant().getSku());
        }
    }
}
