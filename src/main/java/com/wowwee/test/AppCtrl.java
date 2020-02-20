package com.wowwee.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import com.amazon.alexa.avs.wakeword.WakeWordDetectedHandler;
import com.amazon.alexa.avs.wakeword.WakeWordIPC;
import com.amazon.alexa.avs.wakeword.WakeWordIPCFactory;
import com.amazon.alexa.avs.wakeword.WakeWordIPC.IPCCommand;
import com.sun.jna.NativeLibrary;

import com.wowwee.serial.*;

public class AppCtrl{
	public static String SOCKET_ID = null;
	private boolean isEmbedded = false;
	private static final int WAKE_WORD_AGENT_PORT_NUMBER = 5123;
	static private TwoWaySerialComm serialPort = null;
    public static AudioPlayer audioPlayer = null; 	
	private static final String TAG = "SHE";
	private static io.socket.client.Socket socketIO = null;
	private static String userID = "wasabi1@she.ai";
	// private static String userID = "wasabi2@she.ai";
	private static String userPWD = "GoDogLabs123";
	private static Boolean mGreet = false;
	// private static Activity activity;
	private static String userDataString = "";
	private static JSONObject userData;
	private static String mAsrLang = "en-us";
	private static String emitTag = "";
	private static JSONObject emitData = null;
	private static String mSvrResults = "";
	private static String weatherDataString = "";
	private static JSONObject weatherData;
	// private static PHHueSDK phHueSDK = null;
	// private static PHBridge phHueBridge = null;
	private static String mPhoneNumber = "";
	private static String SSID = "";
	private static double gpsLat = 0.0;
	private static double gpsLng = 0.0;
	private static double speed = 0.0;
    public static String URL_ListeningEarcon_WAV = new File("audio/ListeningEarcon.wav").getAbsolutePath();
    public static String URL_WakewordEarcon_WAV = new File("audio/WakewordEarcon.wav").getAbsolutePath();
    public static String URL_DoneListeningEarcon_WAV = new File("audio/DoneListeningEarcon.wav").getAbsolutePath();
    
    public static boolean bAsk = false;

    public AudioPlayer.Listener mListener = new AudioPlayer.Listener() {
		
		@Override
		public void onStopped(String url) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onPaused(String url) {
			// TODO Auto-generated method stub
			
		}
		
		@Override
		public void onFinished(String url) {
			// TODO Auto-generated method stub
			if ( bAsk ) {
				bAsk = false;
				recordAudio(false);
			}
			else if ( url.contains("audio/ListeningEarcon.wav") || url.contains("audio/WakewordEarcon.wav") )
			{
				try {
					final InputStream inputStream = getMicrophoneInputStream(new RecordingRMSListener() {

						@Override
						public void rmsChanged(int rms) {
							String g = "";
							for (int i = 0; i < rms; i++)
								g += "#";
							// System.out.println("<rms> : " + g);
						}
					});
					System.out.println("socketIO.emit  - audio streaming ");
					// socketIO.emit("speechToTextStream", inputStream);

				} catch (LineUnavailableException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
		
		@Override
		public void onError(String url) {
			// TODO Auto-generated method stub
			
		}    	
	};
	private AudioCapture microphone;

	// private static MediaPlayer mediaPlayer = null;

	private static WakeWordIPC wakeWordIPC = null;

	private void initializeMicrophone() {

		if (microphone == null) {
			try {
				getMicrophone();
			} catch (LineUnavailableException e) {
				System.out.println("Could not open the microphone line.");
			}
		}
	}

	private void getMicrophone() throws LineUnavailableException {
		microphone = AudioCapture.getAudioHardware(new AudioFormat(16000f, 16, 1, true, false),
				new MicrophoneLineFactory());
	}

	private InputStream getMicrophoneInputStream(RecordingRMSListener rmsListener)
			throws LineUnavailableException, IOException {

		int numberRetries = 1;

		numberRetries = 5;

		for (; numberRetries > 0; numberRetries--) {
			try {
				InputStream is = microphone.getAudioInputStream(new RecordingStateListener() {

					@Override
					public void recordingStarted() {
						Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						System.out.println(cal.getTimeInMillis() + ":" + "Recording started ...");
					}

					@Override
					public void recordingCompleted(String data) {
						Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						System.out.println(
								cal.getTimeInMillis() + ":" + "Recording completed ... data length: " + data.length());

						System.out
								.println("Recording completed - sending IPC_RESUME ... data length: " + data.length());
						try {
							wakeWordIPC.sendCommand(IPCCommand.IPC_RESUME_WAKE_WORD_ENGINE);
							if (isEmbedded) {
								serialPort.write((byte) Commands.START_BYTE);
								serialPort.write((byte) Commands.RGB_CTRL_COMMAND);
								serialPort.write((byte) Commands.RGB_SOLID_BLUE);
								serialPort.write((byte) 0x00);
							}

							// Thread.sleep(400);
						} catch (IOException e) {
							System.out.println("Could not send the IPC_PAUSE_WAKE_WORD_ENGINE command");
						}

					}

				}, rmsListener, new PostStreamingWithPipe.Listener() {

					@Override
					public void onSpeechRecognized(String text, boolean isFinal) {
						Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
						System.out.println(cal.getTimeInMillis() + ":" + "onSpeechRecognized ...  " + text);
						try {
							JSONObject jsonObj = new JSONObject(text);
							String input = jsonObj.getString("text");
							System.out.println("onSpeechRecognized text:  " + text + " isFinal: " + isFinal);
							if (isFinal) {
								// microphone.stopCapture();
								// search(input);
							}

						} catch (JSONException e) {
							e.printStackTrace();
						}
					}

					@Override
					public void onError(Throwable t) {
						System.out.println("onError  ...");

					}

					@Override
					public void onCompleted() {
						System.out.println("onCompleted  ...");

					}
				});
				return is;
			} catch (LineUnavailableException | IOException e) {
				if (numberRetries == 1) {
					throw e;
				}
				System.out.println("Could not open the microphone line.");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e1) {
					System.out.println("exception:");
				}
			}
		}

		throw new LineUnavailableException();
	}

	private void recordAudio(boolean bWakeword) {
		audioPlayer.stop();			
		if ( bWakeword )
			audioPlayer.playAudio(URL_WakewordEarcon_WAV);
		else
			audioPlayer.playAudio(URL_ListeningEarcon_WAV);
	}

	public AppCtrl() {	
		log(TAG, "app init start ...");
		
		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("config.properties");

			// load a properties file
			prop.load(input);

			// get the property value and print it out
			System.out.println("userID:" + prop.getProperty("userID"));
			
			if ( !prop.getProperty("userID").isEmpty() )
				userID = prop.getProperty("userID");
			
			System.out.println("userPWD:" + prop.getProperty("userPWD"));
			if ( !prop.getProperty("userPWD").isEmpty() )
				userPWD = prop.getProperty("userPWD");	
			
			if ( !prop.getProperty("embedded").isEmpty() ) {
				isEmbedded = prop.getProperty("embedded").equalsIgnoreCase("true") ? true:false;
				System.out.println("Embedded flag " + isEmbedded);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (!isEmbedded) {
			NativeLibrary.addSearchPath("libvlc", "C:/Program Files/VideoLAN/VLC");
			// NativeLibrary.addSearchPath("libvlc", "C:/Program Files
			// (x86)/VideoLAN/VLC");
			login(userID, userPWD, true);
			initializeMicrophone();
		} else {
			serialPort = new TwoWaySerialComm();
			serialPort.write((byte) Commands.START_BYTE);
			serialPort.write((byte) Commands.RGB_CTRL_COMMAND);
			serialPort.write((byte) Commands.RGB_SOLID_BLUE);
			serialPort.write((byte) 0x00);
			login(userID, userPWD, true);
		}
		audioPlayer = new AudioPlayer(mListener);
		try {
			log(TAG, "Creating Wake Word IPC | port number: " + WAKE_WORD_AGENT_PORT_NUMBER);
			this.wakeWordIPC = new WakeWordIPCFactory().createWakeWordIPC(new WakeWordDetectedHandler() {

				@Override
				public void onWakeWordDetected() {
					try {
						System.out.println("<AppCtrl#onWakeWordDetecetd> ...");
					//	wakeWordIPC.sendCommand(IPCCommand.IPC_PAUSE_WAKE_WORD_ENGINE);
						if (isEmbedded) {
							serialPort.write((byte) Commands.START_BYTE);
							serialPort.write((byte) Commands.RGB_CTRL_COMMAND);
							serialPort.write((byte) Commands.RGB_FLASH_GREEN_QUICK);
							serialPort.write((byte) 0x00);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					initializeMicrophone();
					recordAudio(true);

				}
			}, WAKE_WORD_AGENT_PORT_NUMBER);
			this.wakeWordIPC.init();
			Thread.sleep(1000);
			log(TAG, "Created Wake Word IPC ok.");
		} catch (IOException e) {
			log(TAG, "Error creating Wake Word IPC ok.");
		} catch (InterruptedException e) {
			log(TAG, "Error creating Wake Word IPC ok.");
			e.printStackTrace();
		}
		if (!isEmbedded) {
			new Thread(new Runnable() {

				@Override
				public void run() {

					try {
						final BufferedReader br;
						br = new BufferedReader(new InputStreamReader(System.in));
						while (true) {

							System.out.println("Enter something : ");
							String input = br.readLine();

							if ("a01".equals(input)) {
								System.out.println("wakeword detected ...");

								// add your code here ...
							}

							if ("q".equals(input)) {
								System.out.println("Exit!");
								System.exit(0);
							}

							if ("t".equals(input)) {
								System.out.println("what time is it ? ");
								search("what time is it");
							}

							if ("l".equals(input)) {
								System.out.println("where am I ? ");
								search("where am I");
							}

							if ("u".equals(input)) {
								System.out.println("Uploading audio stream ");
								recordAudio(false);
							}

							if ("ip".equals(input)) {
								System.out.println("IPC_PAUSE");
								try {
									wakeWordIPC.sendCommand(IPCCommand.IPC_PAUSE_WAKE_WORD_ENGINE);
									// Thread.sleep(400);
								} catch (IOException e) {
									System.out.println("Could not send the IPC_PAUSE_WAKE_WORD_ENGINE command");
								}
							}

							if ("ir".equals(input)) {
								System.out.println("IPC_RESUME");
								try {
									wakeWordIPC.sendCommand(IPCCommand.IPC_RESUME_WAKE_WORD_ENGINE);
									// Thread.sleep(400);
								} catch (IOException e) {
									System.out.println("Could not send the IPC_PAUSE_WAKE_WORD_ENGINE command");
								}
							}

							System.out.println("input : " + input);
							System.out.println("-----------\n");
						}

					} catch (IOException e) {
						e.printStackTrace();
					} finally {

					}

				}
			}).start();
		}

		log(TAG, "app init end ...");
	}

	public static void main(String[] args) throws Exception {

		new AppCtrl();

	}

	public void login(String user, String pwd, boolean mGreet) {
		if (user == null || pwd == null)
			return;

		// activity = app;

		if (socketIO != null) {
			try {
				socketIO.disconnect();
				socketIO = null;
			} catch (Exception e) {
			}
		}

		userID = user;
		userPWD = pwd;

		this.mGreet = mGreet;
		socketConnect();
	}

	private void socketConnect() {
		try {
			// socketIO = IO.socket("https://api.godogfetch.com");
			socketIO = IO.socket("https://labs.godogfetch.com");
			//socketIO = IO.socket("https://localhost:8081");
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		socketIO.on(Socket.EVENT_CONNECT, onConnect);
		socketIO.on(Socket.EVENT_DISCONNECT, onDisconnect);
		socketIO.on(Socket.EVENT_CONNECT_ERROR, onError);
		socketIO.on(Socket.EVENT_CONNECT_TIMEOUT, onError);
		socketIO.on(Socket.EVENT_PING, onPing);
		socketIO.on("clientAuthOK", onClientauthok);
		socketIO.on("clientAuthNOK", onClientauthok);
		socketIO.on("updateLang", onUpdatelang);
		socketIO.on("updateCards", onUpdatecards);
		socketIO.on("updatechat", onUpdatechat);
		socketIO.on("updateresult", onUpdateresult);
		socketIO.on("updateweather", onUpdateweather);
		socketIO.on("setHueLightsOn", onSetHueLightsOn);
		socketIO.on("setHueLightsOff", onSetHueLightsOff);
		socketIO.on("setHueLightsDim", onSetHueLightsDim);
		socketIO.on("setHueLightsRandom", onSetHueLightsRandom);
		socketIO.on("setHueLightsColor", onSetHueLightsColor);
		socketIO.on("addCalendarEvent", onAddCalendarEvent);
		socketIO.on("addAlarmEvent", onAddAlarmEvent);
		socketIO.on("addTimerEvent", onAddTimerEvent);
		socketIO.on("audioOut", onAudioout);
		socketIO.on("ask", onAsk);
		socketIO.on("call", onCall);
		socketIO.on("dialsecretcode", onDialsecretcode);
		socketIO.on("playSong", onPlaySong);
		socketIO.on("nextTrack", onNextTrack);
		socketIO.on("prevTrack", onPrevTrack);
		socketIO.on("stopMusic", onStopMusic);
		socketIO.on("pauseMusic", onPauseMusic);
		socketIO.on("playMusic", onPlayMusic);
		socketIO.on("sendSMS", onSendSMS);
		socketIO.on("textResponse", onTextResponse);
		socketIO.on("sendEmail", onSendEmail);
		socketIO.on("launchURL", onLaunchurl);
		socketIO.on("control", onControl);
		socketIO.on("location", onLocation);
		socketIO.on("joystick", onJoystick);
		socketIO.on("locationConfig", onLocationConfig);
		socketIO.on("voiceMailAlert", onVoiceMailAlert);
		socketIO.on("voiceMailAlertOff", onVoiceMailAlertOff);
		socketIO.on("pong", onPong);
		socketIO.on("flashlighton", onFlashlighton);
		socketIO.on("flashlightoff", onFlashlightoff);
		socketIO.on("speechToTextResult", new Emitter.Listener() {

			@Override
			public void call(Object... args) {
				JSONObject jo = (JSONObject) args[0];
				try {
					String s = jo.getString("isFinal");
					String t = jo.getString("text");

					Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

					System.out.println(cal.getTimeInMillis() + ":" + "speechToTextResult ...------------- " + s
							+ " --------------------------  " + t);
					microphone.setSpeechFlag();
					if (s.equals("true")) {
						microphone.stopCapture();
						search(t);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		if (socketIO != null)
			socketIO.connect();
	}

	public void search(String q) {
		speakingStop();
		try {
			/*
			 * LocationManager lm = (LocationManager)
			 * activity.getSystemService(Context.LOCATION_SERVICE); if (lm !=
			 * null) { try { Location location =
			 * lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); if
			 * (location != null) { gpsLat = location.getLatitude(); gpsLng =
			 * location.getLongitude(); } } catch (SecurityException e ) {
			 * 
			 * } }
			 */

			JSONObject jsonObj = new JSONObject();
			jsonObj.put("token", userID);
			jsonObj.put("id", userID);
			jsonObj.put("pwd", userPWD);
			jsonObj.put("q", (String) q);
			jsonObj.put("latlong", String.format("%f,%f", gpsLat, gpsLng));
			jsonObj.put("speed", String.format("%f", speed));
			jsonObj.put("os", "android");
			jsonObj.put("cell", "false");

			// set current local time YYYYMMDDHHMMSS
			Calendar c = Calendar.getInstance();
			SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssz");
			jsonObj.put("time", df.format(c.getTime()));

			emit("search", jsonObj);

			// play waiting
			/*
			 * if (mediaPlayer == null) { mediaPlayer = new MediaPlayer(); }
			 * else { mediaPlayer.reset(); }
			 * mediaPlayer.setOnPreparedListener(new
			 * MediaPlayer.OnPreparedListener() { public void
			 * onPrepared(MediaPlayer mp) { mp.start(); } });
			 * mediaPlayer.setLooping(true); mediaPlayer.setDataSource(
			 * "https://labs.godogfetch.com/audio/moh.wav");
			 * mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
			 * mediaPlayer.prepareAsync();
			 */
		} catch (Exception e) {
		}
	}

	public void emit(String tag, JSONObject data) {
		// save emit data in case of reconnect
		emitTag = tag;
		emitData = data;
		try {
			if (socketIO != null && socketIO.connected()) {
				socketIO.emit(emitTag, emitData);
			} else {
				socketIO.connect();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void release() {
		try {
			if (socketIO != null)
				socketIO.disconnect();
		} catch (Exception e) {
			e.printStackTrace();
		}
		socketIO = null;
	}

	public void loginAuth(boolean status) {
		System.out.println("loginAuth: " + status);
	}

	public void SHEneticsListener(JSONObject data) {
	}

	public void setNotificationLight(boolean status) {
	}

	public void speakingStart() {
	}

	public void speakingStop() {
	}
	
    public void speak(String txt) {
        try {
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("id", userID);
            jsonObj.put("pwd", userPWD);
            jsonObj.put("tts", (String) txt);

            emit("speakIt", jsonObj);
        } catch (Exception e) {
        }
    }

	private Emitter.Listener onConnect = new Emitter.Listener() {
		@Override
		public void call(Object... args) {
			try {
				log(TAG, "SocketIO Connected");

				SOCKET_ID = socketIO.id();

				JSONObject jsonObj = new JSONObject();

				// set credentials
				jsonObj.put("id", userID);
				jsonObj.put("pwd", userPWD);
				jsonObj.put("xid", "");

				// set lat long
				double longitude = 0;
				double latitude = 0;
				/*
				 * LocationManager lm = (LocationManager)
				 * activity.getSystemService(Context.LOCATION_SERVICE); if (lm
				 * != null) { try { Location location =
				 * lm.getLastKnownLocation(LocationManager.GPS_PROVIDER); if
				 * (location != null) { longitude = location.getLongitude();
				 * latitude = location.getLatitude(); } } catch (
				 * SecurityException e) { e.printStackTrace(); } }
				 */
				jsonObj.put("latlong", String.format("%f,%f", latitude, longitude));

				// set current local time YYYYMMDDHHMMSS
				Calendar c = Calendar.getInstance();
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssz");
				jsonObj.put("time", df.format(c.getTime()));

				jsonObj.put("greet", "false");  // set to false, we'll greet user on clientauthok
				mGreet = true;

				if (socketIO != null)
					socketIO.emit("clientAuth", jsonObj);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onClientauthok = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				log(TAG, "auth success ...");
				userData = (JSONObject) arguments[0];
				mAsrLang = userData.getString("lang");
				loginAuth(true);
				speak("Hi " + userData.getString("firstName") + ", how are you? What do you want to do!");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};
	private Emitter.Listener onClientauthnok = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				log(TAG, "auth failed ...");
				loginAuth(false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onUpdatelang = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				userData = (JSONObject) arguments[0];
				mAsrLang = userData.toString();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onUpdatecards = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				userData = (JSONObject) arguments[0];
				loadUrl("javascript:showWelcome('" + userData.getString("firstName") + "')");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onUpdatechat = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				loadUrl("javascript:updateChat('')");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onUpdateresult = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				mSvrResults = ((JSONObject) arguments[0]).toString();
				loadUrl("javascript:updateResult('')");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onUpdateweather = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				weatherData = ((JSONObject) arguments[0]);
				weatherDataString = ((JSONObject) arguments[0]).toString();
				String tempType = ((JSONObject) arguments[1]).toString();
				loadUrl("javascript:updateWeather('" + tempType + "')");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onSetHueLightsOn = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {

			log(TAG, " onSetHueLighsOn");

			/*
			 * try { if (phHueBridge == null) return; String device =
			 * ((JSONObject) arguments[0]).toString(); PHLightState lightState =
			 * new PHLightState();
			 * 
			 * lightState.setOn(true);
			 * phHueBridge.setLightStateForDefaultGroup(lightState); } catch
			 * (Exception e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
		}
	};

	private Emitter.Listener onSetHueLightsOff = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			log(TAG, " onSetHueLighsOff");
			/*
			 * try { if (phHueBridge == null) return; String device =
			 * ((JSONObject) arguments[0]).toString(); PHLightState lightState =
			 * new PHLightState(); lightState.setOn(false);
			 * phHueBridge.setLightStateForDefaultGroup(lightState); } catch
			 * (Exception e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
		}
	};

	private Emitter.Listener onSetHueLightsDim = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			log(TAG, " onSetHueLightDim");
			/*
			 * try { if (phHueBridge == null) return; String data =
			 * ((JSONObject) arguments[0]).toString(); String device =
			 * ((JSONObject) arguments[1]).toString(); int bri = 127; if (data
			 * != null) { try { bri = Integer.parseInt(data); } catch (Exception
			 * e) { } }
			 * 
			 * if (bri < 0 || bri > 255) bri = 127;
			 * 
			 * PHLightState lightState = new PHLightState();
			 * lightState.setOn(true); lightState.setBrightness(bri); // range 0
			 * - 255 phHueBridge.setLightStateForDefaultGroup(lightState); }
			 * catch (Exception e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
		}
	};

	private Emitter.Listener onSetHueLightsRandom = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			log(TAG, " onSetHueLightRandom");
			/*
			 * try { if (phHueBridge == null) return; String device =
			 * ((JSONObject) arguments[0]).toString(); Random rand = new
			 * Random(); PHLightState lightState = new PHLightState();
			 * lightState.setOn(true); lightState.setHue(rand.nextInt(65535));
			 * phHueBridge.setLightStateForDefaultGroup(lightState); } catch
			 * (Exception e) { // TODO Auto-generated catch block
			 * e.printStackTrace(); }
			 */
		}
	};

	private Emitter.Listener onSetHueLightsColor = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			/*
			 * try { if (phHueBridge == null) return; String color =
			 * ((JSONObject) arguments[0]).toString(); String device =
			 * ((JSONObject) arguments[1]).toString(); float X = (float) 0.0;
			 * float Y = (float) 0.0; if (color != null) { try { JSONArray xy =
			 * new JSONArray(color); X = (float) xy.getDouble(0); Y = (float)
			 * xy.getDouble(1);
			 * 
			 * PHLightState lightState = new PHLightState();
			 * lightState.setOn(true); lightState.setX(X); lightState.setY(Y);
			 * phHueBridge.setLightStateForDefaultGroup(lightState); } catch
			 * (Exception e) { } } } catch (Exception e) { // TODO
			 * Auto-generated catch block e.printStackTrace(); }
			 */
		}
	};

	private Emitter.Listener onAddCalendarEvent = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};
	private Emitter.Listener onAddAlarmEvent = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onAddTimerEvent = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onAudioout = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				String remoteFile = arguments[0].toString();
				log(TAG, " onAudioout arg0: " + remoteFile);
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				System.out.println("------------------------ timestamp 2:" + cal.getTimeInMillis());

               	audioPlayer.playAudio(remoteFile);
				// play audio file
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onAsk = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				String remoteFile = arguments[0].toString();
				bAsk = true;
				log(TAG, " onAsk arg0: " + remoteFile);
				Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
				System.out.println("------------------------ timestamp 2:" + cal.getTimeInMillis());
               	audioPlayer.playAudio(remoteFile);
				// play audio file, then open microphone when audio file is
				// complete
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onCall = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				mPhoneNumber = arguments[0].toString();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onDialsecretcode = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onPlaySong = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onNextTrack = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onPrevTrack = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onStopMusic = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onPauseMusic = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onPlayMusic = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onSendSMS = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onTextResponse = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onSendEmail = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onLaunchurl = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onLaunchnav = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onControl = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				String cmd = arguments[0].toString();
				cmd = cmd.toUpperCase();
				JSONObject data = new JSONObject();
				data.put("type", "control");
				data.put("mode", cmd);
				SHEneticsListener(data);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onLocation = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				int id = Integer.parseInt((((JSONObject) arguments[0]).toString()));
				JSONObject data = new JSONObject();
				data.put("type", "location");
				data.put("id", id);
				SHEneticsListener(data);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onJoystick = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				int angle = Integer.parseInt(arguments[0].toString());
				int power = Integer.parseInt(arguments[1].toString());
				String deviceID = arguments[2].toString();

				// check if for this device
				if (!SSID.equals(deviceID))
					return;

				JSONObject data = new JSONObject();
				data.put("type", "joystick");
				data.put("angle", angle);
				data.put("power", power);
				data.put("deviceID", deviceID);
				SHEneticsListener(data);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onLocationConfig = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onVoiceMailAlert = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				String id = arguments[0].toString();
				String txt = arguments[1].toString();
				String wav = arguments[2].toString();
				setNotificationLight(true);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onVoiceMailAlertOff = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				setNotificationLight(false);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onPong = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onPing = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				log(TAG, " onPing ");
				// log(TAG, String.format("onMessage: %s",
				// arguments[0].toString()));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onFlashlighton = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};
	private Emitter.Listener onFlashlightoff = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onMessage = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private Emitter.Listener onDisconnect = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
			try {
				log(TAG, String.format("Socket Disconnect"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	};

	private Emitter.Listener onError = new Emitter.Listener() {
		@Override
		public void call(Object... arguments) {
		}
	};

	private void loadUrl(String url) {
	};

	public void log(String tag, String message) {
		// System.out.println("<" + tag + "> " + message);
	}
	
}
