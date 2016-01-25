package com.mhci.gripandtipforce.model;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.mhci.gripandtipforce.R;
import com.sandisk.realstoragepath.RealStoragePathLibrary;

public class ProjectConfig {
	public final static String appTitle = "兒童書寫評量系統";

	public final static String debug_tag = "ProjectConfig";
	public final static String projectName = "GripForce"; 
	public static boolean useSystemBarHideAndShow = true;
	public final static boolean useRealSDCard = false;

	public static final int numBytesPerSensorStrip = 19;
	public static final int numSensorStrips = 5;
	public static final int minSensorVal = 20;
	public static final int maxCachedLogData = 5000;
	public final static float inchPerCM = 0.393700787f;

	public static final UUID UUIDForBT = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	public static final String txtFileExtension = ".txt";
	public static final String imgFileExtension = ".png";

	private static RealStoragePathLibrary storageLib;
	public static String[] defaultPaths = null;
	public static String writableRootPath = null;
	public static String internalSDCardPath = null;
	public static String externalSDCardPath = null;
	public static String exampleCharsFilesDirPath = null;
	private static String defaultProjectDirPath = null;

	//Dataset
    private final static String[] DatasetName = new String[DataSetType.NumDataSetType.ordinal()];

    public final static String exampleCharsDirName = "Example_Characters";
	public final static String gripForceLogPrefix = "GripForce";
	public final static String tipForceLogPrefix = "TipForce";
	public final static String writingImgPrefix = "WritingImage";
    public final static String textviewImgPrefix = "ExCharImage";
    public final static String restartMark = "restart_point";
    //private static final String templateImgRegExp = "^" + DatasetName[DataSetType.TemplateImg.ordinal()] + ".*\\" + imgFileExtension;
    //private static final String additionalSetRegExp =  DatasetName[DataSetType.AdditionalChar.ordinal()] + "(.*)";

	public final static String btConnectingText = "連線中";
	public final static String btConnectedText = "已連線";
	public final static String btDisconnectedText = "已斷線";
	public final static String btReconnectingText = "重新連線中";


	public final static int numOfGrades = 6;

	public static long startTimestampInMilliSec = 0;

	public static String getRootDirPath() {
		if(writableRootPath == null) {
			Log.d(debug_tag, "you should set writable path first");
		}
		return writableRootPath;
	}
	
	public static void setWritableRootPath(Context context) {
		storageLib = new RealStoragePathLibrary(context);
		externalSDCardPath = storageLib.getMicroSDStoragePath();
		internalSDCardPath = storageLib.getInbuiltStoragePath();
		writableRootPath = null;

		String defaultPathToUse = null;

		if(useRealSDCard) {
			if(!isDirPathWritable(externalSDCardPath)) {
				Toast.makeText(context, "外接SD卡無法使用", Toast.LENGTH_LONG).show();
				if (!isDirPathWritable(internalSDCardPath)) {
					Toast.makeText(context, "所有儲存空間都無法使用,\n請確認是否與電腦斷開連線或SD卡正確安裝", Toast.LENGTH_LONG).show();
					return;
				} else {
					defaultPathToUse = internalSDCardPath;
					Toast.makeText(context, "改用內部儲存空間", Toast.LENGTH_LONG).show();
				}
			}
			else {
				defaultPathToUse = externalSDCardPath;
			}
		}
		else {
			if(!isDirPathWritable(internalSDCardPath)) {
				Toast.makeText(context, "內部儲存空間無法使用", Toast.LENGTH_LONG).show();
				if (!isDirPathWritable(externalSDCardPath)) {
					Toast.makeText(context, "所有儲存空間都無法使用,\n請確認是否與電腦斷開連線或SD卡正確安裝", Toast.LENGTH_LONG).show();
					return;
				} else {
					defaultPathToUse = externalSDCardPath;
					Toast.makeText(context, "改用外接SD卡", Toast.LENGTH_LONG).show();
				}
			}
			else {
				defaultPathToUse = internalSDCardPath;
			}
		}

		writableRootPath = defaultPathToUse;

		//Toast.makeText(context, "path become " + writableRootPath, Toast.LENGTH_LONG).show();
		if(externalSDCardPath.equals(writableRootPath)) {
			Toast.makeText(context, "使用SD卡中", Toast.LENGTH_LONG).show();
			Log.d(debug_tag,"currently use external storage");
		}
		else {
			Toast.makeText(context, "使用內部儲存空間,\n需透過平板上的設定才能從電腦存取", Toast.LENGTH_LONG).show();
			Log.d(debug_tag,"currently use internal storage");
		}

	}

	public static String chineseCharFileName(int grade) {
		return chineseCharDatasetName(grade) + txtFileExtension;
	}

	public static String chineseCharDatasetName(int grade) {
		return DatasetName[DataSetType.ChineseChar.ordinal()] + "_Grade" + grade;
	}

	public static String additionalDataSetName(int setNum) {
		return DatasetName[DataSetType.AdditionalChar.ordinal()] + "_" + (setNum + 1);
	}

	public static String templateImageDataSetName() {
		return DatasetName[DataSetType.TemplateImg.ordinal()];
	}

	//SharePreference Keys
	
	//BT Settings
	public final static String Key_Preference_LastSelectedBT = "LastSelectedBT";
	public final static String Key_Preference_LastSelectedBTAddress = "CurrentSelectedBTAddress";
	
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

	//Fonts
	public final static String ChineseDefault = "chinese.ttf";
	public final static String AdditionalSetDefault = "addition.ttf";
	public static HashMap<String, Typeface> fontMap = null;

	public static String dataSetName(DataSetType dataSetType) {
		return DatasetName[dataSetType.ordinal()];
	}

	public static Typeface getFont(String fontType) {
		Typeface storedFont = fontMap.get(fontType);
		if(storedFont == null) {
			storedFont = getFontFromFile(fontType);
			fontMap.put(fontType, storedFont);
		}
		return storedFont;
	}

//	public static boolean isInitDone = false;

	public static void initSomeVars(Context context) {
//		if(isInitDone) {
//			return;
//		}

		Resources res = context.getResources();
		leftHand = res.getString(R.string.leftHand);
		rightHand = res.getString(R.string.rightHand);
		//OneLine = res.getString(R.string.OneLine);
		//SeparateChars = res.getString(R.string.SeparateChars);
		setWritableRootPath(context);
		setDefaultDirPaths(context);
		fontMap = new HashMap<String, Typeface>();
		DatasetName[DataSetType.ChineseChar.ordinal()] = "ChineseCharacters";
		DatasetName[DataSetType.AdditionalChar.ordinal()] = "AdditionalCharacters";
		DatasetName[DataSetType.TemplateImg.ordinal()] = "TemplateImage";

//		isInitDone = true;

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

	public static String[] getAdditionalSetFileName() {
		return (new File(exampleCharsFilesDirPath)).list(additionalSetNameFilter);
	}

	public static Typeface getAdditionalSetFont() {
		return getFont(AdditionalSetDefault);
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
		defaultPaths = new String[FileType.numFileType.ordinal()];
		String rootPath = getRootDirPath();
		defaultProjectDirPath = rootPath + "/" + projectName;
		//defaultPaths[FileType.Log.ordinal()] = defaultProjectDirPath + "/Logs";
		//defaultPaths[FileType.PersonalInfo.ordinal()] = defaultProjectDirPath + "/PersonalInformation";
		//defaultPaths[FileType.GeneratingImage.ordinal()] = defaultProjectDirPath + "/GeneratingImages";
		defaultPaths[FileType.ExampleChars.ordinal()] = defaultProjectDirPath + "/" + exampleCharsDirName;
		defaultPaths[FileType.TemplateImage.ordinal()] = defaultProjectDirPath + "/Template_Images";
		exampleCharsFilesDirPath = defaultPaths[FileType.ExampleChars.ordinal()];
		checkDirExistence(defaultPaths, context);
		
		return defaultPaths;
	}

	public static String getGeneratingImgFileName(String prefix, String userID, String dataSetName, int charIndex) {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(prefix);
		strBuffer.append('_');
		strBuffer.append(userID);
		strBuffer.append('_');
		strBuffer.append(dataSetName);
		strBuffer.append('_');
		strBuffer.append(charIndex + 1);
		strBuffer.append(imgFileExtension);
		return strBuffer.toString();
	}

	public static String getTemplateImagesDirPath(int grade) {
		return defaultPaths[FileType.TemplateImage.ordinal()] + "/Grade" + grade;
	}

    public static final FileFilter templateImageNameFilter = new FileFilter() {
		@Override
		public boolean accept(File file) {
            return file.getName().startsWith(DatasetName[DataSetType.TemplateImg.ordinal()]);
		}
	};

	public static final FilenameFilter additionalSetNameFilter = new FilenameFilter() {
		@Override
		public boolean accept(File dir, String fileName) {
			return fileName.startsWith(DatasetName[DataSetType.AdditionalChar.ordinal()]);
		}
	};

	public static String getGripForceLogFileName(String userID) {
		StringBuffer strBuffer = new StringBuffer();
		strBuffer.append(gripForceLogPrefix);
		strBuffer.append('_');
		strBuffer.append(userID);
		strBuffer.append(txtFileExtension);
		return strBuffer.toString();
	}
	
	public static String getPersonalInfoFileName(String userID) {
		return userID + txtFileExtension;
	}
	
	public static String getDirpathByID(String userID) {
		return defaultProjectDirPath + "/" + userID;
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
				else if(dir.canWrite()) {
					return true;
				}
				else {
					return false;
				}
			}
			else {
				if(dir.isDirectory() && dir.canWrite()) {
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

	public static long getTimestamp() {
		return System.currentTimeMillis() - startTimestampInMilliSec;
	}

	private static void tryToWriteTempFile(File dir) throws Exception {
		//try to save an empty file
		File tmpFile = File.createTempFile("tryToWrite", null, dir);
		tmpFile.delete();
	}

	private static void checkDirExistence(String[] defaultPaths,Context context) {
		for(String path : defaultPaths) {
			try {
                if(path == null) {
                    continue;
                }
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

	public static Typeface getFontFromFile(String fontName) {
		try {
			File fontFile = new File(ProjectConfig.externalSDCardPath + "/" + ProjectConfig.projectName + "/Fonts/" + fontName);
			return Typeface.createFromFile(fontFile);
		}
		catch(Exception e) {
			Log.d("typeface", e.getLocalizedMessage());
			return null;
		}
	}

}
