package com.example.kite.repository;

import com.example.kite.entity.TradingSignalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KiteSignalRepository extends JpaRepository<TradingSignalEntity, Long> {
}
