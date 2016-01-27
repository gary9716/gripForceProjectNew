package com.mhci.gripandtipforce.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mhci.gripandtipforce.view.activity.InputDataActivity;
import com.mhci.gripandtipforce.R;

public final class ViewPagerFragment extends Fragment {
    private static final String debugTag = "testViewPager";

//    private static final String KEY_CONTENT = "ViewPagerFragment:Content";
//    private static final String KEY_PAGEINDEX = "ViewPagerFragment:PageIndex";
//    private static final String KEY_SHOWBUTTON = "ViewPagerFragment:ShowButton";

    private static final String[] contentStrings = new String[] {
    		"歡迎使用本系統",
    		"此評量需約30分鐘，請確認你處於舒適的坐姿且不被干擾",
    		"評量時，需使用我們提供的筆將指定的文字抄寫至空格中",
    		"準備好了，請按開始"
    };
    
    public static ViewPagerFragment newInstance(int pageIndex) {
        Log.d(debugTag, "create new instance,page:" + pageIndex);

        ViewPagerFragment fragment = new ViewPagerFragment();
        fragment.mContent = contentStrings[pageIndex];
        fragment.mPageIndex = pageIndex;

        return fragment;
    }

    //model
    private String mContent = "???";
    public int mPageIndex = -1;

    //view
    private Context mContext = null;
    private Button mStartButton = null;
    private TextView explainText = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this.getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    		
        Log.d(debugTag, "onCreateView");

        View fragmentView = inflater.inflate(R.layout.fragment_viewpager, container, false);
        explainText = (TextView) fragmentView.findViewById(R.id.explanation_text);
        mStartButton = (Button)fragmentView.findViewById(R.id.startButton);

        if(mPageIndex == contentStrings.length - 1) {
            mStartButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    view.setClickable(false);
                    Intent intent = new Intent(mContext, InputDataActivity.class);
                    startActivity(intent);
                    ViewPagerFragment.this.getActivity().finish();
                }
            });

            mStartButton.setVisibility(View.VISIBLE);
        }
        else {
            mStartButton.setVisibility(View.GONE);
        }

        explainText.setText(mContent);

        return fragmentView;
    }

}
