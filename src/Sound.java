import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sound {
	static Clip music;
	static Clip snd_money;
	static Clip snd_placeholder;
	static Clip snd_upgrade;
	static Clip snd_brake;

	public Sound() {
		// TODO Picking up customer
		// TODO Dropping off customer
		// TODO Building new track
		// TODO Braking
		// TODO Upgrade
		try {
			snd_money = AudioSystem.getClip();
			snd_money.open(AudioSystem.getAudioInputStream(new File("res/money.wav").getAbsoluteFile()));
//			snd_brake = AudioSystem.getClip();
//			snd_brake.open(AudioSystem.getAudioInputStream(new File("res/brake.wav").getAbsoluteFile()));
//			snd_upgrade = AudioSystem.getClip();
//			snd_upgrade.open(AudioSystem.getAudioInputStream(new File("res/upgrade.wav").getAbsoluteFile()));
			snd_placeholder = AudioSystem.getClip();
			snd_placeholder.open(AudioSystem.getAudioInputStream(new File("res/money.wav").getAbsoluteFile()));
			music = AudioSystem.getClip();
			music.open(AudioSystem.getAudioInputStream(new File("res/TaxiMusic.wav").getAbsoluteFile()));
		} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}
	
	public void startSound(String sound) {
		switch (sound) {
		case "music":
			music.stop();
			music.setFramePosition(0);
			music.loop(9999);
			break;
		default:
			break;
		}
	}
	
	public void stopSound(String sound) {
		switch (sound) {
		case "music":
			snd_money.stop();
			break;
		default:
			break;
		}
	}

	public void playSound(String sound) {
		switch (sound) {
		case "money":
			snd_money.stop();
			snd_money.setFramePosition(0);
			snd_money.start();
			break;
		case "placeholder":
		default:
			snd_placeholder.stop();
			snd_placeholder.setFramePosition(0);
			snd_placeholder.start();
			break;
		}
	}
}
