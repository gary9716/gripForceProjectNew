package com.mhci.gripandtipforce;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class ProjectConfig {
	public final static String debug_tag = "ProjectConfig";
	
	public final static String projectName = "GripForce"; 
	public static boolean useSystemBarHideAndShow = true;
	public final static boolean useRealSDCard = true;
	public static final int numBytesPerSensorStrip = 19;
	public static final int numSensorStrips = 6;
	
	public final static float inchPerCM = 0.393700787f;
	public static final UUID UUIDForBT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final String txtFileExtension = ".txt";
	public static final String imgFileExtension = ".png";
	
	public static String writableRootPath = null;
	public final static String internalSDCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	private final static String externalStorageName = "extSdCard";
	public final static String externalSDCardPath = tryToFindExtSDCardPath();
	private static String defaultProjectDirPath = null;

	public final static String exampleCharsDirName = "Example_Characters";
	public static String exampleCharsFilesDirPath = null;
	public final static String gripForceLogPrefix = "GripForce_";
	public final static String tipForceLogPrefix = "TipForce_";
	public final static String restartMark = "restart_point";

	public final static int numOfGrades = 6;

	private static String defaultFontName = "msjh.ttf";
	private static Typeface defaultFont = null;

	private final static String[] additionalSetName = {
		"Unseen_Character"
	};

	private static String[] additionalSetPath = null;

	private static String tryToFindExtSDCardPath() {
		int lastIndex = internalSDCardPath.lastIndexOf('/');
		String path = internalSDCardPath.substring(0, lastIndex + 1) + externalStorageName;
		try {
			File dir = new File(path);
			if(dir.exists()) {
				return path;
			}
			else {
				return "/storage/" + externalStorageName;
			}
		}
		catch(Exception e) {
			return "/storage/" + externalStorageName;
		}
	}
	
	public static String getRootDirPath() {
		if(writableRootPath == null) {
			setWritableRootPath(null);
		}
		return writableRootPath;
	}
	
	public static void setWritableRootPath(Context context) {
		writableRootPath = null;
		
		String defaultPathToUse = null;
		if(useRealSDCard) {
			defaultPathToUse = externalSDCardPath;
		}
		else {
			defaultPathToUse = internalSDCardPath;
		}
		
		if(!isDirPathWritable(defaultPathToUse)) {
			if(externalSDCardPath.equals(defaultPathToUse)) {
				Toast.makeText(context, "SD卡無法使用,請確認是否正確安裝", Toast.LENGTH_LONG).show();
			}
			defaultPathToUse = internalSDCardPath;
			if(!isDirPathWritable(defaultPathToUse)) {
				Log.d(debug_tag, "both storage are not available");
				if(context != null) {
					Toast.makeText(context, "所有空間目前都無法使用,請確認是否與電腦斷開連線或SD卡正確安裝", Toast.LENGTH_LONG).show();
				}
				return;
			}
		}
		
		writableRootPath = defaultPathToUse;
		
		if(context != null) {
			//Toast.makeText(context, "path become " + writableRootPath, Toast.LENGTH_LONG).show();
			if(externalSDCardPath.equals(writableRootPath)) {
				Toast.makeText(context, "使用SD卡中", Toast.LENGTH_LONG).show();
			}
			else {
				Toast.makeText(context, "使用內部儲存空間,需透過平板上的設定才能從電腦存取", Toast.LENGTH_LONG).show();
			}
		}
	}

	public static String exampleCharsFileName(int grade) {
		return "Grade_" + grade + "_Characters" + txtFileExtension;
	}
	
	//SharePreference Keys
	
	//BT Settings
	public final static String Key_Preference_LastSelectedBT = "LastSelectedBT";
	public final static String Key_Preference_CurrentSelectedBTAddress = "CurrentSelectedBTAddress";
	
	//User Info
	public final static String userInfo_delimiter = "\n";
	public final static String Key_Preference_UserInfo = "UserInfo";
	public final static String Key_Preference_UserID = "UserID";
	public final static String Key_Preference_UserGrade = "UserGrade";
	public final static String Key_Preference_UserDominantHand = "UserDominantHand";

	public static String leftHand = null;
	public static String rightHand = null;
	public final static String defaultUserID = "DefaultUser";
	
	//Experiment Setting
	public static final String Key_Preference_ExperimentSetting = "expSetting";
	public static final String Key_Preference_TestingLayout = "TestingLayout";


	public static void changeDefaultFont(String fontName) {
		defaultFontName = fontName;
	}

	public static Typeface getDefaultFont() {
		if(defaultFont == null) {
			defaultFont = getFont(defaultFontName);
		}
		return defaultFont;
	}

	public static boolean isInitDone = false;

	public static void initSomeVars(Context context) {
		if(isInitDone) {
			return;
		}

		Resources res = context.getResources();
		leftHand = res.getString(R.string.leftHand);
		rightHand = res.getString(R.string.rightHand);
		//OneLine = res.getString(R.string.OneLine);
		//SeparateChars = res.getString(R.string.SeparateChars);
		setWritableRootPath(context);
		FileDirInfo._defaultDirPath = setDefaultDirPaths(context);
		isInitDone = true;

	}

	public static String getCurrentUserID(Context context) {
		SharedPreferences preferences = context.getSharedPreferences(Key_Preference_UserInfo, Context.MODE_PRIVATE);
		if(preferences != null) {
			return preferences.getString(Key_Preference_UserID, defaultUserID);
		}
		else {
			return defaultUserID;
		}
	}
	
	public static ArrayList<Integer> getTestingGradeSequence(int grade) {
		ArrayList<Integer> testingGrades = new ArrayList<Integer>();
//		for(int i = 0;i < grade;i++) {
//			testingGrades.add(Integer.valueOf(i+1));
//		}
		
		for(int i = grade;i >= 1;i--) {
			testingGrades.add(Integer.valueOf(i));
		}
		
		//Collections.shuffle(testingGrades);
		
		return testingGrades;
	}

	public static String[] getAdditionalSetName() {
		return additionalSetName;
	}

//	public static String[] getAdditionalCharSet() {
//		if(additionalSetPath == null) {
//			if(additionalSetName == null) {
//				return null;
//			}
//			additionalSetPath = new String[additionalSetName.length];
//			for(int i = 0;i < additionalSetName.length;i++) {
//				additionalSetPath[i] = getProjectDirPath() + "/" + additionalSetName[i];
//			}
//		}
//		return additionalSetPath;
//	}

	private static String[] setDefaultDirPaths(Context context) {
		String[] defaultPaths = new String[FileType.numFileType.ordinal()];
		String rootPath = getRootDirPath();
		defaultProjectDirPath = rootPath + "/" + projectName;
		defaultPaths[FileType.Log.ordinal()] = defaultProjectDirPath + "/Logs";
		defaultPaths[FileType.PersonalInfo.ordinal()] = defaultProjectDirPath + "/PersonalInformation";
		defaultPaths[FileType.GeneratingImage.ordinal()] = defaultProjectDirPath + "/GeneratingImages";
		defaultPaths[FileType.ExampleChars.ordinal()] = defaultProjectDirPath + "/" + exampleCharsDirName;
		defaultPaths[FileType.TemplateImage.ordinal()] = defaultProjectDirPath + "/TemplateImages";
		exampleCharsFilesDirPath = defaultPaths[FileType.ExampleChars.ordinal()];
		checkDirExistence(defaultPaths,context);
		
		return defaultPaths;
	}


	public static String getImgFileName(String userID,int grade,int charIndex) {
		return userID + "_" + grade + "_" + (charIndex + 1) + imgFileExtension;
	}

	public static String getTipForceLogFileName(String userID, int grade, int charIndex) {
		return tipForceLogPrefix + userID + "_" + grade + "_" + (charIndex + 1) + txtFileExtension;
	}
	
	public static String getGripForceLogFileName(String userID) {
		return gripForceLogPrefix + userID + txtFileExtension;
	}
	
	public static String getPersonalInfoFileName(String userID) {
		return userID + txtFileExtension;
	}
	
	public static String getDirpathByID(String userID) {
		return getRootDirPath() + "/" + projectName + "/" + userID;
	}

	public static String getProjectDirPath() {
		return defaultProjectDirPath;
	}

	//Utility Functions

	private final static String testPathWritable = "testPathWritable";

	public static boolean isDirPathWritable(String dirPath) {
		try {
			if(dirPath == null) {
				return false;
			}
			File dir = new File(dirPath);
			if(!dir.exists()) {
				if(!dir.mkdirs()) {
					Log.d(testPathWritable, "cannot create dir");
					return false;
				}
				else {
					tryToWriteTempFile(dir);
					return true;
				}
			}
			else {
				if(dir.isDirectory()) {
					tryToWriteTempFile(dir);
					return true;
				}
				else {
					Log.d(testPathWritable, "it's not a dir");
					return false;
				}
			}

		}
		catch(Exception e) {
			Log.d(testPathWritable, e.getLocalizedMessage());
			return false;
		}
	}

	public static long getTimestamp(long startingTime) {
		return System.currentTimeMillis() - startingTime;
	}

	private static void tryToWriteTempFile(File dir) throws Exception {
		//try to save an empty file
		File tmpFile = File.createTempFile("tryToWrite", null, dir);
		tmpFile.delete();
	}

	private static void checkDirExistence(String[] defaultPaths,Context context) {
		for(String path : defaultPaths) {
			try {
				File dir = new File(path);
				if(!dir.exists()) {
					if(!dir.mkdirs()) {
						Toast.makeText(context, path + " is not creatable", Toast.LENGTH_SHORT).show();
						Log.d(debug_tag, path + " is not creatable");
					}
				}
			}
			catch(Exception e) {

			}
		}
	}

	public static Typeface getFont(String fontName) {
		try {
			File fontFile = new File(ProjectConfig.internalSDCardPath + "/" + ProjectConfig.projectName + "/" + fontName);
			return Typeface.createFromFile(fontFile);
		}
		catch(Exception e) {
			Log.d("typeface", e.getLocalizedMessage());
			return null;
		}
	}

}
