package com.example.hsuweien.drawsignalline_06v4;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;

import java.util.Arrays;

public class drawlineView extends SurfaceView implements SurfaceHolder.Callback, Runnable{

    String LOG_TAG = "drawView";

    //draw something
    private Paint mPaint, mPaint_axis, mPaint_axis2, mPaint_time_thick, mPaint_time_thin;
    private Canvas mCanvas;
    private float xlast, xnew, ylast, ynew;
    private float screenH, screenW;    // 設定要畫訊號的畫布（不一定是整個畫布！）
    private float real_screenH, real_screenW;   // 真正的畫布大小
    private int mColor, mColor_axis, mColor_axis2;
    private float init_x_location = 60;  //開始畫圖的x初始位置(是指畫布)

    // surfaceView
    private SurfaceHolder mSurfaceHolder;
    private boolean mIsRunnable;

    // draw for common
    private float[] get_data = null;
    private boolean drawFreq_flag, drawTime_flag;
    private float max_value, min_value, y_scale;
    private float SAMPLE_RATE;
    private float DRAW_DATASIZE;        // 實際要畫的範圍（screenW的銀幕長度下要畫的點數）
    private boolean isDrawFinish = true;
    private int DRAW_NOW;   // 判斷現在畫什麼圖，避免時域與頻域在觸控時互相影響, 1:時域, 2: 頻域強度, 3:頻域相位

    // draw for time
    private int START_DRAW_POINT=0;     // 開始畫圖的點(指y的陣列，第n個data開始)，如果是頻率則一定要為0
    private float Second_per_x;
    private float Setting_max_second, Setting_min_second;   // 將輸入要顯示的秒數，變為全域變數（縮放時需要使用）

    // draw for frequency
    private float Hz_per_x;
    private float yPiexl_per_value;
    private int AmpOrPhase;
    private int LogarithmOrLinear_scale=1;    // 尺度選擇，預設線性尺度(1)
    private float Log_accuracy = 0.001f;   // Log尺度的精度，每格幾單位畫一點

    //123321

    // touch event
    private boolean setDragEnable = false; // 開啟手指拖移畫面？
    private float x1, y1, x2, y2; //記錄手指觸碰的座標
    private float old_x1=0, old_dist=0; //第一次觸碰時的座標
    private VelocityTracker mVelocityTracker = null;

    public drawlineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    public void init(Context context, AttributeSet attrs){
        Log.d(LOG_TAG, "init()");

        // 訊號
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(3);
        mColor = Color.argb(192, 64, 128, 64); //定義顏色ARGB，設定給畫筆用的

        // 邊框
        mPaint_axis = new Paint();
        mPaint_axis.setStrokeWidth(4);
        mColor_axis = Color.GRAY;
        mPaint_axis.setColor(mColor_axis);

        // 細軸
        mPaint_axis2 = new Paint();
        mPaint_axis2.setStrokeWidth(2);
        mColor_axis2 = Color.GRAY;
        mPaint_axis2.setColor(mColor_axis2);
        mPaint_axis2.setTextSize(40);   // 設定畫筆寫字大小

        // 時域 粗格子 格/0.2sec
        mPaint_time_thick = new Paint();
        mPaint_time_thick.setStrokeWidth(5);
        mPaint_time_thick.setColor(Color.BLACK);
        mPaint_time_thick.setAlpha(15);

        // 時域 細格子 格/0.04sec
        mPaint_time_thin = new Paint();
        mPaint_time_thin.setStrokeWidth(2);
        mPaint_time_thin.setColor(mColor_axis);
        mPaint_time_thin.setAlpha(30);


        // surfaceView
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);    // 設定surface生命週期的callback

    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(LOG_TAG, "surfaceCreate()");
        mIsRunnable = true;
        new Thread(this).start();

        // 取得銀幕長寬資訊，順便將畫布背景設為白色
        mCanvas = mSurfaceHolder.lockCanvas();
        mCanvas.drawColor(Color.WHITE);
        real_screenH = mCanvas.getHeight();
        real_screenW = mCanvas.getWidth();
        screenH = mCanvas.getHeight()-60;      // 可畫區域的長度
        screenW = mCanvas.getWidth()-init_x_location;         // 可畫區域的寬度（減去init_x_location代表從右邊內縮一定區域）
        //screenW = mCanvas.getWidth();
        mSurfaceHolder.unlockCanvasAndPost(mCanvas);

        Log.d(LOG_TAG, "可畫區域高度: "+Float.valueOf(screenH).toString());
        Log.d(LOG_TAG, "可畫區域寬度: "+Float.valueOf(screenW).toString());
        Log.d(LOG_TAG, "真實高度: "+Float.valueOf(real_screenH).toString());
        Log.d(LOG_TAG, "真實寬度: "+Float.valueOf(real_screenW).toString());

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d(LOG_TAG, "surfaceChanged()");

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(LOG_TAG, "surfaceDestroyed()");
        mIsRunnable = false;

    }

    // run()是Runnable物件的方法，用於背景執行
    @Override
    public void run() {
        Log.d(LOG_TAG, "run()");
        while(mIsRunnable){
            if(drawFreq_flag){
                axis_init();
//                drawFreq();
                if(AmpOrPhase == 1 && LogarithmOrLinear_scale == 1)
                    drawSomething_freq_amp_Linear();
                else if(AmpOrPhase == 1 && LogarithmOrLinear_scale == 2)
                    drawSomething_freq_amp_Logarithm();
                else if(AmpOrPhase == 2)
                    drawSomething_freq_phase();

                // 畫一次就停，直到下一次addDataPoint()收到新的數據
                drawFreq_flag = false;
            }
            if(drawTime_flag){
                axis_init();
                drawSomething_time();
                drawTime_flag = false;
            }
        }
    }

    /* 觸控實現方法 */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 開啟手指拖移畫面的功能
        if(setDragEnable){
            // 取得mVelocityTracker實例
            if(mVelocityTracker == null)
                mVelocityTracker = VelocityTracker.obtain();
            mVelocityTracker.addMovement(event);    // 將觸控事件event加入mVelocityTracker實例
            final VelocityTracker verTracker = mVelocityTracker;

            if (event.getAction() == event.ACTION_MOVE) {
                // 取得觸控的點數
                switch (event.getPointerCount()){
                    case 1:
                        //左右拖曳
                        verTracker.computeCurrentVelocity(1000);   //每1000ms內運動了多少相素
                        float velocityX = verTracker.getXVelocity(event.getPointerId(0)); // 計算x方向的速度

                        x1 = event.getX();
                        if (old_x1 == 0)
                            old_x1 = x1;
                        if (x1 < old_x1) {
                            // 如果顯示的點到尾巴（所有data減去起始畫圖點 < 畫面可顯示的範圍）...則停止拖曳
                            if ((get_data.length - START_DRAW_POINT) > DRAW_DATASIZE) {
                                // 如果: 總長-起始長度 > 可顯示區域
                                START_DRAW_POINT += (int)(Math.abs(velocityX)*0.05);
                                // 避免起始點加可顯示區域已經大於最大長度（拉太快）
                                if(START_DRAW_POINT+DRAW_DATASIZE > get_data.length)
                                    START_DRAW_POINT = get_data.length-(int)DRAW_DATASIZE;

                                //drawTime_flag = true;   // 重繪
                                touch_re_draw(DRAW_NOW);
                                old_x1 = x1;
                            }
                        } else if (x1 > old_x1) {
                            // 如果往左已經沒有data，則停止拖曳
                            START_DRAW_POINT -= (int)(Math.abs(velocityX)*0.05);
                            if(START_DRAW_POINT < 0)
                                START_DRAW_POINT = 0;
                            //drawTime_flag = true;
                            touch_re_draw(DRAW_NOW);
                            old_x1 = x1;
                        }
                        break;
                    case 2:
                        // 縮放功能
                        x1 = event.getX(event.getPointerId(0));
                        x2 = event.getX(event.getPointerId(1));

                        // 兩個手指點擊下去時，先記錄一開始手指距離
                        if(old_dist == 0){
                            old_dist = Math.abs(x1-x2);
                        }
                        else if(Math.abs(x1-x2) < old_dist && isDrawFinish == true){
                            // 縮小數據
                            /*
                             * 由於畫圖是由thread執行，而觸控一次會改變多次值，會導致繪圖未完成的時候又觸控而改變範圍
                             * 導致繪圖不完整，使用isDrawFinish確保繪圖完成後才進行觸控修改
                             */

                            // 避免要畫的範圍超過數據長度
                            DRAW_DATASIZE+= (int)(DRAW_DATASIZE*0.2); // 繪圖長度一次增加可見的20%
                            if(DRAW_DATASIZE+START_DRAW_POINT >= get_data.length){  // 如果已經到底，但開始繪圖點不是0，則改為減去開始點
                                DRAW_DATASIZE = get_data.length - START_DRAW_POINT;
                                START_DRAW_POINT -= (int)(DRAW_DATASIZE*0.2);
                            }
                            if(START_DRAW_POINT < 0)
                                START_DRAW_POINT = 0;

                            old_dist = Math.abs(x1-x2);
                            //drawTime_flag = true;
                            touch_re_draw(DRAW_NOW);
                        }
                        else if(Math.abs(x1-x2) > old_dist && isDrawFinish == true){
                            // 放大數據，一次減少可顯示長度的20%
                            DRAW_DATASIZE -= DRAW_DATASIZE*0.2;

                            // 最大一次只能看0.5秒
                            if(DRAW_DATASIZE > SAMPLE_RATE*0.5f){
                                //drawTime_flag = true;
                                touch_re_draw(DRAW_NOW);
                            }
                            else {
                                DRAW_DATASIZE = SAMPLE_RATE * 0.5f;
                                //drawTime_flag = true;
                                touch_re_draw(DRAW_NOW);
                            }
                        }
                        break;
                }
            }
            if (event.getAction() == event.ACTION_UP) {
                old_x1 = 0;
                old_dist = 0;
            }
        }
        return true;//super.onTouchEvent(event);
    }

    /* 頻域訊號繪圖 */
    /*
     * 參數說明:
     * value: 原始數據, sampleRate: 採樣率, FFT_len: FFT轉換的長度,
     * hz_min_range: 設定顯示範圍的最小點（初始的時候，預設最小）,
     * hz_max_range: 設定顯示範圍的最大點（初始的時候，0:全部顯示),
     * AmpOrPhase: 1: 畫amp, 2: 畫phase
     * linearORlogarithm: 1: 畫Linear軸, 2: 對數軸
     */
    public void addFreqPoint(float[] value, float sampleRate, float FFT_len, float hz_min_range, float hz_max_range, int ampORphase) {
        Log.d(LOG_TAG, "addFreqPoint()");

        SAMPLE_RATE = sampleRate;   //給全域
        AmpOrPhase = ampORphase;    //給全域
        int dataSize = value.length;    // 取得原始數據的資料長度

        // y值轉換成log，取20log (單位為DB)
        float[] value2 = new float[dataSize];
        for(int i=0; i<dataSize; i++) {
            if(ampORphase == 1)
                value2[i] = 20 * (float) Math.log10(value[i]);
            else if(ampORphase == 2)
                value2[i] = value[i];
        }

        // 尋找翻轉前的最大值
        float[] tmp = new float[dataSize];
        for(int i=0; i<dataSize; i++)
            tmp[i] = value2[i];
        Arrays.sort(tmp);
        max_value = tmp[dataSize-2];
        min_value = tmp[0];
        //y_scale = screenH/(max_value+100);  // 加100是為了不讓整個訊號佔滿銀幕，較好看
        if(ampORphase == 1)
            y_scale = screenH/(max_value-min_value+100);
        if(ampORphase == 2)
            y_scale = screenH/(max_value-min_value);

        //Log.d(LOG_TAG, "翻轉前的y最大值: "+Float.valueOf(max_value).toString());
        //Log.d(LOG_TAG, "翻轉前的y最小值: "+Float.valueOf(min_value).toString());
        //Log.d(LOG_TAG, "y_scale: "+Float.valueOf(y_scale).toString());


        //讀取數據，並且做上下翻轉
        get_data = new float[dataSize];
        for(int i=0; i<dataSize; i++){
//            get_data[i] = (screenH - value[i]);
            get_data[i] = (screenH - (value2[i]-min_value)*y_scale);
        }
        //Log.d(LOG_TAG, "數據長度: "+Float.valueOf(dataSize).toString());

        // 計算y軸位移量
        yPiexl_per_value = (screenH/(int)(max_value-min_value))*y_scale;

        // 計算x軸的寬度
        Hz_per_x = (sampleRate/2)/(FFT_len/2);

        /* 選擇線性或對數尺度 */
        if(LogarithmOrLinear_scale == 1){
            /* 線性尺度 */

            // 用於設定x-axis要顯示的大小
            // DATASIZE * 最小Hz點(Hz_per_x) = 想看的最大Hz（重要！！！）
            if(hz_max_range > dataSize*Hz_per_x)
                hz_max_range = dataSize*Hz_per_x;

            // 要從哪一點開始畫
            START_DRAW_POINT = (int)(hz_min_range/Hz_per_x);

            // 要畫多少點（這裡DATASIZE決定了要花出多少數據，所以改變大小，則可改變要畫的範圍）
            int How_many_I_can_draw = (int)(hz_max_range/Hz_per_x);   // 計算出來是輸入Hz 所對應的資料點數
            if(How_many_I_can_draw < dataSize && How_many_I_can_draw > 0)
                DRAW_DATASIZE = (int)How_many_I_can_draw;
            else
                DRAW_DATASIZE = dataSize;   // 如果輸入的點數比資料量大，就改成畫最大長度
        }
        else if(LogarithmOrLinear_scale == 2){
            /* Logarithm尺度 */
            DRAW_DATASIZE = (float)Math.log10(hz_max_range)/Log_accuracy; // log尺度下，每Log_accuracy單位為一點，總共有多少點

            START_DRAW_POINT = 0;
        }

        //執行thread的畫圖
        drawFreq_flag = true;
    }
    private void drawSomething_freq(){
        Log.d(LOG_TAG, "drawSometing()");
        try{
            mPaint.setColor(mColor);
            mCanvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);

            // 座標主軸
            mCanvas.drawLine(init_x_location, screenH, screenW, screenH, mPaint_axis); //x-axis 下
            mCanvas.drawLine(init_x_location, 2, init_x_location, screenH, mPaint_axis);    // x-axis 左
            mCanvas.drawLine(screenW, 0, screenW, screenH, mPaint_axis); //y-axis 右
            mCanvas.drawLine(init_x_location, 2, screenW, 2, mPaint_axis); // y-axis上

            int axis_10x = 0;

            for(int i=0; i<DRAW_DATASIZE; i++){
                mCanvas.drawLine(xlast, ylast, xnew, get_data[i], mPaint);     //畫線

                xlast = xnew;
                ylast = get_data[i];
                xnew += (screenW-init_x_location)/DRAW_DATASIZE;   // 寬度/數據長度，代表每個數據間的距離，越多數據則間格越小
            }

            // 畫軸
            //freq_xaxis_setting((int)DRAW_DATASIZE);


        }
        catch (Exception e){

        }
        finally {
            //释放canvas对象并提交画布
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }

    }   // 只有繪出圖形（最簡單的版本）
    private void drawSomething_freq_amp(){
        Log.d(LOG_TAG, "drawSometing_freq1()");
        try{
            mPaint.setColor(mColor);
            mCanvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);

            // 座標主軸
            mCanvas.drawLine(init_x_location, screenH, screenW, screenH, mPaint_axis); //x-axis 下
            mCanvas.drawLine(init_x_location, 2, init_x_location, screenH, mPaint_axis);    // x-axis 左
            mCanvas.drawLine(screenW, 0, screenW, screenH, mPaint_axis); //y-axis 右
            mCanvas.drawLine(init_x_location, 2, screenW, 2, mPaint_axis); // y-axis上

            // y軸
            int big_yaxis=5;
            int count = 0;
            // yPiexl_per_value: 值為1對應的可畫銀幕高度
            for(int i=(int)screenH; i>0; i-=(int)yPiexl_per_value*big_yaxis) {
                mCanvas.drawLine(init_x_location, i, init_x_location-10, i, mPaint_axis);
                mCanvas.drawText(Integer.valueOf(count*big_yaxis).toString(), init_x_location-55, i+15, mPaint_axis2);
                count++;    // 用來記錄字要寫在哪個位置，因為i是指銀幕高度的位移
            }

            int thin_xaxis=0, big_xaxis=0;
            for(int i=0; i<DRAW_DATASIZE; i++){
                mCanvas.drawLine(xlast, ylast, xnew, get_data[i], mPaint);     //畫線

                // x軸
                if ((i * Hz_per_x) > 10*thin_xaxis) {
                    mCanvas.drawLine(xlast, screenH, xlast, screenH + 10, mPaint_axis2);
                    thin_xaxis++;
                }
                if ((i * Hz_per_x) > 50*big_xaxis) {
                    mCanvas.drawLine(xlast, screenH, xlast, screenH + 15, mPaint_axis);
                    mCanvas.drawText(Integer.valueOf(50*big_xaxis).toString(), xlast - 35, screenH + 50, mPaint_axis2);
                    big_xaxis++;
                }

                xlast = xnew;
                ylast = get_data[i];
                xnew += (screenW-init_x_location)/DRAW_DATASIZE;   // 寬度/數據長度，代表每個數據間的距離，越多數據則間格越小
            }
        }
        catch (Exception e){

        }
        finally {
            //释放canvas对象并提交画布
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }

    }
    private void drawSomething_freq_phase(){
        Log.d(LOG_TAG, "drawSometing_freq1()");
        try{
            mPaint.setColor(mColor);
            mCanvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);

            // 座標主軸
            mCanvas.drawLine(init_x_location, screenH, screenW, screenH, mPaint_axis); //x-axis 下
            mCanvas.drawLine(init_x_location, 2, init_x_location, screenH, mPaint_axis);    // x-axis 左
            mCanvas.drawLine(screenW, 0, screenW, screenH, mPaint_axis); //y-axis 右
            mCanvas.drawLine(init_x_location, 2, screenW, 2, mPaint_axis); // y-axis上

            int thin_xaxis=0, big_xaxis=0;
            for(int i=0; i<DRAW_DATASIZE; i++){
                mCanvas.drawLine(xlast, ylast, xnew, get_data[i], mPaint);     //畫線

                // x軸
                if ((i * Hz_per_x) > 10*thin_xaxis) {
                    mCanvas.drawLine(xlast, screenH, xlast, screenH + 10, mPaint_axis2);
                    thin_xaxis++;
                }
                if ((i * Hz_per_x) > 50*big_xaxis) {
                    mCanvas.drawLine(xlast, screenH, xlast, screenH + 15, mPaint_axis);
                    mCanvas.drawText(Integer.valueOf(50*big_xaxis).toString(), xlast - 35, screenH + 50, mPaint_axis2);
                    big_xaxis++;
                }

                xlast = xnew;
                ylast = get_data[i];
                xnew += (screenW-init_x_location)/DRAW_DATASIZE;   // 寬度/數據長度，代表每個數據間的距離，越多數據則間格越小
            }
            // y軸
            Log.d(LOG_TAG, "Value_per_y: "+Float.valueOf(yPiexl_per_value).toString());
            int big_yaxis=20;
            int count = 0;
            // yPiexl_per_value: 值為1對應的可畫銀幕高度
//            for(int i=(int)screenH; i>0; i-=(int)yPiexl_per_value*big_yaxis) {
//                mCanvas.drawLine(init_x_location, i, init_x_location-10, i, mPaint_axis);
//                mCanvas.drawText(Integer.valueOf(count*big_yaxis).toString(), init_x_location-55, i+15, mPaint_axis2);
//                count++;    // 用來記錄字要寫在哪個軸，因為i是指銀幕高度的位移
//            }

            int labely = -180;
            for(int i=(int)screenH; i>0; i-=screenH/17){
                mCanvas.drawLine(init_x_location, i, init_x_location-10, i, mPaint_axis);
                mCanvas.drawText(Integer.valueOf(labely).toString(), init_x_location-80, i+15, mPaint_axis2);
                labely+=20;
            }
        }
        catch (Exception e){

        }
        finally {
            //释放canvas对象并提交画布
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
        }

    }
    private void drawSomething_freq_amp_Linear(){
        Log.d(LOG_TAG, "drawSomething_freq_amp_Linear()");
        try{
            isDrawFinish = false;
            mPaint.setColor(mColor);
            mCanvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);

            // 座標主軸
            mCanvas.drawLine(init_x_location, screenH, screenW, screenH, mPaint_axis); //x-axis 下
            mCanvas.drawLine(init_x_location, 2, init_x_location, screenH, mPaint_axis);    // x-axis 左
            mCanvas.drawLine(screenW, 0, screenW, screenH, mPaint_axis); //y-axis 右
            mCanvas.drawLine(init_x_location, 2, screenW, 2, mPaint_axis); // y-axis上

            // y軸
            int big_yaxis=5;
            int count = 0;
            // yPiexl_per_value: 值為1對應的可畫銀幕高度
            for(int i=(int)screenH; i>0; i-=(int)yPiexl_per_value*big_yaxis) {
                mCanvas.drawLine(init_x_location, i, init_x_location-10, i, mPaint_axis);
                mCanvas.drawText(Integer.valueOf(count*big_yaxis).toString(), init_x_location-55, i+15, mPaint_axis2);
                count++;    // 用來記錄字要寫在哪個位置，因為i是指銀幕高度的位移
            }

            int thin_xaxis=0, big_xaxis=0;
            float start_Hz = (START_DRAW_POINT*Hz_per_x);  // 開始顯示的秒數
            float total_Hz = (DRAW_DATASIZE*Hz_per_x);
            for(int i=0; i<DRAW_DATASIZE; i++){
                mCanvas.drawLine(xlast, ylast, xnew, get_data[i+START_DRAW_POINT], mPaint);     //畫線

                // x軸繪製
                if ((i * Hz_per_x) > 10*thin_xaxis) {
                    mCanvas.drawLine(xlast, screenH, xlast, screenH + 10, mPaint_axis2);
                    if(total_Hz <= 50){
                        mCanvas.drawLine(xlast, 0, xlast, screenH + 10, mPaint_time_thick);
                        mCanvas.drawText(String.format("%2.1f", start_Hz + 10f * thin_xaxis), xlast - 30, screenH + 50, mPaint_axis2);
                    }
                    thin_xaxis++;
                }
                if ((i * Hz_per_x) > 50*big_xaxis) {
                    mCanvas.drawLine(xlast, 0, xlast, screenH + 15, mPaint_time_thick);
                    //mCanvas.drawText(Integer.valueOf(50*big_xaxis).toString(), xlast - 35, screenH + 50, mPaint_axis2);
                    mCanvas.drawText(String.format("%3.1f", start_Hz + 50f * big_xaxis), xlast - 30, screenH + 50, mPaint_axis2);
                    big_xaxis++;
                }

                xlast = xnew;
                ylast = get_data[i+START_DRAW_POINT];
                xnew += (screenW-init_x_location)/DRAW_DATASIZE;   // 寬度/數據長度，代表每個數據間的距離，越多數據則間格越小
            }
        }
        catch (Exception e){

        }
        finally {
            //释放canvas对象并提交画布
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            isDrawFinish = true;
        }

    }   // 可位移的頻率強度譜(線性)
    private void drawSomething_freq_amp_Logarithm(){
        Log.d(LOG_TAG, "drawSometing_freq_amp_Logarithm()");
        try{
            isDrawFinish = false;
            mPaint.setColor(mColor);
            mCanvas = mSurfaceHolder.lockCanvas();
            mCanvas.drawColor(Color.WHITE);

            // 座標主軸
            mCanvas.drawLine(init_x_location, screenH, screenW, screenH, mPaint_axis); //x-axis 下
            mCanvas.drawLine(init_x_location, 2, init_x_location, screenH, mPaint_axis);    // x-axis 左
            mCanvas.drawLine(screenW, 0, screenW, screenH, mPaint_axis); //y-axis 右
            mCanvas.drawLine(init_x_location, 2, screenW, 2, mPaint_axis); // y-axis上

            // y軸
            int big_yaxis=5;
            int count = 0;
            // yPiexl_per_value: 值為1對應的可畫銀幕高度
            for(int i=(int)screenH; i>0; i-=(int)yPiexl_per_value*big_yaxis) {
                mCanvas.drawLine(init_x_location, i, init_x_location-10, i, mPaint_axis);
                mCanvas.drawText(Integer.valueOf(count*big_yaxis).toString(), init_x_location-55, i+15, mPaint_axis2);
                count++;    // 用來記錄字要寫在哪個位置，因為i是指銀幕高度的位移
            }

            int x_big_axis = 1, x_small_axis=0, x_count=0;   // 粗軸與細軸
            float setX_Hz=0, getY_value=0, upper_Hz=0, bottom_Hz=0;
            int bottom_index=0, upper_index=0;  // 紀錄要畫的點，前後最靠近資料點（才可用內差求出要畫的點）
            // i是指log尺度下的位移量
            for(int i=0; i<(int)DRAW_DATASIZE; i++){
                setX_Hz = (float)Math.pow(10, i*Log_accuracy);    // 反推目前點數對應的Hz（指log尺度上的點）

                // 欲計算y值對應的Hz/最小Hz單位，計算出離此Hz最近兩點的index，再做內差！
                // 對應Hz除以最小單位點Hz不等於零，代表不是目前知道的值，需做內差
                if(setX_Hz%Hz_per_x != 0){
                    bottom_index = (int)Math.floor(setX_Hz/Hz_per_x);
                    upper_index = bottom_index+1;

                    getY_value = Interpolation(bottom_index*Hz_per_x, get_data[bottom_index], upper_index*Hz_per_x, get_data[upper_index], setX_Hz);
                }
                else{
                    getY_value = get_data[(int)(setX_Hz/Hz_per_x)];     // 剛好整除，直接得到該Hz對應的y值
                }

                mCanvas.drawLine(xlast, ylast, xnew, getY_value, mPaint);     //畫線
                xlast = xnew;
                ylast = getY_value;
                xnew += (screenW-init_x_location)/DRAW_DATASIZE;   // 寬度/數據長度，代表每個數據間的距離，越多數據則間格越小

                // x軸
                if(setX_Hz > x_small_axis){

                    // 粗軸，如果已經達到10的倍數，則畫一條粗軸，否則只畫細軸
                    if(setX_Hz >= x_big_axis){
                        mCanvas.drawLine(xlast, 0, xlast, screenH + 15, mPaint_time_thick);
                        mCanvas.drawText(String.format("%d", x_big_axis), xlast - 30, screenH + 50, mPaint_axis2);
                        x_big_axis*=10;
                        x_count++;  // 用於記錄目前是10的幾次方
                    }
                    else{
                        // 細軸
                        mCanvas.drawLine(xlast, 0, xlast, screenH, mPaint_time_thin);
                        x_small_axis+=Math.pow(10, x_count-1);  //一開始會先畫一個粗軸(10^0)但還沒畫細軸，所以這裡要減1
                    }
                }
            }
        }
        catch (Exception e){

        }
        finally {
            //释放canvas对象并提交画布
            mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            isDrawFinish = true;
        }
    }
    // 內插法，求中間點的y值
    private float Interpolation(float x1, float y1, float x2, float y2, float setX){
        float get_y = ((y2-y1)/(x2-x1))*(setX-x1)+y1;

        return get_y;
    }
    // 選擇頻域繪圖尺度，預設線性尺度
    public void setFreqScaleIsLogarithm(int choose){
        LogarithmOrLinear_scale = choose;
    }


    /* 時域訊號繪圖 */
    // (原始數據, 原始數據長度, 原始數據採樣率, 設定顯示範圍最大點, 設定顯示範圍最小點)
    public void addTimePoint(float[] value, int dataSize, float sampleRate, float second_max, float second_min){
        Log.d(LOG_TAG, "add Time Point()");
        // 尋找翻轉前的最大值
        float[] tmp = new float[dataSize];
        for(int i=0; i<dataSize; i++){
            tmp[i] = value[i];
        }
        Arrays.sort(tmp);
        max_value = tmp[dataSize-1];
        min_value = tmp[0];
        y_scale = screenH/(max_value-min_value);

        Log.d(LOG_TAG, "value[0]: "+value[0]);


        get_data = new float[dataSize];
        // 取得數據，計算實際電壓大小
        for(int i=0; i<dataSize; i++){
            // value[i]原始資料減去最小值，最小值一定落在畫布的最底端
            get_data[i] = (screenH - (value[i]-min_value)*y_scale);
        }

        // 計算畫出指定範圍（將時間換算成要顯示的點數）
//        if(second_max > dataSize/sampleRate || second_max == 0)
//            second_max = dataSize/sampleRate;   // 設定時間不可超過最大時間
//        if(second_min < 0)
//            second_min = 0;
//        int point_max = (int)(second_max*sampleRate-1);    // 陣列是0 - (n-1)
//        int point_min = (int)(second_min*sampleRate);
//        Log.d(LOG_TAG, "point_max: "+Integer.valueOf(point_max).toString());
//        Log.d(LOG_TAG, "point_min: "+Integer.valueOf(point_min).toString());
//        START_DRAW_POINT = point_min;   // 開始的點數
//        DRAW_DATASIZE = point_max - point_min+1;    // 總共要畫的點數

        // 輸入要顯示的秒數，返回長度為2的陣列，回傳對應的點數位置
        int[] point = calculateSecondtoPoint(dataSize, sampleRate, second_max, second_min);
        START_DRAW_POINT = point[1];
        DRAW_DATASIZE = point[0] - point[1]+1;

        // 計算軸的寬度
        Second_per_x = 1/sampleRate;   // 每一點所經過的秒數
        Log.d(LOG_TAG, "Second_per_x: "+Float.valueOf(Second_per_x).toString());

        //儲存sample rate
        SAMPLE_RATE = sampleRate;

        drawTime_flag = true;
    }
    private void drawSomething_time(){
        Log.d(LOG_TAG, "drawSometing_time()");
        synchronized (this){
            try{
                isDrawFinish = false;
                mPaint.setColor(mColor);
                mCanvas = mSurfaceHolder.lockCanvas();
                mCanvas.drawColor(Color.WHITE);

                // 座標主軸
                mCanvas.drawLine(init_x_location, screenH, screenW, screenH, mPaint_axis); //x-axis 下
                mCanvas.drawLine(init_x_location, 2, init_x_location, screenH, mPaint_axis);    // x-axis 左
                mCanvas.drawLine(screenW, 0, screenW, screenH, mPaint_axis); //y-axis 右
                mCanvas.drawLine(init_x_location, 2, screenW, 2, mPaint_axis); // y-axis上


                int big_axis=0, small_axis=0, big_axis_1s=0;    //繪製x軸
                float total_second = (DRAW_DATASIZE/SAMPLE_RATE);   // 可顯示區域的秒數
                float start_time = (START_DRAW_POINT/SAMPLE_RATE);  // 開始顯示的秒數
                for(int i=0; i<DRAW_DATASIZE; i++){

                    // 繪製x軸
                    // 每0.2秒畫一大格
                    if(i*Second_per_x > 0.2*big_axis){
                        mCanvas.drawLine(xlast, 0, xlast, screenH, mPaint_time_thick);
                        if(total_second <= 3) {
                            mCanvas.drawText(String.format("%.1f", start_time + 0.2f * big_axis), xlast - 30, screenH + 50, mPaint_axis2);
                        }
                        big_axis++;
                    }
                    // 每1秒寫一格
                    if(i*Second_per_x > 1*big_axis_1s){
                        if(total_second <= 7 && total_second > 3)
                            mCanvas.drawText(String.format("%.1f", start_time + 1 * big_axis_1s), xlast - 30, screenH + 50, mPaint_axis2);
                        big_axis_1s++;
                    }
                    // 每0.04秒畫一小格
                    if(i*Second_per_x > 0.04*small_axis){
                        if(total_second < 15)
                            mCanvas.drawLine(xlast, 0, xlast, screenH, mPaint_time_thin);
                        small_axis++;
                    }

                    // 畫訊號
                    mCanvas.drawLine(xlast, ylast, xnew, get_data[i+START_DRAW_POINT], mPaint);     //畫data線
                    xlast = xnew;
                    ylast = get_data[i+START_DRAW_POINT];
                    xnew += (screenW-init_x_location)/DRAW_DATASIZE;   // 寬度/數據長度，代表每個數據間的距離，越多數據則間格越小
                }

                // 繪製y軸
                // -((i-screenH)/y_scale-min_value)
                float label_big = -((get_data[0]-screenH)/y_scale-min_value), label_small = -((get_data[0]-screenH)/y_scale-min_value);  // 從0開始畫，代表每多少數值畫一個
                //for迴圈: 是代表pixel的點，所以先轉換真實值0所對應的pixel點開始畫（往上畫）
                // i = (screenH-(get_data[0]-min_value)*y_scale): 不從0畫
                for(float i=get_data[0] ; i>0; i--){
                    //每個i值轉換回真實數值，如果小於當前畫線的真實數值 的0.5，則再畫一條
                    float t1 = -((i-screenH)/y_scale-min_value);
                    if( t1 >= label_big) {
                        mCanvas.drawLine(init_x_location - 20, i, screenW, i, mPaint_time_thick);
                        label_big += 0.5; // 每0.5mV畫一條粗軸
                    }
                    if( t1 > label_small) {
                        mCanvas.drawLine(init_x_location - 20, i, screenW, i, mPaint_time_thin);
                        label_small += 0.1; // 每0.1mV畫一條細軸
                    }
                }
                label_big = -((get_data[0]-screenH)/y_scale-min_value)-0.5f;
                label_small = -((get_data[0]-screenH)/y_scale-min_value)-0.1f;
                for(float i= get_data[0]; i<screenH; i++){
                    //每個i值轉換回真實數值，如果小於當前畫線的真實數值 的0.5，則再畫一條
                    float t1 = -((i-screenH)/y_scale-min_value);
                    if( t1 < label_big) {
                        mCanvas.drawLine(init_x_location - 20, i, screenW, i, mPaint_time_thick);
                        label_big -= 0.5; // 每0.5mV畫一條粗軸
                    }
                    if( t1 < label_small) {
                        mCanvas.drawLine(init_x_location - 20, i, screenW, i, mPaint_time_thin);
                        label_small -= 0.1; // 每0.1mV畫一條粗軸
                    }
                }
            }
            catch (Exception e){

            }
            finally {
                //释放canvas对象并提交画布
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                isDrawFinish = true;
            }
        }

    }
    private int[] calculateSecondtoPoint(int dataSize, float sr, float max, float min){
        /* 計算給定範圍時間內，所對應的點數
         * max: 要顯示的最大時間
         * min: 要顯示的最小時間
         * datasize: 資料長度
         * sr: 資料採樣率
         *
         * 輸出:
         * result[0]: 換算出來的最大點數，即point_max
         * result[1]: 換算出來的最小點數，即point_min
         *
         * 計算結果:
         * 開始繪圖的點數位置 = result[1];
         * 要畫多少點數 = result[0] - result[1]+1;
         */
        int[] result = new int[2];

        if(max > dataSize/sr || max == 0) {
            max = dataSize / sr;
            Setting_max_second = max;    //儲存輸入秒數，給全域使用
        }
        if(min < 0) {
            min = 0;
            Setting_min_second = min;    //儲存輸入秒數，給全域使用
        }
        result[0] = (int)(max*sr-1);   // 最大時間對應的點數
        result[1] = (int)(min*sr);  // 最小時間對應的點數
        Log.d("DRAW_DATASIZE", "max: "+result[0]);
        Log.d("DRAW_DATASIZE", "min: "+result[1]);

        return result;
    }


    /* 參數設定 */
    // 初始化軸
    private void axis_init(){
        xlast= init_x_location;
        xnew= init_x_location;
        ylast=0;
    }

    /* 拖移功能
     * 參數1: 是否開啟拖移功能, 參數2: 目前畫哪一張圖(1:時域, 2:頻域強度, 3:頻域相位)
     * */
    public void setDragEnable(boolean flag, int condition){
        //是否開啟手指拖移畫面
        setDragEnable = flag;
        DRAW_NOW = condition;
    }
    // 選擇重繪的類型
    private void touch_re_draw(int condtion){
        /*
         * 由於同個View要顯示時域與頻域，但軸的尺度不同，故重繪時也需要指定重繪哪張圖
         *
         * 參數說明:
         * 1: 時域, 2:頻域強度, 3:頻域相位
         */
        switch (condtion){
            case 1:
                drawTime_flag = true;
                break;
            case 2:
                drawFreq_flag = true;
                break;
            case 3:
                // 未寫
                break;
        }

    }
}