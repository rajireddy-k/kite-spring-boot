package com.example.kite.mapper;


import com.example.kite.dto.OrderData;
import com.example.kite.dto.TickMessage;
import com.example.kite.dto.TradingSignal;
import com.example.kite.dto.TradingViewSignal;
import com.example.kite.entity.OrderDataEntity;
import com.example.kite.entity.StockTickEntity;
import com.example.kite.entity.TradingSignalEntity;
import com.zerodhatech.models.Tick;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;


import java.sql.Date;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;


@Mapper(componentModel = "spring")
public interface KiteTickMessageMapper {


    @Mappings(
            {
                    @Mapping(target = "timestamp", source = "source.tickTime", qualifiedByName = "convertToLocalDate")
            }
    )
    public TickMessage mapToTickMessageFromEntity(StockTickEntity source);


    @Mappings(
            {
                    @Mapping(target = "tickTime", source = "source.timestamp", qualifiedByName = "convertToSqlDate"),
            }
    )
    public StockTickEntity mapToStockTickEntityFromMessage(TickMessage source);


    @Mappings(
            {
                    @Mapping(target = "lastPrice", source = "source.lastTradedPrice"),
                    @Mapping(target = "changePercent", source = "source.change"),
                    @Mapping(target = "volume", source = "source.volumeTradedToday"),
                    @Mapping(target = "averagePrice", source = "source.averageTradePrice")
            }
    )
    public TickMessage mapToTickMessageFromTicker(String exchange, String symbol, Tick source, LocalDateTime timestamp);


    @Mappings(
            {
                    @Mapping(target = "lastPrice", source = "source.lastTradedPrice"),
                    @Mapping(target = "changePercent", source = "source.change"),
                    @Mapping(target = "volume", source = "source.volumeTradedToday"),
                    @Mapping(target = "averagePrice", source = "source.averageTradePrice"),
                    @Mapping(target = "tickTime", source = "timestamp", qualifiedByName = "convertToSqlDate")
            }
    )
    public StockTickEntity mapToStockTickEntityFromTicker(String exchange, String symbol, Tick source, LocalDateTime timestamp);


    @Mappings(
            {
                    @Mapping(target = "action", source = "source.signal"),
                    @Mapping(target = "source", source = "signalSource"),
                    @Mapping(target = "timestamp", source= "source.timestamp", qualifiedByName = "convertToSqlDate")
            })
    public TradingSignalEntity mapToTradingSignalEntity(String symbol, TradingSignal source, String signalSource);


    @Mapping(target = "timestamp", source= "source.timestamp", qualifiedByName = "convertToSqlDate")
    public OrderDataEntity mapToOrderDataEntity(OrderData source);


    @Mappings(
            {
                    @Mapping(target = "source", source = "signalSource"),
                    @Mapping(target = "entry", source = "source.price")


            })
    public TradingSignalEntity mapToTradingSignalEntity(TradingViewSignal source, String signalSource);


    @Named("convertToSqlDate")
    default java.sql.Timestamp convertToSqlDate(LocalDateTime localDate) {
        return java.sql.Timestamp.valueOf(localDate);
    }


    @Named("convertToLocalDate")
    default LocalDateTime convertToLocalDate(java.sql.Timestamp timestamp) {
        return timestamp.toLocalDateTime();
    }
}
