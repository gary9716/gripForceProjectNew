package com.mhci.gripandtipforce.activity;

import android.os.Bundle;
import android.util.Log;

import com.mhci.gripandtipforce.model.ProjectConfig;
import com.mhci.gripandtipforce.R;

public class UnderlinesStyledFragmentActivity extends CustomizedBaseFragmentActivity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getIntent().getBooleanExtra("EXIT", false)) {
            showSystemBar();
            finish();
            return;
        }
        else {
            hideSystemBar();
        }

        setContentView(R.layout.activity_swipableview);
        
    }

}