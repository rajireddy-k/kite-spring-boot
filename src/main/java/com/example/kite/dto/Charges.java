package com.example.kite.dto;


import lombok.Data;


import java.math.BigDecimal;


@Data
public class Charges {


    private BigDecimal transaction_tax;
    private String transaction_tax_type;
    private BigDecimal exchange_turnover_charge;
    private BigDecimal sebi_turnover_charge;
    private BigDecimal brokerage;
    private BigDecimal stamp_duty;
    private Gst gst;
    private BigDecimal total;


}
