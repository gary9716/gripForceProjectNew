package com.mhci.gripandtipforce;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pen.Spen;
import com.samsung.android.sdk.pen.SpenSettingEraserInfo;
import com.samsung.android.sdk.pen.SpenSettingPenInfo;
import com.samsung.android.sdk.pen.document.SpenNoteDoc;
import com.samsung.android.sdk.pen.document.SpenPageDoc;
import com.samsung.android.sdk.pen.engine.SpenLongPressListener;
import com.samsung.android.sdk.pen.engine.SpenSurfaceView;
import com.samsung.android.sdk.pen.engine.SpenTouchListener;
import com.samsung.android.sdk.pen.pg.tool.SDKUtils;
import com.samsung.android.sdk.pen.settingui.SpenSettingEraserLayout;
import com.samsung.android.sdk.pen.settingui.SpenSettingPenLayout;
import com.mhci.gripandtipforce.BluetoothClientConnectService.LocalBinder;

public class ExperimentActivity extends CustomizedBaseFragmentActivity {

	public final static String debug_tag = "Experiment";
	
	public final static String Action_update_chars = ExperimentActivity.class.getName() + ".update_chars";

	public final static String Key_ExChars = "ExChars";

	private final static String DEBUG_TAG = ExperimentActivity.class.getName();

	private Context mContext = null;
	private LayoutInflater mInflater = null;

	private ImageView mPenBtn;
	private ImageView mEraserBtn;
	private ImageView mCleanBtn;
	private RelativeLayout mNextPageBtn;
	private TextView mPenTipInfo = null;
	private SpenSurfaceView[][] mCharBoxes = null;
	private SpenSurfaceView mOneLine = null;
	private SpenPageDoc mOneLinePageDoc = null;
	private RelativeLayout mOneLineContainer = null;
	private RelativeLayout[][] mWritableCharBoxContainers = null;
	private TextView[][] mExampleCharsTextView;
	private LinearLayout otherUIContainer = null;
	private LinearLayout charGroupsContainer = null;
	private TextView mBTStateInPreAndPostView = null;
	private TextView mBTState = null;
	private TextView mPreOrPostInfo = null;
	private View mExperimentView = null;
	private View mPreOrPostExperimentView = null;
	private Button mPreOrPostNextPageButton = null;
	private ImageView mExampleCharsGroupBG = null;


	private SpenSettingPenLayout mPenSettingView;
	private SpenSettingEraserLayout mEraserSettingView;

	private int mToolType = SpenSurfaceView.TOOL_SPEN;

	private int numCharBoxesInCol = 5;
	private int numWritableCharBoxCols = 1;
	private int numCharBoxesInAPage = numCharBoxesInCol * numWritableCharBoxCols;
	private boolean isToCleanMode = false;
	
	private String[] mExCharNames;
	private String[] mWritableCharBoxNames;

	private SpenSettingPenInfo penInfo;
	private SpenSettingEraserInfo eraserInfo;

	private LocalBroadcastManager mLBCManager = null;
	private Handler uiThreadHandler = null;

	private SpenNoteDoc mSpenNoteDoc;
	private SpenPageDoc[][] mSpenPageDocs = null;
	private HashMap<SpenSurfaceView, SpenPageDoc> viewModelMap = null;

	private Resources mRes;
	private String packageName;

	private ImgFileManager imgFileManager = null;
	private TxtFileManager txtFileManager = null;

	private int oneLineSurfaceViewIndex = 1000;
	private int charIndex = 0;
	private int mGrade = 1; //default we use first grade
	private int testingSetIndex = 0;
	private ArrayList<Integer> testingSetGradesSeq = null;

	private String mUserID = ProjectConfig.defaultUserID;
	private int mUserGrade = 1;
	private UIState preOrPostInterfaceState = UIState.preExperimentTrial;
	private String mUserDominantHand = ProjectConfig.rightHand;

	private HandlerThread mWorkerThread;
	private Handler mWorkerThreadHandler;

	private TaskRunnerAndDisplayProgressDialogAsyncTask asyncTaskStartedViaBroadcast = null;

	private long startingTimestampInMillis = 0;
	
	private String[] cachedChars = null;
	private Intent startBTClientServiceIntent = null;

	private boolean isExperimentOver = false;

	private CharBoxesLayout expLayoutSetting = CharBoxesLayout.SeparateChars;

	private int additionalSetIndex = 0;
	private String[] additionalSetName = null;


	private void initThreadAndHandler() {
		mWorkerThread = new HandlerThread("workerThreadForExperimentAct");
		mWorkerThread.start();
		mWorkerThreadHandler = new Handler(mWorkerThread.getLooper());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(debug_tag, "Experiment started");
		
//		Calendar calendar = Calendar.getInstance();
//		calendar.set(2015, Calendar.JANUARY, 1);
//		startingTimestampInMillis = calendar.getTimeInMillis();

		startingTimestampInMillis = System.currentTimeMillis();

//		SharedPreferences preferences = getSharedPreferences(ProjectConfig.Key_Preference_ExperimentSetting, Context.MODE_PRIVATE);
//		if(preferences != null) {
//			//if use the key cannot find the value, it would be set to a default value.
//			expSetting = preferences.getString(ProjectConfig.Key_Preference_TestingLayout, ProjectConfig.SeparateChars);
//		}
//
//		if(expSetting.equals(ProjectConfig.OneLine)) {
//			numCharBoxesInCol = 1;
//			numWritableCharBoxCols = 1;
//		}
//		else { //default Separate Chars
//			numCharBoxesInCol = 5;
//			numWritableCharBoxCols = 1;
//		}

		numCharBoxesInCol = 5;
		numWritableCharBoxCols = 1;

		numCharBoxesInAPage = numCharBoxesInCol * numWritableCharBoxCols;
		oneLineSurfaceViewIndex = numCharBoxesInAPage;

		viewModelMap = new HashMap<SpenSurfaceView, SpenPageDoc>(numCharBoxesInAPage);
		mSpenPageDocs = new SpenPageDoc[numWritableCharBoxCols][numCharBoxesInCol];
		mExampleCharsTextView = new TextView[numWritableCharBoxCols][numCharBoxesInCol];
		mWritableCharBoxContainers = new RelativeLayout[numWritableCharBoxCols][numCharBoxesInCol];
		mCharBoxes = new SpenSurfaceView[numWritableCharBoxCols][numCharBoxesInCol];
		
		mExCharNames = new String[numCharBoxesInAPage];
		for(int i = 0;i < numCharBoxesInAPage;i++) {
			mExCharNames[i] = "ExChar" + (i + 1);
		}
		
		mWritableCharBoxNames = new String[numCharBoxesInAPage];
		for(int i = 0;i < numCharBoxesInAPage;i++) {
			mWritableCharBoxNames[i] = "WritableChar" + (i + 1);
		}
		
		mContext = this;
		uiThreadHandler = new Handler(getMainLooper());

		packageName = getPackageName();
		mRes = getResources();

		mLBCManager = LocalBroadcastManager.getInstance(mContext);
		IntentFilter filter = new IntentFilter(Action_update_chars);
		filter.addAction(BluetoothClientConnectService.Msg_update_info);
		filter.addAction(TaskRunnerAndDisplayProgressDialogAsyncTask.startAsyncTask);
		filter.addAction(TaskRunnerAndDisplayProgressDialogAsyncTask.stopAsyncTask);
		mLBCManager.registerReceiver(broadcastReceiver, filter);

		additionalSetName = ProjectConfig.getAdditionalSetName();

		initThreadAndHandler();

		/* preOrPostExperimentView */
		mInflater = LayoutInflater.from(mContext);

		mPreOrPostExperimentView = mInflater.inflate(R.layout.activity_start_and_end, null);
		mPreOrPostInfo = (TextView)mPreOrPostExperimentView.findViewById(R.id.text_pre_or_post_info);
		mPreOrPostNextPageButton = (Button)mPreOrPostExperimentView.findViewById(R.id.button_experiment_next_step);
		mPreOrPostNextPageButton.setOnClickListener(mBtnOnClickListener);
		mBTStateInPreAndPostView = (TextView)mPreOrPostExperimentView.findViewById(R.id.text_bt_state);
		mBTStateInPreAndPostView.setText("未連接");

		preOrPostInterfaceState = UIState.preExperimentTrial;

		mPreOrPostNextPageButton.setClickable(false); //wait for other UI

		//this function call contain the execution of setContentView
		//so it should be called before the async task which contains the display of a progress dialog
		showPreExperimentView(mGrade);

		/* preOrPostExperimentView */

		//try to make UX better, I decide to put some task in background 
		//and use progress dialog to make user feel this app faster than before or at least knowing the state of current app.
		(new TaskRunnerAndDisplayProgressDialogAsyncTask(
				mContext,
				new BeforeViewShownTask(mContext),
				null,
				"讀取中...",
				"下一個畫面需要些讀取時間,請稍候")).execute();
	}

	private View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
		private void setSPenToolActionWithAllCanvases(int toolAction) {

			for(int i = 0;i < numWritableCharBoxCols;i++) { 
				for(int j = 0;j < numCharBoxesInCol;j++) {
					if(mCharBoxes[i][j].isActivated()) {
						mCharBoxes[i][j].setToolTypeAction(SpenSurfaceView.TOOL_SPEN, toolAction);
					}
				}
			}
			return;
		}

		@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub

				int id = view.getId();
				isToCleanMode = false;
				if(id == R.id.penBtn) {
					setSPenToolActionWithAllCanvases(SpenSurfaceView.ACTION_STROKE);
					selectButton(mPenBtn);
				}
				else if(id == R.id.eraserBtn) {
					setSPenToolActionWithAllCanvases(SpenSurfaceView.ACTION_ERASER);
					selectButton(mEraserBtn);
				}
				else if(id == R.id.cleanBtn) {
					isToCleanMode = true;
					setSPenToolActionWithAllCanvases(SpenSurfaceView.ACTION_NONE);
					selectButton(mCleanBtn);
				}
				else if(id == R.id.button_experiment_next_step) {
					if(preOrPostInterfaceState == UIState.preExperimentTrial) {
						if(isExperimentOver) {
							//the test should be over now.
							return;
						}

						setContentView(mExperimentView); //switch to experiment view
						
						preOrPostInterfaceState = UIState.postExperimentTrial;
					}
					else if(preOrPostInterfaceState == UIState.postExperimentTrial) {
						showPreExperimentView(mGrade);
//						if(btClientService != null) {
//							btClientService.setStoringDataEnabled(false);
//						}
						preOrPostInterfaceState = UIState.preExperimentTrial;
					}
				}
				else if(id == R.id.nextPageBtn) { //button on experiment view
					nextPage();
				}

			}
	};

	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals(Action_update_chars)) {
				updateExChars(intent.getStringArrayExtra(Key_ExChars));
			}
			else if(action.equals(BluetoothClientConnectService.Msg_update_info)) {
				Bundle bundle = intent.getBundleExtra(BluetoothClientConnectService.MsgBundleKey);
				if(bundle.getString(BluetoothClientConnectService.Key_Info_identifier).equals(BluetoothClientConnectService.Info_dataReceivingConnection)) {
					String msgToShow = bundle.getString(BluetoothClientConnectService.Key_Info_content);
					//Toast.makeText(mContext, "藍芽：" + msgToShow, Toast.LENGTH_LONG).show();
					mBTState.setText(msgToShow);
					mBTStateInPreAndPostView.setText(msgToShow);
				}
			}
			else if(action.equals(TaskRunnerAndDisplayProgressDialogAsyncTask.startAsyncTask)) {
				if(asyncTaskStartedViaBroadcast == null) {
					asyncTaskStartedViaBroadcast = new TaskRunnerAndDisplayProgressDialogAsyncTask(
							mContext, 
							null, 
							null, 
							intent.getStringExtra(TaskRunnerAndDisplayProgressDialogAsyncTask.Key_title),
							intent.getStringExtra(TaskRunnerAndDisplayProgressDialogAsyncTask.Key_msg));
					asyncTaskStartedViaBroadcast.execute();
				}
			}
			else if(action.equals(TaskRunnerAndDisplayProgressDialogAsyncTask.stopAsyncTask)) {
				if(asyncTaskStartedViaBroadcast != null) {
					asyncTaskStartedViaBroadcast.cancel(true);
					asyncTaskStartedViaBroadcast = null;
				}
			}

		}
	};

	private class CustomizedSpenTouchListener implements SpenTouchListener{
		private final static char delimiter = ',';
		private int mCharboxIndex = -1;
		//private SpenSurfaceView mSurfaceView = null;
		private StringBuffer stringBuffer = new StringBuffer();
		public CustomizedSpenTouchListener(int charboxIndex, SpenSurfaceView surfaceView) {
			// TODO Auto-generated constructor stub
			mCharboxIndex = charboxIndex;
			//mSurfaceView = surfaceView;
		}

		@Override
			public boolean onTouch(View view, MotionEvent event) {
				// TODO Auto-generated method stub
				if(event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS && mPenBtn.isSelected()) {
					//Log.d(debug_tag, event.getPressure() + "," + event.getX() + "," + event.getY() + "," + (System.currentTimeMillis() - fixedDateInMillis));
					//mPenTipInfo.setText("it's pen");
					stringBuffer.setLength(0); //clean buffer
					stringBuffer.append(ProjectConfig.getTimestamp(startingTimestampInMillis));
					stringBuffer.append(delimiter);
					stringBuffer.append(event.getX());
					stringBuffer.append(delimiter);
					stringBuffer.append(event.getY());
					stringBuffer.append(delimiter);
					stringBuffer.append(event.getPressure());
					//stringBuffer.append(Float.toString(event.getPressure()));
					//stringBuffer.append(String.format("%s",event.getPressure()));
					//stringBuffer.append(BigDecimal.valueOf(event.getPressure()).toString());
					txtFileManager.appendLogWithNewlineSync(mCharboxIndex, stringBuffer.toString());
				}
				/*
				else {
					mPenTipInfo.setText("it's finger");
				}
				*/

				return false;
			}

	}

//	private boolean isLoadingChars = false;
//	private void loadExCharsFromFile(int grade) {
//		if(!isLoadingChars) {
//			cachedChars = null;
//			isLoadingChars = true;
//			txtFileManager.toLoadChineseCharsAsync(grade);
//		}
//	}

	private void resetExCharsAndCharBoxes() {
		for(int i = 0;i < numWritableCharBoxCols;i++) {
			for(int j = 0;j < numCharBoxesInCol;j++) {
				mExampleCharsTextView[i][j].setText(null);
				if(mCharBoxes[i][j] != null) {
					cleanSurfacView(mCharBoxes[i][j]);
				}
			}
		}
	}
	
	public void updateExChars(String[] exChars) {
		String[] charsUsedForUpdate = null;

//		if(cachedChars != null) {
//			isLoadingChars = false;
//		}

		if(exChars == null) {
			if(cachedChars != null) {
				charsUsedForUpdate = cachedChars;
			}
			else {
				Log.d(DEBUG_TAG, "no available chars, updating exChars failed");
				//loadExCharsFromFile(mGrade);
				return;
			}
		}
		else {
			cachedChars = exChars;
			charsUsedForUpdate = exChars;
		}

		setNextPageButtonClickable(false);
		resetExCharsAndCharBoxes();
		for(int i = 0;i < numWritableCharBoxCols;i++) {
			for(int j = 0;j < numCharBoxesInCol;j++) {
				int charIndexToRetrieve = charIndex + i * numCharBoxesInCol + j;
				if(charIndexToRetrieve < charsUsedForUpdate.length) {
					mExampleCharsTextView[i][j].setText(charsUsedForUpdate[charIndexToRetrieve]);
				}
			}
		}
		setNextPageButtonClickable(true);
		return;
	}

	private void nextPage() { //next page during experiment
		if(cachedChars == null) { //it should not happen
			Toast.makeText(mContext, "換頁發生錯誤，請確認範例文字檔案已被正確讀取", Toast.LENGTH_LONG);
			return;
		}

		//save images
		int numImagesToSave = cachedChars.length - charIndex;
		if(numImagesToSave > numCharBoxesInAPage) {
			numImagesToSave = numCharBoxesInAPage;
		}
		int numImagesSaved = 0;
		for(int i = 0;i < numWritableCharBoxCols;i++) {
			for(int j = 0;j < numCharBoxesInCol && numImagesSaved < numImagesToSave;j++) {
				captureSpenSurfaceView(i, j, ProjectConfig.getImgFileName(mUserID, mGrade, charIndex + numImagesSaved));
				numImagesSaved++;
			}
		}

		//close no-needed files here
		int numFilesToClose = cachedChars.length - charIndex;
		if(numFilesToClose > numCharBoxesInAPage) {
			numFilesToClose = numCharBoxesInAPage;
		}
		for(int i = 0;i < numFilesToClose;i++) {
			txtFileManager.closeFile(i);
		}
		
		charIndex = charIndex + numCharBoxesInAPage;
		if(charIndex >= cachedChars.length) {
			//one session has been done, transit to postExperimentView
			showPostExperimentView();
		}
		else {
			//change for next col of characters
			int numLogsToLoadNow = cachedChars.length - charIndex;
			if(numLogsToLoadNow > numCharBoxesInAPage) {
				numLogsToLoadNow = numCharBoxesInAPage;
			}
			
			if(txtFileManager.rearrangeIndices(
					Pair.create(Integer.valueOf(0), Integer.valueOf(numLogsToLoadNow)), 
					Pair.create(Integer.valueOf(numCharBoxesInAPage), Integer.valueOf(numCharBoxesInAPage + numLogsToLoadNow))
					)) {
				//successfully use previous cached logs
				//preload next logs set
				int nextCharHeadIndex = charIndex + numCharBoxesInAPage;
				if(nextCharHeadIndex < cachedChars.length) {
					mWorkerThreadHandler.post(new CreateOrOpenLogFileTask(nextCharHeadIndex, numCharBoxesInAPage, cachedChars.length - nextCharHeadIndex));
				}
			}
			else {
				//failed to use previous cached logs, then load logs and show progress in foreground
				loadLogsAndShowProgressDialog(0, numLogsToLoadNow);
			}
		}
	}

	private void prepareForNextTestingSet() {
		cachedChars = null;
		charIndex = 0;
		Runnable loadCharTask = null;
		int numLogsToCreateOrOpen = 0;

		if(testingSetIndex < mUserGrade) {
			mGrade = testingSetGradesSeq.get(testingSetIndex);
			loadCharTask = txtFileManager.getLoadChineseCharTask(mGrade);
			numLogsToCreateOrOpen = numCharBoxesInAPage * 2;
			testingSetIndex++;
		}
		else if(additionalSetName != null && additionalSetIndex < additionalSetName.length) {
			loadCharTask = txtFileManager.getLoadCharTask(additionalSetName[additionalSetIndex]);
			numLogsToCreateOrOpen = numCharBoxesInAPage * 2;
			additionalSetIndex++;
		}
		else {
			return;
		}

		mWorkerThreadHandler.post(loadCharTask);
		mWorkerThreadHandler.post(new CreateOrOpenLogFileTask(charIndex, 0, numLogsToCreateOrOpen));
	}
	
	private void loadLogsAndShowProgressDialog(int charBoxIndex, int numLogsToLoad) {
		(new TaskRunnerAndDisplayProgressDialogAsyncTask(
				 mContext, 
				 new CreateOrOpenLogFileTask(charIndex, charBoxIndex, numLogsToLoad), 
				 null,
				 "開啟Log檔案中",
				 "請稍候")).execute();
		return;
	}

	private class CreateOrOpenLogFileTask implements Runnable {

		//charIndex is the order of certain char in Example Chars file
		//charBoxIndex is the order of certain char on the experiment view
		int mCharIndex;
		int mCharBoxIndex;
		int mNumFilesToCreateOrOpen;

		public CreateOrOpenLogFileTask(int charIndex, int charBoxIndex, int numFilesToCreateOrOpen) {
			// TODO Auto-generated constructor stub
			mCharIndex = charIndex;
			mCharBoxIndex = charBoxIndex;
			mNumFilesToCreateOrOpen = numFilesToCreateOrOpen;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			int numLogsToLoad = mNumFilesToCreateOrOpen;
			if(cachedChars != null) {
				if(cachedChars.length - mCharIndex < numLogsToLoad) {
					numLogsToLoad = cachedChars.length - mCharIndex;
				}
			}
			for(int i = 0;i < numLogsToLoad;i++) {
				txtFileManager.createOrOpenLogFileSync(
						ProjectConfig.getTipForceLogFileName(mUserID, mGrade, mCharIndex + i), 
						mCharBoxIndex + i);
			}
		}

	}

	private void showPostExperimentView() {
		mPreOrPostInfo.setText("恭喜你完成了，\n請按下一頁");
		setContentView(mPreOrPostExperimentView);
		prepareForNextTestingSet();
	}

	private void showPreExperimentView(int grade) {
		if(!isExperimentOver) {
			mPreOrPostInfo.setText("即將開始評量，\n準備好後請按下一步");
		}
		else {
			mPreOrPostNextPageButton.setVisibility(View.GONE);
			mPreOrPostInfo.setText("評量到此結束，感謝你的參與");
			endTheAppAfterCertainDuration(5000);
		}
		setContentView(mPreOrPostExperimentView);
	}

	private BluetoothClientConnectService btClientService = null;
	
	private ServiceConnection serviceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			// TODO Auto-generated method stub
			btClientService = null;
			Log.d(debug_tag, "btService disconnected");
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			// TODO Auto-generated method stub
			LocalBinder binder = (LocalBinder)service;
			btClientService = binder.getService();
			btClientService.setStoringDataEnabled(true);
			btClientService.setStartingTimestamp(startingTimestampInMillis);
			Log.d(debug_tag, "btService connected");
		}
	};

	private final int charBoxHeight = inchToPixels(ProjectConfig.inchPerCM * 2.1f);
	private final int charBoxOneLineHeight = inchToPixels(ProjectConfig.inchPerCM * 16.1f);
	private final int charBoxWidth = charBoxHeight;

	private void changeCharBoxesLayout(CharBoxesLayout layoutSetting) {
		expLayoutSetting = layoutSetting;
		//ViewGroup.LayoutParams writableCharBox_LP = mWritableCharBoxContainers[0][0].getLayoutParams();

		if(expLayoutSetting == CharBoxesLayout.SeparateChars) {
			otherUIContainer.setVisibility(View.GONE);
			charGroupsContainer.setVisibility(View.VISIBLE);
		}
		else if(expLayoutSetting == CharBoxesLayout.OneLine) {
			otherUIContainer.setVisibility(View.VISIBLE);
			charGroupsContainer.setVisibility(View.GONE);
		}
	}

	private class BeforeViewShownTask implements Runnable {

		private Context mContext;
		private LayoutInflater inflater;

		public BeforeViewShownTask(Context context) {
			// TODO Auto-generated constructor stub
			mContext = context;
			inflater = LayoutInflater.from(mContext);
		}
		
		private void charboxesLayoutArrangement () {

			otherUIContainer = new LinearLayout(mContext);
			mOneLineContainer = new RelativeLayout(mContext);
			mExampleCharsGroupBG = new ImageView(mContext);

			charGroupsContainer = new LinearLayout(mContext);
			charGroupsContainer.setOrientation(LinearLayout.VERTICAL);

			RelativeLayout.LayoutParams charGroupsContainerLayoutParams = new RelativeLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,  //width
					LinearLayout.LayoutParams.WRAP_CONTENT); //height
			charGroupsContainerLayoutParams.topMargin = 100;

			if(mUserDominantHand.equals(ProjectConfig.leftHand)) {
				charGroupsContainerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				charGroupsContainerLayoutParams.rightMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}
			else {
				charGroupsContainerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				charGroupsContainerLayoutParams.leftMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}

			charGroupsContainer.setLayoutParams(charGroupsContainerLayoutParams);
			RelativeLayout subViewsContainer = (RelativeLayout)mExperimentView.findViewById(R.id.spenViewContainer);
			subViewsContainer.addView(otherUIContainer);
			subViewsContainer.addView(charGroupsContainer);

			RelativeLayout.LayoutParams writableCharBox_LP =
					new RelativeLayout.LayoutParams(
							charBoxWidth,
							charBoxHeight
					);

			LinearLayout.LayoutParams exampleCharBox_LP =
					new LinearLayout.LayoutParams(
							charBoxWidth,
							charBoxHeight
					);

			int charBoxMarginLeftOrRight = (int)mRes.getDimension(R.dimen.char_box_left_or_right_margin);
			int charBoxMarginBottom = (int)mRes.getDimension(R.dimen.char_box_bottom_margin);
			
			LinearLayout.LayoutParams charGroupLayoutParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);

			LinearLayout[] charGroups = new LinearLayout[numCharBoxesInAPage];
			Typeface font = ProjectConfig.getDefaultFont();

			exampleCharBox_LP.bottomMargin = charBoxMarginBottom;

			for(int i = 0;i < numWritableCharBoxCols;i++) {
				for(int j = 0;j < numCharBoxesInCol;j++) {

					int charBoxIndex = i * numCharBoxesInCol + j;
					charGroups[charBoxIndex] = new LinearLayout(mContext);
					charGroups[charBoxIndex].setLayoutParams(charGroupLayoutParams);
					charGroups[charBoxIndex].setOrientation(LinearLayout.HORIZONTAL);
					charGroupsContainer.addView(charGroups[charBoxIndex]);
					
					TextView txtView = new TextView(mContext);
					txtView.setTag(mExCharNames[charBoxIndex]);
					txtView.setTextSize((int) mRes.getDimension(R.dimen.exChars_textSize));
					if(font != null) {
						txtView.setTypeface(font);
					}
					txtView.setGravity(Gravity.CENTER);
					txtView.setTextColor(Color.BLACK);
					txtView.setBackgroundColor(Color.WHITE);
					mExampleCharsTextView[i][j] = txtView;
					
					RelativeLayout writableCharBox = new RelativeLayout(mContext);
					writableCharBox.setTag(mWritableCharBoxNames[charBoxIndex]);
					writableCharBox.setLayoutParams(writableCharBox_LP);
					mWritableCharBoxContainers[i][j] = writableCharBox;


					if(mUserDominantHand.equals(ProjectConfig.leftHand)) {
						exampleCharBox_LP.leftMargin = charBoxMarginLeftOrRight;
						txtView.setLayoutParams(exampleCharBox_LP);
						charGroups[charBoxIndex].addView(writableCharBox);
						charGroups[charBoxIndex].addView(txtView);
						
					}
					else {
						exampleCharBox_LP.rightMargin = charBoxMarginLeftOrRight;
						txtView.setLayoutParams(exampleCharBox_LP);
						charGroups[charBoxIndex].addView(txtView);
						charGroups[charBoxIndex].addView(writableCharBox);
						
					}

				}
			}

			LinearLayout.LayoutParams imgRLP = new LinearLayout.LayoutParams(
					charBoxWidth,
					charBoxOneLineHeight
			);

			LinearLayout.LayoutParams oneLine_LP = new LinearLayout.LayoutParams(
					charBoxWidth,
					charBoxOneLineHeight
			);

			otherUIContainer.setLayoutParams(charGroupsContainerLayoutParams);
			otherUIContainer.setOrientation(LinearLayout.HORIZONTAL);

			if(mUserDominantHand.equals(ProjectConfig.leftHand)) {
				imgRLP.leftMargin = charBoxMarginLeftOrRight;
				//otherUI_LP.rightMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}
			else {
				imgRLP.rightMargin = charBoxMarginLeftOrRight;
				//otherUI_LP.leftMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}


			mExampleCharsGroupBG.setLayoutParams(imgRLP);
			mExampleCharsGroupBG.setBackgroundColor(Color.WHITE);

			mOneLineContainer.setLayoutParams(oneLine_LP);
			mOneLineContainer.setBackgroundColor(Color.WHITE);

			if(mUserDominantHand.equals(ProjectConfig.leftHand)) {
				otherUIContainer.addView(mOneLineContainer);
				otherUIContainer.addView(mExampleCharsGroupBG);
				//otherUI_LP.rightMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}
			else {
				otherUIContainer.addView(mExampleCharsGroupBG);
				otherUIContainer.addView(mOneLineContainer);
				//otherUI_LP.leftMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}

			
		}
		
		@Override
		public void run() {
				// TODO Auto-generated method stub
				/* ExperimentView */
//				final String dirPath = mContext.getFilesDir().getPath();
//				Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.charbox_grid);

				SharedPreferences userInfoPreference = mContext.getSharedPreferences(ProjectConfig.Key_Preference_UserInfo, Context.MODE_PRIVATE);
				
				if(userInfoPreference != null) {
					mUserGrade = (int)userInfoPreference.getLong(ProjectConfig.Key_Preference_UserGrade, 1);
					mUserDominantHand = userInfoPreference.getString(ProjectConfig.Key_Preference_UserDominantHand, ProjectConfig.rightHand);
					mUserID = userInfoPreference.getString(ProjectConfig.Key_Preference_UserID, ProjectConfig.defaultUserID);
					startBTClientServiceIntent = new Intent(mContext, BluetoothClientConnectService.class);
					startBTClientServiceIntent.setAction(BluetoothClientConnectService.Action_start_receiving_data);
					startBTClientServiceIntent.putExtra(BluetoothClientConnectService.DeviceAddrKey, userInfoPreference.getString(ProjectConfig.Key_Preference_CurrentSelectedBTAddress, null));
					Log.d(debug_tag,"bt addr:" + userInfoPreference.getString(ProjectConfig.Key_Preference_CurrentSelectedBTAddress, null));
					bindService(startBTClientServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
				}

				/* Experiment View */
				mExperimentView = inflater.inflate(R.layout.activity_experiment_3, null);

				FileDirInfo dirInfo = new FileDirInfo(FileType.GeneratingImage, ProjectConfig.getDirpathByID(mUserID), null);
				imgFileManager = new ImgFileManager(dirInfo, mContext);
				dirInfo.setFileType(FileType.Log, false);
				//the rest of space is used for preloading
				dirInfo.setOtherInfo(String.valueOf(numCharBoxesInCol * numWritableCharBoxCols * 2));
				txtFileManager = new TxtFileManager(dirInfo, mContext);

				mPenTipInfo = (TextView)mExperimentView.findViewById(R.id.penTipInfo);

				// Set a button
				mPenBtn = (ImageView) mExperimentView.findViewById(R.id.penBtn);
				mPenBtn.setOnClickListener(mBtnOnClickListener);

				mEraserBtn = (ImageView) mExperimentView.findViewById(R.id.eraserBtn);
				mEraserBtn.setOnClickListener(mBtnOnClickListener);

				mCleanBtn = (ImageView) mExperimentView.findViewById(R.id.cleanBtn);
				mCleanBtn.setOnClickListener(mBtnOnClickListener);

				mNextPageBtn = (RelativeLayout) mExperimentView.findViewById(R.id.nextPageBtn);
				mNextPageBtn.setOnClickListener(mBtnOnClickListener);
				
				charboxesLayoutArrangement();
				
				selectButton(mPenBtn);

				mBTState = (TextView)mExperimentView.findViewById(R.id.text_bt_state);
				mBTState.setText("未連接");
				
				/* ExperimentView */

				testingSetGradesSeq = ProjectConfig.getTestingGradeSequence(mUserGrade);
				testingSetIndex = 0;

				prepareForNextTestingSet();

				//do SpenSetUp-Related Work at last
				/* Spen Dependent Part Start */

				// Initialize Spen
				boolean isSpenFeatureEnabled = false;
				Spen spenPackage = new Spen();
				try {
					spenPackage.initialize(mContext);
					isSpenFeatureEnabled = spenPackage.isFeatureEnabled(Spen.DEVICE_PEN);
				} catch (SsdkUnsupportedException e) {
					if (SDKUtils.processUnsupportedException(ExperimentActivity.this, e) == true) {
						return;
					}
				} catch (Exception e1) {
					Toast.makeText(mContext, "Cannot initialize Spen.", Toast.LENGTH_SHORT).show();
					e1.printStackTrace();
					finish();
				}
				initSettingInfo2();

				// Get the dimension of the device screen.
				Display display = getWindowManager().getDefaultDisplay();
				Rect rect = new Rect();
				display.getRectSize(rect);
				// Create SpenNoteDoc
				try {
					mSpenNoteDoc = new SpenNoteDoc(mContext, rect.width(), rect.height());
				} catch (IOException e) {
					Toast.makeText(mContext, "Cannot create new NoteDoc", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
					finish();
				} catch (Exception e) {
					e.printStackTrace();
					finish();
				}
				
				//String imgFileName = dirPath + "/charbox_bg.png";
				//saveBitmapToFileCache(bitmap, imgFileName);
		        String imgFileName = null;
				
				for(int i = 0;i < numWritableCharBoxCols;i++) {
					for(int j = 0;j < numCharBoxesInCol;j++) {
						// Add a Page to NoteDoc, get an instance, and set it to the member variable.

						if(imgFileName != null) {
							//mSpenPageDocs[i][j] = mSpenNoteDoc.insertPage(i * numCharBoxesInCol + j, 0, imgFileName, SpenPageDoc.BACKGROUND_IMAGE_MODE_FIT);
							mSpenPageDocs[i][j] = mSpenNoteDoc.insertPage(i * numCharBoxesInCol + j);
							mSpenPageDocs[i][j].setBackgroundImage(imgFileName);
							mSpenPageDocs[i][j].setBackgroundImageMode(SpenPageDoc.BACKGROUND_IMAGE_MODE_FIT);	
						}
						else {
							mSpenPageDocs[i][j] = mSpenNoteDoc.insertPage(i * numCharBoxesInCol + j);
							mSpenPageDocs[i][j].setBackgroundColor(Color.WHITE);
						}
						mSpenPageDocs[i][j].clearHistory();

					}
				}

				mOneLinePageDoc = mSpenNoteDoc.insertPage(oneLineSurfaceViewIndex);
				mOneLinePageDoc.setBackgroundColor(Color.WHITE);

				uiThreadHandler.post(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						//this part need to be run inside main thread otherwise it would crash.
						//we still need around 2 sec to run this part of code
						for (int i = 0; i < numWritableCharBoxCols; i++) {
							for (int j = 0; j < numCharBoxesInCol; j++) {
								mCharBoxes[i][j] = new SpenSurfaceView(mContext);
								SpenSurfaceView surfaceView = mCharBoxes[i][j];

								//surfaceView.setClickable(true);
								//to disable hover effect, just disable the hover effect in the system setting
								surfaceView.setTouchListener(new CustomizedSpenTouchListener(i * numCharBoxesInCol + j, surfaceView));
								surfaceView.setLongPressListener(new customizedLongPressedListener(surfaceView, i * numCharBoxesInCol + j));
								surfaceView.setZoomable(false);

								//currently we disable finger's function. Maybe we could use it as eraser in the future.
								surfaceView.setToolTypeAction(SpenSurfaceView.TOOL_FINGER, SpenSurfaceView.ACTION_NONE);
								surfaceView.setToolTypeAction(SpenSurfaceView.TOOL_SPEN, SpenSurfaceView.ACTION_STROKE);
								surfaceView.setPenSettingInfo(penInfo);
								surfaceView.setEraserSettingInfo(eraserInfo);
								((RelativeLayout) mExperimentView.findViewWithTag(mWritableCharBoxNames[i * numCharBoxesInCol + j])).addView(surfaceView);

								surfaceView.setPageDoc(mSpenPageDocs[i][j], true);
								viewModelMap.put(surfaceView, mSpenPageDocs[i][j]);

							}
						}

						mOneLine = new SpenSurfaceView(mContext);
						mOneLine.setTouchListener(new CustomizedSpenTouchListener(oneLineSurfaceViewIndex, mOneLine));
						mOneLine.setLongPressListener(new customizedLongPressedListener(mOneLine, oneLineSurfaceViewIndex));
						mOneLine.setZoomable(false);

						mOneLine.setToolTypeAction(SpenSurfaceView.TOOL_FINGER, SpenSurfaceView.ACTION_NONE);
						mOneLine.setToolTypeAction(SpenSurfaceView.TOOL_SPEN, SpenSurfaceView.ACTION_STROKE);
						mOneLine.setPenSettingInfo(penInfo);
						mOneLine.setEraserSettingInfo(eraserInfo);
						mOneLineContainer.addView(mOneLine);

						mOneLine.setPageDoc(mOneLinePageDoc, true);
						viewModelMap.put(mOneLine, mOneLinePageDoc);

					}
				});

				if (isSpenFeatureEnabled == false) {
					mToolType = SpenSurfaceView.TOOL_FINGER;
					Toast.makeText(mContext, "Device does not support Spen. \n You can draw stroke by finger",
							Toast.LENGTH_SHORT).show();
				}

				/* Spen Dependent Part End */

				hideSystemBar();
				mPreOrPostNextPageButton.setClickable(true);

		}

	}

	private void captureExampleTextView(int row, int col, String fileName) {

	}

	private void captureSpenSurfaceView(int row, int col, String fileName) {	
		Bitmap imgBitmap = mCharBoxes[row][col].captureCurrentView(true);
		imgFileManager.saveBMP(imgBitmap, fileName);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if(btClientService != null) {
			btClientService.setStoringDataEnabled(false);
			btClientService.stopSelf();
		}
		
		if(mLBCManager != null) {
			mLBCManager.unregisterReceiver(broadcastReceiver);
		}

		if (mPenSettingView != null) {
			mPenSettingView.close();
		}
		if (mEraserSettingView != null) {
			mEraserSettingView.close();
		}

		if (mSpenNoteDoc != null) {
			try {
				mSpenNoteDoc.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			mSpenNoteDoc = null;
		}
	};

	private View findViewByStr(View viewToSearchIn, String name) {
		int resId = mRes.getIdentifier(name, "id", packageName);
		return viewToSearchIn.findViewById(resId);
	}

	private void initSettingInfo2() {

		// Initialize Pen settings
		penInfo = new SpenSettingPenInfo();
		penInfo.color = Color.BLACK;
		penInfo.size = 1;

		// Initialize Eraser settings
		eraserInfo = new SpenSettingEraserInfo();
		eraserInfo.size = 30;

	}

	private void cleanSurfacView(SpenSurfaceView view) {
		SpenPageDoc model = viewModelMap.get(view);
		model.removeAllObject();
		view.update();
		return;
	}
	
	private class customizedLongPressedListener implements SpenLongPressListener {

		private SpenSurfaceView bindedSurfaceView = null;
		private int mCharBoxIndex;

		public customizedLongPressedListener(SpenSurfaceView surfaceView, int charBoxIndex) {
			bindedSurfaceView = surfaceView;
			mCharBoxIndex = charBoxIndex;
		}

		private void cleanCurrentlySelectedView() {
			txtFileManager.appendLogWithNewlineSync(mCharBoxIndex, ProjectConfig.restartMark);
			SpenPageDoc model = viewModelMap.get(bindedSurfaceView);
			model.removeAllObject();
			bindedSurfaceView.update();
			return;
		}

		@Override
			public void onLongPressed(MotionEvent arg0) {
				// TODO Auto-generated method stub

				if(isToCleanMode) {

					bindedSurfaceView.setSelected(true);

					// 1. Instantiate an AlertDialog.Builder with its constructor
					AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

					// 2. Chain together various setter methods to set the dialog characteristics
					builder.setMessage("你即將清除手寫的筆跡，確定嗎？");

					// Add the buttons
					builder.setPositiveButton("確定", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							// User clicked OK button
							cleanCurrentlySelectedView();
						}
					});

					builder.setNegativeButton("取消", null);

					// 3. Get the AlertDialog from create()
					(builder.create()).show();

				}

			}

	}

	private void selectButton(View v) {
		// Enable or disable the button according to the current mode.
		mPenBtn.setSelected(false);
		mEraserBtn.setSelected(false);
		mCleanBtn.setSelected(false);

		v.setSelected(true);
	}

	private void setBtnEnabled(boolean clickable) {
		// Enable or disable all the buttons.
		mPenBtn.setEnabled(clickable);
		mEraserBtn.setEnabled(clickable);
	}

	private void setNextPageButtonClickable(boolean enable) {
		mNextPageBtn.setClickable(enable);
	}
	
	
}
