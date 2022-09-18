package com.itri.uschart;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UsbBroadcastReceiver extends BroadcastReceiver {

    private static final String ACTION_USB_PERMISSION = "com.bnhsu.androidusbhost.USB_PERMISSION";
    private static String TAG = "UsbBroadcastReceiver";

    private UsbManager usbManager;
//    private UsbDevice usbDevice;
   // private SettingActivity settingActivity;
    //private USDisplayActivity USDisplayActivity;
   // private ItemAdapter itemAdapter;
    private UsbDeviceConnection usbDeviceConnection;
    private UsbInterface usbInterface;


    public UsbBroadcastReceiver(UsbManager usbManager){
        this.usbManager = usbManager;
        //this.usbDevice = usbDevice;
        //this.settingActivity = settingActivity;
        //this.itemAdapter = itemAdapter;

        //this.USDisplayActivity = USDisplayActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();


        if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
            synchronized (this) {
                UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                //HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
                //List<UsbDevice> usbDeviceList = new ArrayList<>(deviceHashMap.values());
                //settingActivity.recyclerView.setAdapter(new ItemAdapter(usbDeviceList, usbManager));

                if(usbDevice != null){
                    //

                    Log.d(TAG,"DEATTCHED-" + usbDevice.getProductName());

                }
            }
        }
//
        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {

            HashMap<String, UsbDevice> deviceHashMap = usbManager.getDeviceList();
            List<UsbDevice> usbDeviceList = new ArrayList<>(deviceHashMap.values());
            //settingActivity.recyclerView.setAdapter(new ItemAdapter(usbDeviceList,usbManager));

            synchronized (this) {
                UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                    if(usbDevice != null){
                        //

                        Log.d(TAG,"ATTACHED-" + usbDevice.getProductId());
                    }
                }
                else {
                    Log.d(TAG,"ATTACHED-" + usbDevice +"Try to get permission");
                    /*
                    PendingIntent mPermissionIntent;
                    mPermissionIntent = PendingIntent.getBroadcast(settingActivity, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT);
                    usbManager.requestPermission(usbDevice, mPermissionIntent);
                    */
                }
            }
        }
//
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice usbDevice = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {

                    if(usbDevice != null){
                        //
                        Log.d(TAG,"PERMISSION-" + usbDevice);
                    }
                }
            }
        }
    }
}
