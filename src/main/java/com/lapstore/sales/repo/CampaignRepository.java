package com.lapstore.sales.repo;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lapstore.sales.domain.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    boolean existsByCode(String code);

    List<Campaign> findAllByOrderByIdDesc();

    /** Campaign đang chạy: ACTIVE và trong thời hạn. */
    @Query("select c from Campaign c where c.status = 'ACTIVE' and c.startAt <= :now and c.endAt > :now order by c.priority desc, c.id")
    List<Campaign> findRunning(@Param("now") Instant now);
}
