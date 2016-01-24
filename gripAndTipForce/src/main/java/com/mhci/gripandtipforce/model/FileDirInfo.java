package com.mhci.gripandtipforce.model;

import java.io.File;

import android.util.Log;

public class FileDirInfo {
	private FileType mFileType = FileType.Log;
	private String mDirPath;
	private String mOtherInfo;
	private final static String debug_tag = FileDirInfo.class.getName();
	
	public FileDirInfo(FileType fileType, String dirPath, String otherInfo) {
		
		mFileType = fileType;
		mOtherInfo = otherInfo;
		if(dirPath == null) {
			mDirPath = ProjectConfig.defaultPaths[fileType.ordinal()];
		}
		else {
			mDirPath = dirPath;
		}
		
		//test existence, if not existed, try to create the whole path
		try {
			//Log.d(debug_tag, "dir path:" + mDirPath);
			File dir = new File(mDirPath);
			if(!dir.exists()) {
				//Log.d(debug_tag, "create dir");
				dir.mkdirs(); //make the whole path, including parent directory
			}
			//Log.d(debug_tag, "done creating dir");
		}
		catch(Exception e) {
			Log.d(debug_tag, e.getLocalizedMessage());
		}
		
	}
	
	public void setFileType(FileType fileType, boolean toUseDefaultDirPath) {
		mFileType = fileType;
		if(toUseDefaultDirPath) {
			mDirPath = ProjectConfig.defaultPaths[fileType.ordinal()];
		}
	}
	
	public void setDirPath(String dirPath) {
		mDirPath = dirPath;
	}
	
	public void setOtherInfo(String otherInfo) {
		mOtherInfo = otherInfo;
	}
	
	public FileType getFileType() {
		return mFileType;
	}
	
	public String getDirPath() {
		return mDirPath;
	}
	
	public String getOtherInfo() {
		return mOtherInfo;
	}

}
