package com.example.kite.trading.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.lang.Double;
import java.time.LocalDateTime;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Candle {


    private LocalDateTime timestamp;


    private Double open;


    private Double high;


    private Double low;


    private Double close;


    private Long volume;


    private String symbol;
}
