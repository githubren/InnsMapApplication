package com.example.yfsl.innsmapapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.innsmap.InnsMap.INNSMapSDK;
import com.innsmap.InnsMap.INNSMapView;
import com.innsmap.InnsMap.net.http.listener.forout.NetMapLoadListener;

public class MainActivity extends AppCompatActivity {
    private INNSMapView mInnsmapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        INNSMapSDK.init(getApplicationContext());
        setContentView(R.layout.activity_main);

        mInnsmapView = findViewById(R.id.inns_map);
        mInnsmapView.loadMap("a18558598ad343c8ba7dfa0984ecf8bf","27d42593fcf34e51b88ce443f1a6d47c" , new NetMapLoadListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onFail(String s) {
                Toast.makeText(MainActivity.this,"初始化失败："+s,Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mInnsmapView.onDestroy();
    }
}
