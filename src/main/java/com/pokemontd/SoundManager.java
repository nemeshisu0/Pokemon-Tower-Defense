package com.pokemontd;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;

public class SoundManager {
    private static boolean muted = false;

    public static boolean isMuted() {
        return muted;
    }

    public static void toggleMute() {
        muted = !muted;
    }

    public static void setMuted(boolean m) {
        muted = m;
    }

    private static void playTone(byte[] buffer, AudioFormat format) {
        if (muted) return;
        new Thread(() -> {
            try {
                DataLine.Info info = new DataLine.Info(Clip.class, format);
                if (!AudioSystem.isLineSupported(info)) {
                    return;
                }
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(format, buffer, 0, buffer.length);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception ignored) {
                // Ignore audio line exceptions gracefully in headless or silent environments
            }
        }).start();
    }

    public static void playShoot() {
        if (muted) return;
        float sampleRate = 22050f;
        int numSamples = (int) (sampleRate * 0.08); // 80ms
        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double freq = 900.0 - (i / (double) numSamples) * 550.0;
            double angle = 2.0 * Math.PI * i / (sampleRate / freq);
            buffer[i] = (byte) (Math.sin(angle) > 0 ? 45 : -45);
        }
        playTone(buffer, new AudioFormat(sampleRate, 8, 1, true, false));
    }

    public static void playHit() {
        if (muted) return;
        float sampleRate = 22050f;
        int numSamples = (int) (sampleRate * 0.05); // 50ms
        byte[] buffer = new byte[numSamples];
        for (int i = 0; i < numSamples; i++) {
            double decay = 1.0 - (i / (double) numSamples);
            buffer[i] = (byte) ((Math.random() * 80 - 40) * decay);
        }
        playTone(buffer, new AudioFormat(sampleRate, 8, 1, true, false));
    }

    public static void playSuperEffective() {
        if (muted) return;
        float sampleRate = 22050f;
        int numSamples = (int) (sampleRate * 0.16); // 160ms
        byte[] buffer = new byte[numSamples];
        int half = numSamples / 2;
        for (int i = 0; i < numSamples; i++) {
            double freq = (i < half) ? 659.25 : 987.77; // E5 -> B5
            double angle = 2.0 * Math.PI * i / (sampleRate / freq);
            buffer[i] = (byte) (Math.sin(angle) * 55);
        }
        playTone(buffer, new AudioFormat(sampleRate, 8, 1, true, false));
    }

    public static void playBuy() {
        if (muted) return;
        float sampleRate = 22050f;
        int numSamples = (int) (sampleRate * 0.2); // 200ms
        byte[] buffer = new byte[numSamples];
        int third = numSamples / 3;
        for (int i = 0; i < numSamples; i++) {
            double freq = (i < third) ? 523.25 : (i < 2 * third ? 659.25 : 783.99); // C5 -> E5 -> G5
            double angle = 2.0 * Math.PI * i / (sampleRate / freq);
            buffer[i] = (byte) (Math.sin(angle) * 50);
        }
        playTone(buffer, new AudioFormat(sampleRate, 8, 1, true, false));
    }

    public static void playWaveStart() {
        if (muted) return;
        float sampleRate = 22050f;
        int numSamples = (int) (sampleRate * 0.24);
        byte[] buffer = new byte[numSamples];
        int step = numSamples / 4;
        double[] freqs = {440.0, 554.37, 659.25, 880.0};
        for (int i = 0; i < numSamples; i++) {
            int idx = Math.min(3, i / step);
            double freq = freqs[idx];
            double angle = 2.0 * Math.PI * i / (sampleRate / freq);
            buffer[i] = (byte) (Math.sin(angle) * 45);
        }
        playTone(buffer, new AudioFormat(sampleRate, 8, 1, true, false));
    }

    public static void playGameOver() {
        if (muted) return;
        float sampleRate = 22050f;
        int numSamples = (int) (sampleRate * 0.4);
        byte[] buffer = new byte[numSamples];
        int step = numSamples / 4;
        double[] freqs = {440.0, 415.30, 392.0, 329.63};
        for (int i = 0; i < numSamples; i++) {
            int idx = Math.min(3, i / step);
            double freq = freqs[idx];
            double angle = 2.0 * Math.PI * i / (sampleRate / freq);
            buffer[i] = (byte) (Math.sin(angle) * 50);
        }
        playTone(buffer, new AudioFormat(sampleRate, 8, 1, true, false));
    }
}
