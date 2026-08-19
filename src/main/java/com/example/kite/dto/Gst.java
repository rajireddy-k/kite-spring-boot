package com.example.kite.dto;


import lombok.Data;


import java.math.BigDecimal;


@Data
public class Gst {
    private BigDecimal igst;
    private BigDecimal cgst;
    private BigDecimal sgst;
    private BigDecimal total;
}
