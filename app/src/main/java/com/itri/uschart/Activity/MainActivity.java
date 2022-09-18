package com.itri.uschart.Activity;

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

public class MainActivity extends AppCompatActivity {

    TextView textView;
    Button button2;
    RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getSupportActionBar().hide();
        textView = findViewById(R.id.usb_otg_support);
        button2 = findViewById(R.id.get_usb_otg_devices);
        recyclerView = findViewById(R.id.list_usb_otg);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));



        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
                List<UsbDevice> usbDeviceList = new ArrayList<>(deviceHashMap.values());

                recyclerView.setAdapter(new ItemAdapter(usbDeviceList));
            }
        });
    }
}
