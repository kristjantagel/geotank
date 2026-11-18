package ee.taltech.wiseones.geotank.clientcore.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import ee.taltech.wiseones.geotank.shared.util.Logger;

import static ee.taltech.wiseones.geotank.shared.constant.Constants.*;

/**
 * Manages all audio playback including background music and sound effects.
 * Handles loading and playing various sounds that occur during gameplay.
 */
public class AudioManager {
    private Music backgroundMusic;
    private Sound shootSound;
    private Sound levelUpSound;
    private Sound deathSound;
    private Sound upgradeSound;

    private float musicVolume = DEFAULT_MUSIC_VOLUME;
    private float effectsVolume = DEFAULT_EFFECTS_VOLUME;
    private float masterVolume = 1.0f;

    public AudioManager() {
        loadAudio();
    }

    /**
     * Loads all audio files. Call this during game initialization.
     */
    private void loadAudio() {
        try {
            // Load music
            backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("sounds/music/background.mp3"));
            backgroundMusic.setLooping(true);
            backgroundMusic.setVolume(effectiveMusicVolume());

            // Load sound effects
            shootSound = Gdx.audio.newSound(Gdx.files.internal("sounds/effects/shoot.wav"));
            levelUpSound = Gdx.audio.newSound(Gdx.files.internal("sounds/effects/levelup.wav"));
            deathSound = Gdx.audio.newSound(Gdx.files.internal("sounds/effects/death.wav"));
            upgradeSound = Gdx.audio.newSound(Gdx.files.internal("sounds/effects/upgrade.wav"));

            Logger.info("Audio files loaded successfully");
        } catch (Exception e) {
            Logger.error("Failed to load audio files", e);
        }
    }

    /**
     * Starts playing background music on game launch.
     */
    public void startBackgroundMusic() {
        if (backgroundMusic != null) {
            backgroundMusic.play();
            Logger.info("Background music started");
        }
    }

    /**
     * Plays the shoot sound effect.
     */
    public void playShootSound() {
        playSoundEffect(shootSound);
    }

    /**
     * Plays the level up sound effect.
     */
    public void playLevelUpSound() {
        playSoundEffect(levelUpSound);
    }

    /**
     * Plays the death sound effect.
     */
    public void playDeathSound() {
        playSoundEffect(deathSound);
    }

    /**
     * Plays the upgrade sound effect (for both class and stat upgrades).
     */
    public void playUpgradeSound() {
        playSoundEffect(upgradeSound);
    }

    /**
     * Helper method to play a sound effect with volume control.
     */
    private void playSoundEffect(Sound sound) {
        if (sound != null) {
            sound.play(effectiveEffectsVolume());
        }
    }

    private float effectiveMusicVolume() {
        return Math.clamp(musicVolume * masterVolume, 0, 1);
    }

    private float effectiveEffectsVolume() {
        return Math.clamp(effectsVolume * masterVolume, 0, 1);
    }

    /**
     * Sets the music volume independently.
     * @param volume Volume level from 0.0f to 1.0f
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.clamp(volume, 0, 1);
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(effectiveMusicVolume());
        }
    }

    /**
     * Sets the volume for sound effects.
     * @param volume Volume level from 0.0f to 1.0f
     */
    public void setEffectsVolume(float volume) {
        this.effectsVolume = Math.clamp(volume, 0, 1);
    }

    /**
     * Sets the master volume, which scales both music and SFX.
     * @param volume Volume level from 0.0f to 1.0f
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.clamp(volume, 0, 1);
        if (backgroundMusic != null) {
            backgroundMusic.setVolume(effectiveMusicVolume());
        }
    }

    /**
     * Disposes of all audio resources. Call this during game shutdown.
     */
    public void dispose() {
        if (backgroundMusic != null) {
            backgroundMusic.dispose();
        }
        if (shootSound != null) {
            shootSound.dispose();
        }
        if (levelUpSound != null) {
            levelUpSound.dispose();
        }
        if (deathSound != null) {
            deathSound.dispose();
        }
        if (upgradeSound != null) {
            upgradeSound.dispose();
        }
        Logger.info("Audio resources disposed");
    }
}
