package com.mhci.gripandtipforce.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.mhci.gripandtipforce.R;
import com.mhci.gripandtipforce.model.BTEvent;

import app.akexorcist.bluetotohspp.library.BluetoothSPP;
import app.akexorcist.bluetotohspp.library.BluetoothState;
import app.akexorcist.bluetotohspp.library.DeviceList;
import de.greenrobot.event.EventBus;

public class BluetoothManager{
	public final static String DEBUG_TAG = "MyBTManager";
	private BluetoothSPP btSPP;
	private Context mContext = null;
	private EventBus eventBus;
	private BTEvent sharedEvent;
	private BluetoothAdapter mAdapter;
	private Handler uiThreadHandler;

	private static BluetoothManager instance = null;
	public static BluetoothManager getInstance() {
		if(instance == null) {
			Log.d(DEBUG_TAG,"you should call init first");
		}
		return instance;
	}


	//Utils
	public void decodeActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
			if(resultCode == Activity.RESULT_OK) {
				sharedEvent.type = BTEvent.Type.DeviceSelected;
				sharedEvent.deviceAddr = data.getExtras().getString(BluetoothState.EXTRA_DEVICE_ADDRESS);
				sharedEvent.deviceName = getDeviceName(sharedEvent.deviceAddr);
				sendBTEventInMainThread();
				connect(sharedEvent.deviceAddr);
			}
		} else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
			if(resultCode == Activity.RESULT_OK) {
				initBTSPPService();
			} else {
				sharedEvent = new BTEvent();
				sharedEvent.type = BTEvent.Type.EnablingFailed;
				eventBus.post(sharedEvent);
			}
		}
	}


	public void disconnect() {
		btSPP.disconnect();
	}

	public void connect(String addr) {
		sharedEvent.type = BTEvent.Type.Connecting;
		sendBTEventInMainThread();
		btSPP.connect(addr);
	}

	public void cancelDiscovery() {
		if(btSPP.isDiscovery()) {
			btSPP.cancelDiscovery();
		}
	}

	public void enableBT() {
		if(!btSPP.isBluetoothEnabled()) {
			btSPP.enable();
		}
	}

	public String getDeviceName(String addr) {
		BluetoothDevice device = mAdapter.getRemoteDevice(addr);
		if(device != null) {
			return device.getName();
		}
		else {
			return null;
		}
	}

	//UI related
	public void startDiscoveryAndShowList(Activity activity) {
		Intent intent = new Intent(activity, DeviceList.class);
		intent.putExtra("bluetooth_devices", "Bluetooth devices");
		intent.putExtra("no_devices_found", "No device");
		intent.putExtra("scanning", "Scanning");
		intent.putExtra("scan_for_devices", "Scan");
		intent.putExtra("select_device", "Select");
		intent.putExtra("layout_list", R.layout.select_bt);
		activity.startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
	}

	public static BluetoothManager init(Context context) {
		if(instance == null) {
			synchronized (BluetoothManager.class) {
				if(instance == null) {
					instance = new BluetoothManager(context);
				}
			}
		}

		return instance;
	}

	private BluetoothManager(Context context) {
		mContext = context;

		btSPP = new BluetoothSPP(mContext);

		if(!btSPP.isBluetoothAvailable()) {
			Log.d(DEBUG_TAG, "BT is not available");
			return;
		}

		if(!btSPP.isBluetoothEnabled()) {
			btSPP.enable();
		}
		else if(!btSPP.isServiceAvailable()) {
			initBTSPPService();
		}

		uiThreadHandler = new Handler(Looper.getMainLooper());

		eventBus = EventBus.getDefault();
		mAdapter = btSPP.getBluetoothAdapter();
		sharedEvent = new BTEvent();

		btSPP.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
			@Override
			public void onDeviceConnected(String name, String address) {
				sharedEvent.type = BTEvent.Type.Connected;
				sharedEvent.deviceAddr = address;
				sharedEvent.deviceName = name;
				eventBus.post(sharedEvent);
			}

			@Override
			public void onDeviceDisconnected() {
				sharedEvent.type = BTEvent.Type.Disconnected;
				eventBus.post(sharedEvent);
			}

			@Override
			public void onDeviceConnectionFailed() {
				sharedEvent.type = BTEvent.Type.ConnectionFailed;
				eventBus.post(sharedEvent);
			}
		});

		//TODO: add data received listener and do the IO part;

	}

	private void sendBTEventInMainThread() {
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				eventBus.post(sharedEvent);
			}
		});
	}

	private void initBTSPPService() {
		btSPP.setupService();
		btSPP.startService(BluetoothState.DEVICE_OTHER);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		mContext = null;
		btSPP = null;
	}
}
