package edu.nju.ics.btsona;

import java.io.IOException;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Environment;

public class AudioPlayer implements MediaPlayer.OnErrorListener {

	private MediaPlayer mediaPlayer;

	public AudioPlayer() {

	}

	public void startPlay(AudioManager am) {
		try {
			this.mediaPlayer = new MediaPlayer();
			this.mediaPlayer.setOnErrorListener(this);
			this.mediaPlayer
					.setOnCompletionListener(new OnCompletionListener() {
						@Override
						public void onCompletion(MediaPlayer mp) {
							mp.release();
						}
					});
			// this.mediaPlayer.setDataSource(Environment
			// .getExternalStorageDirectory().getAbsolutePath()
			// + "/"
			// + MainActivity.folderName + "/source/stereo.wav");
			this.mediaPlayer.setDataSource(Environment
					.getExternalStorageDirectory().getAbsolutePath()
					+ "/"
					+ MainActivity.folderName + "/source/stereo.wav");
			this.mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

			this.mediaPlayer.prepare();
			this.mediaPlayer.start();
			
			System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaa");
			
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void stopPlay() {
		this.mediaPlayer.stop();
		this.mediaPlayer.release();
		this.mediaPlayer = null;
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		System.err.println("media error " + arg1 + "" + arg2);
		return false;
	}
}
