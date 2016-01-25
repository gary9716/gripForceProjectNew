package com.mhci.gripandtipforce.view.activity;

import android.os.Bundle;

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