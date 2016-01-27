package com.mhci.gripandtipforce.model.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.mhci.gripandtipforce.R;
import com.mhci.gripandtipforce.model.BTEvent;
import com.mhci.gripandtipforce.model.FileDirInfo;
import com.mhci.gripandtipforce.model.FileType;
import com.mhci.gripandtipforce.model.ProjectConfig;

import java.util.LinkedList;
import java.util.List;

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
	private StringBuffer stringBuffer;
	private byte[] dataBuffer;
//	private List<String> cachedLogs;
	private boolean toStoreData;
	private String mUserID;
	private int remainedBytesToCollect = 0;
	private TxtFileManager txtFileManager = null;
	private String lastConnectedBTAddr = null;
	private boolean isAutoReconnectingMode = false;

	private Handler mWorkHandler = null;
	private HandlerThread mWorkerThread = null;

	private final static int totalNumDataPoints = ProjectConfig.numBytesPerSensorStrip * ProjectConfig.numSensorStrips;
	private final static byte headerByte1 = Byte.decode("0x0D");
	private final static byte headerByte2 = Byte.decode("0x0A");
	private final int logFileIndex = 0;

	private static BluetoothManager instance = null;
	public static BluetoothManager getInstance() {
		if(instance == null) {
			Log.d(DEBUG_TAG,"you should call init first");
		}
		return instance;
	}

	private void initThreadAndHandler() {
		mWorkerThread = new HandlerThread("BTClientWorkerThread");
		mWorkerThread.start();
		mWorkHandler = new Handler(mWorkerThread.getLooper());
	}

	public void reconnectIfAllowed() {
		if(isAutoReconnectingMode && lastConnectedBTAddr != null) {
			uiThreadHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					BTEvent btEvent = new BTEvent();
					btEvent.type = BTEvent.Type.Reconnecting;
					eventBus.post(btEvent);
					connect(lastConnectedBTAddr);
				}
			},2000);
		}
	}

	public void setAutoReconnectingMode(boolean enable) {
		isAutoReconnectingMode = enable;
	}

	public int getBTState() {
		return btSPP.getServiceState();
	}

	public void startLogging(String userID) {
		setAutoReconnectingMode(true);
		mUserID = userID;
		FileDirInfo dirInfo = new FileDirInfo(FileType.Log, ProjectConfig.getDirpathByID(mUserID), null);
		txtFileManager = new TxtFileManager(dirInfo, mContext);
		txtFileManager.createOrOpenLogFileSync(ProjectConfig.getGripForceLogFileName(mUserID), logFileIndex);
		toStoreData = true;
	}

	public void endLogging() {
		toStoreData = false;
		setAutoReconnectingMode(false);
		disconnect();
//		if(cachedLogs.size() > 0) {
//			txtFileManager.appendLogs(logFileIndex, cachedLogs);
//		}
		txtFileManager.closeFile(logFileIndex);
	}

	public void disconnect() {
		btSPP.disconnect();
	}

	public void connect(String addr) {
		sharedEvent = new BTEvent();
		sharedEvent.type = BTEvent.Type.Connecting;
		sendBTEventInMainThread(sharedEvent);
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

		initThreadAndHandler();
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
		stringBuffer = new StringBuffer();
		dataBuffer = new byte[totalNumDataPoints];
		toStoreData = false;
//		cachedLogs = new LinkedList<String>();

		btSPP.setBluetoothConnectionListener(new BluetoothSPP.BluetoothConnectionListener() {
			@Override
			public void onDeviceConnected(String name, String address) {
				sharedEvent = new BTEvent();
				sharedEvent.type = BTEvent.Type.Connected;
				sharedEvent.deviceAddr = address;
				lastConnectedBTAddr = address;
				sharedEvent.deviceName = name;
				eventBus.post(sharedEvent);
			}

			@Override
			public void onDeviceDisconnected() {
				sharedEvent = new BTEvent();
				sharedEvent.type = BTEvent.Type.Disconnected;
				eventBus.post(sharedEvent);
				reconnectIfAllowed();
			}

			@Override
			public void onDeviceConnectionFailed() {
				sharedEvent = new BTEvent();
				sharedEvent.type = BTEvent.Type.ConnectionFailed;
				eventBus.post(sharedEvent);
				reconnectIfAllowed();
			}
		});

		remainedBytesToCollect = 0;

		btSPP.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
			@Override
			public void onDataReceived(byte[] data, String message) {
				if(!toStoreData) {
					return;
				}

//				Log.d(DEBUG_TAG,"len:" + data.length);
				int numIncomingBytes = data.length;

				//due to current library would get rid of 0x0D and 0x0A
				//we only need to check whether current array size = 114
				if(numIncomingBytes == 114) { //which means complete data
					parsingDataAndStored(data, 0);
				}


//				if(remainedBytesToCollect > 0) {
//					if(remainedBytesToCollect <= numIncomingBytes) {
//						copyBytesIntoBuffer(dataBuffer, data, totalNumDataPoints - remainedBytesToCollect, 0, remainedBytesToCollect);
//						parsingDataAndStored(dataBuffer, 0);
//						int numBytesHasBeenUsed = remainedBytesToCollect;
//						remainedBytesToCollect = 0;
//						if(numIncomingBytes - numBytesHasBeenUsed > 0) {
//							startDetectHeader(data, numBytesHasBeenUsed);
//						}
//					}
//					else {
//						copyBytesIntoBuffer(dataBuffer, data, totalNumDataPoints - remainedBytesToCollect, 0, numIncomingBytes);
//						remainedBytesToCollect -= numIncomingBytes;
//					}
//				}
//				else {
//					startDetectHeader(data, 0);
//				}
			}

//			private void startDetectHeader(byte[] data, int startIndex) {
//				int numIncomingBytes = data.length;
//				boolean headerNotDetected = true;
//				for (int i = startIndex + 1; i < numIncomingBytes; i++) {
////					Log.d(DEBUG_TAG, "data:" + (data[i - 1] & 0xFF) + "," + (data[i] & 0xFF));
//					if ( ((data[i - 1] & 0xFF) == 0x0D) && ((data[i] & 0xFF) == 0x0A) ) {
//						Log.d(DEBUG_TAG,"header detected");
//						headerNotDetected = false;
//						int remainedBytesInData = (numIncomingBytes - i - 1);
//						if(remainedBytesInData >= totalNumDataPoints) {
//							parsingDataAndStored(data, i + 1);
//							remainedBytesInData -= totalNumDataPoints;
//							if(remainedBytesInData > 0) {
//								startDetectHeader(data, totalNumDataPoints + i + 1);
//							}
//						}
//						else {
//							copyBytesIntoBuffer(dataBuffer, data, 0, i + 1, remainedBytesInData);
//							remainedBytesToCollect = totalNumDataPoints - remainedBytesInData;
//						}
//						break;
//					}
//				}
//
//				if(headerNotDetected) {
//					remainedBytesToCollect = 0;
//				}
//			}
//
//			private void copyBytesIntoBuffer(byte[] buffer, byte[] src, int startIndexInBuffer, int startIndexInSrc, int numBytesToCopy) {
//				int end = numBytesToCopy + startIndexInSrc;
//				for(int j = startIndexInSrc;j < end;j++) {
//					buffer[startIndexInBuffer + j - startIndexInSrc] = src[j];
//				}
//			}
		});
	}

	private void sendBTEventInMainThread(BTEvent btEvent) {
		final BTEvent localEvent = btEvent;
		uiThreadHandler.post(new Runnable() {
			@Override
			public void run() {
				Log.d(DEBUG_TAG, "send BT event in Main thread");
				eventBus.post(localEvent);
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
		if(txtFileManager != null) {
			txtFileManager.closeFile(logFileIndex);
		}
		mContext = null;
		btSPP = null;
	}

	//Utils
	private void parsingDataAndStored(byte[] buffer, int startIndex) {
		stringBuffer.setLength(0);

		int numStrips = ProjectConfig.numSensorStrips;
		int numBytesInOneSensorStrip = ProjectConfig.numBytesPerSensorStrip;
		long timestampToLog = ProjectConfig.getTimestamp();

		for(int sensorStripIndex = 0;sensorStripIndex < numStrips;sensorStripIndex++) {
			stringBuffer.append(timestampToLog);
			for(int sensorDataIndex = 0;sensorDataIndex < numBytesInOneSensorStrip;sensorDataIndex++) {
				stringBuffer.append(',');
				stringBuffer.append(((buffer[startIndex + sensorStripIndex*numBytesInOneSensorStrip +  sensorDataIndex]) & 0xFF) - ProjectConfig.minSensorVal);
			}
			stringBuffer.append("\r\n");
		}

		txtFileManager.appendLogSync(logFileIndex, stringBuffer.toString());

//		Log.d(DEBUG_TAG,"add to cached logs, len:" + stringBuffer.toString().length());
//		cachedLogs.add(stringBuffer.toString());
//		if(cachedLogs.size() >= ProjectConfig.maxCachedLogData) {
//			txtFileManager.appendLogs(logFileIndex, cachedLogs);
//			cachedLogs = new LinkedList<String>();
//		}

	}

	public void decodeActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
			if(resultCode == Activity.RESULT_OK) {
				sharedEvent = new BTEvent();
				sharedEvent.type = BTEvent.Type.DeviceSelected;
				sharedEvent.deviceAddr = data.getExtras().getString(BluetoothState.EXTRA_DEVICE_ADDRESS);
				sharedEvent.deviceName = getDeviceName(sharedEvent.deviceAddr);
				sendBTEventInMainThread(sharedEvent);
				connect(sharedEvent.deviceAddr);
			}
		} else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
			if(resultCode == Activity.RESULT_OK) {
				initBTSPPService();
			} else {
				BTEvent btEvent = new BTEvent();
				btEvent.type = BTEvent.Type.EnablingFailed;
				eventBus.post(btEvent);
			}
		}
	}

}
