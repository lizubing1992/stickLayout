package com.bing.sticklayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.OverScroller;
import android.widget.ScrollView;

/**
 * 修改bug,增加点击Fling停止，以及在顶部不隐藏是下拉事件的交由子控件处理
 * 上滑悬停控件，底部内容区域支持 ScrollView ，ListView
 */
public class StickyNavLayout extends LinearLayout {
    private View mTop;
    private View mNav;
    private ViewPager mViewPager;
    private int mTopViewHeight;
    private ViewGroup mInnerScrollView;
    private boolean isTopHidden = false;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private int mTouchSlop;
    private int mMaximumVelocity, mMinimumVelocity;
    private float mLastY;
    private boolean mDragging;
    private boolean isStickNav;
    private boolean isInControl = false;
    //悬浮框距离顶部的距离
    private int stickOffset;
    private int mViewPagerMaxHeight;
    private int mTopViewMaxHeight;
    public StickyNavLayout(Context context) {
        this(context, null);
    }

    public StickyNavLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyNavLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(LinearLayout.VERTICAL);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.StickNavLayout);
        isStickNav = a.getBoolean(R.styleable.StickNavLayout_isStickNav, false);
        stickOffset = a.getDimensionPixelSize(R.styleable.StickNavLayout_stickOffset, 0);
        a.recycle();

        mScroller = new OverScroller(context);
        mVelocityTracker = VelocityTracker.obtain();
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context)
                .getScaledMinimumFlingVelocity();
    }

    public void setisStickNav(boolean isStickNav) {
        this.isStickNav = isStickNav;
    }

    /****
     * 设置顶部区域的高度
     *
     * @param height height
     */
    public void setTopViewHeight(int height) {
        mTopViewHeight = height;
        if (isStickNav)
            scrollTo(0, mTopViewHeight);
    }

    /****
     * 设置顶部区域的高度
     *
     * @param height height
     * @param offset offset
     */
    public void setTopViewHeight(int height, int offset) {
        mTopViewHeight = height;
        if (isStickNav)
            scrollTo(0, mTopViewHeight - offset);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTop = findViewById(R.id.id_stickynavlayout_topview);
        mNav = findViewById(R.id.id_stickynavlayout_indicator);
        View view = findViewById(R.id.id_stickynavlayout_viewpager);
        if (!(view instanceof ViewPager)) {
            throw new RuntimeException(
                    "id_stickynavlayout_viewpager show used by ViewPager !");
        } else if (mTop instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) mTop;
            if (viewGroup.getChildCount() >= 2) {
                throw new RuntimeException(
                        "if the TopView(android:id=\"R.id.id_stickynavlayout_topview\") is a ViewGroup(ScrollView,LinearLayout,FrameLayout, ....) ,the children count should be one  !");
            }
        }
        mViewPager = (ViewPager) view;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewGroup.LayoutParams params = mViewPager.getLayoutParams();
        //修复键盘弹出后键盘关闭布局高度不对问题
        int height = getMeasuredHeight() - mNav.getMeasuredHeight();
        mViewPagerMaxHeight = (height >= mViewPagerMaxHeight ? height : mViewPagerMaxHeight);
        params.height = mViewPagerMaxHeight - stickOffset;
        //修复键盘弹出后Top高度不对问题
        ViewGroup.LayoutParams topParams = mTop.getLayoutParams();
        int topHeight = mTop.getMeasuredHeight();
        mTopViewMaxHeight = (topHeight >= mTopViewMaxHeight ? topHeight : mTopViewMaxHeight);
        topParams.height = mTopViewMaxHeight;
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        final ViewGroup.LayoutParams params = mTop.getLayoutParams();
        mTop.post(new Runnable() {
            @Override
            public void run() {
                if (mTop instanceof ViewGroup) {
                    ViewGroup viewGroup = (ViewGroup) mTop;
                    int height = viewGroup.getChildAt(0).getHeight();
                    mTopViewHeight = height - stickOffset;
                    params.height = height;
                    mTop.setLayoutParams(params);
                    mTop.requestLayout();
                } else {
                    mTopViewHeight = mTop.getMeasuredHeight() - stickOffset;
                }
//                LogUtils.e("mTopViewHeight" + mTopViewHeight);
                if (null != mInnerScrollView) {
//                    LogUtils.e("mInnerScrollViewHeight" + mInnerScrollView.getMeasuredHeight());
                }
                if (isStickNav) {
                    scrollTo(0, mTopViewHeight);
                }
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                getCurrentScrollView();

                if (mInnerScrollView instanceof ScrollView) {
                    if (mInnerScrollView.getScrollY() == 0 && isTopHidden && dy > 0
                            && !isInControl) {
//                        LogUtils.d("dispatchTouchEvent ** ScrollViewACTION_MOVE"+isInControl);
                        isInControl = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        MotionEvent ev2 = MotionEvent.obtain(ev);
                        dispatchTouchEvent(ev);
                        ev2.setAction(MotionEvent.ACTION_DOWN);
                        isSticky = true;
                        return dispatchTouchEvent(ev2);
                    }
                } else if (mInnerScrollView instanceof ListView) {

                    ListView lv = (ListView) mInnerScrollView;
                    View c = lv.getChildAt(lv.getFirstVisiblePosition());

                    if (!isInControl && c != null && c.getTop() == 0 && isTopHidden
                            && dy > 0) {
                        isInControl = true;
                        ev.setAction(MotionEvent.ACTION_CANCEL);
                        MotionEvent ev2 = MotionEvent.obtain(ev);
                        dispatchTouchEvent(ev);
                        ev2.setAction(MotionEvent.ACTION_DOWN);
                        isSticky = true;
                        return dispatchTouchEvent(ev2);
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP://处理悬停后立刻抬起的处理
                float distance = y - mLastY;
                if (isSticky && /*distance==0.0f*/Math.abs(distance) <= mTouchSlop) {
                    isSticky = false;
                    return true;
                } else {
                    isSticky = false;
                    return super.dispatchTouchEvent(ev);
                }
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean isSticky;//mNav-view 是否悬停的标志

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;

                getCurrentScrollView();

                if (Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                    if (mInnerScrollView instanceof ScrollView) {
                        if (!isTopHidden) {
                            if(mInnerScrollView.getScrollY() >0){
                                initVelocityTrackerIfNotExists();
                                mVelocityTracker.addMovement(ev);
                                mLastY = y;
                                if(dy>0){
                                    //当头部显示并且是下拉状态时不拦截，事件交给子控件中的ScrollView
                                    return false;
                                }else{
                                    //当头部显示并且是上拉状态时拦截，事件交给本身处理
                                    return true;
                                }
                            }
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;

                        }else{
                            if((mInnerScrollView.getScrollY() == 0 && dy > 0)){
                                initVelocityTrackerIfNotExists();
                                mVelocityTracker.addMovement(ev);
                                mLastY = y;
                                return true;
                            }
                        }
                    } else if (mInnerScrollView instanceof ListView) {
                        ListView lv = (ListView) mInnerScrollView;
                        View c = lv.getChildAt(lv.getFirstVisiblePosition());
                        int scrollLength =  getScrollY(lv);
                        if (!isTopHidden) {
                            if( scrollLength > 0){
                                initVelocityTrackerIfNotExists();
                                mVelocityTracker.addMovement(ev);
                                mLastY = y;
                                if(dy>0){
                                    //当头部显示并且是下拉状态时不拦截，事件交给子控件中的ScrollView
                                    return false;
                                }else{
                                    //当头部显示并且是上拉状态时拦截，事件交给本身处理
                                    return true;
                                }
                            }
                            initVelocityTrackerIfNotExists();
                            mVelocityTracker.addMovement(ev);
                            mLastY = y;
                            return true;
                        }else{
                            if((c != null && c.getTop() == 0  && dy > 0)){
                                initVelocityTrackerIfNotExists();
                                mVelocityTracker.addMovement(ev);
                                mLastY = y;
                                return true;
                            }
                            if (lv.getAdapter() != null && lv.getAdapter().getCount() == 0) {
                                //当ListView或ScrollView 没有数据为空时
                                initVelocityTrackerIfNotExists();
                                mVelocityTracker.addMovement(ev);
                                mLastY = y;
                                return true;
                            }
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                mDragging = false;
                recycleVelocityTracker();
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }
    public int getScrollY(ListView mListView) {
        View c = mListView.getChildAt(0);
        if (c == null ) {
            return 0;
        }
        int firstVisiblePosition = mListView.getFirstVisiblePosition();
        int top = c.getTop();
        return -top + firstVisiblePosition * c.getHeight() ;
    }


    private void getCurrentScrollView() {
        int currentItem = mViewPager.getCurrentItem();
        PagerAdapter a = mViewPager.getAdapter();
        if (a instanceof FragmentPagerAdapter) {
            FragmentPagerAdapter fadapter = (FragmentPagerAdapter) a;
            Fragment item = fadapter.getItem(currentItem);
            View v = item.getView();
            if (v != null) {
                mInnerScrollView = (ViewGroup) (v
                        .findViewById(R.id.id_stickynavlayout_innerscrollview));
            }
        } else if (a instanceof FragmentStatePagerAdapter) {
            FragmentStatePagerAdapter fsAdapter = (FragmentStatePagerAdapter) a;
            Fragment item = fsAdapter.getItem(currentItem);
            View v = item.getView();
            if (v != null) {
                mInnerScrollView = (ViewGroup) (v
                        .findViewById(R.id.id_stickynavlayout_innerscrollview));
            }
        } else {
            throw new RuntimeException(
                    "mViewPager  should be  used  FragmentPagerAdapter or  FragmentStatePagerAdapter  !");
        }
        //...
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        int action = event.getAction();
        float y = event.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mLastY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                float dy = y - mLastY;
                if (!mDragging && Math.abs(dy) > mTouchSlop) {
                    mDragging = true;
                }
                if (mDragging) {
                    scrollBy(0, (int) -dy);
                    // 如果topView隐藏，且上滑动时，则改变当前事件为ACTION_DOWN
                    if (getScrollY() == mTopViewHeight && dy < 0) {
                        event.setAction(MotionEvent.ACTION_DOWN);
                        dispatchTouchEvent(event);
                        isInControl = false;
                        isSticky = true;
                    } else {
                        isSticky = false;
                    }
                }
                mLastY = y;
                break;
            case MotionEvent.ACTION_CANCEL:
                mDragging = false;
                recycleVelocityTracker();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                mDragging = false;
                mVelocityTracker.computeCurrentVelocity(500, mMaximumVelocity);
                int velocityY = (int) mVelocityTracker.getYVelocity();
                if (Math.abs(velocityY) > mMinimumVelocity) {
                    fling(-velocityY);
                }
                recycleVelocityTracker();
                break;
        }
        return super.onTouchEvent(event);
    }

    public void fling(int velocityY) {
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mTopViewHeight);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y) {
        if (y < 0) {
            y = 0;
        }
        if (y > mTopViewHeight) {
            y = mTopViewHeight;
        }
        if (y != getScrollY()) {
            super.scrollTo(x, y);
        }

        isTopHidden = getScrollY() == mTopViewHeight;


        //set  listener 设置悬浮监听回调
        if (listener != null) {
//            if(lastIsTopHidden!=isTopHidden){
//                lastIsTopHidden=isTopHidden;
            listener.isStick(isTopHidden);
//            }
            listener.scrollPercent((float) getScrollY() / (float) mTopViewHeight);
        }
    }
//    private  boolean lastIsTopHidden;//记录上次是否悬浮

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            postInvalidate();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private onStickStateChangeListener listener;

    /**
     * 悬浮状态回调
     */
    public interface onStickStateChangeListener {
        /**
         * 是否悬浮的回调
         *
         * @param isStick true 悬浮 ,false 没有悬浮
         */
        void isStick(boolean isStick);

        /**
         * 距离悬浮的距离的百分比
         *
         * @param percent 0~1(向上) or 1~0(向下) 的浮点数
         */
        void scrollPercent(float percent);
    }

    public void setOnStickStateChangeListener(onStickStateChangeListener listener) {
        this.listener = listener;
    }

}
