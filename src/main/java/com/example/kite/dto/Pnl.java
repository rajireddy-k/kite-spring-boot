package com.example.kite.dto;


import lombok.Data;


import java.math.BigDecimal;


@Data
public class Pnl {


    private BigDecimal realised;
    private BigDecimal unrealised;


}
