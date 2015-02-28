package com.dg.simmonitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class MainActivity extends Activity {

	boolean serviceIsStarted = false;
	boolean receiverIsRegistered = false;
	MyReceiver myReceiver;

	private ProgressDialog progressDialog;
	private static final String TAG = "MainActivity";
	final static String UPLOAD_ACTION = "UPLOAD_ACTION";
	private EditText editText;
	private Button btnStartService;
	private Button btnStopService;
	private TelephonyManager tm;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//Create folder if not exist
		createFolders();
		//Copy script to folder
		CopyScriptToFolder();

		//Set default settings
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		btnStartService= (Button) findViewById(R.id.btnStartService);
		btnStopService = (Button) findViewById(R.id.btnStopService);

		checkIfServiceIsRunning();

		final Button btnExecute = (Button) findViewById(R.id.btnExecuteNow);
		editText = (EditText)findViewById(R.id.editText1);

		btnStartService.setOnClickListener(new View.OnClickListener() {              
			public void onClick(View v) {
				btnStartService.setEnabled(false);
				StartService(ScriptService.NO_ACTION);
				btnStopService.setEnabled(true);
			}          
		});		

		btnStopService.setOnClickListener(new View.OnClickListener() {              
			public void onClick(View v) {
				btnStopService.setEnabled(false);		
				StopService();
				btnStartService.setEnabled(true);
			}          
		});

		btnExecute.setOnClickListener(new View.OnClickListener() {              
			public void onClick(View v) {
				setEnabledProgressDialog(true, "Waiting for results...");
				btnStartService.setEnabled(false);
				StartService(ScriptService.MY_ACTION);
				btnStopService.setEnabled(true);
			}          
		});	


	}

	private class SUploadTask extends AsyncTask<String, Void, String>
	{    	
		@Override
		protected String doInBackground(String... urls) 
		{
			String retVal = "";
			File file = new File(getString(R.string.dbFolder) + "/" + getString(R.string.dbcsv));
			File fileWithDate = new File(getString(R.string.dbFolder) + String.format("/db-Android-%s.csv", (new SimpleDateFormat("yyyy-MM-dd-HHmmss")).format(new Date())));
			file.renameTo(fileWithDate);

			String m1 = "";
			try 
			{
				m1 = SUpload(fileWithDate);
				Log.v(TAG, m1);		
				Upload(fileWithDate);

			} 
			catch (JSchException e) 
			{
				e.printStackTrace();
				Log.e(TAG, "JSchException: " + e.getMessage());
			} 
			catch (SftpException e)
			{
				e.printStackTrace();
				Log.e(TAG, "SFTPException: " + e.getMessage());
			}

			try
			{
				File fileToMove = new File(getString(R.string.uploadHistoryFolder) + "/" + fileWithDate.getName());                
				if(fileWithDate.renameTo(fileToMove))       			
					retVal = "Files uploaded successfully.\rFile moved to upload-history";
				else
				{
					retVal = "Files uploaded successfully.\rFailed to move file to upload-history. File will be deleted.";
					file.delete();
				} 
			}
			catch(Exception ex)
			{
				retVal = "Failed: " + ex.getMessage();
				File fileRollBack = new File(getString(R.string.dbFolder) + "/" + getString(R.string.dbcsv));
				fileWithDate.renameTo(fileRollBack);
			}
			finally
			{
				Log.e(TAG,  retVal);
			}
			return m1;
		}	  

		@Override
		protected void onPostExecute(String result) 
		{
			setEnabledProgressDialog(false, "");
			Toast.makeText(MainActivity.this, result, Toast.LENGTH_LONG).show();

		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);

		return true;
	}

	public void checkIfServiceIsRunning()
	{
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) 
		{
			if (ScriptService.class.getName().equals(service.service.getClassName())) {
				{
					btnStartService.setEnabled(false);
					btnStopService.setEnabled(true);
				}
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			onSettingsRequested();
			return true;
		case R.id.upload_data:
			onUploadDataClick();
			return true;
		case R.id.clear_data:
			onDataCleared();
			return true;
		case R.id.delete_history:
			onHistoryDelete();
			return true;
		default:
			return false;
		}
	}

	private void StartService(String action)
	{   
		myReceiver = new MyReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(action);

		registerReceiver(myReceiver, intentFilter);
		receiverIsRegistered = true;

		Intent intent = new Intent(this, ScriptService.class);        
		startService(intent);
		if(!serviceIsStarted)
			editText.setText("Service has started.\n");
		serviceIsStarted = true;

	}

	private void StopService()
	{    
		Intent intent = new Intent(this, ScriptService.class);        
		stopService(intent);
		editText.setText("Service has been killed.\n");
		serviceIsStarted = false;
	}

	private boolean isNetworkAvailable() 
	{
		ConnectivityManager connectivityManager  = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public void onUploadDataClick() 
	{											
		if(isNetworkAvailable())
		{
			setEnabledProgressDialog(true, "Uploading...");
			SUploadTask suploadTask = new SUploadTask();
			suploadTask.execute();
		}
		else
		{
			String message = "There is not network available";
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
			Log.e(TAG, message);
		}
	}   

	public void onSettingsRequested()
	{
		Intent i = new Intent(this, SettingsActivity.class);
		startActivity(i);
	}

	public void onDataCleared()
	{
		editText.setText("");
	}

	public void onHistoryDelete()
	{
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Delete Upload History");
		alertDialog.setMessage("All the upload history files will be deleted. Are you sure?");
		alertDialog.setCancelable(true);

		alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				DeleteHistoryFiles();
			}
		});

		alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});

		//alertDialog.setIcon(R.drawable.icon);
		alertDialog.show();
	}

	private void DeleteHistoryFiles()
	{
		setEnabledProgressDialog(true, "Deleting files...");
		File dir = new File(getString(R.string.uploadHistoryFolder));
		String[] files = dir.list();
		for(int i=0; i<files.length; i++)
		{
			String url = getString(R.string.uploadHistoryFolder) + "/" + files[i];
			File fileToDel = new File(url);
			fileToDel.delete();
		}
		setEnabledProgressDialog(false, "");
	}

	private void setEnabledProgressDialog(boolean enabled, String msg)
	{
		if(enabled)
		{
			progressDialog = ProgressDialog.show(MainActivity.this, "SimMonitor", msg);
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
		}
		else
		{
			progressDialog.dismiss();
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}

	}

	private void Upload(File file) throws JSchException, IOException {
		if (!file.exists()) {
			Log.v(TAG, "File ot exists");
			return;
		}

		FTPClient client = new FTPClient();
		FileInputStream fis = null;

		try {

			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

			String serverUrl = sharedPref.getString(SettingsActivity.DB_SERVER_IP, "");
			String userName = sharedPref.getString(SettingsActivity.DB_SERVER_USER, "");
			String password = sharedPref.getString(SettingsActivity.DB_SERVER_PASSWORD, "");
			boolean enableDbUpload = sharedPref.getBoolean(SettingsActivity.DB_SERVER_ENABLE, false);
			if(!enableDbUpload) return;
			

			client.enterLocalPassiveMode();
			client.setFileType(FTP.BINARY_FILE_TYPE);
			
			Log.e(TAG, "Connecting to ftp server... ");	   
			client.connect(serverUrl);
			client.login(userName, password);

			String filename = file.getPath();
			fis = new FileInputStream(filename);

			client.storeFile(file.getName(), fis);
			fis.close();
			client.logout();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			e.printStackTrace();
			throw e;
		}
	}

	private String SUpload(File fileWithDate) throws JSchException, SftpException
	{			
		String retVal = "";
		if(fileWithDate.exists())
		{        	
			try
			{        		
				SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
				sharedPref.getString(SettingsActivity.BACKUP_SERVER_IP, "");

				String serverUrl = sharedPref.getString(SettingsActivity.BACKUP_SERVER_IP, "");//getString(R.string.server);
				String userName = sharedPref.getString(SettingsActivity.BACKUP_SERVER_USER, "");//getString(R.string.username);
				String password = sharedPref.getString(SettingsActivity.BACKUP_SERVER_PASSWORD, "");//getString(R.string.password);

				JSch jsch = new JSch();

				Properties config  = new Properties();
				config.put("StrictHostKeyChecking", "no");
				config.put("compression.s2c", "zlib,none");
				config.put("compression.c2s", "zlib,none");

				Session session = jsch.getSession(userName, serverUrl, 22);
				session.setConfig(config);
				//session.setPort(22);
				session.setPassword(password);
				session.connect();

				ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");

				channel.connect();
				channel.cd(getString(R.string.sftp_folder) + "/");

				String imsi = sharedPref.getString(SettingsActivity.IMSI_VALUE_PREF, "");


				try
				{
					if(imsi != null && !imsi.equals(""))
						channel.cd(imsi);
					else
					{
						channel.mkdir(imsi);
						channel.chown(511, imsi);
						channel.cd(imsi+"/");

					}
				}
				catch(SftpException ex)
				{

					channel.mkdir(imsi);
					channel.chmod(511, imsi);
					channel.cd(imsi+"/");

				}


				/*for (Iterator<File> i = insertLstFiles.iterator(); i.hasNext(); )
        		{
        			//File currentFile = (File) i.next();        			

        			FileInputStream in = new FileInputStream(localPath + "/" + currentFile.getName());
        			channel.put(in, currentFile.getName());        	
        			in.close();
        			currentFile.delete();
        		}*/

				FileInputStream in = new FileInputStream(fileWithDate);
				channel.put(in, fileWithDate.getName());        	
				in.close();    			      		
				channel.disconnect();
				session.disconnect();


			}

			catch(Exception ex)
			{
				retVal = "Failed: " + ex.getMessage();

			}
		}		

		else
		{
			retVal = "There are not files to upload.";  
		}		
		Log.v(TAG, retVal);
		return retVal;   
	}

	private void createFolders() 
	{
		//createFolderIfNotExists(getString(R.string.rootFolder));
		createFolderIfNotExists(getString(R.string.dbFolder));
		//createFolderIfNotExists(getString(R.string.responseHistoryFolder));
		createFolderIfNotExists(getString(R.string.scriptsFolder));
		//createFolderIfNotExists(getString(R.string.tmpFolder));
		createFolderIfNotExists(getString(R.string.uploadHistoryFolder));
	}

	private void CopyScriptToFolder()
	{
		CopyScript(getString(R.string.scriptNameNoRil), R.raw.atcommandsnoril2);
		CopyScript(getString(R.string.scriptNameStopRil), R.raw.atcommandsstopril2);
		CopyScript("atcommandsnorilgs0.sh", R.raw.atcommandsnorilgs0);
		CopyScript("test.sh", R.raw.test);
		/*CopyScript("ate.sh", R.raw.ate);
		CopyScript("cimi.sh", R.raw.cimi);
		CopyScript("cops.sh", R.raw.cops);
		CopyScript("creg.sh", R.raw.creg);
		CopyScript("crsm28589.sh", R.raw.crsm28589);
		CopyScript("crsm28448.sh", R.raw.crsm28448);
		CopyScript("crsm20256.sh", R.raw.crsm20256);
		CopyScript("crsm28498.sh", R.raw.crsm28498);
		CopyScript("crsm20306.sh", R.raw.crsm20306);
		CopyScript("crsm28424.sh", R.raw.crsm28424);
		CopyScript("crsm28542.sh", R.raw.crsm28542);
		CopyScript("crsm28499.sh", R.raw.crsm28499);
		CopyScript("crsm28531.sh", R.raw.crsm28531);
		CopyScript("crsm28508.sh", R.raw.crsm28508);*/

	}

	private void CopyScript(String scriptName, int resId)
	{		
		try
		{
			InputStream in = getResources().openRawResource(resId);
			FileOutputStream out = new FileOutputStream(getString(R.string.scriptsFolder) + "/" + scriptName);
			byte[] buff = new byte[1024];
			int read = 0;

			try {
				while ((read = in.read(buff)) > 0) {
					out.write(buff, 0, read);
				}
			} finally {
				in.close();

				out.close();
			} 

		}
		catch(Exception ex)
		{
			Log.d(TAG, "Failed to copy script to folder scripts");
		}
	}

	private void createFolderIfNotExists(String path) {
		File dir = new File(path);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	/*@Override
	protected void onStop() 
	{
		unregisterReceiver(myReceiver);
		super.onStop();
	}*/

	@Override
	public void onDestroy()
	{
		if(receiverIsRegistered)
			unregisterReceiver(myReceiver);
		receiverIsRegistered = false;

		super.onDestroy();
	}

	private class MyReceiver extends BroadcastReceiver
	{		 
		@Override
		public void onReceive(Context arg0, Intent arg1) 
		{
			String datapassed = arg1.getStringExtra("Results");
			if(datapassed != null)
			{
				String cur = editText.getText().toString();
				//editText.setText(String.valueOf(datapassed));
				editText.setText("");
				editText.setText(cur + String.valueOf(datapassed));				
			}
			unregisterReceiver(myReceiver);
			receiverIsRegistered = false;
			setEnabledProgressDialog(false, "");
		}
	}



}
