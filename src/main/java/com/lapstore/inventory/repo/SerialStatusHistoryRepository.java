package com.lapstore.inventory.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lapstore.inventory.domain.SerialStatusHistory;

public interface SerialStatusHistoryRepository extends JpaRepository<SerialStatusHistory, Long> {
}
