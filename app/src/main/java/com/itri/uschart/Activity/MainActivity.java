package com.itri.uschart.Activity;
import android.content.Intent;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.itri.uschart.ItemAdapter;
import com.itri.uschart.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    Button button2;
    RecyclerView recyclerView;
    EditText Name_txt,Number_txt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getSupportActionBar().hide();
        textView = findViewById(R.id.usb_otg_support);
        button2 = findViewById(R.id.get_usb_otg_devices);
        recyclerView = findViewById(R.id.list_usb_otg);

        Name_txt = findViewById(R.id.EditName);
        Number_txt = findViewById(R.id.EditNumber);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));



        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
                List<UsbDevice> usbDeviceList = new ArrayList<>(deviceHashMap.values());
                String Name = Name_txt.getText().toString(); // 用getText取得輸入在Name_txt的內容
                Intent intent = new Intent(MainActivity.this,USChartActivity.class);
                // 建立Intent設定要前往的頁面

                Bundle bundle = new Bundle();
                bundle.putString("name", Name);
                // 建立Bundle並放入name

                intent.putExtras(bundle);
                recyclerView.setAdapter(new ItemAdapter(usbDeviceList));

                //將bundle交給intent

//                startActivity(intent);
                //啟動intent，將帶著bundle前往另外一個Activity

            }
        });
    }
}
