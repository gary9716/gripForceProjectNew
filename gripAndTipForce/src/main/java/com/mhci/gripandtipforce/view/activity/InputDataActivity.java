package com.mhci.gripandtipforce.view.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mhci.gripandtipforce.model.FileDirInfo;
import com.mhci.gripandtipforce.model.FileType;
import com.mhci.gripandtipforce.R;
import com.mhci.gripandtipforce.model.utils.TxtFileManager;
import com.mhci.gripandtipforce.model.ProjectConfig;

import java.util.LinkedList;

public class InputDataActivity extends CustomizedBaseFragmentActivity {
	
	public final static int fileIndex = 0;
	
	private TxtFileManager txtFileManager;
	private String mName;
	private String mGender;
	private String mBirthday;
	private String mDominant_hand;
	private long mUserGrade;
	private String mLivingCity;
	private String mUserID = null;
	private SharedPreferences preferences = null;
	
	private String getCheckedRadioButtonText(View fragmentView, int radioGroupId) {
		return ((RadioButton)fragmentView.findViewById(((RadioGroup)fragmentView.findViewById(radioGroupId)).getCheckedRadioButtonId())).getText().toString();
	}
	
	private boolean strIsEmptyOrNull(String str) {
		return (str == null || str.equals(""));
	}
	
	private boolean getUserInfo(View fragmentView) {
		boolean dataIsComplete = true;
		mName = ((EditText)fragmentView.findViewById(R.id.EditText_Name)).getText().toString();
		mGender = getCheckedRadioButtonText(fragmentView,R.id.RadioGroup_gender);
		mBirthday = ((TextView)fragmentView.findViewById(R.id.customizedClass_BirthDayDatePicker)).getText().toString();
		mDominant_hand = getCheckedRadioButtonText(fragmentView, R.id.RadioGroup_dominant_hand);
		mUserGrade = Long.valueOf(getCheckedRadioButtonText(fragmentView, R.id.RadioGroup_grades));
		mLivingCity = ((EditText)fragmentView.findViewById(R.id.EditText_City)).getText().toString();
		mUserID = ((EditText)fragmentView.findViewById(R.id.Number_ID)).getText().toString();
		if(strIsEmptyOrNull(mUserID)) {
			mUserID = preferences.getString(ProjectConfig.Key_Preference_UserID, ProjectConfig.defaultUserID);
		}
		dataIsComplete = !strIsEmptyOrNull(mName) && 
						 !strIsEmptyOrNull(mBirthday) &&
						 !strIsEmptyOrNull(mLivingCity);
		return dataIsComplete;
	}
	
	private void saveIntoPreference() {
		//save grade,dominant_hand,id
		Editor preferencesEditor = getSharedPreferences(ProjectConfig.Key_Preference_UserInfo, Context.MODE_PRIVATE).edit();
		preferencesEditor.putLong(ProjectConfig.Key_Preference_UserGrade, mUserGrade);
		preferencesEditor.putString(ProjectConfig.Key_Preference_UserDominantHand, mDominant_hand);
		preferencesEditor.putString(ProjectConfig.Key_Preference_UserID, mUserID);
		preferencesEditor.commit();
	}
	
	private String getFieldName(String identifier,View fragmentView, Resources res, String packageName) {
		return ((TextView)fragmentView.findViewById(res.getIdentifier(identifier, "id", packageName))).getText().toString();
	}
	
	private void saveIntoFile(View fragmentView) {
		Resources res = getResources();
		String packageName = getPackageName();
		LinkedList<String> userData = new LinkedList<>();
		userData.add(getFieldName("Disp_1", fragmentView, res, packageName) + ":" + mName);
		userData.add(getFieldName("Disp_2", fragmentView, res, packageName) + ":" + mGender);
		userData.add(getFieldName("Disp_3", fragmentView, res, packageName) + ":" + mBirthday);
		userData.add(getFieldName("Disp_4", fragmentView, res, packageName) + ":" + mDominant_hand);
		userData.add(getFieldName("Disp_5", fragmentView, res, packageName) + ":" + mUserGrade);
		userData.add(getFieldName("Disp_6", fragmentView, res, packageName) + ":" + mLivingCity);
		userData.add(getFieldName("Disp_7", fragmentView, res, packageName) + ":" + mUserID);
		txtFileManager.appendLogs(fileIndex, userData);
	}

	private boolean positiveButtonClicked = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_inputdata);
		Button confirmButton = (Button)findViewById(R.id.button_complete_personal_info);
		final View fragmentView = findViewById(R.id.input_data_fragment);
		final Context mContext = this;

		preferences = getSharedPreferences(ProjectConfig.Key_Preference_UserInfo, Context.MODE_PRIVATE);
		confirmButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//display an dialog to ask
				//save into a file and name according to some rules
				String additionalMsg = "";
				
				if(!getUserInfo(fragmentView)) {
					//not completed
					additionalMsg = "\n有些欄位是空白的喔，請再次確認";
				}
				
				// 1. Instantiate an AlertDialog.Builder with its constructor
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				
				// 2. Chain together various setter methods to set the dialog characteristics
				builder.setMessage("資料都確定填好且正確無誤了嗎？" + additionalMsg);

				// Add the buttons
				builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// User clicked OK button
						//fresh buffered data
						if(!positiveButtonClicked) {
							positiveButtonClicked = true;
							getUserInfo(fragmentView);
							FileDirInfo dirInfo = new FileDirInfo(FileType.PersonalInfo, ProjectConfig.getDirpathByID(mUserID), null);
							txtFileManager = new TxtFileManager(dirInfo, mContext);
							txtFileManager.createOrOpenLogFileSync(ProjectConfig.getPersonalInfoFileName(mUserID), fileIndex);
							saveIntoPreference();
							saveIntoFile(fragmentView);
							txtFileManager.closeFile(fileIndex);
							//add loading animation
							Intent intent = new Intent(mContext, BluetoothSettingActivity.class);
							startActivity(intent);
							finish();
						}
					}
				});
				
				builder.setNegativeButton("還沒", null);

				// 3. Get the AlertDialog from create()
				(builder.create()).show();
				
			}
		});
		
	}
}
