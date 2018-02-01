package com.example.hsuweien.drawsignalline_06v4;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class fftActivity extends AppCompatActivity {

    private Button mbtnOK, mbtnCancel;
    private EditText medtxt1, medtxt2;

    private Intent intent = new Intent();
    private Bundle bundle = new Bundle();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fft);

        mbtnOK = findViewById(R.id.fft_ok);
        mbtnCancel = findViewById(R.id.fft_cancel);

        medtxt2 = findViewById(R.id.fft_edtxt2);


        mbtnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String fft_length = medtxt2.getText().toString();
                intent.putExtra("fft_flag", 1);
                bundle.putString("fft_length", fft_length);
                intent.putExtra("FFT_BUNDLE", bundle);
                setResult(1, intent);
                finish();
            }
        });

        // 不傳值
        mbtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent.putExtra("fft_flag", 0);
                setResult(1, intent);
                finish();
            }
        });
    }
}
