package com.example.kite.repository;

import com.example.kite.entity.CandleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KiteCandleRepository extends JpaRepository<CandleEntity, Long> {
}
