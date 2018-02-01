package com.example.hsuweien.drawsignalline_06v4;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class filterActivity extends AppCompatActivity {

    private String LOG_TAG = "filter_page";

    private CheckBox mchk_bas, mchk_low, mchk_high, mchk_notch, mchk_iir, mchk_fir;
    private EditText mch1_edtxt1, mch2_edtxt1, mch2_edtxt2, mch3_edtxt1, mch3_edtxt2, mch4_edtxt1, mch4_edtxt2, mch4_edtxt3;
    private Button mbtn_ok, mbtn_cancel;

    private Intent intent = new Intent();
    private Bundle bundle = new Bundle();

    // Handler方法更新UI介面
    private int CHECK_IIR=0, CHECK_FIR = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "filter page: onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        mch1_edtxt1 = findViewById(R.id.ch1_edtxt1);
        mch2_edtxt1 = findViewById(R.id.ch2_edtxt1);
        mch2_edtxt2 = findViewById(R.id.ch2_edtxt2);
        mch3_edtxt1 = findViewById(R.id.ch3_edtxt1);
        mch3_edtxt2 = findViewById(R.id.ch3_edtxt2);
        mch4_edtxt1 = findViewById(R.id.ch4_edtxt1);
        mch4_edtxt2 = findViewById(R.id.ch4_edtxt2);
        mch4_edtxt3 = findViewById(R.id.ch4_edtxt3);
        mbtn_ok = findViewById(R.id.filter_btnOK);
        mbtn_cancel = findViewById(R.id.filter_btnCancel);
        mchk_bas = findViewById(R.id.chk_bas);
        mchk_low = findViewById(R.id.chk_low);
        mchk_high = findViewById(R.id.chk_high);
        mchk_notch = findViewById(R.id.chk_torch);
        mchk_iir = findViewById(R.id.chk_iir);
        mchk_fir = findViewById(R.id.chk_fir);

        //將主頁面設定的此分頁號碼傳過來
        final int PAGE_NUM = getIntent().getIntExtra("FILTER_PAGE", 9999);

        mbtn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("filter_flag", 1);  // 這裡的1是指按確定，0是指按取消

                try {
                    //數值
                    float[] value_array = new float[8];
                    value_array[0] = Float.parseFloat(mch1_edtxt1.getText().toString());
                    value_array[1] = Float.parseFloat(mch2_edtxt1.getText().toString());
                    value_array[2] = Float.parseFloat(mch2_edtxt2.getText().toString());
                    value_array[3] = Float.parseFloat(mch3_edtxt1.getText().toString());
                    value_array[4] = Float.parseFloat(mch3_edtxt2.getText().toString());
                    value_array[5] = Float.parseFloat(mch4_edtxt1.getText().toString());
                    value_array[6] = Float.parseFloat(mch4_edtxt2.getText().toString());
                    value_array[7] = Float.parseFloat(mch4_edtxt3.getText().toString());
                    bundle.putFloatArray("filter_value_flag", value_array);

                    //勾選狀態
                    boolean[] chk_array = new boolean[4];
                    chk_array[0] = mchk_bas.isChecked();
                    chk_array[1] = mchk_low.isChecked();
                    chk_array[2] = mchk_high.isChecked();
                    chk_array[3] = mchk_notch.isChecked();
                    bundle.putBooleanArray("filter_chk_flag", chk_array);

                    // 濾波器選擇
                    boolean[] type_array = new boolean[2];
                    type_array[0] = mchk_iir.isChecked();
                    type_array[1] = mchk_fir.isChecked();
                    bundle.putBooleanArray("filter_type_flag", type_array);

                    // 如果有選擇濾波器...
                    if(type_array[0] || type_array[1]) {
                        intent.putExtra("FILTER_BUNDLE", bundle);   // 將bundle物件存入intent中
                        setResult(PAGE_NUM, intent);
                        finish();
                        if(mchk_iir.isChecked())
                            Toast.makeText(filterActivity.this, "你選擇了: "+mchk_iir.getText().toString(), Toast.LENGTH_SHORT).show();
                        else if(mchk_fir.isChecked())
                            Toast.makeText(filterActivity.this, "你選擇了: "+mchk_fir.getText().toString(), Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(filterActivity.this, "請選擇濾波器類型", Toast.LENGTH_SHORT).show();
                    }
                }
                catch (Exception e){
                    Toast.makeText(filterActivity.this, "請輸入數字", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mbtn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("filter_flag", 0);
                setResult(PAGE_NUM, intent);
                finish();
            }
        });

        mchk_fir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mchk_iir.setChecked(false);

                mch1_edtxt1.setEnabled(false);
                mch2_edtxt1.setEnabled(false);
                mch2_edtxt2.setEnabled(false);
                mch3_edtxt1.setEnabled(false);
                mch3_edtxt2.setEnabled(false);
                mch4_edtxt1.setEnabled(false);
                mch4_edtxt2.setEnabled(false);
                mch4_edtxt3.setEnabled(false);

            }
        });

        mchk_iir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mchk_fir.setChecked(false);

                mch1_edtxt1.setEnabled(true);
                mch2_edtxt1.setEnabled(true);
                mch2_edtxt2.setEnabled(true);
                mch3_edtxt1.setEnabled(true);
                mch3_edtxt2.setEnabled(true);
                mch4_edtxt1.setEnabled(true);
                mch4_edtxt2.setEnabled(true);
                mch4_edtxt3.setEnabled(true);
            }
        });
    }
}
