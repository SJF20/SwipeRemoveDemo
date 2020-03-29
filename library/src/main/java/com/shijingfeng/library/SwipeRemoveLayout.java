package com.shijingfeng.library;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * function: 侧滑删除布局
 * date:     2019年7月27日
 * author:   石景峰
 */
public class SwipeRemoveLayout extends ViewGroup {

    /** 向左滑动 */
    private static final int LEFT = 0;
    /** 向右滑动 */
    private static final int RIGHT = 1;
    /** 关闭动画时长（毫秒值）*/
    private static final int CLOSE_DURATION = 300;
    /** 展开动画时长（毫秒值）*/
    private static final int EXPAND_DURATION = 300;
    /** 在此时间段内（毫秒值） 速度采集分析 */
    private static final int SPEED_ANALYZE_DURATION = 1000;
    /** 最大滑动速度（像素点数量/毫秒值） */
    private static final int MAX_SWIPE_SPEED = 1000;

    /** 当前展开的SwipeRemoveLayout */
    @SuppressLint("StaticFieldLeak")
    private static SwipeRemoveLayout sViewCache;
    /** 是否已经触摸过了（用于多点触摸情况）*/
    private static boolean mIsTouching;

    private Context mContext;
    /** 内容View */
    private View mContentView;
    /** ACTION_DOWN 记录坐标（屏幕坐标系） */
    private PointF mDownPointF = new PointF();
    /** ACTION_DOWN ACTION_MOVE 记录坐标（屏幕坐标系）*/
    private PointF mMovePointF = new PointF();
    /** 滑动速度采集器 */
    private VelocityTracker mVelocityTracker;
    /** 滑动伸展动画 */
    private ValueAnimator mExpandAnim;
    /** 滑动收缩动画 */
    private ValueAnimator mCloseAnim;
    /** 滑动菜单宽度 */
    private int mMenuWidth;
    /** 布局高度 */
    private int mHeight;
    /** 当滑动大于mLimitWidth, 则展开，否则收缩 */
    private int mLimitWidth;
    /** 系统最小滑动距离 */
    private int mScaledTouchSlop;
    /** 系统最大滑动速度 */
    private int mScaledMaximumFlingVelocity;
    /** 多点触摸的某一点ID，用于速度分析采集（默认多点触摸只算第一根手指的速度）*/
    private int mPointerId;
    /** 滑动方向 默认 {@value LEFT}*/
    private int mSwipeDirection;
    /** 是否开启滑动 默认开启 */
    private boolean mSwipeEnable;
    /** 侧滑菜单是否展开? true: 展开  false: 没有展开 */
    private boolean mIsExpanded;
    /** 用户是否滑动了?  true: 滑动了  false: 没有滑动 */
    private boolean mIsUserSwiped;
    /** 是否滑动了？(用于在OnInterceptTouchEvent中做判断)  true: 滑动了  false: 没有滑动 (点击事件或长按事件) */
    private boolean mIsMoved;

    public SwipeRemoveLayout(Context context) {
        this(context, null);
    }

    public SwipeRemoveLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeRemoveLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * 初始化
     */
    private void init(Context context, AttributeSet attrs) {
        mContext = context;
        mScaledTouchSlop = ViewConfiguration.get(mContext).getScaledTouchSlop();
        mScaledMaximumFlingVelocity = ViewConfiguration.get(mContext).getScaledMaximumFlingVelocity();

        TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.SwipeRemoveLayout);
        mSwipeEnable = typedArray.getBoolean(R.styleable.SwipeRemoveLayout_swipeEnable, true);
        mSwipeDirection = typedArray.getInt(R.styleable.SwipeRemoveLayout_swipeDirection, LEFT);
        typedArray.recycle();
    }

    /**
     * 获取速度采集器
     * @param event 触摸事件
     */
    private void acquireVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
    }

    /**
     * 销毁释放速度采集器
     */
    private void releaseVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    /**
     * 使用动画特效关闭
     */
    private void smoothClose() {
        sViewCache = null;

        cancelAnim();

        mCloseAnim = ValueAnimator.ofInt(getScrollX(), 0);
        mCloseAnim.setInterpolator(new AccelerateInterpolator());
        mCloseAnim.setDuration(CLOSE_DURATION);
        mCloseAnim.addUpdateListener(animation -> scrollTo((Integer) animation.getAnimatedValue(), 0));
        mCloseAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsExpanded = false;

                if (mContentView != null) {
                    //内容View 设置可长按
                    mContentView.setLongClickable(true);
                }
            }
        });
        mCloseAnim.start();
    }

    /**
     * 使用动画特效展开
     */
    private void smoothExpand() {

        sViewCache = SwipeRemoveLayout.this;

        cancelAnim();

        if (mContentView != null) {
            //内容View 设置不可长按
            mContentView.setLongClickable(false);
        }

        mExpandAnim = ValueAnimator.ofInt(getScrollX(), mSwipeDirection == LEFT ? mMenuWidth : -mMenuWidth);
        mExpandAnim.setInterpolator(new OvershootInterpolator());
        mExpandAnim.setDuration(EXPAND_DURATION);
        mExpandAnim.addUpdateListener(animation -> scrollTo((Integer) animation.getAnimatedValue(), 0));
        mExpandAnim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIsExpanded = true;
            }
        });
        mExpandAnim.start();
    }

    /**
     * 取消动画
     */
    private void cancelAnim() {
        //取消关闭动画
        if (mCloseAnim != null && mCloseAnim.isStarted()) {
            mCloseAnim.cancel();
        }
        //取消伸展动画
        if (mExpandAnim != null && mExpandAnim.isStarted()) {
            mExpandAnim.cancel();
        }
    }

    /**
     * 强制统一侧滑栏高度
     * @param childCount       侧滑栏View数量
     * @param widthMeasureSpec 本ViewGroup的widthMeasureSpec;
     */
    private void forceUniformHeight(int childCount, final int widthMeasureSpec) {
        //手动构建一个父ViewGroup的HeightMeasureSpec
        final int heightMeasureSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY);

        for (int i = 0; i < childCount; ++i) {
            final View childView = getChildAt(i);

            if (childView.getVisibility() != GONE) {
                final MarginLayoutParams lp = (MarginLayoutParams) childView.getLayoutParams();

                if (lp.height == MATCH_PARENT) {
//                    final int oldWidth = lp.width;
//                    lp.width = childView.getMeasuredWidth();
                    measureChildWithMargins(childView, widthMeasureSpec, 0, heightMeasureSpec, 0);
//                    lp.width = oldWidth;
                }
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        //设置自己可点击，获取触摸事件
        setClickable(true);

        //解决ListView或RecyclerView复用问题
        mMenuWidth = 0;
        mHeight = 0;

        final int childCount = getChildCount();
        final boolean parentWidthNotExactly = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        int contentViewWidth = 0;
        boolean isNeedMeasureChildHeight = false;

        for (int i = 0; i < childCount; ++i) {
            final View childView = getChildAt(i);

            //设置自己可点击，获取触摸事件
            childView.setClickable(true);

            if (childView.getVisibility() != GONE) {
                //加入上滑、下滑，则将不再支持Item的margin, 应使用measureChild()方法
                measureChildWithMargins(childView, widthMeasureSpec, 0, heightMeasureSpec, 0);

                final MarginLayoutParams childLp = (MarginLayoutParams) childView.getLayoutParams();
                final int leftMargin = childLp.leftMargin;
                final int rightMargin = childLp.rightMargin;
                final int topMargin = childLp.topMargin;
                final int bottomMargin = childLp.bottomMargin;

                mHeight = Math.max(mHeight, childView.getMeasuredHeight());

                if (parentWidthNotExactly && childLp.height == MATCH_PARENT) {
                    isNeedMeasureChildHeight = true;
                }

                if (i == 0) {
                    //内容View
                    mContentView = childView;
//                    contentViewWidth = childView.getMeasuredWidth() + leftMargin + rightMargin;
                    contentViewWidth = childView.getMeasuredWidth();
                } else {
                    //侧滑菜单
//                    mMenuWidth += childView.getMeasuredWidth() + leftMargin + rightMargin;
                    mMenuWidth += childView.getMeasuredWidth();
                }
            }
        }

        setMeasuredDimension(getPaddingLeft() + contentViewWidth + getPaddingRight(), getPaddingTop() + mHeight + getPaddingBottom());

        mLimitWidth = mMenuWidth * 4 / 10;

        if (isNeedMeasureChildHeight) {
            forceUniformHeight(childCount, widthMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int childCount = getChildCount();
        int left = getPaddingLeft();
        int right = getPaddingLeft();

        for (int i = 0; i < childCount; ++i) {
            final View childView = getChildAt(i);

            if (childView.getVisibility() != GONE) {
                if (i == 0) {
                    //内容View
                    childView.layout(left, getPaddingTop(), left + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                    left = left + childView.getMeasuredWidth();
                } else {
                    //侧滑菜单
                    if (mSwipeDirection == LEFT) {
                        //向左滑
                        childView.layout(left, getPaddingTop(), left + childView.getMeasuredWidth(), getPaddingTop() + childView.getMeasuredHeight());
                        left = left + childView.getMeasuredWidth();
                    } else if (mSwipeDirection == RIGHT){
                        //向右滑
                        childView.layout(right - childView.getMeasuredWidth(), getPaddingTop(), right, getPaddingTop() + childView.getMeasuredHeight());
                        right = right - childView.getMeasuredWidth();
                    }
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    /** tan = y/x */
    final double tan20 = 0.3639702342662F;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mSwipeEnable) {
            acquireVelocityTracker(ev);

            switch (ev.getAction()) {
                case ACTION_DOWN:
                    if (mIsTouching) {
                        //如果有了一个触摸点，则其他触摸点对本View无效
                        return false;
                    } else {
                        mIsTouching = true;
                    }
                    mIsMoved = false;
                    mIsUserSwiped = false;

                    mDownPointF.set(ev.getRawX(), ev.getRawY());
                    mMovePointF.set(ev.getRawX(), ev.getRawY());

                    if (sViewCache != null) {
                        if (sViewCache != this) {
                            sViewCache.smoothClose();
                        }
                        //只要有一个侧滑菜单处于打开状态， 就不给外层布局上下滑动了
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    mPointerId = ev.getPointerId(0);
                    break;
                case ACTION_MOVE:
                    final float scrollX = mMovePointF.x - ev.getRawX();
                    final float scrollY = mMovePointF.y - ev.getRawY();

                    if (Math.abs(scrollX) > mScaledTouchSlop || Math.abs(getScrollX()) > mScaledTouchSlop) {
                        getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    final float ratio = Math.abs(scrollY) / Math.abs(scrollX);

                    if (ratio < tan20) {
                        if (Math.abs(scrollX) > mScaledTouchSlop) {
                            mIsMoved = true;
                        }

                        scrollBy((int) scrollX, 0);
                    }

                    //越界修正
                    if (mSwipeDirection == LEFT) {
                        if (getScrollX() < 0) {
                            scrollTo(0, 0);
                        } else if (getScrollX() > mMenuWidth) {
                            scrollTo(mMenuWidth, 0);
                        }
                    } else if (mSwipeDirection == RIGHT) {
                        if (getScrollX() > 0) {
                            scrollTo(0, 0);
                        } else if (getScrollX() < - mMenuWidth) {
                            scrollTo(- mMenuWidth, 0);
                        }
                    }

                    mMovePointF.set(ev.getRawX(), ev.getRawY());
                    break;
                case ACTION_UP:
                case ACTION_CANCEL:
                    //判断用户是否滑动了，如果滑动了，则屏蔽一切点击事件
                    if (Math.abs(ev.getRawX() - mDownPointF.x) > mScaledTouchSlop) {
                        mIsUserSwiped = true;
                    }

                    mVelocityTracker.computeCurrentVelocity(SPEED_ANALYZE_DURATION, mScaledMaximumFlingVelocity);

                    final float velocityX = mVelocityTracker.getXVelocity(mPointerId);

                    if (Math.abs(velocityX) > MAX_SWIPE_SPEED) {
                        //超过滑动速度阈值
                        if (velocityX > 0) {
                            //向右滑动
                            if (mSwipeDirection == LEFT) {
                                //关闭侧滑菜单
                                smoothClose();
                            } else if (mSwipeDirection == RIGHT) {
                                if (mIsMoved) {
                                    //伸展侧滑菜单
                                    smoothExpand();
                                } else {
                                    //关闭侧滑菜单
                                    smoothClose();
                                }
                            }
                        } else {
                            //向左滑动
                            if (mSwipeDirection == LEFT) {
                                if (mIsMoved) {
                                    //伸展侧滑菜单
                                    smoothExpand();
                                } else {
                                    //关闭侧滑菜单
                                    smoothClose();
                                }
                            } else if (mSwipeDirection == RIGHT) {
                                //关闭侧滑菜单
                                smoothClose();
                            }
                        }
                    } else {
                        //没有超过滑动速度阈值
                        if (Math.abs(getScrollX()) > mLimitWidth) {
                            if (mIsMoved) {
                                //伸展侧滑菜单
                                smoothExpand();
                            } else {
                                //关闭侧滑菜单
                                smoothClose();
                            }
                        } else {
                            //关闭侧滑菜单
                            smoothClose();
                        }
                    }
                    releaseVelocityTracker();
                    mIsTouching = false;
                    break;
                default:
                    break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mSwipeEnable) {
            switch (ev.getAction()) {
                case ACTION_MOVE:
                    if (Math.abs(mDownPointF.x - ev.getRawX()) > mScaledTouchSlop) {
                        return true;
                    }
                    break;
                case ACTION_UP:
                case ACTION_CANCEL:
                    if (mSwipeDirection == LEFT) {
                        if (getScrollX() > mScaledTouchSlop && ev.getRawX() < (getWidth() - getScrollX())) {
                            smoothClose();
                            return true;
                        }
                    } else if (mSwipeDirection == RIGHT) {
                        if (getScrollX() > mScaledTouchSlop && ev.getRawX() > - getScrollX()) {
                            smoothClose();
                            return true;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CustomLayoutParams(mContext, attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new CustomLayoutParams(p);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new CustomLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    /**
     * 外部调用关闭侧滑菜单
     */
    public void close() {
        if (sViewCache == this) {
            smoothClose();
        }
    }

    @Override
    public boolean performLongClick() {
        if (Math.abs(getScaleX()) > mScaledTouchSlop) {
            return false;
        }
        return super.performLongClick();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (sViewCache == this) {
            smoothClose();
        }
    }

    private static class CustomLayoutParams extends MarginLayoutParams {

        private CustomLayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        private CustomLayoutParams(int width, int height) {
            super(width, height);
        }

        private CustomLayoutParams(LayoutParams source) {
            super(source);
        }
    }

}