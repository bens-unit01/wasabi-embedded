package com.wowwee.serial;

public class Commands {
    public static final int   START_BYTE                   = (byte)0xFA;
    public static final int   DRIVE                        = (byte)0x81;
    public static final int   WAKE_WORD_DETECTED           = (byte)0x42;



    public static final int   RGB_CTRL_COMMAND          = 0x54;

        // RGB supported values
	//
	// /*
	// 1) Active Internet connection - Blue
	// 2) No Internet connection - Red
	// 3) Listening - Amber / orange
	// 4) Thinking Green long pulse
	// 5) Speaking? Green flashing (with intonation of voice?)
	// 6) resting state - green solid (if none of the above present)
	//
    public static final int   RGB_OFF                      = 0x00;
    public static final int   RGB_SOLID_RED                = 0x01;
    public static final int   RGB_SOLID_BLUE               = 0x02;
    public static final int   RGB_SOLID_AMBER              = 0x03;
    public static final int   RGB_SOLID_GREEN              = 0x04;
    public static final int   RGB_FLASH_GREEN_SLOW         = 0x05;
    public static final int   RGB_FLASH_GREEN_QUICK        = 0x06;
    public static final int   RGB_FLASH_BLUE_SLOWi         = 0x07;
    public static final int   RGB_FLASH_BLUE_QUICK         = 0x08;


}
