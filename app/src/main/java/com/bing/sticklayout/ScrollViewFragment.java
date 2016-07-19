package com.bing.sticklayout;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by yhqz on 2016/6/1.
 */
public class ScrollViewFragment extends BaseFragment{
    public static ScrollViewFragment newInstance() {
        Bundle args = new Bundle();
        ScrollViewFragment fragment = new ScrollViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scroll_view, container, false);
    }

    @Override
    public String getTitle() {
        return " ScrollView";
    }
}
