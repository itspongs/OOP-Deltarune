import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

/**
 * Game audio. JDK built-in sound only, no libraries.
 * SFX are preloaded into small clip pools at startup, so plays are
 * instant (no file/line opening on the UI thread) and rapid sounds
 * overlap instead of cutting out. Music loops from music.wav.
 * Anything unloadable is skipped silently so the shop never breaks.
 */
public class AudioPlay {
    private static final int POOL = 3;
    private final Clip[] moveClips = new Clip[POOL];
    private final Clip[] selectClips = new Clip[POOL];
    private volatile boolean moveReady, selectReady;
    private int moveNext, selectNext;
    private Clip musicClip;

    public AudioPlay() {
        Thread loader = new Thread(() -> {
            loadPool(moveClips, "snd_menumove/snd_menumove");
            moveReady = true;
            loadPool(selectClips, "snd_select/snd_select");
            selectReady = true;
        });
        loader.setDaemon(true);
        loader.start();
    }

    private File find(String name) {
        for (String p : new String[]{"src/assets/sound/" + name, "assets/sound/" + name}) {
            File f = new File(p);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    private void loadPool(Clip[] pool, String name) {
        try {
            File f = find(name);
            if (f == null) {
                return;
            }
            AudioInputStream in = AudioSystem.getAudioInputStream(f);
            byte[] data = in.readAllBytes();
            for (int i = 0; i < pool.length; i++) {
                Clip c = AudioSystem.getClip();
                c.open(in.getFormat(), data, 0, data.length);
                pool[i] = c;
            }
        } catch (Exception ignored) {
        }
    }

    /** Looped background music. Call once at startup. */
    public void startMusic() {
        for (String n : new String[]{"music.wav", "music.mp3"}) {
            try {
                File f = find(n);
                if (f == null) {
                    continue;
                }
                AudioInputStream in = AudioSystem.getAudioInputStream(f);
                musicClip = AudioSystem.getClip();
                musicClip.open(in);
                musicClip.loop(Clip.LOOP_CONTINUOUSLY);
                musicClip.start();
                return;
            } catch (Exception ignored) {
            }
        }
        System.err.println("AudioPlay: no playable music (add music.wav next to music.mp3).");
    }

    public void stopMusic() {
        if (musicClip != null) {
            musicClip.stop();
        }
    }

    /** Cursor-move blip. */
    public void playMove() {
        if (!moveReady) {
            return;
        }
        fire(moveClips, true);
    }

    /** Confirm/select ding. */
    public void playSelect() {
        if (!selectReady) {
            return;
        }
        fire(selectClips, false);
    }

    private void fire(Clip[] pool, boolean move) {
        int i = move ? moveNext : selectNext;
        Clip c = pool[i];
        if (move) {
            moveNext = (moveNext + 1) % POOL;
        } else {
            selectNext = (selectNext + 1) % POOL;
        }
        if (c == null) {
            return;
        }
        c.stop();
        c.setFramePosition(0);
        c.start();
    }
}
