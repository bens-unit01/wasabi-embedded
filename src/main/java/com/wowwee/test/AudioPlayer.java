package com.wowwee.test;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;

public class AudioPlayer {
	public interface Listener {

		void onFinished(String url);

		void onError(String url);

		void onStopped(String url);
		
		void onPaused(String url);
	}	
    static String TAG = "AudioPlayer"; 
    Listener mListener = null;
	public AudioPlayer(Listener _listener){
		setupAudioPlayer(); 
		mListener = _listener;
	}
	

	private AudioMediaPlayerComponent audioPlayer;
	
	public class DownloadThread implements Runnable {
	
		private String url;
		public void setURL( String _url ){
			url = _url;
		}
		@Override
		public void run() {
			try{
				URL uri = new URL(url);			 
			    Path newFilePath = Paths.get("audio/"+url.substring(url.lastIndexOf("/")+1, url.length()));
			    Files.createFile(newFilePath);				
				try( InputStream stream = uri.openStream()) {
					 Files.copy(stream,  newFilePath, StandardCopyOption.REPLACE_EXISTING);
				 }
			 } catch(Exception e){
				 e.printStackTrace();
			 }
		}	 
	};
	
	public boolean playAudio(String url) {
		
		url = url.trim();
		if (url.startsWith("http"))
        {
			int n = url.lastIndexOf('/');
            if (n != -1) {
                String mp3 = url.substring(n + 1);
                log(TAG, mp3);

                String localFile = "audio/" + mp3;

                File file = new File(localFile);
                // Check if the audio file already exists
                if (file.exists() )                         	
    				url = file.getAbsolutePath();                        
            }                      
        } 
		
		if ( url.startsWith("http") ) {
			DownloadThread thread = new DownloadThread();
			thread.setURL(url);			
			thread.run();
		}
	    synchronized (audioPlayer.getMediaPlayer()) {
	    	 
	    	log(TAG, " playing audio"); 
	         // we are no longer in "PAUSED" state
	     //    stopOffset = -1;
	
	         // Reset url caches and state information
	      //   streamUrls = new HashSet<String>();
	       //  attemptedUrls = new HashSet<String>();
	
	        setupAudioPlayer();
	
	//         String url = stream.getUrl();
	//         long offset = stream.getOffsetInMilliseconds();
	
	        log("playing {}", url);
	
	        if (audioPlayer.getMediaPlayer().startMedia(url)) {
	        	audioPlayer.getMediaPlayer().setVolume(88);
	//             audioPlayer.getMediaPlayer().mute(currentlyMuted);
	 //            if (offset > 0) {
	            audioPlayer.getMediaPlayer().setTime(0);
	 //           }
	
	  //           if (stream.hasAttachedContent()) {
	               //  cachedAudioFiles.put(audioPlayer.getMediaPlayer().mrl(), url);
	   //          }
	
	            return true;
	        }
	        return false;
	    }
	}
 
	private void log(String TAG, String message) { 
		System.out.println(TAG + " " + message);  
	}
	public void stop() {
		if ( audioPlayer != null && audioPlayer.getMediaPlayer().isPlaying() ) 		  
			audioPlayer.getMediaPlayer().stop();	  
	}
	private void setupAudioPlayer() {
        audioPlayer = new AudioMediaPlayerComponent();

        audioPlayer.getMediaPlayer().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

            private boolean playbackStartedSuccessfully;

            private boolean bufferUnderrunInProgress;

            private boolean isPaused;

            @Override
            public void newMedia(MediaPlayer mediaPlayer) {
                log("newMedia: {}", mediaPlayer.mrl());
                playbackStartedSuccessfully = false;
                bufferUnderrunInProgress = false;
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                log("stopped: {}", mediaPlayer.mrl());
                mListener.onStopped(mediaPlayer.mrl());
            }

            @Override
            public void playing(MediaPlayer mediaPlayer) {
                log("playing: {}", mediaPlayer.mrl());
                long length = audioPlayer.getMediaPlayer().getLength();
                log("    length: ", length + " ");

                if (isPaused && playbackStartedSuccessfully) {
                    isPaused = false;
                }
            }

            @Override
            public void buffering(MediaPlayer mediaPlayer, float newCache) {
                if (playbackStartedSuccessfully && !bufferUnderrunInProgress) {
                    // We started buffering mid playback
                    bufferUnderrunInProgress = true;
                   // playbackStutterStartedOffsetInMilliseconds = getCurrentOffsetInMilliseconds();
                   // audioPlayerStateMachine.playbackStutterStarted();
                }

                if (bufferUnderrunInProgress && newCache >= 100.0f) {
                    // We are fully buffered after a buffer underrun event
                    //bufferUnderrunInProgress = false;
                    //audioPlayerStateMachine.playbackStutterFinished();
                }

                if (!playbackStartedSuccessfully && newCache >= 100.0f) {
                    // We have successfully buffered the first time and started playback
                    playbackStartedSuccessfully = true;
                    //audioPlayerStateMachine.playbackStarted();

                    //Stream stream = playQueue.peek();
                    

                }
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                log("paused: {}", mediaPlayer.mrl());
                mListener.onPaused(mediaPlayer.mrl());
                if (playbackStartedSuccessfully) {
                //    audioPlayerStateMachine.playbackPaused();
                }
                isPaused = true;
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                log("Finished playing {}", mediaPlayer.mrl());
                mListener.onFinished(mediaPlayer.mrl());
           /*     List<String> items = mediaPlayer.subItems();
                // Remember the url we just tried
                attemptedUrls.add(mediaPlayer.mrl());

                if (cachedAudioFiles.containsKey(mediaPlayer.mrl())) {
                    String key = mediaPlayer.mrl();
                    String cachedUrl = cachedAudioFiles.get(key);
                    deleteCachedFile(cachedUrl);
                    cachedAudioFiles.remove(key);
                }

                if ((items.size() > 0) || (streamUrls.size() > 0)) {
                    // Add to the set of URLs to attempt playback
                    streamUrls.addAll(items);

                    // Play any url associated with this play item that
                    // we haven't already tried
                    for (String mrl : streamUrls) {
                        if (!attemptedUrls.contains(mrl)) {
                            log("Playing {}", mrl);
                            mediaPlayer.playMedia(mrl);
                            return;
                        }
                    }
                    
                }

                // wait for any pending events to finish(playbackStarted/progressReport)
                while (controller.eventRunning()) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception e) {
                    }
                }

                // remove the item from the queue since it has finished playing
                playQueue.poll();

                progressReporter.stop();
                audioPlayerStateMachine.playbackNearlyFinished();
                audioPlayerStateMachine.playbackFinished();

                // unblock playback now that playbackFinished has been sent
                waitForPlaybackFinished = false;
                if (!playQueue.isEmpty()) {
                    // start playback if it wasn't the last item
                    startPlayback();
                }
                */
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                log("Error playing: {}", mediaPlayer.mrl());
                mListener.onError(mediaPlayer.mrl());
              

            }
        });
    }
}
