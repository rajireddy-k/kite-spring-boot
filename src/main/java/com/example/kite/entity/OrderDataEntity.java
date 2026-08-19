package com.example.kite.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.sql.Timestamp;
import java.time.OffsetDateTime;

@Entity
@Table(name="kite_order_data")
@Data
public class OrderDataEntity {

    @Id
    private String orderId;
    @Column
    private Double price;
    @Column
    private int quantity;
    @Column
    private String action;
    @Column
    private String exchange;
    @Column
    private String symbol;
    @Column
    private Timestamp timestamp;
}
