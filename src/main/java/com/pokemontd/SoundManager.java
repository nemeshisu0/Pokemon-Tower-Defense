package com.pokemontd;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundManager {
    private static double volume = 0.70; // 0.0 to 1.0 (70% default)
    private static boolean muted = false;
    private static final float SAMPLE_RATE = 48000f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE, 16, 2, true, false);
    private static final BlockingQueue<byte[]> soundQueue = new LinkedBlockingQueue<>(25);

    static {
        Thread worker = new Thread(() -> {
            SourceDataLine line = null;
            try {
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, FORMAT);
                if (AudioSystem.isLineSupported(info)) {
                    line = (SourceDataLine) AudioSystem.getLine(info);
                    line.open(FORMAT, 8192);
                    line.start();
                }
            } catch (Exception ignored) {
            }

            while (true) {
                try {
                    byte[] buf = soundQueue.take();
                    if (line != null && line.isOpen()) {
                        line.write(buf, 0, buf.length);
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {
                }
            }

            if (line != null) {
                try { line.close(); } catch (Exception ignored) {}
            }
        }, "PokemonTD-AudioThread");
        worker.setDaemon(true);
        worker.start();
    }

    public static boolean isMuted() {
        return muted || volume <= 0.001;
    }

    public static void toggleMute() {
        muted = !muted;
        if (!muted && volume <= 0.001) {
            volume = 0.50;
        }
    }

    public static void setMuted(boolean m) {
        muted = m;
    }

    public static double getVolume() {
        return volume;
    }

    public static int getVolumePercent() {
        if (muted) return 0;
        return (int) Math.round(volume * 100);
    }

    public static void setVolume(double v) {
        volume = Math.max(0.0, Math.min(1.0, v));
        if (volume > 0.001) {
            muted = false;
        } else {
            muted = true;
        }
    }

    private static void queueSound(short[] monoSamples) {
        if (muted || volume <= 0.001) return;
        int numFrames = monoSamples.length;
        byte[] buffer = new byte[numFrames * 4]; // 16-bit stereo = 4 bytes per frame
        double volScale = Math.pow(volume, 1.8);

        for (int i = 0; i < numFrames; i++) {
            short sample = (short) Math.max(-32768, Math.min(32767, Math.round(monoSamples[i] * volScale)));
            byte bLow = (byte) (sample & 0xFF);
            byte bHigh = (byte) ((sample >> 8) & 0xFF);
            // Left channel
            buffer[i * 4] = bLow;
            buffer[i * 4 + 1] = bHigh;
            // Right channel
            buffer[i * 4 + 2] = bLow;
            buffer[i * 4 + 3] = bHigh;
        }
        soundQueue.offer(buffer);
    }

    public static void playPreviewBeep() {
        if (muted || volume <= 0.001) return;
        int numFrames = (int) (SAMPLE_RATE * 0.08);
        short[] samples = new short[numFrames];
        for (int i = 0; i < numFrames; i++) {
            double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / 660.0);
            samples[i] = (short) (Math.sin(angle) * 14000);
        }
        queueSound(samples);
    }

    public static void playShoot() {
        if (muted || volume <= 0.001) return;
        int numFrames = (int) (SAMPLE_RATE * 0.08); // 80ms
        short[] samples = new short[numFrames];
        for (int i = 0; i < numFrames; i++) {
            double freq = 900.0 - (i / (double) numFrames) * 550.0;
            double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq);
            samples[i] = (short) (Math.sin(angle) * 12000);
        }
        queueSound(samples);
    }

    public static void playHit() {
        if (muted || volume <= 0.001) return;
        int numFrames = (int) (SAMPLE_RATE * 0.05); // 50ms
        short[] samples = new short[numFrames];
        for (int i = 0; i < numFrames; i++) {
            double decay = 1.0 - (i / (double) numFrames);
            samples[i] = (short) ((Math.random() * 24000 - 12000) * decay);
        }
        queueSound(samples);
    }

    public static void playSuperEffective() {
        if (muted || volume <= 0.001) return;
        int numFrames = (int) (SAMPLE_RATE * 0.16); // 160ms
        short[] samples = new short[numFrames];
        int half = numFrames / 2;
        for (int i = 0; i < numFrames; i++) {
            double freq = (i < half) ? 659.25 : 987.77; // E5 -> B5
            double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq);
            samples[i] = (short) (Math.sin(angle) * 14000);
        }
        queueSound(samples);
    }

    public static void playBuy() {
        if (muted || volume <= 0.001) return;
        int numFrames = (int) (SAMPLE_RATE * 0.20); // 200ms
        short[] samples = new short[numFrames];
        int third = numFrames / 3;
        for (int i = 0; i < numFrames; i++) {
            double freq = (i < third) ? 523.25 : (i < 2 * third ? 659.25 : 783.99); // C5 -> E5 -> G5
            double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq);
            samples[i] = (short) (Math.sin(angle) * 13000);
        }
        queueSound(samples);
    }

    public static void playWaveStart() {
        if (muted || volume <= 0.001) return;
        int numFrames = (int) (SAMPLE_RATE * 0.24);
        short[] samples = new short[numFrames];
        int step = numFrames / 4;
        double[] freqs = {440.0, 554.37, 659.25, 880.0};
        for (int i = 0; i < numFrames; i++) {
            int idx = Math.min(3, i / step);
            double freq = freqs[idx];
            double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq);
            samples[i] = (short) (Math.sin(angle) * 13000);
        }
        queueSound(samples);
    }

    public static void playGameOver() {
        if (muted || volume <= 0.001) return;
        int numFrames = (int) (SAMPLE_RATE * 0.40);
        short[] samples = new short[numFrames];
        int step = numFrames / 4;
        double[] freqs = {440.0, 415.30, 392.0, 329.63};
        for (int i = 0; i < numFrames; i++) {
            int idx = Math.min(3, i / step);
            double freq = freqs[idx];
            double angle = 2.0 * Math.PI * i / (SAMPLE_RATE / freq);
            samples[i] = (short) (Math.sin(angle) * 14000);
        }
        queueSound(samples);
    }
}
