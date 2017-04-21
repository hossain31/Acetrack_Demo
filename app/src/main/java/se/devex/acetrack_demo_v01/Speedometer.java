package se.devex.acetrack_demo_v01;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import static se.devex.acetrack_demo_v01.R.styleable;

public class Speedometer extends View implements SpeedChangeListener {
    private static final String TAG = Speedometer.class.getSimpleName();
    public static final float DEFAULT_MAX_SPEED = 99; // Assuming this is max value

    // Speedometer internal state
    private float mMaxSpeed;
    private float mCurrentSpeed;

    // Scale configuration
    private float centerX;
    private float centerY;

    public Speedometer(Context context){
        super(context);
    }

    public Speedometer(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, styleable.Speedometer, 0, 0);
        try{
            mMaxSpeed = a.getFloat(styleable.Speedometer_maxSpeed, DEFAULT_MAX_SPEED);
            mCurrentSpeed = a.getFloat(styleable.Speedometer_currentSpeed, 0);

        } finally{
            a.recycle();
        }
    }

    public float getCurrentSpeed() {
        return mCurrentSpeed;
    }

    public void setCurrentSpeed(float mCurrentSpeed) {
        if(mCurrentSpeed > this.mMaxSpeed)
            this.mCurrentSpeed = mMaxSpeed;
        else if(mCurrentSpeed < 0)
            this.mCurrentSpeed = 0;
        else
            this.mCurrentSpeed = mCurrentSpeed;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);

        int chosenDimension = Math.min(chosenWidth, chosenHeight);
        centerX = chosenDimension / 2;
        centerY = chosenDimension / 2;
        setMeasuredDimension(chosenDimension, chosenDimension);
    }

    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else { // (mode == MeasureSpec.UNSPECIFIED)
            return getPreferredSize();
        }
    }

    // in case there is no size specified
    private int getPreferredSize() {
        return 300;
    }

    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        int x = getWidth();
        int y = getHeight();
        int radius =0;

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        canvas.drawPaint(paint);
        //set background of the speedometer
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.analyzing),0,0,null);
        paint.setColor(Color.GRAY);
        //draw circle color by using mCurrentSpeed
        for(int i = -360; i < (mCurrentSpeed/mMaxSpeed)*360 - 360; i+=2){
            radius = radius+2;
            if(mCurrentSpeed>=1 && mCurrentSpeed<60){
                paint.setColor(Color.BLUE);
            }
            else if (mCurrentSpeed>=60 && mCurrentSpeed<=74){
                paint.setColor(Color.GREEN);
            }
            else if(mCurrentSpeed>74 && mCurrentSpeed<=99){
                paint.setColor(Color.RED);
            }
            else paint.setColor(Color.GRAY);
        }
        canvas.drawCircle(x/2, y/2, radius, paint);
    }

    @Override
    public void onSpeedChanged(float newSpeedValue) {
        this.setCurrentSpeed(newSpeedValue);
        this.invalidate();
    }
}