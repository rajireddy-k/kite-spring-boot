package com.example.kite.util;

import com.example.kite.enums.SignalSourceType;
import lombok.extern.slf4j.Slf4j;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

@Slf4j
public class AlertSoundListener {

    public static void beep(SignalSourceType source) {
        String environment = System.getProperty("env");
        String dir = System.getProperty("file.dir.path");
        if(dir == null) {
            dir = "C:\\Windows\\Media\\";
        }
        if("local".equals(environment)) {
            try {
                String filePath = soundFilePath(source, dir);
                log.info("sound file path: {}", filePath);
                File soundFile = new File(filePath);
                //File soundFile = new File("beep.mp3");
                try (AudioInputStream audioIn =
                             AudioSystem.getAudioInputStream(soundFile)) {

                    Clip clip = AudioSystem.getClip();
                    clip.open(audioIn);
                    clip.start();

                    // Wait until sound finishes
                    Thread.sleep(clip.getMicrosecondLength() / 1000);

                    clip.close();
                }
            } catch (Exception e) {
                log.error("Failed load audio clip");
            }
        } else {
            log.info("audio clip disabled for remote");
        }
    }

    private static String soundFilePath(SignalSourceType source, String filePath) {
        log.info("Signal source: {} and sound file source: {}", source, filePath);
        return switch (source) {
            case SignalSourceType.WEBHOOK: yield filePath+"Ring01.wav";
            case SignalSourceType.emaCrossoverStrategy: yield filePath+"Ring08.wav";
            case SignalSourceType.IntradayTradingEngine: yield filePath+"Ring04.wav";
            case SignalSourceType.IntradayTradingEngineV2: yield filePath+"Alarm08.wav";
        };
    }
}
