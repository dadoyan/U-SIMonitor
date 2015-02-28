package com.dg.simmonitor;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity    {

	EditText intervalText;
	CheckBox rilCheckBox;
	public static final String TIME_INTERVAL_PREF = "set_key_time_interval";
	public static final String STOP_RIL_DAEMON_PREF = "set_key_stop_ril_daemon";
	public static final String IMSI_VALUE_PREF = "set_key_imsi_value";
	public static final String SHUTDOWN_FLAG = "set_key_shutdown_flag";
	
	public static final String BACKUP_SERVER_IP = "set_key_backup_server_ip";
	public static final String BACKUP_SERVER_USER = "set_key_backup_server_username";
	public static final String BACKUP_SERVER_PASSWORD = "set_key_backup_server_password";
	
	public static final String DB_SERVER_IP = "set_key_db_server_ip";
	public static final String DB_SERVER_USER = "set_key_db_server_username";
	public static final String DB_SERVER_PASSWORD = "set_key_db_server_password";
	public static final String DB_SERVER_ENABLE = "set_key_db_server_enable";
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings);//.layout.activity_settings);
						
	}

	
	
	
///-----------------------------------------------BACKUP - SUPPORTS 3.0+ VERSIONS-----------------------------------------------------------------
	
//	public class SettingsActivity extends Activity   {
//
//		EditText intervalText;
//		CheckBox rilCheckBox;
//		public static final String TIME_INTERVAL_PREF = "set_key_time_interval";
//		public static final String STOP_RIL_DAEMON_PREF = "set_key_stop_ril_daemon";
//		public static final String IMSI_VALUE_PREF = "set_key_imsi_value";
//		
//		@Override
//		protected void onCreate(Bundle savedInstanceState)
//		{
//			super.onCreate(savedInstanceState);
//			//addPreferencesFromResource(R.xml.settings);//.layout.activity_settings);
//
//
//			// Display the fragment as the main content.
//			getFragmentManager().beginTransaction()
//			.replace(android.R.id.content, new SettingsFragment())
//			.commit();
//
//		
//			/*final Button btnSave = (Button) findViewById(R.id.btnSave);
//			final Button btnCancel = (Button) findViewById(R.id.btnCancel);
//
//			intervalText = (EditText) findViewById(R.id.intervalText);
//			rilCheckBox = (CheckBox) findViewById(R.id.rilCheckBox);
//					
//			btnSave.setOnClickListener(new View.OnClickListener() {              
//				public void onClick(View v) {
//					SaveSettings();
//				}          
//			});	
//			
//			btnCancel.setOnClickListener(new View.OnClickListener() {              
//				public void onClick(View v) {
//					DiscardSettings();
//				}          
//			});	*/
//					
//		}

	
	///NOTUSED ANYMORE
	/*private void SaveSettings()
	{
		GlobalData.setTimeInterval(Integer.parseInt(intervalText.getText().toString()));
		GlobalData.setStopRilDaemon(rilCheckBox.isChecked());
		finishActivity(0);
	}

	private void DiscardSettings()
	{
		finishActivity(0);
	}*/
///-----------------------------------------------BACKUP - SUPPORTS 3.0+ VERSIONS-----------------------------------------------------------------


}
