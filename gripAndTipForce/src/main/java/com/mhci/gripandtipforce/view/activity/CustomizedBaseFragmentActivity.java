package com.mhci.gripandtipforce.view.activity;

import com.mhci.gripandtipforce.R;
import com.mhci.gripandtipforce.model.ProjectConfig;
import com.mhci.gripandtipforce.model.utils.Utils;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class CustomizedBaseFragmentActivity extends FragmentActivity{
	
	private boolean isSystemBarShown = true;
	
	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
	}
	
	@Override
	public void onBackPressed() {
		Toast.makeText(this, "此鍵已被暫時關閉，要離開請按右上角的選單", Toast.LENGTH_SHORT).show();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.experiment_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menuitem_end_application:
				endTheAppAfterCertainDuration(0);
				break;
			case R.id.menuitem_show_bottom_bar:
				if(isSystemBarShown) {
					hideSystemBar();
				}
				else {
					showSystemBar();
				}
				break;
			default:
				return super.onOptionsItemSelected(item);
		}
		return true;
	}

	protected void endTheAppAfterCertainDuration(long delayMillis) {
		(new Handler(getMainLooper())).postDelayed(new Runnable() {
			@Override
			public void run() {
//				if(this.getClass().isInstance(UnderlinesStyledFragmentActivity.class)) {
//					finish();
//					return;
//				}

//				Intent intent = new Intent(getApplicationContext(), UnderlinesStyledFragmentActivity.class);
//				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//				intent.putExtra("EXIT", true);
//				startActivity(intent);
//				finish();

//				Intent intent = new Intent(Intent.ACTION_MAIN);
//				intent.addCategory(Intent.CATEGORY_HOME);
//				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//				startActivity(intent);
//				finish();

				finish();

			}
		}, delayMillis);
	}

	protected void showSystemBar() {
		if(!ProjectConfig.useSystemBarHideAndShow) {
			return;
		}
		String commandStr = "am startservice -n com.android.systemui/.SystemUIService";
		Utils.runAsRoot(commandStr);
		isSystemBarShown = true;
	}

	protected void hideSystemBar() {
		if(!ProjectConfig.useSystemBarHideAndShow) {
			return;
		}

		try {
			//REQUIRES ROOT
			Build.VERSION_CODES vc = new Build.VERSION_CODES();
			Build.VERSION vr = new Build.VERSION();
			String ProcID = "79"; //HONEYCOMB AND OLDER

			//v.RELEASE  //4.0.3
			if (vr.SDK_INT >= vc.ICE_CREAM_SANDWICH) {
				ProcID = "42"; //ICS AND NEWER
			}

			String commandStr = "service call activity " + 
				ProcID + " s16 com.android.systemui";
			Utils.runAsRoot(commandStr);
			isSystemBarShown = false;
		} catch (Exception e) {
			// something went wrong, deal with it here
			ProjectConfig.useSystemBarHideAndShow = false; //disable this feature
		}
		
	}

	protected static DisplayMetrics mMetrics = new DisplayMetrics();

}
