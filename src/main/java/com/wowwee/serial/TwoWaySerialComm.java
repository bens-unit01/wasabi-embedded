package com.wowwee.serial;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;


public class TwoWaySerialComm {
         public OutputStream out = null;  

	 public TwoWaySerialComm(){
		 try{
			 this.connect( "/dev/ttyS0" );
		 } catch( Exception e ) {
			 e.printStackTrace();
		 }


	 }
	void connect( String portName ) throws Exception {
		CommPortIdentifier portIdentifier = CommPortIdentifier
			.getPortIdentifier( portName );
		if( portIdentifier.isCurrentlyOwned() ) {
			System.out.println( "Error: Port is currently in use" );
		} else {
			int timeout = 2000;
			CommPort commPort = portIdentifier.open( this.getClass().getName(), timeout );

			if( commPort instanceof SerialPort ) {
				SerialPort serialPort = ( SerialPort )commPort;
				serialPort.setSerialPortParams( 115200,
						SerialPort.DATABITS_8,
						SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE );

				InputStream in = serialPort.getInputStream();
				out = serialPort.getOutputStream();

//				( new Thread( new SerialReader( in ) ) ).start();
//				( new Thread( new SerialWriter( out ) ) ).start();

			} else {
				System.out.println( "Error: Only serial ports are handled by this example." );
			}
		}
	}

	public static class SerialReader implements Runnable {

		InputStream in;

		public SerialReader( InputStream in ) {
			this.in = in;
		}

		public void run() {
			byte[] buffer = new byte[ 1024 ];
			int len = -1;
			try {
				while( ( len = this.in.read( buffer ) ) > -1 ) {
					System.out.print( new String( buffer, 0, len ) );
				}
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public static class SerialWriter implements Runnable {

		OutputStream out;

		public SerialWriter( OutputStream out ) {
			this.out = out;
		}

		public void run() {
			try {
				System.out.println("serial-writer start"); 
				int c = 0;
				while( ( c = System.in.read() ) > -1 ) {
					this.out.write( c );
					try {
						System.out.println("- " + c);
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}			
				}
				System.out.println("serial-writer end"); 
			} catch( IOException e ) {
				e.printStackTrace();
			}
		}
	}

	public void write(byte b) {
	

			try {
				if( this.out != null){
					this.out.write((int)b); 
					System.out.println("sending data " + b); 	
				}
			} catch (IOException e) {

				e.printStackTrace(); 
          		}

	}
	

	public static void test2(){
	
        System.out.println("test2 running ..."); 	
		try {
			final TwoWaySerialComm sc = new TwoWaySerialComm(); 
				
//				 sc.connect( "/dev/ttyS2" );

			new Thread(new Runnable() {

				public void run() {
					while(true){
						try {
							sc.write((byte)5); 
							sc.write((byte)0); 
							sc.write((byte)0); 
							Thread.sleep(400);
						} catch (InterruptedException e) {
							e.printStackTrace();
			                         }

					}
				}
			}).start(); 
		} catch( Exception e ) {
			e.printStackTrace();
		}
	
	}
	//public static void main( String[] args ) {
	public static void test1( ) {
		try {
			final TwoWaySerialComm sc = new TwoWaySerialComm(); 
				
//				 sc.connect( "/dev/ttyS2" );

			new Thread(new Runnable() {

				public void run() {
					while(true){
						try {
							if( sc.out != null){
								sc.out.write(5); 
								sc.out.write(5); 
								sc.out.write(5); 
								System.out.println("sending data"); 	
							}
							Thread.sleep(400);
						} catch (InterruptedException e) {
							e.printStackTrace();
						} catch( IOException e ) {
				                       e.printStackTrace();
			                         }

					}
				}
			}).start(); 
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
}
