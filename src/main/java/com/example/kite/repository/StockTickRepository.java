package com.example.kite.repository;


import com.example.kite.entity.StockTickEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface StockTickRepository extends JpaRepository<StockTickEntity, Long> {


    @Query(value = """
   SELECT *
   FROM stock_tick
   WHERE exchange = :exchange
     AND symbol = :symbol
   ORDER BY tick_time DESC
   LIMIT 1
   """, nativeQuery = true)
    Optional<StockTickEntity> findTopByExchangeAndSymbolOrderByTickTimeDesc(
            @Param("exchange") String exchange,
            @Param("symbol") String symbol);


    @Query(value = """
       SELECT *
       FROM stock_tick
       WHERE exchange = :exchange
         AND symbol = :symbol
       ORDER BY tick_time
       """, nativeQuery = true)
    List<StockTickEntity> findAllStocksForASymbol(@Param("exchange") String exchange,
                                                  @Param("symbol") String symbol);

    @Query(value = """
    SELECT *
    FROM (
        SELECT
            st.*,
            ROW_NUMBER() OVER (
                PARTITION BY st.symbol
                ORDER BY st.tick_time DESC, st.id DESC
            ) AS rn
        FROM kite.stock_tick st
        WHERE
                 st.tick_time >= :tickStartTime
                AND st.tick_time < :tickEndTime
                AND
                (st.open_price = st.low_price
           OR st.open_price = st.high_price)
            AND st.high_price != st.low_price
    ) t
    WHERE t.rn = 1
    ORDER BY t.tick_time DESC
    """, nativeQuery = true)
    List<StockTickEntity> findOpeningRangeTicks(
            @Param("tickStartTime") java.sql.Timestamp tickStartTime,
            @Param("tickEndTime") java.sql.Timestamp tickEndTime);
}

