package com.example.hsuweien.drawsignalline_06v4;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class otherActivity extends AppCompatActivity {

    private Intent intent = new Intent();
    private Bundle bundle = new Bundle();

    Button mbtnOK, mbtnCancel;
    EditText mmV_per_AD;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);

        final int PAGE_NUM = getIntent().getIntExtra("OTHER_PAGE", 9999);
        mbtnOK = findViewById(R.id.other_btnOK);
        mbtnCancel = findViewById(R.id.other_btnCancel);
        mmV_per_AD = findViewById(R.id.edtxt_AD_per_mV);

        Bundle bundle_main = getIntent().getBundleExtra("go_OTHER_BUNDLE");  // 用getIntent()直接取得bundle物件對應的key
        mmV_per_AD.setText(bundle_main.getString("go_mV_per_AD"));   //再取得bunlde裡面的key來取值

        mbtnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("other_flag", 1);   // 這裡的1是指按確定，0是指按取消
                bundle.putString("mV_per_AD", mmV_per_AD.getText().toString());
                intent.putExtra("OTHER_BUNDLE", bundle);

                setResult(PAGE_NUM, intent);
                finish();
            }
        });
        mbtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("other_flag", 0);
                setResult(PAGE_NUM, intent);
                finish();
            }
        });

    }
}
