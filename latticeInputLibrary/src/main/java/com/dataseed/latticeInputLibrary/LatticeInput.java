package com.dataseed.latticeInputLibrary;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.text.InputFilter;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.EditText;

/**
 * Created by mikezhu on 6/28/17.
 */

public class LatticeInput extends EditText {
    private final static int DEFAULT_COUNT = 6; // digits
    private final static int DEFAULT_TEXT_SIZE = 36; // dp
    private final static int DEFAULT_ITEM_WIDTH = 50; // dp
    private final static int DEFAULT_ITEM_HEIGHT = 70; // dp
    private final static int DEFAULT_ITEM_LINE_WIDTH = 30; // dp

    private CharSequence content;
    private Paint textPaint;
    private Paint linePaint;
    private int defaultTextSize;
    private int itemDefaultWidth;
    private int itemDefaultHeight;
    private int itemDefaultLineWidth;
    private int itemWidth = DEFAULT_ITEM_WIDTH;
    private int itemHeight = DEFAULT_ITEM_HEIGHT;
    private int itemLineWidth = DEFAULT_ITEM_LINE_WIDTH;
    private int itemCount = DEFAULT_COUNT;
    private float scale = 1;
    //记录每个字的二维数组
    int[][] textPosition;
    int[][] linePosition;

    private int bottomColorFocus = Color.RED;
    private int bottomColorStatic = Color.GRAY;

    public LatticeInput(Context context) {
        this(context, null);
    }

    public LatticeInput(Context context, AttributeSet attrs) {
        super(context, attrs);
        initSpaceInputAttrs(attrs, 0);
        init();
    }

    public LatticeInput(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSpaceInputAttrs(attrs, defStyle);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public LatticeInput(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);
        content = text;
        calculateTextPositions();
        this.invalidate();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        content = text;
        calculateTextPositions();
        this.invalidate();
    }

    private void initSpaceInputAttrs(AttributeSet attrs, int defStyle){
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.SpaceInputAppearance, defStyle, 0);

        int bottomColor = a.getColor(R.styleable.SpaceInputAppearance_bottomColor, 0);
        if (bottomColor != 0) {
            this.bottomColorFocus = bottomColor;
        }

        int bottomColorStatic = a.getColor(R.styleable.SpaceInputAppearance_bottomColorStatic, 0);
        if (bottomColorStatic != 0) {
            this.bottomColorStatic = bottomColorStatic;
        }

        a.recycle();
    }

    private void init() {

        textPaint = getPaint();

        final float dpScale = getContext().getResources().getDisplayMetrics().density;
        defaultTextSize = (int) (DEFAULT_TEXT_SIZE * dpScale + 0.5);
        itemDefaultWidth = (int) (DEFAULT_ITEM_WIDTH * dpScale + 0.5);
        itemDefaultHeight = (int) (DEFAULT_ITEM_HEIGHT * dpScale + 0.5);
        itemDefaultLineWidth = (int) (DEFAULT_ITEM_LINE_WIDTH * dpScale + 0.5);

        itemWidth = itemDefaultWidth;
        itemHeight = itemDefaultHeight;
        itemLineWidth = itemDefaultLineWidth;


        textPaint.setTextSize(defaultTextSize);

        linePaint = new Paint();
        linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linePaint.setStrokeWidth(dpScale * 1);

        setBackgroundDrawable(null);
        setSingleLine();
        setLongClickable(false);
        setTextIsSelectable(false);

        setItemCount(DEFAULT_COUNT);

    }

    public void setItemCount(int count) {
        itemCount = count;
        setFilters(new InputFilter[] { new InputFilter.LengthFilter(itemCount) });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        boolean isWidthExactly = false;
        boolean isHeightExactly = false;

        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
            isWidthExactly = true;
        } else {
            width = itemDefaultWidth * itemCount;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            height = heightSize;
            isHeightExactly = true;
        } else {
            height = itemDefaultHeight;
        }

        if (isWidthExactly && isHeightExactly) {
            if ((width / height) > ((itemDefaultWidth * itemCount) / itemDefaultHeight)) {
                scale = (float) height / (float) itemDefaultHeight;
                width = (int) (itemDefaultWidth * itemCount * scale);
            } else {
                scale = (float) width / (float) (itemDefaultWidth * itemCount);
                height = (int) (itemDefaultHeight * scale);
            }
        } else if (isWidthExactly) {
            scale = (float) width / (float) (itemDefaultWidth * itemCount);
            height = (int) (itemDefaultHeight * scale);
        } else if (isHeightExactly) {
            scale = (float) height / (float) itemDefaultHeight;
            width = (int) (itemDefaultWidth * itemCount * scale);
        }

        textPaint.setTextSize(defaultTextSize * scale);
        itemWidth = (int) (itemDefaultWidth * scale);
        itemHeight = (int) (itemDefaultHeight * scale);
        itemLineWidth = (int) (itemDefaultLineWidth * scale);
        Log.e("XTAG", "XXXXXX:" + scale + ";" + height +";" + width + ";" + textPaint.getTextSize());
        calculateLinePositions();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        linePaint.setColor(hasFocus() ? bottomColorFocus : bottomColorStatic);
        // TODO:
        // Unknown issue, while scale is smaller than 1, the position it draw has deviation on Y axis.
        // Try to fix this with canvas translate.
        if (scale < 0.7) {
            canvas.translate(0, 50 * ((1 - scale) * (1 - scale)));
        }
        for (int i = 0; i < itemCount; i++) {
            if (content != null && content.length() > i) {
                canvas.drawText(String.valueOf(content.charAt(i)), textPosition[i][0], textPosition[i][1], textPaint);
            }
            canvas.drawLine(linePosition[i][0], linePosition[i][1], linePosition[i][0] + itemLineWidth, linePosition[i][1], linePaint);
        }
    }

    private void calculateTextPositions() {
        textPosition = new int[itemCount][2];
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            String str = String.valueOf(ch);

            //根据画笔获得每一个字符的显示的rect 就是包围框（获得字符宽度）
            Rect rect = new Rect();
            textPaint.getTextBounds(str, 0, 1, rect);
            int strWidth = rect.width();
            int offsetX = (itemWidth - strWidth) / 2;
            //记录每一个字的位置
            textPosition[i][0] = i * itemWidth + offsetX;
            textPosition[i][1] = -(int) textPaint.ascent() + (int) (30 * scale);
        }
    }

    private void calculateLinePositions() {
        linePosition = new int[itemCount][2];
        for (int i = 0; i < itemCount; i++) {
            int offsetX = (itemWidth - itemLineWidth) / 2;
            linePosition[i][0] = i * itemWidth + offsetX;
            linePosition[i][1] = itemHeight - (int) (6 * scale);
        }
    }

}
