package com.example.kite.util;


import lombok.extern.slf4j.Slf4j;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;


@Slf4j
public class DateUtils {


    public static Long DateToMilliseconds(String date) {


        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        long millis =0;
        try {
            LocalDateTime ldt = LocalDateTime.parse(date, formatter);
            millis = ldt.toInstant(ZoneOffset.ofHoursMinutes(5, 30)).toEpochMilli();
        }catch(Exception e) {
            millis = System.currentTimeMillis();
            log.error("Failed tp parse the intput date:{}", date, e);
        }
        return millis;
    }


    public static Instant formatDate(String dateString) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        Instant instant =null;
        try {
            LocalDateTime ldt = LocalDateTime.parse(dateString, formatter);
            instant = ldt.toInstant(ZoneOffset.ofHoursMinutes(5, 30));
        }catch(Exception e) {
            instant = Instant.now();
            log.error("Failed tp parse the intput date:{}", dateString, e);
        }
        return instant;
    }


    public static LocalDateTime formatStringToLocalDate(String dateString) {
        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime ldt =null;
        try {
            ldt = LocalDateTime.parse(dateString, formatter);


        }catch(Exception e) {

            formatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
            try {
                ldt = LocalDateTime.parse(dateString, formatter);
            } catch(Exception e1) {
                log.error("Failed tp parse the intput date:{}", dateString, e1);
                ldt = LocalDateTime.now();
            }
        }
        return ldt;
    }
}
