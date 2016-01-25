package com.mhci.gripandtipforce.model.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.mhci.gripandtipforce.view.activity.ExperimentActivity;
import com.mhci.gripandtipforce.model.FileDirInfo;
import com.mhci.gripandtipforce.model.FileType;
import com.mhci.gripandtipforce.model.ProjectConfig;

import cn.trinea.android.common.util.FileUtils;

public class TxtFileManager extends FileManager {
	private final static String DEBUG_TAG = "TxtFileManager";
	private File mFileDir = null;
	private FileType mFileType = FileType.Log;
	private Context mContext;
	private BufferedWriter[] writerArray;
	private File[] logFiles;
	private LocalBroadcastManager mLBCManager = null;

	public TxtFileManager(FileDirInfo dirInfo, Context context) {
		super(context, dirInfo.getFileType());
		readUserConfig();

		mContext = context;
		mFileType = dirInfo.getFileType();
		mLBCManager = LocalBroadcastManager.getInstance(mContext);
		
		switch(mFileType) {
			case Log:
				int numOfInstanceToAlloc = 1;
				if(dirInfo.getOtherInfo() != null) {
					numOfInstanceToAlloc = (new Integer(dirInfo.getOtherInfo())).intValue();
				}
				writerArray = new BufferedWriter[numOfInstanceToAlloc];
				logFiles = new File[numOfInstanceToAlloc];
				break;
			case PersonalInfo:
				writerArray = new BufferedWriter[1];
				logFiles = new File[1];
				break;
			default:
				break;
		}

		mFileDir = new File(dirInfo.getDirPath());
		
	}
	
	private File createOrOpenLogFile(String fileName) {
		if(mFileDir == null) {
			Toast.makeText(mContext, "txtDir is null ,failed to create log", Toast.LENGTH_LONG).show();
			return null;
		}

		File txtFile = null;

		try {
//			Log.d(debug_tag,"filePath:" + mFileDir.getAbsolutePath());
			txtFile = new File(mFileDir, fileName);
			if(!txtFile.exists()) {
				if(!txtFile.createNewFile()) {
					Toast.makeText(mContext, "creating txt file failed", Toast.LENGTH_LONG).show();
					return null;
				}
			}
		}
		catch(Exception e) {
			txtFile = null;
			Log.d(debug_tag,e.getLocalizedMessage());
			Toast.makeText(mContext, "creating or openning txt file failed", Toast.LENGTH_LONG).show();
		}
		
		return txtFile;
	}

	//fileIndex is used for indexing in dictionary
	public boolean createOrOpenLogFileSync(String fileName, int arrayIndex) {
		if(arrayIndex >= writerArray.length) {
			Log.d(debug_tag, "indexing out of bound in createOrOpenLogFileSync");
			return false;
		}

		File logFile = createOrOpenLogFile(fileName);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(logFile,true));
			closeFile(arrayIndex);
			writerArray[arrayIndex] = writer;
			return true;
		}
		catch(Exception e) {
			Log.d(debug_tag, e.getLocalizedMessage());
			return false;
		}

	}

	public Runnable getCreateOrOpenCharLogFileTask(String logType,
												   String userID,
												   String dataSetName,
												   String[] cachedChars,
												   int logIndex,
												   int charBoxIndex,
												   int numFilesToCreateOrOpen) {
		return new CreateOrOpenCharLogFileTask(
				logType,
				userID,
				dataSetName,
				cachedChars,
				logIndex,
				charBoxIndex,
				numFilesToCreateOrOpen);
	}

	private class CreateOrOpenCharLogFileTask implements Runnable {

		//charIndex is the order of certain char in Example Chars file
		//charBoxIndex is the order of certain char on the experiment view
		int mLogIndex;
		int mCharBoxIndex;
		int mNumFilesToCreateOrOpen;
		String mFileNamePrefix = null;
		String[] cachedChars = null;

		public CreateOrOpenCharLogFileTask(String logType,
										   String userID,
										   String dataSetName,
										   String[] cachedChars,
										   int logIndex,
										   int charBoxIndex,
										   int numFilesToCreateOrOpen) {
			this.cachedChars = cachedChars;
			this.mFileNamePrefix = logType + "_" + userID + "_" + dataSetName + "_";
			this.mLogIndex = logIndex;
			this.mCharBoxIndex = charBoxIndex;
			this.mNumFilesToCreateOrOpen = numFilesToCreateOrOpen;
		}

		@Override
		public void run() {
			int numLogsToLoad = mNumFilesToCreateOrOpen;
			if(cachedChars != null) {
				if(cachedChars.length - mLogIndex < numLogsToLoad) {
					numLogsToLoad = cachedChars.length - mLogIndex;
				}
			}
			for(int i = 0;i < numLogsToLoad;i++) {
				createOrOpenLogFileSync(
						mFileNamePrefix + (mLogIndex + i + 1) + ProjectConfig.txtFileExtension,
						mCharBoxIndex + i);
			}
		}

	}

	public Runnable getAppendListLogTask(int arrayIndex, List<String> dataList) {
		return new AppendListLogTask(arrayIndex,dataList);
	}

	private class AppendListLogTask implements Runnable {

		int mArrayIndex;
		List<String> mDataList;

		public AppendListLogTask(int arrayIndex, List<String> dataList) {
			mArrayIndex = arrayIndex;
			mDataList = dataList;
		}

		@Override
		public void run() {
			appendLogs(mArrayIndex,mDataList);
		}
	}

	public void appendLogWithNewlineSync(int arrayIndex, String data) {
		BufferedWriter writer = null;
		try {
			writer = writerArray[arrayIndex];
		}
		catch (Exception e) {
			writer = null;
		}

		if(writer == null) {
			return;
		}

		try {
			writer.write(data);
			writer.newLine();
		}
		catch(Exception e) {
			Toast.makeText(mContext, "寫入Log失敗", Toast.LENGTH_LONG).show();
			Log.d(debug_tag,"exception in AppendLogTask,e:" + e.getLocalizedMessage());
		}
		//Log.d(debug_tag,"done append log");
		
	}

	public void appendLogs(int arrayIndex, List<String> linesOfData) {
		File logFileToWrite = logFiles[arrayIndex];
		FileUtils.writeFile(logFileToWrite.getAbsolutePath(), linesOfData, true);
	}


	//read
	public Runnable getLoadChineseCharTask(int grade) {
		return (new LoadCharsTask(grade));
	}

	public Runnable getLoadCharTask(String fileName) {
		return (new LoadCharsTask(fileName));
	}
	
	private class LoadCharsTask implements Runnable {
		
		private int mGrade;
		private String mFileName;
		
		public LoadCharsTask(int grade) {
			mGrade = grade;
		}

		public LoadCharsTask(String fileName) {
			mGrade = -1;
			mFileName = fileName;
		}
		
		@Override
		public void run() {
			String[] result = null;
			if(mGrade != -1) {
				result = loadChineseCharsDependOnGrade(mGrade);
			}
			else {
				result = loadCharsDependOnFileName(mFileName);
			}

			if(result != null) {
				
				Intent intent = new Intent(ExperimentActivity.Action_update_chars);
				intent.putExtra(ExperimentActivity.Key_ExChars, result);
				mLBCManager.sendBroadcast(intent);
				
			}
		}
		
	}

	private String[] loadCharsDependOnFileName(String fileName) {
		File exampleCharsFile = null;
		try {
			exampleCharsFile = new File(ProjectConfig.exampleCharsFilesDirPath, fileName);
		}
		catch(Exception e) {
			return null;
		}

		if(exampleCharsFile == null || !exampleCharsFile.exists()) {
			Toast.makeText(mContext, "找不到" + fileName + ",請再次確認檔案已放到正確的資料夾底下" , Toast.LENGTH_LONG).show();
			return null;
		}
		else if(!exampleCharsFile.canRead()){
			Toast.makeText(mContext, "無法讀取" + fileName + ",請再次確認無與電腦連接USB" , Toast.LENGTH_LONG).show();
			return null;
		}

		return readCharsFromFile(exampleCharsFile, "讀取" + fileName + "時發生錯誤");
	}

	private String[] loadChineseCharsDependOnGrade(int grade) {

		File exampleCharsFile = null;
		try {
			exampleCharsFile = new File(ProjectConfig.exampleCharsFilesDirPath, ProjectConfig.chineseCharFileName(grade));
		}
		catch(Exception e) {
			return null;
		}

		if(exampleCharsFile == null || !exampleCharsFile.exists()) {
			Toast.makeText(mContext, "找不到" + grade + "年級的範例文字,請再次確認檔案已放到正確的資料夾底下" , Toast.LENGTH_LONG).show();
			return null;
		}
		else if(!exampleCharsFile.canRead()){
			Toast.makeText(mContext, "無法讀取" + grade + "年級的範例文字,請再次確認無與電腦連接USB" , Toast.LENGTH_LONG).show();
			return null;
		}

		return readCharsFromFile(exampleCharsFile, "讀取" + grade + "年級的範例文字時發生錯誤");
	}

	private String[] readCharsFromFile(File fileToRead, String exceptionMessage) {
		ArrayList<String> container = new ArrayList<String>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileToRead), "UTF-8"));
			while(true) {
				String singleChar = reader.readLine();
				if(singleChar != null) {
					singleChar = singleChar.trim();
					container.add(singleChar);
				}
				else {
					break;
				}

			}
		}
		catch(Exception e) {
			Toast.makeText(mContext, exceptionMessage , Toast.LENGTH_LONG).show();
			Log.d(DEBUG_TAG, e.getLocalizedMessage());
		}
		finally {
			if(reader != null) {
				try {
					reader.close();
				}
				catch(Exception e) {

				}
			}
		}

		String[] buffer = new String[container.size()];
		return container.toArray(buffer);
	}


	public void closeFile(int arrayIndex) {
		BufferedWriter writer = writerArray[arrayIndex];
		if(writer != null) {
			try {
				writer.flush();
				writer.close();
			}
			catch(Exception e) {
				Log.d(DEBUG_TAG, e.getLocalizedMessage());
			}
			writerArray[arrayIndex] = null;
			logFiles[arrayIndex] = null;
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		if(writerArray != null) {
			for(int i = 0;i < writerArray.length;i++) {
				closeFile(i);
			}
		}
	}

}
