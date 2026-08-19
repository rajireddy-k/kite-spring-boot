package com.example.kite.repository;

import com.example.kite.entity.OrderDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KiteOrderDataRepository extends JpaRepository<OrderDataEntity, String> {
}
