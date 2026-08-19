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
        try {
            String filePath = soundFilePath(source);
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
    }

    private static String soundFilePath(SignalSourceType source) {

        return switch (source) {
            case SignalSourceType.WEBHOOK: yield "C:\\Windows\\Media\\Ring01.wav";
            case SignalSourceType.emaCrossoverStrategy: yield "C:\\Windows\\Media\\Ring08.wav";
            case SignalSourceType.IntradayTradingEngine: yield "C:\\Windows\\Media\\Ring04.wav";
            case SignalSourceType.IntradayTradingEngineV2: yield "C:\\Windows\\Media\\Alarm08.wav";
        };
    }
}
