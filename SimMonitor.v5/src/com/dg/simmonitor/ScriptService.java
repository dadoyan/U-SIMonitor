package com.dg.simmonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class ScriptService extends Service 
{
	private ScriptTask scrTask;
	final static String MY_ACTION = "MY_ACTION";
	final static String NO_ACTION = "NO_ACTION";
	private static final String TAG = "ScriptService";
	private PowerManager.WakeLock wakelock;
	private PowerManager pm;
	private Handler mHandler = new Handler();
	//private Handler callHandler = new Handler();
	public static int TIME_INTERVAL = 300000; 
	public static final int IMMEDIATELY = 1000;
	public static final int ONE_MINUTE = 60000;
	private CallStateListener callStateListener;
	private TelephonyManager tm;
	private EventsReceiver mEventsReceiver;
	private boolean isIncomingCall = false;
	private boolean isOutgoingCall = false;
	private boolean isScreenOff = false;
	private boolean isScreenOn = false;
	private boolean runAgain = true;
	//private boolean isShuttingDown = false;
	private boolean hasJustBooted = false;
	private boolean callIsOnHook = false;
	private boolean smsReceived = false;
	private Location myLocation = null;

	@Override
	public IBinder onBind(Intent args0) { 
		return null;
	}

	private Runnable collectAndParseTask = new Runnable() {
		public void run() {
			Log.v(TAG,"PeriodicTimerService - Awake");

			//if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) 
			//{
				RunCommands();

			//}
			/*else
			{
				scrTask = new ScriptTask();
				scrTask.execute(); 
			}*/
			//mHandler.removeCallbacks(collectAndParseTask);

			//RunScript();
			//ParseResults();
			mHandler.postDelayed(collectAndParseTask, TIME_INTERVAL);           
		}
	};
	
	private Runnable callTask = new Runnable() {
		public void run() {
			Log.v(TAG,"PeriodicTimerService - Awake");

			//if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) 
			//{
				RunCommands();

			//}
			/*else
			{
				scrTask = new ScriptTask();
				scrTask.execute(); 
			}*/
			//mHandler.removeCallbacks(collectAndParseTask);

			//RunScript();
			//ParseResults();
			mHandler.postDelayed(callTask, ONE_MINUTE);           
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		TIME_INTERVAL = Integer.parseInt(sharedPref.getString(SettingsActivity.TIME_INTERVAL_PREF, "")) * 60 * 1000;

		//String lala = "";
		Notification note=new Notification(R.drawable.ic_launcher, "Service started", System.currentTimeMillis());
		Intent i=new Intent(this, MainActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pi=PendingIntent.getActivity(this, 0, i, 0);

		note.setLatestEventInfo(this, "SimMonitor", "Service is running", pi);
		note.flags|=Notification.FLAG_NO_CLEAR;

		mHandler.removeCallbacks(collectAndParseTask);
		mHandler.postDelayed(collectAndParseTask, IMMEDIATELY);
		startForeground(1337, note);
		return(START_NOT_STICKY);
	}

	private class ScriptTask extends AsyncTask<String, Void, String>
	{    	
		@Override
		protected String doInBackground(String... urls) 
		{
			try
			{
				RunCommands();
				//ParseResults();
				return "";//m1;
			} 
			catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return e.getMessage();
			}
		}	    
	}

	@Override
	public void onCreate() 
	{
		callStateListener = new CallStateListener();
		tm = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE);	


		mEventsReceiver = new EventsReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
		/*intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
		intentFilter.addAction(Intent.ACTION_SCREEN_ON);*/
		intentFilter.addAction(Intent.ACTION_REBOOT);
		
		intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
						
		intentFilter.addAction(Intent.ACTION_SHUTDOWN);
		registerReceiver(mEventsReceiver, intentFilter);

		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
		wakelock.acquire();
		Log.v(TAG, "Script Service created.");

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopForeground(true);
		tm.listen(callStateListener, PhoneStateListener.LISTEN_NONE);
		unregisterReceiver(mEventsReceiver);
		mHandler.removeCallbacks(collectAndParseTask);
		mHandler.removeCallbacks(callTask);
		Log.v(TAG, "Script Service destroyed.");
		wakelock.release();
	}	

	private void RunCommands()
	{				
		/*try{

		//tm = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
		//tm.getAllCellInfo();
		tm.getCallState();
		tm.getCellLocation();
		tm.getDeviceSoftwareVersion();
		tm.getLine1Number();
		tm.getNeighboringCellInfo();
		tm.getNetworkOperator();
		tm.getNetworkOperatorName();
		tm.getNetworkType();
		tm.getPhoneType();
		tm.getSimCountryIso();
		tm.getSimOperator();
		tm.getSimOperatorName();
		tm.getSimSerialNumber();
		tm.getSimState();
		tm.getSubscriberId();
		tm.isNetworkRoaming();

		}
		catch(Exception ex)
		{
			Log.e(TAG, ex.getMessage());
		}*/


		String retVal = "";		
		try
		{		
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			boolean stopRil = sharedPref.getBoolean(SettingsActivity.STOP_RIL_DAEMON_PREF, false);
			hasJustBooted = sharedPref.getBoolean(SettingsActivity.SHUTDOWN_FLAG, false);

			String script = stopRil ? getString(R.string.scriptNameStopRil) : getString(R.string.scriptNameNoRil);
			String command = "sh " + getString(R.string.scriptsFolder) + "/" + script;		

			Process process = Runtime.getRuntime().exec("su");

			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			os.writeBytes(command + "\n");
			os.flush();
			//os.writeBytes("echo \"exit\"\n");
			//os.flush();
			os.writeBytes("exit\n");
			os.flush();

			os.close();
			process.waitFor();

			/*String line;
					StringBuilder sb = new StringBuilder();
					while((line = reader.readLine()) != null)
					{
						sb.append(line).append("\n");

					}
					String line2 = sb.toString();


					Intent intent = new Intent();
					intent.setAction(MY_ACTION);
					intent.putExtra("Results", line2);
					sendBroadcast(intent);*/


			ParseResults(reader);
			process.destroy();

			retVal = "Commands executed successfully.";
			Log.v(TAG, retVal);
		}
		catch(Exception ex)
		{
			retVal = "Commands execution failed. Message: " + ex.getMessage();
			Log.e(TAG, retVal);
		}	

		//return retVal;
	}

	private void ParseResults(BufferedReader reader)
	{		
		try 
		{			
			String line;

			String imsiResp = "";
			String copsResp = "";
			String cregResp = "";        		
			String ciphMode =  "";
			String KcSim = "";
			String KcUSim = "";
			String KcGprsSim = "";
			String KcGprsUSim = "";
			String CK = "";
			String IK = "";
			String tmsi = "";
			String tmsiTime = "";
			String lai = "";
			String ptmsiSim = "";
			String ptmsiSimSignVal = "";
			String raiSim = "";
			String rausSim = "";
			String ptmsiUSim = "";
			String ptmsiUSimSignVal = "";
			String raiUSim = "";
			String rausUSim = "";
			String usimThreshold = "";
			String providerId = "0";
			String provider = "";
			String lac = "";
			String cellID = "";

			String gpsLatitude = "";
			String gpsLongitude = "";
			String eventType = "";
			String networkType = "";
			String isNetworkRoaming = "";

			line = reader.readLine();
			while (line != null) 
			{            	
				/*if(line.contains("+CIMI"))
				{
					String newLine = reader.readLine();
					imsiResp = newLine.replace('"',  ' ').trim();
					while(!newLine.contains("CIMI_END"))
					{								
						if(!newLine.replace('"',  ' ').trim().equals("") && !newLine.replace('"',  ' ').trim().equals("0") && !newLine.replace('"',  ' ').trim().equals("4") && !newLine.contains("+") && !newLine.contains("AT") && !newLine.contains("CIMI_END") && !newLine.trim().contains("OK"))
							imsiResp = newLine.replace('"',  ' ').trim();
						newLine = reader.readLine();
					}
					line = reader.readLine();
					continue;
				}	*/			
				//Provider
				if(line.contains("+COPS:"))
				{            		           		           		      		
					provider = getSimpleResponseValue(line, "+COPS:")[2];
					line = reader.readLine();
					continue;
				}
				//LAC, CellID
				if(line.contains("+CREG:"))
				{
					String[] args = getSimpleResponseValue(line, "+CREG:");
					line = reader.readLine();
					if(args.length < 4)
						continue;
					lac = args[2];
					cellID = args[3];
				}            	
				//IMSI
				if(line.contains("AT+CRSM=176,28423,0,0,9"))
				{    
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
					{
						//Get Byte 2 to 9, Byte is in little endian
						imsiResp = hexSwap(value.substring(2, 18)).substring(1, value.substring(2, 18).length());
						
					}
					line = reader.readLine();
					continue;
				} 
				//Ciphering Mode
				if(line.contains("AT+CRSM=176,28589,0,0,3"))
				{    
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
					{
						//Get only Byte 3, Byte is in little endian
						String ofmBit = HexToBinary(value.substring(4, 6)).substring(0, 1);
						ciphMode = ofmBit.equals("1") ? "ON" : "OFF";
					}
					line = reader.readLine();
					continue;
				} 
				//Kc SIM
				if(line.contains("AT+CRSM=176,28448,0,0,9"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
						KcSim = value.substring(0, 16);
					line = reader.readLine();
					continue;
				}
				//Kc USIM
				if(line.contains("AT+CRSM=176,20256,0,0,9"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
						KcUSim = value.substring(0, 16);
					line = reader.readLine();
					continue;
				}
				//KcGPRS SIM
				if(line.contains("AT+CRSM=176,28498,0,0,9"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
						KcGprsSim = value.substring(0, 16);
					line = reader.readLine();
					continue;
				}
				//KcGPRS USIM
				if(line.contains("AT+CRSM=176,20306,0,0,9"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
						KcGprsUSim = value.substring(0, 16);
					line = reader.readLine();
					continue;
				}
				//CK, IK only USIM
				if(line.contains("AT+CRSM=176,28424,0,0,33"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
					{
						CK = value.substring(2, 34);
						IK = value.substring(34, 66);
					}
					line = reader.readLine();
					continue;      		
				}
				//TMSI, LAI, TMSI TIME 
				if(line.contains("AT+CRSM=176,28542,0,0,11"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
					{
						tmsi = value.substring(0, 8);
						lai =  value.substring(8, 18);
						tmsiTime = value.substring(18, 20);
					}
					line = reader.readLine();
					continue;
				}
				//PTMSI, PTMSI Signature Value, RAI, RAUS for SIM
				if(line.contains("AT+CRSM=176,28499,0,0,14"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
					{
						ptmsiSim = value.substring(0, 8);
						ptmsiSimSignVal = value.substring(8, 14);
						raiSim =  value.substring(14, 26);
						rausSim = value.substring(26, 28);
					}
					line = reader.readLine();
					continue;
				}
				//PTMSI, PTMSI Signature Value, RAI, RAUS for USIM
				if(line.contains("AT+CRSM=176,28531,0,0,14"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
					{
						ptmsiUSim = value.substring(0, 8);
						ptmsiUSimSignVal = value.substring(8, 14);
						raiUSim =  value.substring(14, 26);
						rausUSim = value.substring(26, 28);
					}
					line = reader.readLine();
					continue;
				}
				//THRESHOLD USIM only
				if(line.contains("AT+CRSM=176,28508,0,0,3"))
				{
					String value = getCrsmResponseValue(reader);
					if(value.contains("AT+CRSM="))
					{
						line = value;
						continue;
					}
					if(!value.equals(""))
						usimThreshold = value.trim();
					line = reader.readLine();
					continue;            		
				}
				if(line.contains("exit"))
				{
					reader.close();
					line = null;
					break;
				}
				line = reader.readLine();
			}

			//Set IMSI Setting if it doesnt have value or if it is different from the current value
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			String imsi = sharedPref.getString(SettingsActivity.IMSI_VALUE_PREF, "");
			if(imsi == null || imsi.equals("") || !imsi.equals(imsiResp))
			{
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putString(SettingsActivity.IMSI_VALUE_PREF, imsiResp);
				editor.commit();
			}

			//-----------------Append Values in a CSV file. If not exists create one--------------------------------------------
			File csvFile = new File(getString(R.string.dbFolder)+ "/" + getString(R.string.dbcsv)); // current directory
			boolean csvExists = csvFile.exists();

			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date date = new Date();

			FileWriter fw = new FileWriter(csvFile, true);
			java.io.PrintWriter pw = new java.io.PrintWriter(fw);

			//Write to file for the first row
			if(!csvExists)
				pw.println("Imsi,Type,Value,ProviderId,Provider,LAC,CellID,NetworkType,EventType,gpsLatitude,gpsLongitude,IsRoaming,Time");


			String finalKc = !KcSim.equals("") ? KcSim : KcUSim;
			String finalKcGprs = !KcGprsSim.equals("") ? KcGprsSim : KcGprsUSim;
			String finalPtmsi = !ptmsiSim.equals("") ? ptmsiSim : ptmsiUSim;
			String finalRai = !raiSim.equals("") ? raiSim : raiUSim;
			String finalRaus = !rausSim.equals("") ? rausSim : rausUSim;
			String finalPtmsiSignVal = !ptmsiSimSignVal.equals("") ? ptmsiSimSignVal : ptmsiUSimSignVal;
			
			if(!imsi.equals(""))
				providerId = imsi.substring(0, 5);
			Location gpsLoc = getGpsLocation();
			if(gpsLoc != null)
			{
				gpsLatitude = Double.toString(gpsLoc.getLatitude());//String.format("%d", gpsLoc.getLatitude());
				gpsLongitude = Double.toString(gpsLoc.getLongitude());//String.format("%d", gpsLoc.getLongitude());
			}

			tm.getCallState();
			tm.getNetworkType();

			eventType = getEventType();
			networkType = getNetworkType(tm.getNetworkType());
			isNetworkRoaming = String.format("%b", tm.isNetworkRoaming());

			//pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", "IMSI", imsiResp, provider, lac, cellID, networkType, eventType, gpsLatitude, gpsLongitude, isNetworkRoaming, date.toString())).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "Kc", finalKc, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "KcGPRS", finalKcGprs, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "CK", CK, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "IK", IK, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "TMSI", tmsi, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "TMSI TIME", tmsiTime, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "LAI", lai, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "PTMSI", finalPtmsi, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "PTMSI SIGNATURE VALUE", finalPtmsiSignVal, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : "0"), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "RAI", finalRai, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "RAUS", finalRaus, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			pw.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", imsiResp, "THRESHOLD", usimThreshold, providerId, provider, lac, cellID, networkType, eventType, (!gpsLatitude.equals("") ? gpsLatitude : ""), (!gpsLongitude.equals("") ? gpsLongitude : ""), (isNetworkRoaming.equals("true") ? "1" : "0"), dateFormat.format(date))).append("\n");
			//Flush the output to the file
			pw.flush();
			//Close the Print Writer
			pw.close();
			//Close the File Writer
			fw.close();       

			StringBuilder sb = new StringBuilder();
			sb.append("----").append(new Date().toString()).append("----\n");
			sb.append("Results from AT commands: \n");
			sb.append("IMSI: ").append(!imsiResp.equals("") ? imsiResp : "Unavailable").append("\n");
			sb.append("Ciph Mode: ").append(!ciphMode.equals("") ? ciphMode : "Unavailable").append("\n");
			sb.append("Kc: ").append(!finalKc.equals("") ? finalKc : "Unavailable").append("\n");			
			sb.append("KcGPRS: ").append(!finalKcGprs.equals("") ? finalKcGprs : "Unavailable").append("\n");			
			sb.append("CK: ").append(!CK.equals("") ? CK : "Unavailable").append("\n");
			sb.append("IK: ").append(!IK.equals("") ? IK : "Unavailable").append("\n");
			sb.append("TMSI: ").append(!tmsi.equals("") ? tmsi : "Unavailable").append("\n");
			sb.append("TMSI TIME: ").append(!tmsiTime.equals("") ? tmsiTime : "Unavailable").append("\n");
			sb.append("LAI: ").append(!lai.equals("") ? lai : "Unavailable").append("\n");
			sb.append("PTMSI SIGN VAL: ").append(!finalPtmsiSignVal.equals("") ? finalPtmsiSignVal : "Unavailable").append("\n");
			sb.append("PTMSI: ").append(!finalPtmsi.equals("") ? finalPtmsi : "Unavailable").append("\n");
			sb.append("RAI: ").append(!finalRai.equals("") ? finalRai : "Unavailable").append("\n");
			sb.append("RAUS: ").append(!finalRaus.equals("") ? finalRaus : "Unavailable").append("\n");
			sb.append("THRESHOLD: ").append(!usimThreshold.equals("") ? usimThreshold : "Unavailable").append("\n");
			sb.append("Provider: ").append(!provider.equals("") ? provider : "Unavailable").append("\n");
			sb.append("LAC: ").append(!lac.equals("") ? lac : "Unavailable").append("\n");		
			sb.append("CellID: ").append(!cellID.equals("") ? cellID : "Unavailable").append("\n");
			sb.append("NetworkType: ").append(!networkType.equals("") ? networkType : "Unavailable").append("\n");
			sb.append("EventType: ").append(!eventType.equals("") ? eventType : "Unavailable").append("\n");
			sb.append("Latitude: ").append(!gpsLatitude.equals("") ? gpsLatitude : "Unavailable").append("\n");
			sb.append("Longitude: ").append(!gpsLongitude.equals("") ? gpsLongitude : "Unavailable").append("\n");
			sb.append("Roaming: ").append(!isNetworkRoaming.equals("") ? isNetworkRoaming : "Unavailable").append("\n").append("\n");

			Intent intent = new Intent();
			intent.setAction(MY_ACTION);
			intent.putExtra("Results", sb.toString());
			sendBroadcast(intent);

		}
		catch(IOException ex)
		{
			Log.d(TAG, ex.getMessage());
		}

		finally 
		{

			try {
				reader.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Log.d(TAG, "DONE parsing.");

	}

	private Location getGpsLocation()
	{
		LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		boolean isGPSEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean isNetworkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		//get Your Current Location
		//LocationManager locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		MyCurrentLoctionListener locationListener = new MyCurrentLoctionListener();
		lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);

		Location loc = null;
		if (!isGPSEnabled && !isNetworkEnabled) 
			return null;
		else if (isGPSEnabled)
		{

			Log.d("GPS Enabled", "GPS Enabled");
			if (lm != null) 
				loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		}
		if (isNetworkEnabled)
		{
			if (loc == null)
			{			
				Log.d("GPS Enabled", "GPS Enabled");
				if (lm != null) 
					loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
		}		return loc;

	}

	private String getNetworkType(int networkType)
	{
		switch(networkType)
		{
		case TelephonyManager.NETWORK_TYPE_UNKNOWN: return "UNKNOWN";
		case TelephonyManager.NETWORK_TYPE_GPRS: return "GPRS";
		case TelephonyManager.NETWORK_TYPE_EDGE: return "EDGE";
		case TelephonyManager.NETWORK_TYPE_UMTS: return "UMTS";
		case TelephonyManager.NETWORK_TYPE_HSDPA: return "HSDPA";
		case TelephonyManager.NETWORK_TYPE_HSUPA: return "HSUPA";
		case TelephonyManager.NETWORK_TYPE_HSPA: return "HSPA";
		case TelephonyManager.NETWORK_TYPE_CDMA: return "CDMA";
		case TelephonyManager.NETWORK_TYPE_EVDO_0: return "EVDO_0";
		case TelephonyManager.NETWORK_TYPE_EVDO_A: return "EVDO_A";
		case TelephonyManager.NETWORK_TYPE_EVDO_B: return "EVDO_B";
		case TelephonyManager.NETWORK_TYPE_1xRTT: return "1xRTT";
		case TelephonyManager.NETWORK_TYPE_IDEN: return "IDEN";
		case TelephonyManager.NETWORK_TYPE_LTE: return "LTE";
		case TelephonyManager.NETWORK_TYPE_EHRPD: return "EHRPD";
		case TelephonyManager.NETWORK_TYPE_HSPAP: return "HSPAP";		
		default: return "UNKNOWN";
		}
	}

	private String getEventType()
	{
		/*if(tm.getCallState() == TelephonyManager.CALL_STATE_RINGING)
			return "RINGING";
		if(tm.getCallState() == TelephonyManager.CALL_STATE_OFFHOOK)
		{

		}
		else
		{*/
		if(isOutgoingCall)
		{
			//isOutgoingCall = false;
			return "OUTGOING_CALL";
		}
		else if(smsReceived)
		{
			smsReceived = false;
			return "OUTGOING_SMS";
		}
		else if(isIncomingCall)
		{
			//isIncomingCall = false;
			return "INCOMING_CALL";
		}
		else if(isScreenOn)
		{
			isScreenOn = false;
			return "SCREEN_ON";
		}
		else if(isScreenOff)
		{
			isScreenOff = false;
			return "SCREEN_OFF";
		}
		/*else if(isShuttingDown)
		{
			//isShuttingDown = false;
			return "POWER_OFF";
		}*/
		else if(hasJustBooted)
		{
			hasJustBooted = false;
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = sharedPref.edit();
			editor.putBoolean(SettingsActivity.SHUTDOWN_FLAG, false);
			editor.commit();
			return "POWER_ON";
		}
		else
			return "PERIODIC";
		//}
	}
	//------------------Helper Methods----------------------------------------------------
	private String getCrsmResponseValue(BufferedReader reader) throws IOException
	{
		String crsmPrefix = "+CRSM:";
		String newLine = reader.readLine();
		//String[] responses = new String[]{};
		while (!newLine.contains(crsmPrefix) && !newLine.contains("exit") && !newLine.contains("AT+CRSM"))
			newLine = reader.readLine();
		if(newLine.contains("exit")) return "";
		String[] args = newLine.substring(newLine.indexOf(crsmPrefix) + crsmPrefix.length()).trim().split(",");

		if(args[0].trim().equals("144"))
			return args[2].replace('"', ' ').trim();	
		if(newLine.contains("AT+CRSM="))
			return newLine;
		return "";
	}

	private String[] getSimpleResponseValue(String line, String reponsePrefix) throws IOException
	{		
		String[] args = line.substring(line.indexOf(reponsePrefix) + reponsePrefix.length()).trim().split(",");    
		for(int i=0; i<args.length; i++)
			args[i] = args[i].replace('"', ' ').trim();
		return args;		
	}

	private String HexToBinary(String Hex) 
	{
		int i = Integer.parseInt(Hex, 16);
		String Bin = Integer.toBinaryString(i);
		return Bin;
	}
	
	private String hexSwap(String origHex) 
	{
		String newHex = "";
		for(int i=0; i<origHex.length()/2; i++)
		{
			String temp = origHex.substring(i*2, i*2+2);
			//Swap
			newHex += temp.substring(1,2);
			newHex += temp.substring(0,1);
		}
		return newHex;
	}

	private class CallStateListener extends PhoneStateListener 
	{
		@Override
		public void onCallStateChanged(int state, String incomingNumber) 
		{
			switch (state) 
			{
			case TelephonyManager.CALL_STATE_RINGING:
				runAgain = false;
				isIncomingCall = true;
				//Intent intent = new Intent(getApplicationContext(), ScriptService.class);        
				//startService(intent);
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				callIsOnHook = true;
				mHandler.removeCallbacks(collectAndParseTask);
				mHandler.postDelayed(callTask, IMMEDIATELY);
				
				if(runAgain)
				{
					isOutgoingCall = true;
					//Intent intent1 = new Intent(getApplicationContext(), ScriptService.class);        
					//startService(intent1);
				}
				runAgain = true;
				break;
			case TelephonyManager.CALL_STATE_IDLE:
				if(callIsOnHook)
				{
					//mHandler.removeCallbacks(collectAndParseTask);
					mHandler.removeCallbacks(callTask);
					mHandler.postDelayed(collectAndParseTask, IMMEDIATELY);
				}
				callIsOnHook = false;
				isIncomingCall = false;
				isOutgoingCall = false;
				runAgain = true;
				/*mHandler.removeCallbacks(collectAndParseTask);
				mHandler.postDelayed(collectAndParseTask, TIME_INTERVAL);*/
				break;			
			}
		}
	}

	public class EventsReceiver extends BroadcastReceiver
	{
		public EventsReceiver() 
		{
		}

		@Override
		public void onReceive(Context context, Intent intent) 
		{
			String number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
			/*if(intent.getAction().equals(Intent.ACTION_ANSWER))
			{
				isIncomingCall = true;
				Intent intent2 = new Intent(getApplicationContext(), ScriptService.class);        
				startService(intent2);
			}
			else*/ if(intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
			{				
				isOutgoingCall = true;
				Intent intent2 = new Intent(getApplicationContext(), ScriptService.class);        
				startService(intent2);
			}
			/*else if(intent.getAction().equals(Intent.ACTION_SCREEN_OFF))
			{
				if(callIsOnHook) return;
				isScreenOff = true;
				Intent intent2 = new Intent(getApplicationContext(), ScriptService.class);        
				startService(intent2);
			}
			else if(intent.getAction().equals(Intent.ACTION_SCREEN_ON))
			{
				if(isIncomingCall || callIsOnHook) return;
				isScreenOn = true;
				Intent intent2 = new Intent(getApplicationContext(), ScriptService.class);        
				startService(intent2);
			}*/
			else if(intent.getAction().equals(Intent.ACTION_REBOOT) || intent.getAction().equals(Intent.ACTION_SHUTDOWN))// || intent.getAction().equals(Intent.QUICKBOOT_POWEROFF))
			{
				/*try 
				{
					Intent.ACTION_SHUTDOWN;
				} 
				catch (InterruptedException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
				SharedPreferences.Editor editor = sharedPref.edit();
				editor.putBoolean(SettingsActivity.SHUTDOWN_FLAG, true);
				editor.commit();

			}
			else if(intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
			{
				smsReceived = true;
				Intent intent2 = new Intent(getApplicationContext(), ScriptService.class);        
				startService(intent2);
			}
			
		}
	}

	public class MyCurrentLoctionListener implements LocationListener
	{
		@Override
		public void onLocationChanged(Location location) 
		{
			location.getLatitude();
			location.getLongitude();

			String myLocation = "Latitude = " + location.getLatitude() + " Longitude = " + location.getLongitude();

			//I make a log to see the results
			Log.e("MY CURRENT LOCATION", myLocation);

		}

		@Override
		public void onStatusChanged(String s, int i, Bundle bundle) {

		}

		@Override
		public void onProviderEnabled(String s) {

		}

		@Override
		public void onProviderDisabled(String s) {

		}
	}

}
