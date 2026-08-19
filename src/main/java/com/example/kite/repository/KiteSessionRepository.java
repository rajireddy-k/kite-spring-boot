package com.example.kite.repository;

import com.example.kite.entity.KiteSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KiteSessionRepository extends JpaRepository<KiteSessionEntity, Long> {
}
