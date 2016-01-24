package com.mhci.gripandtipforce;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class TaskRunnerAndDisplayProgressDialogAsyncTask extends AsyncTask<Void, Integer, Void> {
	public final static String startAsyncTask = TaskRunnerAndDisplayProgressDialogAsyncTask.class.getName() + ".start";
	public final static String stopAsyncTask = TaskRunnerAndDisplayProgressDialogAsyncTask.class.getName() + ".stop";
	public final static String Key_title = "Title";
	public final static String Key_msg = "Msg";

	private Context mContext = null;
	private ProgressDialog progressDialog;
	private Runnable[] mBGTaskList;
	private Runnable mPostTaskAfterBGTask;
	private final static String defaultDialogTitle = "處理中...";
	private final static String defaultDialogMessage = "系統忙碌中,請稍候";
	private String userDefinedTitle = null;
	private String userDefinedMsg = null;

	public TaskRunnerAndDisplayProgressDialogAsyncTask(Context context,Runnable[] bgTaskList,Runnable postTaskAfterBGTask) {
		mContext = context;
		mBGTaskList = bgTaskList;
		mPostTaskAfterBGTask = postTaskAfterBGTask;
	}

	public TaskRunnerAndDisplayProgressDialogAsyncTask(Context context,Runnable taskToRunInBG,Runnable postTaskAfterBGTask) {
		mContext = context;
		mBGTaskList = new Runnable[]{taskToRunInBG};
		mPostTaskAfterBGTask = postTaskAfterBGTask;	
	}
	
	public TaskRunnerAndDisplayProgressDialogAsyncTask(Context context,Runnable taskToRunInBG,Runnable postTaskAfterBGTask,String title,String msg) {
		mContext = context;
		mBGTaskList = new Runnable[]{taskToRunInBG};
		mPostTaskAfterBGTask = postTaskAfterBGTask;
		userDefinedMsg = msg;
		userDefinedTitle = title;
	}
	
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		String msg;
		if(userDefinedMsg != null) {
			msg = userDefinedMsg;
		}
		else {
			msg = defaultDialogMessage;
		}
		String title;
		if(userDefinedTitle != null) {
			title = userDefinedTitle;
		}
		else {
			title = defaultDialogTitle;
		}

		//this should be called after content view set
		//so this line should be called after "setContentView" this function
		try {
            progressDialog = ProgressDialog.show(mContext, title, msg, true, false); //indeterminate , cancellable
        }
        catch(Exception e) {
            progressDialog = null;
        }
	}
	
	@Override
	protected Void doInBackground(Void... arg0) {
		if(mBGTaskList != null) {
			for(Runnable task : mBGTaskList) {
				task.run();
			}
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		if(mPostTaskAfterBGTask != null) {
			mPostTaskAfterBGTask.run();
		}
        if(progressDialog != null) {
            progressDialog.dismiss();
        }
	}

}
