package com.dg.simmonitor;

public class GlobalData 
{
	public static int _timeInterval = 5;
	public static boolean _stopRilDaemon = true;
	
	public static int getTimeInterval()
	{
		return _timeInterval;
	}
	
	public static void setTimeInterval(int value)
	{
		_timeInterval = value;
	}
	
	public static boolean getStopRilDaemon()
	{
		return _stopRilDaemon;
	}
	
	public static void setStopRilDaemon(boolean value)
	{
		_stopRilDaemon = value;
	}

}
