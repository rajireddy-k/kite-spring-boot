package com.example.kite.dto;


import com.example.kite.enums.SignalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;


@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class TradingSignal {

    private  SignalType signal;
    private  double entry;
    private  double target;
    private  double stopLoss;
    private  double risk;
    private  double riskReward;
    private  double momentum;
    private  double atr;
    private  double averageClose;
    private  double resistance;
    private  double support;
    private  String reason;
    private long instrumentId;
    private LocalDateTime timestamp;
}
