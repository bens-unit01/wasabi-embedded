/** 
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Amazon Software License (the "License"). You may not use this file 
 * except in compliance with the License. A copy of the License is located at
 *
 *   http://aws.amazon.com/asl/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package com.wowwee.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Calendar;
import java.util.TimeZone;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import com.wowwee.audio.IChunkUploader;
import com.wowwee.ISpeechDelegate;
import com.wowwee.audio.ISpeechEncoder;
import com.wowwee.audio.OggOpusEnc;

public class AudioCapture{
    private static AudioCapture sAudioCapture;
    private final TargetDataLine microphoneLine;
    private AudioFormat audioFormat;
    private AudioBufferThread thread;
    private static final int BUFFER_SIZE_IN_SECONDS = 6;
    private final int BUFFER_SIZE_IN_BYTES;
    private ISpeechDelegate delegate = null;
    private byte[][] buffers;
    private Object mLock = new Object();
    private int cntBuffers;
    private int maxBuffers = 5000;
    private ISpeechEncoder encoder = null;
    private EncoderThread encoderThread = null;
    private long startTime=0;
    private boolean bSpeechFlag = false;
    public void setDelegate(ISpeechDelegate val) {
        this.delegate = val;
    }
    
    private void log(String message) {
//     System.out.println(message);	
    } 
    public static AudioCapture getAudioHardware(final AudioFormat audioFormat,
            MicrophoneLineFactory microphoneLineFactory) throws LineUnavailableException {
        if (sAudioCapture == null) {
            sAudioCapture = new AudioCapture(audioFormat, microphoneLineFactory);
        }
        return sAudioCapture;
    }

    private AudioCapture(final AudioFormat audioFormat, MicrophoneLineFactory microphoneLineFactory)
            throws LineUnavailableException {
        super();
        buffers = new byte[maxBuffers][];
        this.audioFormat = audioFormat;
        microphoneLine = microphoneLineFactory.getMicrophone();
        try{
        encoder = new OggOpusEnc();        
        } catch (Exception e )
        {
        	e.printStackTrace();
        }          
        if (microphoneLine == null) {
            throw new LineUnavailableException();
        }
        BUFFER_SIZE_IN_BYTES = (int) ((audioFormat.getSampleSizeInBits() * audioFormat.getSampleRate()) / 8
                * BUFFER_SIZE_IN_SECONDS);
    }

    public InputStream getAudioInputStream(final RecordingStateListener stateListener,
            final RecordingRMSListener rmsListener, PostStreamingWithPipe.Listener sheneticsServiceListener) throws LineUnavailableException, IOException {
        try {
//            startCapture();
            PipedInputStream inputStream = new PipedInputStream(BUFFER_SIZE_IN_BYTES);
            encoderThread = new EncoderThread();
            thread = new AudioBufferThread(inputStream, stateListener, rmsListener, sheneticsServiceListener);
    		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));					  
            startTime = cal.getTimeInMillis();
            bSpeechFlag = false;
            thread.start();
            return inputStream;
        } catch (/*LineUnavailableException |*/ IOException e) {
            stopCapture();
            throw e;
        }
    }
    public void setSpeechFlag(){
    	bSpeechFlag = true;
    }
    public void stopCapture() {
        AppCtrl.audioPlayer.playAudio(AppCtrl.URL_DoneListeningEarcon_WAV);
        microphoneLine.stop();
        microphoneLine.close();

    }

    private void startCapture() throws LineUnavailableException {
        microphoneLine.open(audioFormat);
        microphoneLine.start();
    }

    public int getAudioBufferSizeInBytes() {
        return BUFFER_SIZE_IN_BYTES;
    }
    private class EncoderThread extends Thread {
        private boolean bStopped = true;
        private int uploadedAudioSize = 0;
        @Override
        public void run() {
        	bStopped = false;
            try {
                int i = 0;
                while(true) {
                	    Thread.sleep(20);
						while (cntBuffers > i ) {
								try{
						            uploadedAudioSize = encoder.encodeAndWrite(buffers[i], buffers[i].length);									
						            i++;
								}catch( Exception e){
									e.printStackTrace();
								}
                        }
                        if (bStopped || this.isInterrupted()) {
                            break;
						}               
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            bStopped = true;
        }        
        public void stopEncoding(){
        	bStopped = true;
        }
    }
    private class AudioBufferThread extends Thread implements IChunkUploader {

        private final AudioStateOutputStream audioStateOutputStream;
        private final PostStreamingWithPipe streamingPipeThread;        
        //private final FileOutputStream os;
        private RecordingStateListener stateListener;  
        private final byte[] buf = new byte[160000]; 
        //private ISpeechEncoder encoder = null;
        public AudioBufferThread(PipedInputStream inputStream,
                RecordingStateListener recordingStateListener, RecordingRMSListener rmsListener, PostStreamingWithPipe.Listener sheneticsServiceListener)
                        throws IOException {
            audioStateOutputStream =
                    new AudioStateOutputStream(inputStream, recordingStateListener, rmsListener);
            streamingPipeThread = new PostStreamingWithPipe();
	        streamingPipeThread.addListener(sheneticsServiceListener);	        
	        streamingPipeThread.start(); 

/*	        this.encoder = new OggOpusEnc();
	        this.encoder.initEncoderWithUploader(this);*/
	        encoder.initEncoderWithUploader(this);	        
	        this.stateListener = recordingStateListener;
        }

        @Override
        public void run() {
        	try{
        	cntBuffers = 0;
        	startCapture();
        	while(!streamingPipeThread.isRunning())
        		sleep(10);
//        	encoder.onStart();
//        	encoderThread.start();
            while (microphoneLine.isOpen() ) {
//            	System.out.println("<AudioCapture#run counterMic " + counterMic++);  
        		Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));					  
                long curTime = cal.getTimeInMillis();
                if ( !bSpeechFlag && curTime - startTime > 8000 ){
                	streamingPipeThread.stop();
                	stopCapture();
                	break;
                }
                if ( curTime - startTime > 60000 ) {
                	stopCapture();
                	break;
                }
                copyAudioBytesFromInputToOutput();
            }
            log("<AudioCapture#run> streaming end ..."); 
            closePipedOutputStream();
        	} catch ( Exception e ){
        		e.printStackTrace();
        	}        	
        }

        private void copyAudioBytesFromInputToOutput() {
        	byte[] data = new byte[3200];
            int numBytesRead = microphoneLine.read(data, 0, data.length);
            int uploadedAudioSize = 0;
            try {
            	streamingPipeThread.addStreamData(data, data.length);
/*                    try {
                        buffers[cntBuffers] = new byte[numBytesRead];
                        System.arraycopy(data, 0, buffers[cntBuffers], 0, numBytesRead);
                        cntBuffers++;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }*/
            } catch (Exception e) {
            	e.printStackTrace();
        	}
        }

        public void closePipedOutputStream() {
            try {
                audioStateOutputStream.close();
                streamingPipeThread.stopStreaming();
                stateListener.recordingCompleted(" ");
//                encoderThread.stopEncoding();                   
            } catch (IOException e) {
                log("Failed to close audio stream ");
            }
        }
        
        /**
         * Write data into socket
         *
         * @param data
         */
        public void upload(byte[] data){
          try {
              streamingPipeThread.addStreamData(data, data.length);
//              System.out.println("<AudioCapture#copyAudioBytesFromInput..> buffer size : " + microphoneLine.getBufferSize()); 
           //   System.arraycopy(data, 0, buf, counter, data.length); 
          } catch (Exception e) {
          	 e.printStackTrace();
              stopCapture();
          }
        }
        /**
         * Stop by sending out zero byte of data
         */
        public void stopUpload(){
            byte[] stopData = new byte[0];
            this.upload(stopData);
        }        
    }
}
