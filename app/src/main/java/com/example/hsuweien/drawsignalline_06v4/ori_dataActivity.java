package com.example.hsuweien.drawsignalline_06v4;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class ori_dataActivity extends AppCompatActivity {

    String LOG_TAG = "main";

    private Button mbtn1, mbtn2, mbtn3;
    private EditText medtxt1, medtxt_sampleRate;
    private TextView mtxt1;

    Intent intent = new Intent();
    Bundle bundle = new Bundle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ori_data);

        mbtn2 = findViewById(R.id.ori_btn2);
        mbtn3 = findViewById(R.id.ori_btn3);
        medtxt1 = findViewById(R.id.ori_edtxt1);
        medtxt_sampleRate = findViewById(R.id.ori_edtxt2);

        // 確定按鈕
        mbtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(checkFileIsExist(medtxt1.getText().toString())) {
                    intent.putExtra("read_flag", 1);
                    bundle.putString("file_name", medtxt1.getText().toString());
                    bundle.putString("ori_samplerate", medtxt_sampleRate.getText().toString());
                    intent.putExtra("ORI_BUNDLE", bundle);
//                intent.setClass(ori_dataActivity.this, MainActivity.class);
//                startActivity(intent);
                    setResult(0, intent);
                    finish();   // 換頁後結束此頁
                }
            }
        });

        // 取消按鈕
        mbtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("read_flag", 0);
                setResult(0, intent);
                finish();   // 換頁後結束此頁
            }
        });
    }

    public boolean checkFileIsExist(String filename){
        File mSDFile = null;

        if(Environment.getExternalStorageState().equals( Environment.MEDIA_REMOVED)) {
            Toast.makeText(ori_dataActivity.this, "沒有SD卡！", Toast.LENGTH_SHORT).show();
            return false;
        }
        else{
            //取得SD卡儲存路徑
            mSDFile = Environment.getExternalStorageDirectory();

            File mFile = new File(mSDFile.getParent() + "/" + mSDFile.getName() + "/MyAndroid" + "/" + filename + ".txt");
//            Log.d(LOG_TAG, mFile.getPath());    //  顯示路徑
            if(mFile.exists()){
                Toast.makeText(ori_dataActivity.this, "讀取成功", Toast.LENGTH_SHORT).show();
                return true;
            }
            else{
                Toast.makeText(ori_dataActivity.this, "讀取失敗，請確認檔案是否存在", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
    }
}
