import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sound {
	static Clip snd_money;
	static Clip snd_placeholder;
	
	public Sound() {
		try {
			snd_money = AudioSystem.getClip();
			snd_money.open(AudioSystem.getAudioInputStream(new File("res/money.wav").getAbsoluteFile()));
			snd_placeholder = AudioSystem.getClip();
			snd_placeholder.open(AudioSystem.getAudioInputStream(new File("res/money.wav").getAbsoluteFile()));
		} catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}
	
	public void playSound(String sound) {
		switch (sound) {
		case "money":
			snd_money.start();
			break;
		case "placeholder":
			snd_placeholder.start();
			break;
		default:
			break;
		}
	}
}
