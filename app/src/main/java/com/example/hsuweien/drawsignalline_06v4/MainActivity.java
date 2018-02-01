package com.example.hsuweien.drawsignalline_06v4;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.jtransforms.fft.DoubleFFT_1D;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;

import uk.me.berndporr.iirj.Butterworth;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    String LOG_TAG = "Main_page";
    final int ORIGINAL_PAGE=0, FFT_PAGE=1, FILTER_PAGE=2, OTHER_PAGE=3;

    // 元件
    private drawlineView mdrawlineview;
    private Button mscale_btn;
    private boolean actionbar_setting_condition = false;

    // original data
    double[] data = null;    //讀入的數據（如果有設定filter，則是濾波完的數據）
    private int DATASIZE;    // 讀入資料的大小
    private float ORI_sample_rate;
    private String ori_file_name;       // 讀入的檔名
    int BTN_IS_READY = 1;
    private float mV_per_AD=1.5f;

    // filter參數，在init_filter設定
    float[] filter_setting = null;   // 濾波器的參數
    boolean[] filter_choose = null;    // 選擇的濾波器種類
    boolean[] filter_type = null;
    double[] filtered_data = null;          // 儲存filter後的data

    // FFT
    int FFT_length=0;   // 預設0，自動做最大範圍的FFT長度轉換
    double[] fft_result = null;

    // draw something
    boolean draw_fft_flag = false;
    boolean draw_time_domain_flag = false;
    boolean Draw_amp = false, Draw_phase = false;
    private int chooseScale_linearORlog = 2;    // 頻譜的尺度選擇: 1:線性軸, 2:對數軸


    /*
    * 子線程更新UI的方法（避免Memory Leak）
    * 使用靜態類別方法，而不是直接宣告Handler物件

    static class MyHandler extends Handler{
        private final WeakReference<MainActivity> mActivity;
        public MyHandler(MainActivity activity){
            mActivity = new WeakReference<MainActivity>(activity);  //此行繼承了主畫面的所有物件
        }
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = mActivity.get();
            if(activity != null){
                if(msg.what == activity.BTN_IS_READY){
                    // 更新UI介面元件的狀態
                    activity.mbtn_draw_time_domain.setEnabled(true);
                    activity.mbtn_draw_freq_domain.setEnabled(true);
                }
            }
            super.handleMessage(msg);
        }
    }
    private final MyHandler mHandler = new MyHandler(this);*/


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        mdrawlineview = findViewById(R.id.draw);
        mscale_btn = findViewById(R.id.scale_btn);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        Log.d(LOG_TAG, "onBackPressed");
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(LOG_TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        menu.getItem(0).setEnabled(actionbar_setting_condition);
        menu.getItem(1).setEnabled(actionbar_setting_condition);
        menu.getItem(2).setEnabled(actionbar_setting_condition);
        return true;
    }


    // 觸發繪圖寫在這
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onOptionsItemSelected");
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_draw_time) {
            mscale_btn.setBackgroundResource(R.drawable.shape_scale_btn_noenable);

            Thread t = new MyThread1();
            draw_time_domain_flag = true;
            t.start();
            return true;
        }
        else if(id == R.id.action_draw_freq_amp){
            mscale_btn.setBackgroundResource(R.drawable.shape_scale_btn_linear);

            Thread t = new MyThread2();
            draw_fft_flag = true;
            Draw_amp = true;    // 畫fft強度
            t.start();
            return true;
        }
        else if(id == R.id.action_draw_freq_phase){
            // 繪製相位
            Thread t = new MyThread3();
            draw_fft_flag = true;
            Draw_phase = true;  //  畫fft相位
            t.start();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    // 每次開啟選單的時候都會呼叫此方法
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.getItem(0).setEnabled(actionbar_setting_condition);
        menu.getItem(1).setEnabled(actionbar_setting_condition);
        menu.getItem(2).setEnabled(actionbar_setting_condition);
        return super.onPrepareOptionsMenu(menu);
    }

    /*
     * 這裡撰寫連結其他頁面的方法，使用Intent
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        Log.d(LOG_TAG, "onNavigationItemSelected");

        Intent intent = new Intent();   // 宣告intent物件，用於換頁
        Bundle bundle = new Bundle();

        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.nav_ori_data) {
            // 原始數據
            intent.setClass(MainActivity.this, ori_dataActivity.class);
//            startActivity(intent);
//            finish();   // 換頁後結束此頁
            startActivityForResult(intent, ORIGINAL_PAGE);

        } else if (id == R.id.nav_fft) {
            // FFT數據參數
            intent.setClass(MainActivity.this, fftActivity.class);
            startActivityForResult(intent, FFT_PAGE);

        } else if (id == R.id.nav_filter) {
            intent.putExtra("FILTER_PAGE", FILTER_PAGE);
            intent.setClass(MainActivity.this, filterActivity.class);
            startActivityForResult(intent, FILTER_PAGE);

        } else if (id == R.id.nav_other) {
            bundle.putString("go_mV_per_AD", Float.valueOf(mV_per_AD).toString());
            intent.putExtra("go_OTHER_BUNDLE", bundle);

            intent.putExtra("OTHER_PAGE", OTHER_PAGE);  // 將主頁設定的分頁號碼傳給下一頁，就不用每個地方改
            intent.setClass(MainActivity.this, otherActivity.class);    // 設定intent要傳到哪一頁
            startActivityForResult(intent, OTHER_PAGE); // 傳遞intent，並設定requestCode

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // 使用 startActivityForResult()，需要複寫的方法
    // 換頁回來執行的方法
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(LOG_TAG, "onActivityResult()");

        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case ORIGINAL_PAGE:
                Log.d(LOG_TAG, "Run case ORIGINAL_PAGE");
                if(data.getIntExtra("read_flag", 0) == 1){
                    Bundle bundle = data.getBundleExtra("ORI_BUNDLE");
                    ori_file_name = bundle.getString("file_name");
                    ORI_sample_rate = Float.valueOf(bundle.getString("ori_samplerate"));

                    ReadFileThread(bundle.getString("file_name"));   // 背景讀檔

                    // 控制Action_bar的setting狀態
                    actionbar_setting_condition = true;
                }
                break;
            case FFT_PAGE:
                Log.d(LOG_TAG, "Run case FFT_PAGE");
                if(data.getIntExtra("fft_flag", 0) == 1){
                    Bundle bundle = data.getBundleExtra("FFT_BUNDLE");
                    FFT_length = Integer.valueOf(bundle.getString("fft_length"));
                }
                break;
            case FILTER_PAGE:
                Log.d(LOG_TAG, "Run case FILTER_PAGE");
                if(data.getIntExtra("filter_flag", 0) == 1){
                    // 計算時間
                    double startTime = System.currentTimeMillis();

                    Bundle bundle = data.getBundleExtra("FILTER_BUNDLE");
                    filter_setting = bundle.getFloatArray("filter_value_flag");
                    filter_choose = bundle.getBooleanArray("filter_chk_flag");
                    filter_type = bundle.getBooleanArray("filter_type_flag");
                    Log.d(LOG_TAG, "filter_choose: "+Boolean.toString(filter_choose[0])+" "+Boolean.toString(filter_choose[1])+" "+Boolean.toString(filter_choose[2])+" "+Boolean.toString(filter_choose[3]));

                    // 這裡this.data是指外部存原始數據的陣列，不是Intent物件！
                    if(this.data != null){
                        // 判別是IIR或FIR
                        if(filter_type[0]) {
                            Log.d(LOG_TAG, "IIR filter processing!");
                            /* 這裡必須複製一份原始數據，再丟入濾波器的function內進行運算
                             * 不然會將原始數據直接更改
                             * */
                            double[] tmp = new double[this.data.length];
                            for(int i=0; i<this.data.length; i++)
                                tmp[i] = this.data[i];
                            filtered_data = IIR_filter_func(tmp, filter_setting, filter_choose);  // IIR濾波
                        }
                        else if(filter_type[1]){
                            // 做FIR濾波
                            Log.d(LOG_TAG, "FIR filter processing!");
                            double[] tmp = new double[this.data.length];
                            for(int i=0; i<this.data.length; i++)
                                tmp[i] = this.data[i];
                            filtered_data = FIR_filter_func(tmp, filter_setting, filter_choose);
                        }
                    }
                    else{
                        Toast.makeText(MainActivity.this, "請先讀取原始訊號", Toast.LENGTH_SHORT).show();
                    }

                    // 計算時間
                    double result = (System.currentTimeMillis() - startTime)/1000f;
                    Log.d(LOG_TAG, "====== 濾波花費時間: "+result+" 秒 =========");
                }
                break;
            case OTHER_PAGE:
                Log.d(LOG_TAG, "Run case OTHER_PAGE");
                if(data.getIntExtra("other_flag", 0) == 1){
                    Bundle bundle = data.getBundleExtra("OTHER_BUNDLE");
                    mV_per_AD = Float.valueOf(bundle.getString("mV_per_AD"));
                }
                break;
        }
    }

    /* 執行thread: 繪圖 */
    // 時域繪圖
    class MyThread1 extends Thread{
        public void run(){
            super.run();
            Log.d(LOG_TAG, "ＭyThread1 running !!");
            float[] tmp = new float[data.length];

            //如果有設定任何filter，則畫出濾波的波形，否則畫出原始訊號
            if(filter_choose[0] || filter_choose[1] || filter_choose[2] || filter_choose[3]) {
                Log.d(LOG_TAG, "filter ok");
                // 畫圖需要float格式，須轉換
                for(int i=0; i<filtered_data.length; i++)
                    tmp[i] = (float)filtered_data[i];
                SaveFile(tmp, "filtered_"+ori_file_name);
            }
            else{
                Log.d(LOG_TAG, "filter no");
                // 畫圖需要float格式，須轉換
                for(int i=0; i<data.length; i++)
                    tmp[i] = (float)data[i];
                SaveFile(tmp, "ori_"+ori_file_name);
            }

            addTimePoint(tmp);  // 使用UiThread執行繪圖物件中的addTimePoint()方法
            draw_time_domain_flag = false;
        }
    }
    private void addTimePoint(final float[] point){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mdrawlineview.setDragEnable(true, 1);  // 時域圖開啟拖曳
                // (原始數據, 原始數據長度, 原始數據採樣率, 要顯示的秒數)
                mdrawlineview.addTimePoint(point, point.length, ORI_sample_rate, 3, 0);
            }
        });
    }
    // 頻域 強度 繪圖
    class MyThread2 extends Thread{
        public void run() {
            super.run();
            Log.d(LOG_TAG, "ＭyThread2 running !!");
            // 如果有設定filter
            if(filter_choose[0] || filter_choose[1] || filter_choose[2] || filter_choose[3]){
                fft_result = fft_go_amp(filtered_data, data.length, FFT_length, ORI_sample_rate, ori_file_name);
            }
            else {
                fft_result = fft_go_amp(data, data.length, FFT_length, ORI_sample_rate, ori_file_name);
            }

            // 將double轉換為float型別，因為FFT出來是double型別，但畫圖是float型別
            float[] tmp = new float[fft_result.length];
            for(int i=0; i<tmp.length; i++){
                tmp[i] = (float)fft_result[i];
            }

            addFreqPoint_amp(tmp);
            draw_fft_flag = false;  // 停止繪圖

            Draw_amp = false;
            Draw_phase = false;
        }
    }
    private void addFreqPoint_amp(final float[] point){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // (fft數據, 採樣率, FFT長度, 最小顯示範圍, 最大顯示範圍, 相位圖或強度圖, 線性或對數尺度)
                Log.d(LOG_TAG, "addFreqPoint_amp");
                mdrawlineview.setFreqScaleIsLogarithm(2);   // 選擇尺度
                mdrawlineview.setDragEnable(true, 2);   // 頻域開啟拖曳
                mdrawlineview.addFreqPoint(point, ORI_sample_rate, FFT_length, 0,ORI_sample_rate/2, 1); // 1: 繪製amp
            }
        });
    }
    // 頻域 相位 繪圖
    class MyThread3 extends Thread{
        public void run() {
            super.run();
            Log.d(LOG_TAG, "ＭyThread2 running !!");
            if(draw_fft_flag){
                // 輸入0則自動選擇最大的FFT長度進行轉換
                // Log.d(LOG_TAG, "FFT_length: "+Integer.valueOf(FFT_length).toString());

                // 如果有設定filter
                if(filter_choose[0] || filter_choose[1] || filter_choose[2] || filter_choose[3]){
                    fft_result = fft_go_phase(filtered_data, data.length, FFT_length, ORI_sample_rate, ori_file_name);
                }
                else {
                    fft_result = fft_go_phase(data, data.length, FFT_length, ORI_sample_rate, ori_file_name);
                }

                // 將double轉換為float型別，因為FFT出來是double型別，但畫圖是float型別
                float[] tmp = new float[fft_result.length];
                for(int i=0; i<tmp.length; i++){
                    tmp[i] = (float)fft_result[i];
                }

                addFreqPoint_phase(tmp);
                draw_fft_flag = false;  // 停止繪圖

                Draw_amp = false;
                Draw_phase = false;
            }
        }
    }
    private void addFreqPoint_phase(final float[] point){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG, "addFreqPoint_phase");
                mdrawlineview.setDragEnable(false, 3);
                // (fft數據, 採樣率, FFT長度, 最小顯示範圍, 最大顯示範圍, 相位圖或強度圖, 線性或對數尺度)
                mdrawlineview.addFreqPoint(point, ORI_sample_rate, FFT_length, 0,ORI_sample_rate/2, 2); // 1: 繪製phase
            }
        });
    }

    /* 讀檔 */
    private void ReadFileThread(final String file_name){
        new Thread(new Runnable() {
            @Override
            public void run() {
                data = null;
                init_filter();
                data = ReadFile(file_name); //讀取txt檔案

                // 將數據換算為電壓值
                if(mV_per_AD != 0)
                    data = mVperAD(data, mV_per_AD);


                /*// 用於更新UI的按鈕狀態（使用Handler方法）
                Message msg = mHandler.obtainMessage();
                msg.what = BTN_IS_READY;
                msg.sendToTarget();*/
            }
        }).start();
    }
    private double[] ReadFile(String filename){
        //檢查有沒有SD卡裝置
        File mSDFile = null;
        double[] record_data = null;
        double[] error_flag = new double[0];

        if(Environment.getExternalStorageState().equals( Environment.MEDIA_REMOVED)) {
            Log.d(LOG_TAG, "沒有SD卡!!!");
            return error_flag;
        }
        else {
            //取得SD卡儲存路徑
            mSDFile = Environment.getExternalStorageDirectory();

            File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid" + "/" + filename + ".txt");
            Log.d(LOG_TAG, mFile.getPath());    //  顯示路徑

            //Read text from file
            int text_num = 0;
            try {
                BufferedReader br = new BufferedReader(new FileReader(mFile));
                String line;
                while ((line = br.readLine()) != null) {
                    text_num++; // 用來記錄讀了多少data
                }

                br = new BufferedReader(new FileReader(mFile));
                DATASIZE = text_num;
                text_num = 0;
                record_data = new double[DATASIZE];
                while ((line = br.readLine()) != null) {
                    record_data[text_num] = Float.parseFloat(line); // 存成Float陣列
                    text_num++;
                }
                br.close();
            }
            catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }

            Log.d(LOG_TAG, "數據長度: "+Integer.toString(text_num));

            return record_data;
        }
    }

    /* 設定AD對應電壓的尺度 */
    private double[] mVperAD(double[] data, float mVperAD){
        //mV_per_AD = 1.5f;

        //換算為mV數值

        // 尋找翻轉前的最大值
        float[] tmp = new float[data.length];
        for(int i=0; i<data.length; i++){
            tmp[i] = (float)data[i];
        }
        Arrays.sort(tmp);
        float max_value = tmp[data.length-1];
        float min_value = tmp[0];

        // 轉換為mV
        for(int i=0; i<data.length; i++){
            //換算每個值在整個範圍的比例，再乘上 AD對應的電壓值
            data[i] = ((data[i]-min_value)/(max_value-min_value))*mVperAD;
        }

        return data;
    }

    /* 存檔 */
    private void SaveFile(float[] f, String filename){
        File mSDFile = null;
        if(Environment.getExternalStorageState().equals( Environment.MEDIA_REMOVED))
        {
            Toast.makeText(MainActivity.this , "沒有SD卡!!!" , Toast.LENGTH_SHORT ).show();
            return ;
        }
        else
        {
            //取得SD卡儲存路徑
            mSDFile = Environment.getExternalStorageDirectory();
            Log.d(LOG_TAG, mSDFile.toString());
        }
        //建立文件檔儲存路徑
        File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid");
        //若沒有檔案儲存路徑時則建立此檔案路徑
        if(!mFile.exists())
        {
            mFile.mkdirs();
        }
        //建立空間，存檔，以String的型態存檔
        String[] tmp = new String[f.length];
        try{
            FileWriter mFileWriter = new FileWriter( mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid/" + filename.toString() + ".txt", false);
            for(int i=0; i<f.length; i++) {
                tmp[i] = Double.toString(f[i]);
                mFileWriter.write(tmp[i]+"\r\n");
            }
            mFileWriter.close();
        }
        catch (Exception e){
            Log.d(LOG_TAG, e.toString());
        }
    }

    /* FFT轉換 */
    private double[] fft_go_amp(double[] ori_data, int dataSize, int fft_length, float sr, String input_file_name) {
        Log.d(LOG_TAG, "fft_go_amp");
        int size_determine = 65536;
        // 使用最大可用的FFT長度
        while (size_determine > dataSize) {
            size_determine /= 2;
        }
        // 如果無輸入，則自動選擇FFT長度
        if(fft_length == 0 || fft_length > dataSize) {
            fft_length = size_determine;
            FFT_length = fft_length;    // 畫軸線的時候需要fft長度
        }

        DoubleFFT_1D fftDo = new DoubleFFT_1D(fft_length);    // 宣告FFT物件，設定資料總長度
        double[] fft_data = new double[fft_length];     // 新增儲存FFT結果的空間
        System.arraycopy(ori_data, (ori_data.length-fft_length), fft_data, 0, fft_length);  // 將原始數據copy到新位置
        fftDo.realForward(fft_data);     // 進行轉換，取得實部與虛部

        // 計算頻譜圖
        //fft_freq_data = new double[(fft_data.length+1)/2];
        double[] fft_freq_data = get_freq_data(fft_data);
        saveFFTresult(fft_freq_data, "freq_amp_"+input_file_name+".txt");

        // axis的單位
        // (sample rate/2) / (dataSize/2)
        float bin = sr/(float)(fft_length);
        double[] freq_bin = new double[fft_freq_data.length];
        for(int i=0; i<fft_freq_data.length; i++){
            freq_bin[i] = i*bin;
        }
        saveFFTresult(freq_bin, "faxis_amp_"+input_file_name+".txt");

        return fft_freq_data;
    }
    private double[] fft_go_phase(double[] ori_data, int dataSize, int fft_length, float sr, String input_file_name){
        Log.d(LOG_TAG, "fft_go_phase");
        int size_determine = 65536;
        // 使用最大可用的FFT長度
        while (size_determine > dataSize) {
            size_determine /= 2;
        }
        // 如果無輸入，則自動選擇FFT長度
        if(fft_length == 0 || fft_length > dataSize) {
            fft_length = size_determine;
            FFT_length = fft_length;    // 畫軸線的時候需要fft長度
        }

        DoubleFFT_1D fftDo = new DoubleFFT_1D(fft_length);    // 宣告FFT物件，設定資料總長度
        double[] fft_data = new double[fft_length];     // 新增儲存FFT結果的空間
        System.arraycopy(ori_data, (ori_data.length-fft_length), fft_data, 0, fft_length);  // 將原始數據copy到新位置
        fftDo.realForward(fft_data);     // 進行轉換，取得實部與虛部

        // 計算相位
        double[] fft_phase_data = get_phase_data(fft_data);
        saveFFTresult(fft_phase_data, "freq_phase_"+input_file_name+".txt");

        // axis的單位
        // (sample rate/2) / (dataSize/2)
        float bin = sr/(float)(fft_length);
        double[] freq_bin = new double[fft_phase_data.length];
        for(int i=0; i<fft_phase_data.length; i++){
            freq_bin[i] = i*bin;
        }
        saveFFTresult(freq_bin, "faxis_phase_"+input_file_name+".txt");

        return fft_phase_data;
    }
    // 儲存FFT結果function
    private void saveFFTresult(double f[], String filename){
        File mSDFile = null;
        if(Environment.getExternalStorageState().equals( Environment.MEDIA_REMOVED))
        {
            Toast.makeText(MainActivity.this , "沒有SD卡!!!" , Toast.LENGTH_SHORT ).show();
            return ;
        }
        else
        {
            //取得SD卡儲存路徑
            mSDFile = Environment.getExternalStorageDirectory();
            Log.d(LOG_TAG, mSDFile.toString());
        }
        //建立文件檔儲存路徑
        File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid");
        //若沒有檔案儲存路徑時則建立此檔案路徑
        if(!mFile.exists())
        {
            mFile.mkdirs();
        }
        //建立空間，存檔
        String[] tmp = new String[f.length];
        try{
            FileWriter mFileWriter = new FileWriter( mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid/" + filename.toString(), false);
            for(int i=0; i<f.length; i++) {
                tmp[i] = Double.toString(f[i]);
                mFileWriter.write(tmp[i]+"\r\n");
            }
            mFileWriter.close();
        }
        catch (Exception e){
            Log.d("FFT", e.toString());
        }
    }
    // 頻譜計算，輸入a+bi格式，輸出強度
    private double[] get_freq_data(double[] fft_data){
        /*
        data數目為基數的公式：
            a[2*k] = Re[k], 0<=k<(n+1)/2
            a[2*k+1] = Im[k], 0<k<(n-1)/2
            a[1] = Im[(n-1)/2]

        data數目為偶數的公式:
            a[2*k] = Re[k], 0<=k<n/2
            a[2*k+1] = Im[k], 0<k<n/2
            a[1] = Re[n/2]
         */

        if(fft_data.length%2 != 0){
            // 分離實部與虛部，以4501個data為例
            double[] Re = new double[(fft_data.length+1)/2];  // 實數的數目, 2251
            double[] Im = new double[(fft_data.length+1)/2];  // 虛數的數目, 2251
            double[] freq = new double[(fft_data.length+1)/2];

            // 原始數據為基數，注意：Im[0]是空的
            // 分離實部與虛部
            Re[0] = fft_data[0];
            Im[(fft_data.length-1)/2] = fft_data[1];    // Im[2249] = fft[1]
            for(int i=2; i<fft_data.length-2; i+=2){    //做到4499
                Re[i/2] = fft_data[i];
                Im[i/2] = fft_data[i+1];    // Im[2249] = fft[4499]
            }
            Re[(fft_data.length-1)/2] = fft_data[fft_data.length-1];    // Re[2250] = fft[4500]

            //換算為頻率
            for(int i=0; i<(fft_data.length+1)/2; i++){
                freq[i] = Math.sqrt((Math.pow(Re[i], 2)+Math.pow(Im[i], 2)));
            }
            return freq;
        }
        else {
            // 分離實部與虛部，以4500個data為例
            double[] Re = new double[(fft_data.length)/2+1];  // 實數的數目, 2251
            double[] Im = new double[(fft_data.length)/2];  // 虛數的數目, 2250
            double[] freq = new double[(fft_data.length+1)/2];

            // 原始數據的數目為偶數，注意: Im[0], Im[2250]都是空的
            Re[0] = fft_data[0];
            for(int i=2; i<(fft_data.length); i+=2){
                Re[i/2] = fft_data[i];
                Im[i/2] = fft_data[i+1];
            }
            Re[fft_data.length/2] = fft_data[1];
            //換算為頻率
            for(int i=0; i<fft_data.length/2; i++){
                freq[i] = Math.sqrt((Math.pow(Re[i],2)+Math.pow(Im[i],2)));
            }
            return freq;
        }
    }
    // 相位計算，輸入a+bi格式，輸出相位
    private double[] get_phase_data(double[] fft_data){
        if(fft_data.length%2 != 0){
            // 分離實部與虛部，以4501個data為例
            double[] Re = new double[(fft_data.length+1)/2];  // 實數的數目, 2251
            double[] Im = new double[(fft_data.length+1)/2];  // 虛數的數目, 2251
            double[] phase = new double[(fft_data.length+1)/2];

            // 原始數據為基數，注意：Im[0]是空的
            // 分離實部與虛部
            Re[0] = fft_data[0];
            Im[(fft_data.length-1)/2] = fft_data[1];    // Im[2249] = fft[1]
            for(int i=2; i<fft_data.length-2; i+=2){    //做到4499
                Re[i/2] = fft_data[i];
                Im[i/2] = fft_data[i+1];    // Im[2249] = fft[4499]
            }
            Re[(fft_data.length-1)/2] = fft_data[fft_data.length-1];    // Re[2250] = fft[4500]

            //換算為相位
            for(int i=0; i<(fft_data.length+1)/2; i++){
                phase[i] = Math.atan2(Im[i], Re[i])*180/Math.PI;    // 計算出來為弧度。乘上180/pi變為角度
            }
            return phase;
        }
        else {
            // 分離實部與虛部，以4500個data為例
            double[] Re = new double[(fft_data.length)/2+1];  // 實數的數目, 2251
            double[] Im = new double[(fft_data.length)/2];  // 虛數的數目, 2250
            double[] phase = new double[(fft_data.length+1)/2];

            // 原始數據的數目為偶數，注意: Im[0], Im[2250]都是空的
            Re[0] = fft_data[0];
            for(int i=2; i<(fft_data.length); i+=2){
                Re[i/2] = fft_data[i];
                Im[i/2] = fft_data[i+1];
            }
            Re[fft_data.length/2] = fft_data[1];
            //換算為頻率
            for(int i=0; i<fft_data.length/2; i++){
                phase[i] = Math.atan2(Im[i], Re[i])*180/Math.PI;    // 計算出來為弧度。乘上180/pi變為角度
            }
            return phase;
        }
    }


    /* ＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊
    * filter_setting結構（長度為8的float陣列）
    * 0:基線平均點數, 1:低通截止, 2:低通階數, 3:高通截止, 4:高通階數, 5:帶陷中心頻率, 6:帶陷寬度, 7:階數
    *
    * filter_choose（長度為4的布林陣列）
    * 0:去基線, 1:低通, 2:高通, 3:帶陷
    *
    * filter_type（選擇是IIR或FIR）
    * 0: IIR, 1: FIR
    * ＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊＊ */
    // IIR濾波
    private double[] IIR_filter_func(double[] ori_data, float[] filter_setting, boolean[] filter_choose){
        Log.d(LOG_TAG, "IIR_filter_func");

        Butterworth bufferworth = new Butterworth();


//        if(filter_choose[3]) {
//            bufferworth.bandStop((int)filter_setting[7], ORI_sample_rate, filter_setting[5], filter_setting[6]);
//            for (int i = 0; i < data.length; i++) {
//                ori_data[i] = bufferworth.filter(ori_data[i]);
//            }
//        }
        // remove baseline
        if(filter_choose[0]) {
            Log.d(LOG_TAG, "IIR: remove baseline");
            ori_data = removeBaselineDrift(ori_data, (int) filter_setting[0]);
        }

        if(filter_choose[1]) {
            // Lowpass
            Log.d(LOG_TAG, "IIR: low pass");
            bufferworth.lowPass((int)filter_setting[2], ORI_sample_rate, filter_setting[1]);
            for (int i = 0; i < data.length; i++) {
                ori_data[i] = bufferworth.filter(ori_data[i]);
            }
        }

        if(filter_choose[2]) {
            Log.d(LOG_TAG, "IIR: high pass");
            /// Highpass
            bufferworth.highPass((int)filter_setting[4], ORI_sample_rate, filter_setting[3]);
            for (int i = 0; i < data.length; i++) {
                ori_data[i] = bufferworth.filter(ori_data[i]);
            }
        }

        // torch filter
        // 第一個for: 去除基頻電源雜訊與雜訊的諧頻
        if(filter_choose[3]) {
            Log.d(LOG_TAG, "IIR: notch");
            for (int k = 1; k <= (ORI_sample_rate / filter_setting[5])/2; k++) {
                bufferworth.bandStop((int) filter_setting[7], ORI_sample_rate, filter_setting[5] * k, filter_setting[6]);
                for (int i = 0; i < data.length; i++) {
                    ori_data[i] = bufferworth.filter(ori_data[i]);
                }
            }
        }
        // 完成濾波的訊號，轉換成float，畫圖需要
        return ori_data;
    }
    // 去基線
    private double[] removeBaselineDrift(double[] ori_data, int n){
        /*
        * 去除訊號基線
        * ori_data: 原始訊號
        * n: 要平均的點數
        */
        double[] modify_data = new double[ori_data.length];

        // 避免奇數，此function只針對偶數點
        if(n%2 != 0)
            n++;
        // 計算基線
        for(int i=0; i<data.length; i++){
            if(i<(n/2-1)){
                // 如果左邊不足n/2個點
                double total = 0;
                int num = 0;
                // 往左算
                for(int k=i; k>=i-(n/2-1); k--){
                    if(k < 0)
                        break;
                    total += data[k];
                    num++;
                }
                // 往右算
                for(int p=i+1; p<=i+(n/2); p++){
                    total += data[p];
                    num++;
                }
                modify_data[i] = total/num;
            }
            else if( (i+n/2) > data.length-1){
                // 如果右邊不足n/2個點
                double total=0;
                int num = 0;
                // 往左算
                for(int k=i; k>=i-(n/2-1); k--){
                    total += data[k];
                    num++;
                }
                // 往右算
                for(int p=i+1; p<=i+(n/2); p++){
                    if(p>=data.length)
                        break;
                    total += data[p];
                    num++;
                }
                modify_data[i] = total/num;
            }
            else{
                // 兩邊都足夠有n/2個點
                double total=0;
                int num = 0;
                // 往左算
                for(int k=i; k>=i-(n/2-1); k--){
                    total += data[k];
                    num++;
                }
                // 往右算
                for(int p=i+1; p<=i+(n/2); p++){
                    total += data[p];
                    num++;
                }
                modify_data[i] = total/num;
            }
        }

        // 去除基線
        for(int i=0; i<modify_data.length; i++){
            modify_data[i] = ori_data[i] - modify_data[i];
        }

        return modify_data;
    }
    // FIR濾波
    private double[] FIR_filter_func(double[] ori_data, float[] filter_setting, boolean[] filter_choose){
        Log.d(LOG_TAG, "FIR_filter_func");

        double[] low_coefs = ReadFile("FIR_LP_kaiser_f150_o100_b4_sr500");
        double[] high_coefs = ReadFile("FIR_HP_kaiser_f05_o100_b4_sr500");
//        double[] torch50_coefs = ReadFile("FIR_BS_kaiser_f45_55_o350_b12_sr500");
//        double[] torch100_coefs = ReadFile("FIR_BS_kaiser_f95_105_o350_b12_sr500");
//        double[] torch150_coefs = ReadFile("FIR_BS_kaiser_f145_155_o350_b12_sr500");
//        double[] torch200_coefs = ReadFile("FIR_BS_kaiser_f195_205_o350_b12_sr500");
        double[] torch50_coefs = ReadFile("FIR_BS_kaiser_f48_52_o780_b10_sr500");
        double[] torch100_coefs = ReadFile("FIR_BS_kaiser_f98_102_o780_b10_sr500");
        double[] torch150_coefs = ReadFile("FIR_BS_kaiser_f148_152_o780_b10_sr500");
        double[] torch200_coefs = ReadFile("FIR_BS_kaiser_f198_202_o780_b10_sr500");

        FIR kaiser_window = null;

        // remove baseline
        if(filter_choose[0]) {
            Log.d(LOG_TAG, "FIR: remove baseline");
            ori_data = removeBaselineDrift(ori_data, (int) filter_setting[0]);
        }

        // low pass
        if(filter_choose[1]) {
            Log.d(LOG_TAG, "FIR: low pass");
            kaiser_window = new FIR(low_coefs);
            for (int i = 0; i < data.length; i++) {
                ori_data[i] = kaiser_window.getOutputSample(ori_data[i]);
            }
        }

        // high pass
        if(filter_choose[2]) {
            Log.d(LOG_TAG, "FIR: high pass");
            kaiser_window = new FIR(high_coefs);

            for (int i = 0; i < data.length; i++) {
                ori_data[i] = kaiser_window.getOutputSample(ori_data[i]);
            }
        }

        // torch filter
        if(filter_choose[3]) {
            Log.d(LOG_TAG, "FIR: notch");
            kaiser_window = new FIR(torch50_coefs);
            for (int i = 0; i < data.length; i++) {
                ori_data[i] = kaiser_window.getOutputSample(ori_data[i]);
            }
            kaiser_window = new FIR(torch100_coefs);
            for (int i = 0; i < data.length; i++) {
                ori_data[i] = kaiser_window.getOutputSample(ori_data[i]);
            }
            kaiser_window = new FIR(torch150_coefs);
            for (int i = 0; i < data.length; i++) {
                ori_data[i] = kaiser_window.getOutputSample(ori_data[i]);
            }
            kaiser_window = new FIR(torch200_coefs);
            for (int i = 0; i < data.length; i++) {
                ori_data[i] = kaiser_window.getOutputSample(ori_data[i]);
            }
        }

        return ori_data;
    }
    // 初始化filter參數
    private void init_filter(){
        filter_setting = new float[8];   // 濾波器的參數
        filter_choose = new boolean[4];    // 選擇的濾波器種類
        filter_type = new boolean[2];
        filtered_data = null;          // 儲存filter後的data
    }
}


class FIR {
    private int length;
    private double[] delayLine;
    private double[] impulseResponse;
    private int count = 0;

    FIR(double[] coefs) {

        length = coefs.length;
        impulseResponse = coefs;
        delayLine = new double[length];
    }

    double getOutputSample(double inputSample) {
        delayLine[count] = inputSample;
        double result = 0.0;
        int index = count;
        for (int i=0; i<length; i++) {
            result += impulseResponse[i] * delayLine[index--];
            if (index < 0) index = length-1;
        }

        if (++count >= length) count = 0;

        return result;
    }
}
