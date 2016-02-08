package org.votingsystem.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import org.votingsystem.android.R;

import java.util.ArrayList;
import java.util.List;

import static org.votingsystem.util.LogUtils.LOGD;

/**
 * https://github.com/xyxyLiu/PatternLockView
 *
 * Copyright 2016 xyxyLiu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
public class PatternLockView  extends ViewGroup {

    public static final int CODE_PASSWORD_CORRECT = 1;
    public static final int CODE_PASSWORD_ERROR = 2;

    private static final String TAG = PatternLockView.class.getSimpleName();

    private boolean mIsTouchEnabled = true;
    private long mFinishTimeout = 1000;
    private boolean mIsFinishInterruptable = true;
    private boolean mIsAutoLink;

    private List<NodeView> mNodeList = new ArrayList<>();
    private NodeView currentNode;
    private float mPositionX;
    private float mPositionY;

    private Drawable mNodeSrc;
    private Drawable mNodeHighlightSrc;
    private Drawable mNodeCorrectSrc;
    private Drawable mNodeErrorSrc;

    private int mSize;

    private float mNodeAreaExpand;
    private int mNodeOnAnim;
    private int mLineColor;
    private float mLineWidth;

    private float mNodeSize;
    private boolean mIsSquareArea = true;
    private float mPadding;
    private float mSpacing;
    private float mMeasuredPadding;
    private float mMeasuredSpacing;


    private Vibrator mVibrator;
    private boolean mEnableVibrate;
    private int mVibrateTime;

    private Paint mPaint;

    private CallBack mCallBack;

    private OnNodeTouchListener mOnNodeTouchListener;

    private Runnable mFinishAction = new Runnable() {
        @Override
        public void run() {
            reset();
            setTouchEnabled(true);
        }
    };

    public PatternLockView(Context context) {
        super(context);
        initFromAttributes(context, null, 0);
    }

    public PatternLockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFromAttributes(context, attrs, 0);
    }

    public PatternLockView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initFromAttributes(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PatternLockView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initFromAttributes(context, attrs, defStyleAttr);
    }

    public void setCallBack(CallBack callBack) {
        this.mCallBack = callBack;
    }

    public void setOnNodeTouchListener(OnNodeTouchListener callBack) {
        this.mOnNodeTouchListener = callBack;
    }

    public void setTouchEnabled(boolean isEnabled) {
        mIsTouchEnabled = isEnabled;
    }

    public void setFinishTimeout(long timeout) {
        if (timeout < 0)
            timeout = 0;
        mFinishTimeout = timeout;
    }

    public void setFinishInterruptable(boolean isInterruptable) {
        mIsFinishInterruptable = isInterruptable;
    }

    public void setIsMidCheckEnabled(boolean isEnabled) {
        mIsAutoLink = isEnabled;
    }

    private void initFromAttributes(Context context, AttributeSet attrs, int defStyleAttr) {
        final TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PatternLockView, defStyleAttr, 0);

        mSize = a.getInt(R.styleable.PatternLockView_lock_size, 3);
        mNodeSrc = a.getDrawable(R.styleable.PatternLockView_lock_nodeSrc);
        mNodeHighlightSrc = a.getDrawable(R.styleable.PatternLockView_lock_nodeHighlightSrc);
        mNodeCorrectSrc = a.getDrawable(R.styleable.PatternLockView_lock_nodeCorrectSrc);
        mNodeErrorSrc = a.getDrawable(R.styleable.PatternLockView_lock_nodeErrorSrc);
        mNodeSize = a.getDimension(R.styleable.PatternLockView_lock_nodeSize, 0);
        mNodeAreaExpand = a.getDimension(R.styleable.PatternLockView_lock_nodeTouchExpand, 0);
        mNodeOnAnim = a.getResourceId(R.styleable.PatternLockView_lock_nodeOnAnim, 0);
        mLineColor = a.getColor(R.styleable.PatternLockView_lock_lineColor, Color.argb(0xb2, 0xff, 0xff, 0xff));
        mLineWidth = a.getDimension(R.styleable.PatternLockView_lock_lineWidth, (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
        mPadding = a.getDimension(R.styleable.PatternLockView_lock_padding, 0);
        mSpacing = a.getDimension(R.styleable.PatternLockView_lock_spacing, -1);
        mIsAutoLink = a.getBoolean(R.styleable.PatternLockView_lock_autoLink, false);

        mEnableVibrate = a.getBoolean(R.styleable.PatternLockView_lock_enableVibrate, false);
        mVibrateTime = a.getInt(R.styleable.PatternLockView_lock_vibrateTime, 20);

        a.recycle();
        if (mNodeSize <= 0) {
            throw new IllegalStateException("nodeSize must be provided and larger than zero!");
        }
        if (mEnableVibrate) {
            mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
        mPaint = new Paint(Paint.DITHER_FLAG);
        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(mLineWidth);
        mPaint.setColor(mLineColor);
        mPaint.setAntiAlias(true);

        for (int n = 0; n < mSize * mSize; n++) {
            NodeView node = new NodeView(getContext(), n);
            if (mNodeOnAnim != 0) {
                node.setNodeOnAnim(AnimationUtils.loadAnimation(getContext(), mNodeOnAnim));
            }
            addView(node);
        }
        setWillNotDraw(false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean needRemeasure = false;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int gaps = mSize - 1;
        float nodesize = mNodeSize;
        mMeasuredPadding = mPadding;
        mMeasuredSpacing = mSpacing;
        float maxNodeWidth, maxNodeHeight, maxNodeSize;
        LOGD(TAG, String.format("onMeasure(), raw width = %d, height = %d)", width, height));
        // Spacing&Padding mode:
        if (mSpacing >= 0) {
            maxNodeWidth = ((width - mPadding * 2 - mSpacing * gaps) / mSize);
            maxNodeHeight = ((height - mPadding * 2 - mSpacing * gaps) / mSize);
            maxNodeSize = maxNodeWidth < maxNodeHeight ? maxNodeWidth : maxNodeHeight;
            LOGD(TAG, String.format("maxNodeWidth = %f, maxNodeHeight = %f, maxNodeSize = %f)",
                    maxNodeWidth, maxNodeHeight, maxNodeSize));

            // if maximum available nodesize if smaller than desired nodesize with paddings & spacing unchanged
            if (nodesize > maxNodeSize) {
                int xRemains = (int) (width - mSize * nodesize);
                int yRemains = (int) (height - mSize * nodesize);
                int minRemains = xRemains < yRemains ? xRemains : yRemains;
                int paddingsAndSpacings = (int) (mPadding * 2 + mSpacing * gaps);

                LOGD(TAG, String.format("xRemains = %d, yRemains = %d, before shrink: mPadding = %f, mSpacing = %f",
                        xRemains, yRemains, mPadding, mSpacing));
                // keep nodesize & shrink paddings and spacing if there are enough space
                if (minRemains > 0 && paddingsAndSpacings > 0) {
                    float shrinkRatio = (float) minRemains / paddingsAndSpacings;
                    mMeasuredPadding *= shrinkRatio;
                    mMeasuredSpacing *= shrinkRatio;
                    LOGD(TAG, String.format("shrinkRatio = %f, mMeasuredPadding = %f, mMeasuredSpacing = %f",
                            shrinkRatio, mMeasuredPadding, mMeasuredSpacing));
                } else { // otherwise shrink nodesize & keep paddings and spacing
                    nodesize = maxNodeSize;
                }
            }
            
            // if result nodesize is smaller than zero, remeasure without using spacings.
            if (nodesize <= 0) {
                needRemeasure = true;
                // remeasure without using mSpacing
                LOGD(TAG, String.format("remeasure without using mSpacing"));
            }
        }

        // Identical-Area mode:
        // if no spacing is provided, divide the layout into 9 identical area for each node
        if (needRemeasure || mSpacing < 0) {
            mMeasuredSpacing = -1;
            nodesize = mNodeSize;
            maxNodeWidth = width / mSize;
            maxNodeHeight = height / mSize;
            maxNodeSize = maxNodeWidth < maxNodeHeight ? maxNodeWidth : maxNodeHeight;
            LOGD(TAG, String.format("maxNodeWidth = %f, maxNodeHeight = %f, maxNodeSize = %f)",
                    maxNodeWidth, maxNodeHeight, maxNodeSize));

            // if maximum available nodesize if smaller than desired nodesize
            if (nodesize > maxNodeSize) {
                nodesize = maxNodeSize;
            }
        }

        LOGD(TAG, String.format("measured nodeSize = %f)", nodesize));

        if (width > height && widthMode == MeasureSpec.AT_MOST) {
            width = height;
        } else if (width < height && heightMode == MeasureSpec.AT_MOST) {
            height = width;
        }

        setMeasuredDimension(width, height);

        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int widthSpec = MeasureSpec.makeMeasureSpec((int) nodesize, MeasureSpec.EXACTLY);
            int heightSpec = MeasureSpec.makeMeasureSpec((int) nodesize, MeasureSpec.EXACTLY);
            v.measure(widthSpec, heightSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int gaps = mSize - 1;
        int height = bottom - top;
        int width = right - left;
        float nodeSize = getChildAt(0).getMeasuredWidth();

        // Identical-Area mode:
        if (mMeasuredSpacing < 0) {
            float areaWidth = width / mSize;
            float areaHeight = height / mSize;
            float areaSize = areaWidth < areaHeight ? areaWidth : areaHeight;
            float widthPadding = 0f;
            float heightPadding = 0f;
            // whether to keep each cell as square (width = height)
            if (mIsSquareArea) {
                areaWidth = areaSize;
                areaHeight = areaSize;
                widthPadding = (width - mSize * areaSize) / 2;
                heightPadding = (height - mSize * areaSize) / 2;
            }
            LOGD(TAG, String.format("nodeSize = %f, areaWidth = %f, areaHeight = %f, widthPadding = %f, heightPadding = %f",
                    nodeSize, areaWidth, areaHeight, widthPadding, heightPadding));

            for (int n = 0; n < mSize * mSize; n++) {
                NodeView node = (NodeView) getChildAt(n);
                int row = n / mSize;
                int col = n % mSize;
                int l = (int) (widthPadding + col * areaWidth + (areaWidth - node.getMeasuredWidth()) / 2);
                int t = (int) (heightPadding + row * areaHeight + (areaHeight - node.getMeasuredHeight()) / 2);
                int r = (int) (l + node.getMeasuredWidth());
                int b = (int) (t + node.getMeasuredHeight());
                node.layout(l, t, r, b);
            }
        } else { // Spacing&Padding mode:
            float widthPadding = (width - mSize * nodeSize - mMeasuredSpacing * gaps) / 2;
            float heightPadding = (height - mSize * nodeSize - mMeasuredSpacing * gaps) / 2;
            LOGD(TAG, String.format("nodeSize = %f, widthPadding = %f, heightPadding = %f",
                    nodeSize, widthPadding, heightPadding));
            for (int n = 0; n < mSize * mSize; n++) {
                NodeView node = (NodeView) getChildAt(n);
                int row = n / mSize;
                int col = n % mSize;
                int l = (int) (widthPadding + col * (nodeSize + mMeasuredSpacing));
                int t = (int) (heightPadding + row * (nodeSize + mMeasuredSpacing));
                int r = (int) (l + nodeSize);
                int b = (int) (t + nodeSize);
                node.layout(l, t, r, b);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsTouchEnabled)
            return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mIsFinishInterruptable && mFinishAction != null) {
                    removeCallbacks(mFinishAction);
                    mFinishAction.run();
                }
            case MotionEvent.ACTION_MOVE:
                mPositionX = event.getX();
                mPositionY = event.getY();
                NodeView nodeAt = getNodeAt(mPositionX, mPositionY);

                if (currentNode == null) {
                    if (nodeAt != null) {
                        currentNode = nodeAt;
                        currentNode.setState(NodeView.STATE_HIGHLIGHT);
                        addNodeToList(currentNode);
                        invalidate();
                    }
                } else {
                    if (nodeAt != null && !nodeAt.isHighLighted()) {
                        if (mIsAutoLink) {
                            autoLinkNode(currentNode, nodeAt);
                        }
                        currentNode = nodeAt;
                        currentNode.setState(NodeView.STATE_HIGHLIGHT);
                        addNodeToList(currentNode);
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mNodeList.size() > 0) {

                    if (!mIsFinishInterruptable) {
                        setTouchEnabled(false);
                    }

                    if (mCallBack != null) {
                        int result = mCallBack.onFinish(new Password(mNodeList));
                        switch (result) {
                            case CODE_PASSWORD_CORRECT:
                                setFinishState(NodeView.STATE_CORRECT);
                                break;
                            case CODE_PASSWORD_ERROR:
                                setFinishState(NodeView.STATE_ERROR);
                                break;
                        }
                    }

                    currentNode = null;
                    invalidate();
                    postDelayed(mFinishAction, mFinishTimeout);

                }
                break;
        }
        return true;
    }

    private void setFinishState(int state) {
        for (NodeView nodeView : mNodeList) {
            nodeView.setState(state);
        }
    }

    private void addNodeToList(NodeView nodeView) {
        mNodeList.add(nodeView);
        if (mOnNodeTouchListener != null) {
            mOnNodeTouchListener.onNodeTouched(nodeView.getNodeId());
        }
    }

    public void reset() {
        mNodeList.clear();
        currentNode = null;

        for (int n = 0; n < getChildCount(); n++) {
            NodeView node = (NodeView) getChildAt(n);
            node.setState(NodeView.STATE_NORMAL);
        }

        mPaint.setStyle(Style.STROKE);
        mPaint.setStrokeWidth(mLineWidth);
        mPaint.setColor(mLineColor);
        mPaint.setAntiAlias(true);

        invalidate();
    }

    /**
     * auto link the nodes between first and second
     */
    private void autoLinkNode(NodeView first, NodeView second) {
        LOGD(TAG, String.format("autoLinkNode(%s, %s)", first, second));
        int xDiff = second.getColumn() - first.getColumn();
        int yDiff = second.getRow() - first.getRow();
        if (yDiff == 0 && xDiff == 0) {
            return;
        } else if (yDiff == 0) {
            int row = first.getRow();
            int step = xDiff > 0 ? 1 : -1;
            int column = first.getColumn();
            while ((column += step) != second.getColumn()) {
                tryAppendMidNode(row, column);
            }
        } else if (xDiff == 0) {
            int column = first.getColumn();
            int step = yDiff > 0 ? 1 : -1;
            int row = first.getRow();
            while ((row += step) != second.getRow()) {
                tryAppendMidNode(row, column);
            }
        } else {
            float tan = yDiff / (float) xDiff;
            int xstep = xDiff > 0 ? 1 : -1;
            LOGD(TAG, String.format("tan = %f, xstep = %d", tan, xstep));
            int xdiff = 0;
            float ydiff = 0f;
            while ((xdiff += xstep) != xDiff) {
                ydiff = xdiff * tan;
                LOGD(TAG, String.format("xdiff = %d, ydiff = %f, ydiff == (int)ydiff? %b", xdiff, ydiff, (ydiff == (int) ydiff)));
                if (ydiff == (int) ydiff) {
                    tryAppendMidNode(first.getRow() + (int) ydiff, first.getColumn() + xdiff);
                }
            }
        }
    }

    private void tryAppendMidNode(int row, int column) {
        LOGD(TAG, String.format("tryAppendMidNode(row = %d, column = %d)", row, column));
        NodeView mid = (NodeView) getChildAt(row * mSize + column);
        if (mNodeList.contains(mid))
            return;
        mid.setState(NodeView.STATE_HIGHLIGHT);
        addNodeToList(mid);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < mNodeList.size() - 1; i++) {
            NodeView first = mNodeList.get(i);
            NodeView second = mNodeList.get(i + 1);
            canvas.drawLine(first.getCenterX(), first.getCenterY(), second.getCenterX(), second.getCenterY(), mPaint);
        }
        if (currentNode != null) {
            canvas.drawLine(currentNode.getCenterX(), currentNode.getCenterY(), mPositionX, mPositionY, mPaint);
        }
    }

    private NodeView getNodeAt(float x, float y) {
        for (int n = 0; n < getChildCount(); n++) {
            NodeView node = (NodeView) getChildAt(n);
            if (!(x >= node.getLeft() - mNodeAreaExpand && x < node.getRight() + mNodeAreaExpand)) {
                continue;
            }
            if (!(y >= node.getTop() - mNodeAreaExpand && y < node.getBottom() + mNodeAreaExpand)) {
                continue;
            }
            return node;
        }
        return null;
    }

    /**
     * Callback to handle pattern input complete event
     */
    public interface CallBack {
        int onFinish(Password password);
    }

    /**
     * Callback to handle node touch event
     */
    public interface OnNodeTouchListener {
        void onNodeTouched(int NodeId);
    }


    private class NodeView extends View {

        public static final int STATE_NORMAL = 0;
        public static final int STATE_HIGHLIGHT = 1;
        public static final int STATE_CORRECT = 2;
        public static final int STATE_ERROR = 3;

        private int mId;
        private int mState = STATE_NORMAL;
        private Animation mNodeAnimation;

        public NodeView(Context context, int num) {
            super(context);
            this.mId = num;
            setBackgroundDrawable(mNodeSrc);
        }

        public void setNodeOnAnim(Animation animation) {
            mNodeAnimation = animation;
        }

        public boolean isHighLighted() {
            return mState == STATE_HIGHLIGHT;
        }

        public void setState(int state) {

            if (mState == state)
                return;

            switch (state) {

                case STATE_NORMAL:
                    setBackgroundDrawable(mNodeSrc);
                    clearAnimation();
                    break;
                case STATE_HIGHLIGHT:
                    if (mNodeHighlightSrc != null) {
                        setBackgroundDrawable(mNodeHighlightSrc);
                    }
                    if (mNodeAnimation != null) {
                        startAnimation(mNodeAnimation);
                    }

                    if (mEnableVibrate) {
                        mVibrator.vibrate(mVibrateTime);
                    }
                    break;
                case STATE_CORRECT:
                    if (mNodeCorrectSrc != null) {
                        setBackgroundDrawable(mNodeCorrectSrc);
                    }
                    break;
                case STATE_ERROR:
                    if (mNodeCorrectSrc != null) {
                        setBackgroundDrawable(mNodeErrorSrc);
                    }
                    break;
            }
            mState = state;
        }

        public int getCenterX() {
            return (getLeft() + getRight()) / 2;
        }

        public int getCenterY() {
            return (getTop() + getBottom()) / 2;
        }

        public int getNodeId() {
            return mId;
        }

        public int getRow() {
            return mId / mSize;
        }

        public int getColumn() {
            return mId % mSize;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof NodeView && mId == ((NodeView) obj).getNodeId()) {
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            return String.format("NodeView[mId = %d, row = %d, column = %d]", mId, getRow(), getColumn());
        }
    }

    public static class Password{
        public List<Integer> list;
        public String string;

        public Password(List<NodeView> nodeViewList){
            list = new ArrayList<>();
            StringBuilder passwordBuilder = new StringBuilder("[");
            for (int i = 0; i < nodeViewList.size(); i++) {
                int id = nodeViewList.get(i).getNodeId();
                list.add(id);
                if (i != 0) {
                    passwordBuilder.append("-");
                }
                passwordBuilder.append(id);

            }
            passwordBuilder.append("]");
            string = passwordBuilder.toString();
        }
    }

}