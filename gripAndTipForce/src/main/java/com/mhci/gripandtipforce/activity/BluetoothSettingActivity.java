package com.mhci.gripandtipforce.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mhci.gripandtipforce.model.BTState;
import com.mhci.gripandtipforce.model.BTEvent;
import com.mhci.gripandtipforce.utils.BluetoothManager;
import com.mhci.gripandtipforce.R;
import com.mhci.gripandtipforce.model.ProjectConfig;

import de.greenrobot.event.EventBus;

public class BluetoothSettingActivity extends CustomizedBaseFragmentActivity {
	public final static String debug_tag = BluetoothSettingActivity.class.getName();
	
	private final static String emptyStateName = "無";
	
	private Context mContext = null;
	private TextView mLastSelectedBT = null;
	private TextView mCurrentSelectedBT = null;
	private TextView mCurrentBTState = null;
	private Button nextPageButton = null;
	private Button btConnectButton = null;

	//data
	private String mCurrentSelectedBTName = null;
	private String mCurrentSelectedBTAddress = null;
	private String mLastSelectedBTAddress = null;

	//Utils
	private SharedPreferences mBTSettings;
	private BluetoothManager mBTManager;
	private EventBus eventBus;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bt_setting);
		mContext = this;
		mBTManager = BluetoothManager.getInstance();
		eventBus = EventBus.getDefault();

		initUI();

		mBTSettings = getSharedPreferences(ProjectConfig.Key_Preference_UserInfo, Context.MODE_PRIVATE);
		if(mBTSettings != null) {
			mLastSelectedBT.setText(mBTSettings.getString(ProjectConfig.Key_Preference_LastSelectedBT, emptyStateName));
			mLastSelectedBTAddress = mBTSettings.getString(ProjectConfig.Key_Preference_LastSelectedBTAddress, null);
		}

	}

	private void initUI() {

		mCurrentSelectedBT = (TextView)findViewById(R.id.text_currently_selected_device);
		mCurrentSelectedBT.setText(emptyStateName);
//		mCurrentSelectedBT.addTextChangedListener(textChangedListener);

		mLastSelectedBT = (TextView)findViewById(R.id.text_lastly_selected_device);
		mLastSelectedBT.setText(emptyStateName);
//		mLastSelectedBT.addTextChangedListener(textChangedListener);

		mCurrentBTState = (TextView)findViewById(R.id.text_current_bt_state);

		((Button)findViewById(R.id.button_select_from_available_devices)).setOnClickListener(buttonListener);

		nextPageButton = (Button)findViewById(R.id.button_experiment_next_step);
		nextPageButton.setOnClickListener(buttonListener);

		btConnectButton = (Button)findViewById(R.id.button_bt_connect);
		btConnectButton.setOnClickListener(buttonListener);

		BTEvent sharedBTEvent = new BTEvent();
		sharedBTEvent.type = BTEvent.Type.Disconnected;
		setRelatedUI(sharedBTEvent);
	}

	@Override
	protected void onResume() {
		super.onResume();
		eventBus.register(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		eventBus.unregister(this);
		Log.d(debug_tag, "onPause in " + BluetoothSettingActivity.class.getName());
	}

//	private TextWatcher textChangedListener = new TextWatcher() {
//
//		private final static String debug_tag = "textWatcherTest";
//
//		private void setButtonsVisibility(boolean toShow) {
//			if(toShow) {
//				nextPageButton.setVisibility(View.VISIBLE);
//			}
//			else {
//				nextPageButton.setVisibility(View.GONE);
//			}
//		}
//
//		@Override
//		public void onTextChanged(CharSequence s, int start, int before, int count) {
//			//Log.d(debug_tag, "onTextChanged:" + s.toString() + ",start:" + start + ",before:" + before + ",count:" + count);
//			if(!canGoToNextPage()) {
//				setButtonsVisibility(false);
//			}
//			else {
//				setButtonsVisibility(true);
//			}
//		}
//
//		@Override
//		public void beforeTextChanged(CharSequence s, int start, int count,
//				int after) {
//			//Log.d(debug_tag, "beforeTextChanged:" + s.toString() + ",start:" + start + ",count:" + count);
//		}
//
//		@Override
//		public void afterTextChanged(Editable s) {
//			//Log.d(debug_tag, "afterTextChanged:" + s.toString());
//		}
//	};

//	private boolean canGoToNextPage() {
//		return !(mCurrentSelectedBT.getText().toString().equals(emptyStateName));
//	}

	private void setBTName(String name) {
		mCurrentSelectedBTName = name;
		if(name == null) {
			mCurrentSelectedBT.setText(emptyStateName);
		}
		else {
			mCurrentSelectedBT.setText(name);
		}
	}
	
	private void setBTNameAndAddress(String name, String addr) {
		setBTName(name);
		mCurrentSelectedBTAddress = addr;
	}
	
	private void resetDataContainer() {
		setBTNameAndAddress(null, null);
	}

	private void setViewUnclickable(View v) {
		v.setClickable(false);
	}
	
	private OnClickListener buttonListener = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			if(v.getId() == R.id.button_select_from_available_devices) {
//				setViewUnclickable(v);
				mBTManager.startDiscoveryAndShowList(BluetoothSettingActivity.this);
			}
			else if(v.getId() == R.id.button_bt_connect) {
				String btnText = ((Button)v).getText().toString();
				if(btnText.equals(toConnectText)) {
					if(mCurrentSelectedBTAddress == null && mLastSelectedBTAddress != null) {
						setBTNameAndAddress(mLastSelectedBT.getText().toString(), mLastSelectedBTAddress);
					}

					if(mCurrentSelectedBTAddress != null) {
						mBTManager.connect(mCurrentSelectedBTAddress);
					}
				}
				else if(btnText.equals(toDisconnectText) || btnText.equals(cancelConnectingText)) {
					mBTManager.disconnect();
				}
			}
			else if(v.getId() == R.id.button_experiment_next_step) {
				// 1. Instantiate an AlertDialog.Builder with its constructor
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

				// 2. Chain together various setter methods to set the dialog characteristics
				builder.setMessage("確定選擇了正確的藍芽裝置嗎？\n如果選到錯誤的藍芽裝置將導致系統不正常反應");

				// Add the buttons
				builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if(mCurrentSelectedBTName != null && mCurrentSelectedBTAddress != null) { 
							//save current selected BT and use this setting
							Editor editor = mBTSettings.edit();
							editor.putString(ProjectConfig.Key_Preference_LastSelectedBT, mCurrentSelectedBTName);
							editor.putString(ProjectConfig.Key_Preference_LastSelectedBTAddress, mCurrentSelectedBTAddress);
							editor.commit();	
						}
						//else use old setting
						
						Intent intent = new Intent(mContext, ExperimentActivity.class);
						startActivity(intent);
						finish();
					}
				});
				
				builder.setNegativeButton("還沒", null);

				// 3. Get the AlertDialog from create()
				(builder.create()).show();
				
			}
			
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		mBTManager.decodeActivityResult(requestCode, resultCode, data);
	}

	public void onEvent(BTEvent event) {
		Log.d("BTEvent","onEvent:" + event.type.toString());
		setRelatedUI(event);
	}

	private void setRelatedUI(BTEvent event) {

		if(event.type == BTEvent.Type.Connecting) {
			mCurrentBTState.setText(ProjectConfig.btConnectingText);
			btConnectButton.setText(cancelConnectingText);
		}
		else if(event.type == BTEvent.Type.Connected) {
			mCurrentBTState.setText(ProjectConfig.btConnectedText);
			btConnectButton.setText(toDisconnectText);
			nextPageButton.setVisibility(View.VISIBLE);
		}
		else if(event.type == BTEvent.Type.Disconnected){
			mCurrentBTState.setText(ProjectConfig.btDisconnectedText);
			btConnectButton.setText(toConnectText);
			nextPageButton.setVisibility(View.GONE);
		}
		else if(event.type == BTEvent.Type.DeviceSelected) {
			setBTNameAndAddress(event.deviceName, event.deviceAddr);
		}

	}

	private final static String toConnectText = "開始連線";
	private final static String toDisconnectText = "斷開連線";
	private final static String cancelConnectingText = "取消連線";

//	progressBarLoadingBTDevices = (ProgressBar)mPopwinView.findViewById(R.id.progressbar_loading_progress);
//	progressBarLoadingBTDevices.setIndeterminate(true);
//	progressBarLoadingBTDevices.setVisibility(View.GONE);

	
}
