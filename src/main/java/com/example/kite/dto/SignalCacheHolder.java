package com.example.kite.dto;

import com.zerodhatech.models.OrderParams;
import lombok.Data;

@Data
public class SignalCacheHolder {
    private OrderParams params;
    private boolean isReturnOrder;
    private boolean duplicateSignal;
}
