package game;

import javax.sound.sampled.*;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

/**
 * AudioManager — generates all game sounds in code using javax.sound.sampled.
 * No audio files are needed. Every effect is a synthesized sine-wave tone
 * built from raw PCM data, so the game works out of the box on any machine.
 * How it works (good for students to read):
 *   Sound is just numbers. We fill a byte array with values that describe how
 *   a speaker cone should move 44,100 times per second. A sine wave at a given
 *   frequency produces a pure musical tone; changing the frequency over time
 *   creates sweeps, wah effects, and melodies.
 * EXTENSION: Replace any makeXxxData() method with your own wave shape, or
 * load a real WAV file with AudioSystem.getClip() and AudioSystem.getAudioInputStream().
 */

public class AudioManager {

    private static final int    SAMPLE_RATE = 44100;
    private static final double VOLUME      = 0.35;   // 0.0 – 1.0; keep below 0.5 to avoid clipping
    private static final AudioFormat FORMAT =
            new AudioFormat(SAMPLE_RATE, 16, 1, true, false); // 44.1 kHz, 16-bit, mono, signed, little-endian

    private final Clip chomp;
    private final Clip pellet;
    private final Clip ghostEaten;
    private final Clip death;
    private final Clip bonus;

    MediaPlayer mediaPlayer;

    public void playSong(String song)
    {
        if (mediaPlayer != null) mediaPlayer.stop();
        java.net.URL resource = getClass().getResource("/music/" + song + ".mp3");
        assert resource != null;
        Media sound = new Media(resource.toExternalForm());
        mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
        mediaPlayer.play();
    }

//    private final java.net.URL resource = getClass().getResource("/music/town.mp3");
//    private final MediaPlayer mediaPlayer;
//    {
//        assert resource != null;
//        Media sound = new Media(resource.toExternalForm());
//        mediaPlayer = new MediaPlayer(sound);
//        mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
//    }

    public AudioManager() {
        chomp      = makeClip(chompData());
        pellet     = makeClip(pelletData());
        ghostEaten = makeClip(ghostEatenData());
        death      = makeClip(deathData());
        bonus      = makeClip(bonusData());
    }

    // -----------------------------------------------------------------------
    // Public API — called by the game loop
    // ---------------------------=--------------------------------------------

    public void playChomp()      { play(chomp); }
    public void playPellet()     { play(pellet); }
    public void playGhostEaten() { play(ghostEaten); }
    public void playDeath()      { play(death); }
    public void playBonus()      { play(bonus); }

    // -----------------------------------------------------------------------
    // Playback helpers
    // -----------------------------------------------------------------------

    private void play(Clip clip) {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    // -----------------------------------------------------------------------
    // Clip factory
    // -----------------------------------------------------------------------

    private static Clip makeClip(byte[] pcm) {
        try {
            Clip clip = (Clip) AudioSystem.getLine(new DataLine.Info(Clip.class, FORMAT));
            clip.open(FORMAT, pcm, 0, pcm.length);
            return clip;
        } catch (Exception e) {
            return null; // no audio device — game still works silently
        }
    }

    // -----------------------------------------------------------------------
    // Sound synthesis
    // Each method returns a byte[] of 16-bit mono PCM at SAMPLE_RATE.
    // -----------------------------------------------------------------------

    // Waka-waka: two short notes with a brief silent gap between them
    private static byte[] chompData() {
        byte[] gap = new byte[(SAMPLE_RATE / 100) * 2]; // 10 ms silence (zeroes)
        return concat(tone(440, 0.025), gap, tone(330, 0.025));
    }

    // Power pellet: two rising notes
    private static byte[] pelletData() {
        return concat(tone(600, 0.08), tone(900, 0.10));
    }

    // Ghost eaten: bright high sting
    private static byte[] ghostEatenData() {
        return concat(tone(900, 0.05), tone(1200, 0.08));
    }

    // Death: exponential frequency sweep from 700 Hz down to 80 Hz over 1.2 s
    private static byte[] deathData() {
        int n = (int)(SAMPLE_RATE * 1.2);
        byte[] data = new byte[n * 2];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double frac = (double) i / n;
            double freq = 700 * Math.pow(80.0 / 700.0, frac);  // exponential sweep
            double amp  = VOLUME * (1.0 - frac);                // fade out as it falls
            phase += 2 * Math.PI * freq / SAMPLE_RATE;
            putSample(data, i, amp * Math.signum(Math.sin(phase)));
        }
        return data;
    }

    // Bonus: quick three-note ascending jingle (C E G)
    private static byte[] bonusData() {
        return concat(tone(523, 0.07), tone(659, 0.07), tone(784, 0.12));
    }

    // -----------------------------------------------------------------------
    // Low-level PCM helpers
    // -----------------------------------------------------------------------

    /** Pure sine tone at freqHz for the given number of seconds. */
    private static byte[] tone(double freqHz, double seconds) {
        int n = (int)(SAMPLE_RATE * seconds);
        byte[] data = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * freqHz * i / SAMPLE_RATE;
            // Fade out the last 20 % to prevent clicks at the end of the clip
            double amp = (i > n * 0.8) ? VOLUME * (1.0 - (i - n * 0.8) / (n * 0.2)) : VOLUME;
            putSample(data, i, amp * Math.signum(Math.sin(angle)));
        }
        return data;
    }

    /** Write a normalized sample value in [-1, 1] as little-endian 16-bit PCM. */
    private static void putSample(byte[] buf, int index, double value) {
        short s = (short)(value * Short.MAX_VALUE);
        buf[index * 2]     = (byte)(s & 0xFF);
        buf[index * 2 + 1] = (byte)((s >> 8) & 0xFF);
    }

    /** Concatenate any number of byte arrays into one. */
    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] a : arrays) total += a.length;
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] a : arrays) { System.arraycopy(a, 0, out, pos, a.length); pos += a.length; }
        return out;
    }
}
