package com.bing.sticklayout;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        setViewpagerAdapter();
    }

    private void initView() {
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = (ViewPager) findViewById(R.id.id_stickynavlayout_viewpager);
    }

    private void setViewpagerAdapter() {
        BasePagerAdapter mAdapter = new BasePagerAdapter(getSupportFragmentManager());
        ListViewFragment listViewFragment = ListViewFragment.newInstance();// 基本信息fragment
        ScrollViewFragment scrollViewFragment = ScrollViewFragment.newInstance();
        mAdapter.addFragment(listViewFragment, "ListView");
        mAdapter.addFragment(scrollViewFragment, "ScrollView");
        viewPager.setAdapter(mAdapter);
        tabLayout.post(new Runnable() {
            @Override
            public void run() {
                tabLayout.setupWithViewPager(viewPager);
            }
        });
    }
}
