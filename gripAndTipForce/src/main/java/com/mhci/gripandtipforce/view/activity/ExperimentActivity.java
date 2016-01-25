package com.mhci.gripandtipforce.view.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
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

import com.mhci.gripandtipforce.model.BTEvent;
import com.mhci.gripandtipforce.model.utils.BluetoothManager;
import com.mhci.gripandtipforce.view.CharBoxesLayout;
import com.mhci.gripandtipforce.model.FileDirInfo;
import com.mhci.gripandtipforce.model.FileType;
import com.mhci.gripandtipforce.model.utils.ImgFileManager;
import com.mhci.gripandtipforce.R;
import com.mhci.gripandtipforce.view.TaskRunnerAndDisplayProgressDialogAsyncTask;
import com.mhci.gripandtipforce.model.utils.TxtFileManager;
import com.mhci.gripandtipforce.model.UIState;
import com.mhci.gripandtipforce.model.ProjectConfig;
import com.mhci.gripandtipforce.model.utils.Utils;
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

import app.akexorcist.bluetotohspp.library.BluetoothState;
import de.greenrobot.event.EventBus;


public class ExperimentActivity extends CustomizedBaseFragmentActivity {

	public final static String debug_tag = "Experiment";
	
	public final static String Action_update_chars = ExperimentActivity.class.getName() + ".update_chars";

	public final static String Key_ExChars = "ExChars";

	private final static String DEBUG_TAG = ExperimentActivity.class.getName();

	/* View */
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
	private ImageView mExampleCharsGroup = null;
	private SpenSettingPenLayout mPenSettingView;
	private SpenSettingEraserLayout mEraserSettingView;

	private ImgFileManager genImgFileManager = null;
	private ImgFileManager templateImgFileManager = null;
	private TxtFileManager txtFileManager = null;

	private Context mContext = null;
	private LayoutInflater mInflater = null;
	private Resources mRes;
	private String packageName;
	private int mToolType = SpenSurfaceView.TOOL_SPEN;
	private int numCharBoxesInCol = 5;
	private int numWritableCharBoxCols = 1;
	private int numCharBoxesInAPage = numCharBoxesInCol * numWritableCharBoxCols;
	private boolean isToCleanMode = false;
	private String[] mExCharNames;
	private String[] mWritableCharBoxNames;
	private SpenSettingPenInfo penInfo;
	private SpenSettingEraserInfo eraserInfo;
	private SpenNoteDoc mSpenNoteDoc;
	private SpenPageDoc[][] mSpenPageDocs = null;
	private HashMap<SpenSurfaceView, SpenPageDoc> viewModelMap = null;
	private LocalBroadcastManager mLBCManager = null;
	private Handler uiThreadHandler = null;
	private int oneLineSurfaceViewIndex = 1000;
	private int charIndex = 0;
	private int templateIndex = 0;
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
	private String[] cachedChars = null;
	private boolean isExperimentOver = false;
	private boolean isOneLineSessionOver = false;
	private boolean toChangeExperimentLayout = false;
	private CharBoxesLayout layoutChangeTo = CharBoxesLayout.SeparateChars;
	private CharBoxesLayout expLayoutSetting = CharBoxesLayout.SeparateChars;
	private int additionalSetIndex = 0;
	private String[] additionalSetName = null;
    private Canvas bmpCanvas;
	private BluetoothManager mBTManager;
	private EventBus eventBus;
	private List<String>[] tipForceData;
	private final int firstCharBoxIndex = 0;
	private String currentDataSetName = null;
	private IntentFilter filter;

//	private final static int mToolType = SpenSurfaceView.TOOL_SPEN;

	//private int reqWidthInPx = 50;
	//private int reqHeightInPx = 100;

	private void initThreadAndHandler() {
		mWorkerThread = new HandlerThread(ExperimentActivity.class.getName() + System.currentTimeMillis());
		mWorkerThread.start();
		mWorkerThreadHandler = new Handler(mWorkerThread.getLooper());
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(debug_tag, "Experiment started");
		mContext = this;
		numCharBoxesInCol = 5;
		numWritableCharBoxCols = 1;
		numCharBoxesInAPage = numCharBoxesInCol * numWritableCharBoxCols;
		oneLineSurfaceViewIndex = numCharBoxesInAPage;

		ProjectConfig.startTimestampInMilliSec = System.currentTimeMillis();
		eventBus = EventBus.getDefault();
		mBTManager = BluetoothManager.getInstance();
		uiThreadHandler = new Handler(getMainLooper());
		packageName = getPackageName();
		mRes = getResources();

		initThreadAndHandler();

		/* preOrPostExperimentView */

		mInflater = LayoutInflater.from(mContext);
		mPreOrPostExperimentView = mInflater.inflate(R.layout.activity_start_and_end, null);
		mPreOrPostInfo = (TextView) mPreOrPostExperimentView.findViewById(R.id.text_pre_or_post_info);
		mPreOrPostNextPageButton = (Button) mPreOrPostExperimentView.findViewById(R.id.button_experiment_next_step);
		mPreOrPostNextPageButton.setOnClickListener(mBtnOnClickListener);

		mBTStateInPreAndPostView = (TextView) mPreOrPostExperimentView.findViewById(R.id.text_bt_state);
		setBTStateText(mBTStateInPreAndPostView);

		preOrPostInterfaceState = UIState.preExperimentTrial;

		mPreOrPostNextPageButton.setClickable(false); //wait for other UI

		//this function call contain the execution of setContentView
		//so it should be called before the async task which contains the display of a progress dialog
		showPreExperimentView();

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

	//TODO: fix with EventBus
	private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(action.equals(Action_update_chars)) {
				String[] newLoadedChars = intent.getStringArrayExtra(Key_ExChars);
				if(newLoadedChars == null) {
					Toast.makeText(mContext, "無法讀取範例文字" , Toast.LENGTH_LONG).show();
				}
				else {
					updateExChars(newLoadedChars);
				}
			}
		}
	};

	private void appendTipForceLogAsync(int charBoxIndex) {
		mWorkerThreadHandler.post(txtFileManager.getAppendListLogTask(charBoxIndex,tipForceData[charBoxIndex]));
		tipForceData[charBoxIndex] = new LinkedList<>();
	}

	public void updateExChars(String[] exChars) {
		String[] charsUsedForUpdate = null;

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

        //update chars according to charIndex and charsUsedForUpdate
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
		int logIndex = 0;
		int numLogsToLoadNow = 0;
		//ArrayList<Runnable> taskList = new ArrayList<Runnable>();

		if(expLayoutSetting == CharBoxesLayout.SeparateChars) {
			if (cachedChars == null) { //it should not happen
				Toast.makeText(mContext, "換頁發生錯誤，請確認範例文字檔案已被正確讀取", Toast.LENGTH_LONG).show();
				return;
			}

			//save writing and textview images
			int numImagesToSave = cachedChars.length - charIndex;
			if (numImagesToSave > numCharBoxesInAPage) {
				numImagesToSave = numCharBoxesInAPage;
			}
			int numImagesSaved = 0;
			for (int i = 0; i < numWritableCharBoxCols; i++) {
				for (int j = 0; j < numCharBoxesInCol && numImagesSaved < numImagesToSave; j++) {
					int charBoxIndex = i * numCharBoxesInCol + j;
					if(tipForceData[charBoxIndex].size() > 0) {
						appendTipForceLogAsync(charBoxIndex);
					}

					captureSpenSurfaceView(mCharBoxes[i][j],
							               ProjectConfig.getGeneratingImgFileName(
                                                   ProjectConfig.writingImgPrefix,
                                                   mUserID,
                                                   currentDataSetName,
                                                   charIndex + numImagesSaved));
                    captureExampleTextView(mExampleCharsTextView[i][j],
                                           ProjectConfig.getGeneratingImgFileName(
                                                   ProjectConfig.textviewImgPrefix,
                                                   mUserID,
                                                   currentDataSetName,
                                                   charIndex + numImagesSaved));

					numImagesSaved++;
				}
			}

			//close no-needed files here
			int numFilesToClose = cachedChars.length - charIndex;
			if (numFilesToClose > numCharBoxesInAPage) {
				numFilesToClose = numCharBoxesInAPage;
			}
			for (int i = 0; i < numFilesToClose; i++) {
				txtFileManager.closeFile(i);
			}

			charIndex = charIndex + numCharBoxesInAPage;
			if (charIndex >= cachedChars.length) {
				//one session has been done, transit to postExperimentView
				showPostExperimentView();
				return;
			} else {
				//change for next col of characters
				logIndex = charIndex;
				numLogsToLoadNow = cachedChars.length - charIndex;
				if (numLogsToLoadNow > numCharBoxesInAPage) {
					numLogsToLoadNow = numCharBoxesInAPage;
				}

				updateExChars(null);

			}
		}
		else if(expLayoutSetting == CharBoxesLayout.OneLine) {
			if(tipForceData[0].size() > 0) {
				appendTipForceLogAsync(0);
			}
			txtFileManager.closeFile(0);

			captureSpenSurfaceView(mOneLine, ProjectConfig.getGeneratingImgFileName(
                    ProjectConfig.writingImgPrefix,
                    mUserID,
                    currentDataSetName,
                    templateIndex));

			logIndex = templateIndex;
			numLogsToLoadNow = 1;

            cleanSurfacView(mOneLine);
			loadNextImageIntoExCharGroup();

			if (isOneLineSessionOver) {
				showPostExperimentView();
				return;
			}
		}

		(new TaskRunnerAndDisplayProgressDialogAsyncTask(
				mContext,
				txtFileManager.getCreateOrOpenCharLogFileTask(
						ProjectConfig.tipForceLogPrefix,
						mUserID,
						currentDataSetName,
						cachedChars,
						logIndex,
						firstCharBoxIndex,
						numCharBoxesInAPage),
                null)).execute();

	}

	private void loadNextImageIntoExCharGroup() {
		String msg = templateImgFileManager.loadImageIntoImageViewWithSizeDependOnGradeAndIndex(
				templateIndex,
				mUserGrade,
				mExampleCharsGroup);
		if(msg.equals(ImgFileManager.endMsg) || msg.equals(ImgFileManager.errorMsg)) {
			isOneLineSessionOver = true;
		}
		templateIndex++;
	}

	private void changeExCharFont(Typeface font) {
		for(int i = 0;i < numWritableCharBoxCols;i++) {
			for(int j = 0;j < numCharBoxesInCol;j++) {
				mExampleCharsTextView[i][j].setTypeface(font);
			}
		}
	}

	private void prepareForNextTestingSet() {
		int logIndex = 0;
		cachedChars = null;
		currentDataSetName = null;

		ArrayList<Runnable> taskList = new ArrayList<Runnable>();

		if(testingSetIndex < mUserGrade) {
			charIndex = 0;
			logIndex = charIndex;
			mGrade = testingSetGradesSeq.get(testingSetIndex);
			taskList.add(txtFileManager.getLoadChineseCharTask(mGrade));
			currentDataSetName = ProjectConfig.chineseCharDatasetName(mGrade);
			testingSetIndex++;
		}
		else if(additionalSetName != null && additionalSetIndex < additionalSetName.length) {
			charIndex = 0;
			logIndex = charIndex;
			taskList.add(txtFileManager.getLoadCharTask(additionalSetName[additionalSetIndex]));
			currentDataSetName = ProjectConfig.additionalDataSetName(additionalSetIndex);
			Typeface font = ProjectConfig.getAdditionalSetFont();
			if(font != null) {
				changeExCharFont(font);
			}
			additionalSetIndex++;
		}
		else if(!isOneLineSessionOver && expLayoutSetting == CharBoxesLayout.SeparateChars){
			//prepare to transit to one line layout
			templateIndex = 0;
			logIndex = templateIndex;
			numCharBoxesInAPage = 1;
			layoutChangeTo = CharBoxesLayout.OneLine;
			toChangeExperimentLayout = true;
			currentDataSetName = ProjectConfig.templateImageDataSetName();
			loadNextImageIntoExCharGroup();
            if(isOneLineSessionOver) {
                prepareForNextTestingSet();
                return;
            }
		}
		else {
			isExperimentOver = true;
			return;
		}

		taskList.add(txtFileManager.getCreateOrOpenCharLogFileTask(
				ProjectConfig.tipForceLogPrefix,
				mUserID,
				currentDataSetName,
				cachedChars,
				logIndex,
				firstCharBoxIndex,
				numCharBoxesInAPage));


		Runnable[] taskArray = new Runnable[taskList.size()];

		(new TaskRunnerAndDisplayProgressDialogAsyncTask(
				mContext,
				taskList.toArray(taskArray),
				null)).execute();

	}

	private void showPostExperimentView() {
		mPreOrPostInfo.setText("恭喜你完成了，\n請按下一頁");
		setContentView(mPreOrPostExperimentView);
		prepareForNextTestingSet();
	}

	private void showPreExperimentView() {
		if(!isExperimentOver) {
			mPreOrPostInfo.setText("即將開始評量，\n準備好後請按下一步");
		}
		else {
			mPreOrPostNextPageButton.setVisibility(View.GONE);
			mPreOrPostInfo.setText("評量到此結束，感謝你的參與");
			endTheApp();
		}
		setContentView(mPreOrPostExperimentView);
	}

	private void setBTStateText(TextView btStateText) {
		if(mBTManager.getBTState() == BluetoothState.STATE_CONNECTED) {
			btStateText.setText(ProjectConfig.btConnectedText);
		}
		else if(mBTManager.getBTState() == BluetoothState.STATE_CONNECTING) {
			btStateText.setText(ProjectConfig.btConnectingText);
		}
		else {
			btStateText.setText(ProjectConfig.btDisconnectedText);
		}
	}

	private final int charBoxHeight = Utils.inchToPixels(this.mMetrics, ProjectConfig.inchPerCM * 2.1f);
	private final int charBoxOneLineHeight = Utils.inchToPixels(this.mMetrics,ProjectConfig.inchPerCM * 16.1f);
	private final int charBoxWidth = charBoxHeight;
    private final int oneLineWidth = (int)(charBoxWidth * 2f);

	private void changeExperimentLayout(CharBoxesLayout layoutSetting) {
		expLayoutSetting = layoutSetting;

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
			mContext = context;
			inflater = LayoutInflater.from(mContext);
		}

		RelativeLayout subViewsContainer;

		private void experimentViewArrangement() {

			otherUIContainer = new LinearLayout(mContext);
			mOneLineContainer = new RelativeLayout(mContext);
			mExampleCharsGroup = new ImageView(mContext);

			charGroupsContainer = new LinearLayout(mContext);
			charGroupsContainer.setOrientation(LinearLayout.VERTICAL);

			RelativeLayout.LayoutParams charGroupsContainerLayoutParams = new RelativeLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,  //width
					LinearLayout.LayoutParams.WRAP_CONTENT); //height

			charGroupsContainerLayoutParams.topMargin = 70;

			if(mUserDominantHand.equals(ProjectConfig.leftHand)) {
				charGroupsContainerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				charGroupsContainerLayoutParams.rightMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}
			else {
				charGroupsContainerLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				charGroupsContainerLayoutParams.leftMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}

			charGroupsContainer.setLayoutParams(charGroupsContainerLayoutParams);
			subViewsContainer = (RelativeLayout)mExperimentView.findViewById(R.id.spenViewContainer);
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

            bmpCanvas = new Canvas();

			int charBoxMarginLeftOrRight = (int)mRes.getDimension(R.dimen.char_box_left_or_right_margin);
			int charBoxMarginBottom = (int)mRes.getDimension(R.dimen.char_box_bottom_margin);
			
			LinearLayout.LayoutParams charGroupLayoutParams = new LinearLayout.LayoutParams(
					LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT
			);

			LinearLayout[] charGroups = new LinearLayout[numCharBoxesInAPage];
			Typeface font = ProjectConfig.getFont(ProjectConfig.ChineseDefault);
			if(font == null) {
				Toast.makeText(mContext, "無法讀取中文字形檔", Toast.LENGTH_LONG);
			}

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
					oneLineWidth,
                    LinearLayout.LayoutParams.WRAP_CONTENT
			);

			LinearLayout.LayoutParams oneLine_LP = new LinearLayout.LayoutParams(
					oneLineWidth,
                    LinearLayout.LayoutParams.MATCH_PARENT
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


			mExampleCharsGroup.setLayoutParams(imgRLP);
			mExampleCharsGroup.setBackgroundColor(Color.WHITE);

			mOneLineContainer.setLayoutParams(oneLine_LP);
			mOneLineContainer.setBackgroundColor(Color.WHITE);

			if(mUserDominantHand.equals(ProjectConfig.leftHand)) {
				otherUIContainer.addView(mOneLineContainer);
				otherUIContainer.addView(mExampleCharsGroup);
				//otherUI_LP.rightMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}
			else {
				otherUIContainer.addView(mExampleCharsGroup);
				otherUIContainer.addView(mOneLineContainer);
				//otherUI_LP.leftMargin = (int)mRes.getDimension(R.dimen.charGroupsMarginLeftOrRight);
			}


		}
		
		@Override
		public void run() {

			//init variables
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

			//TODO: use EventBus
			mLBCManager = LocalBroadcastManager.getInstance(mContext);
			filter = new IntentFilter(Action_update_chars);
//			filter.addAction(BluetoothClientConnectService.Msg_update_info);
//			filter.addAction(TaskRunnerAndDisplayProgressDialogAsyncTask.startAsyncTask);
//			filter.addAction(TaskRunnerAndDisplayProgressDialogAsyncTask.stopAsyncTask);

			/* Preference and Other Manager Settings */
			additionalSetName = ProjectConfig.getAdditionalSetFileName();

			SharedPreferences userInfoPreference = mContext.getSharedPreferences(ProjectConfig.Key_Preference_UserInfo, Context.MODE_PRIVATE);
			if(userInfoPreference != null) {
				mUserGrade = (int)userInfoPreference.getLong(ProjectConfig.Key_Preference_UserGrade, 1);
				mUserDominantHand = userInfoPreference.getString(ProjectConfig.Key_Preference_UserDominantHand, ProjectConfig.rightHand);
				mUserID = userInfoPreference.getString(ProjectConfig.Key_Preference_UserID, ProjectConfig.defaultUserID);
			}

			mBTManager.startLogging(mUserID);
			FileDirInfo dirInfo = new FileDirInfo(FileType.GeneratingImage, ProjectConfig.getDirpathByID(mUserID), null);
			genImgFileManager = new ImgFileManager(dirInfo, mContext);

			//instead of using default path, use the path specified above(ProjectConfig.getDirpathByID(mUserID))
			dirInfo.setFileType(FileType.Log, false);
			dirInfo.setOtherInfo(String.valueOf(numCharBoxesInCol * numWritableCharBoxCols));
			txtFileManager = new TxtFileManager(dirInfo, mContext);

			dirInfo.setFileType(FileType.TemplateImage, true);
			dirInfo.setOtherInfo(null);
			templateImgFileManager = new ImgFileManager(dirInfo, mContext);

			tipForceData = new List[numCharBoxesInAPage];
			for(int i = 0;i < numCharBoxesInAPage;i++) {
				tipForceData[i] = new LinkedList<>();
			}

			/* Experiment View */

			mExperimentView = inflater.inflate(R.layout.activity_experiment_3, null);

			mPenTipInfo = (TextView)mExperimentView.findViewById(R.id.penTipInfo);

			// Set button Listener
			mPenBtn = (ImageView) mExperimentView.findViewById(R.id.penBtn);
			mPenBtn.setOnClickListener(mBtnOnClickListener);

			mEraserBtn = (ImageView) mExperimentView.findViewById(R.id.eraserBtn);
			mEraserBtn.setOnClickListener(mBtnOnClickListener);

			mCleanBtn = (ImageView) mExperimentView.findViewById(R.id.cleanBtn);
			mCleanBtn.setOnClickListener(mBtnOnClickListener);

			mNextPageBtn = (RelativeLayout) mExperimentView.findViewById(R.id.nextPageBtn);
			mNextPageBtn.setOnClickListener(mBtnOnClickListener);

			//programmatically generate layout
			experimentViewArrangement();

			selectButton(mPenBtn);

			//TODO:set the two bt states together in EventBus handler
			mBTState = (TextView)mExperimentView.findViewById(R.id.text_bt_state);
			setBTStateText(mBTState);

			/* ExperimentView */

			testingSetGradesSeq = ProjectConfig.getTestingGradeSequence(mUserGrade);
			testingSetIndex = 0;
			prepareForNextTestingSet();

			//do SpenSetUp-Related Work at last
			/* Spen Dependent Part Start */

			Log.d(debug_tag,"Spen Start init");

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
			initSettingInfo();

			Log.d(debug_tag,"Spen init done");

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

			Log.d(debug_tag,"SpenNoteDoc allocated");

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

			Log.d(debug_tag,"ready to allocate surface view");

			//this part of work needs to be executed in Main thread (UI thread)
			uiThreadHandler.post(new Runnable() {

				@Override
				public void run() {

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

                    changeExperimentLayout(CharBoxesLayout.SeparateChars);
				}
			});

			Log.d(debug_tag,"allocating surface view done");

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

	private View.OnClickListener mBtnOnClickListener = new View.OnClickListener() {
		private void setSPenToolActionWithAllCanvases(int toolAction) {

			for(int i = 0;i < numWritableCharBoxCols;i++) {
				for(int j = 0;j < numCharBoxesInCol;j++) {
					mCharBoxes[i][j].setToolTypeAction(SpenSurfaceView.TOOL_SPEN, toolAction);
				}
			}
			return;
		}

		@Override
		public void onClick(View view) {

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

					if(toChangeExperimentLayout) {
						changeExperimentLayout(layoutChangeTo);
						toChangeExperimentLayout = false;
					}

					setContentView(mExperimentView); //switch to experiment view

					preOrPostInterfaceState = UIState.postExperimentTrial;
				}
				else if(preOrPostInterfaceState == UIState.postExperimentTrial) {
					showPreExperimentView();
					preOrPostInterfaceState = UIState.preExperimentTrial;
				}
			}
			else if(id == R.id.nextPageBtn) { //button on experiment view
				nextPage();
			}

		}
	};

	private class CustomizedSpenTouchListener implements SpenTouchListener{
		private final static char delimiter = ',';
		private int mCharboxIndex = -1;
		//private SpenSurfaceView mSurfaceView = null;
		private StringBuffer stringBuffer = new StringBuffer();

		public CustomizedSpenTouchListener(int charboxIndex, SpenSurfaceView surfaceView) {
			mCharboxIndex = charboxIndex;
			//mSurfaceView = surfaceView;
		}

		@Override
		public boolean onTouch(View view, MotionEvent event) {
			if(event.getToolType(0) == SpenSurfaceView.TOOL_SPEN && mPenBtn.isSelected()) {
				Log.d(debug_tag, event.getPressure() + "," + event.getX() + "," + event.getY());
				//mPenTipInfo.setText("it's pen");
				stringBuffer.setLength(0); //clean buffer
				stringBuffer.append(ProjectConfig.getTimestamp());
				stringBuffer.append(delimiter);
				stringBuffer.append(event.getX());
				stringBuffer.append(delimiter);
				stringBuffer.append(event.getY());
				stringBuffer.append(delimiter);
				stringBuffer.append(event.getPressure());

//				txtFileManager.appendLogWithNewlineSync(mCharboxIndex, stringBuffer.toString());
				tipForceData[mCharboxIndex].add(stringBuffer.toString());
				if(tipForceData[mCharboxIndex].size() >= ProjectConfig.maxCachedLogData) {
					appendTipForceLogAsync(mCharboxIndex);
				}

			}

			return false;
		}

	}

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

	private void captureExampleTextView(TextView textView, String fileName) {
        Bitmap bufferBMP = Bitmap.createBitmap(charBoxWidth, charBoxHeight, Bitmap.Config.ARGB_8888);
        bmpCanvas.setBitmap(bufferBMP);
        textView.draw(bmpCanvas);
        genImgFileManager.saveBMP(bufferBMP, fileName, true);
	}

	private void captureSpenSurfaceView(SpenSurfaceView surfaceView, String fileName) {
		Bitmap imgBitmap = surfaceView.captureCurrentView(true);
		genImgFileManager.saveBMP(imgBitmap, fileName, true);
	}

	private View findViewByStr(View viewToSearchIn, String name) {
		int resId = mRes.getIdentifier(name, "id", packageName);
		return viewToSearchIn.findViewById(resId);
	}

	private void initSettingInfo() {

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

	public void onEvent(BTEvent event) {
		setRelatedUI(event);
	}

	private void setRelatedUI(BTEvent event) {

		if(event.type == BTEvent.Type.Connecting) {
			mBTState.setText(ProjectConfig.btConnectingText);
			mBTStateInPreAndPostView.setText(ProjectConfig.btConnectingText);
		}
		else if(event.type == BTEvent.Type.Connected) {
			mBTState.setText(ProjectConfig.btConnectedText);
			mBTStateInPreAndPostView.setText(ProjectConfig.btConnectedText);
		}
		else if(event.type == BTEvent.Type.Disconnected) {
			mBTState.setText(ProjectConfig.btDisconnectedText);
			mBTStateInPreAndPostView.setText(ProjectConfig.btDisconnectedText);
		}
		else if(event.type == BTEvent.Type.Reconnecting) {
			mBTState.setText(ProjectConfig.btReconnectingText);
			mBTStateInPreAndPostView.setText(ProjectConfig.btReconnectingText);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		eventBus.register(this);
		mLBCManager.registerReceiver(broadcastReceiver, filter);

	}

	@Override
	protected void onPause() {
		super.onPause();
		eventBus.unregister(this);
		mLBCManager.unregisterReceiver(broadcastReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		savingData();

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
	}

	private void savingData() {
		mBTManager.endLogging();
	}

	private void endTheApp() {
		savingData();
		endTheAppAfterCertainDuration(7000);
	}
}
