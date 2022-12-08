package com.itri.uschart.Activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.widget.EditText;
import android.widget.TextView;

import android.graphics.Rect;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.content.ComponentCallbacks2;


import java.util.*;
import java.lang.*;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.itri.uschart.DataSaveToFile;
import com.itri.uschart.R;

import org.apache.commons.math3.util.MathArrays;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


public class USChartActivity extends AppCompatActivity
    implements ComponentCallbacks2{
    //private static final byte CR  = 0x0d;
    //private static final byte LF  = 0x0a;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static String TAG = "UsbHost";

    private UsbManager usbManager;
    private PendingIntent mPermissionIntent;
    private UsbDeviceConnection deviceConnection;
    //private UsbSerialDevice serialDevice;
    private UsbDevice device;

    UsbInterface usbInterface;
    UsbEndpoint endpointIn;
    UsbEndpoint endpointOut;


    ToggleButton toggle_ReceiveData;


    private boolean forceClaim = true;
    static int TIMEOUT = 100;


    AtomicInteger AdjustLength = new AtomicInteger(4096);
    private int PacketSize = 8192;
    private int dataSize = PacketSize/2;
    private int displayDataSize = 2048;

    private byte[] PacketBuffer = new byte[PacketSize];
    boolean isRunning = false;
    boolean isDataProcessRunning = true;
    boolean isRecord = false;
    boolean isTracking = false; //初始化是否有按下追蹤按鈕
    BlockingQueue<byte[]> UsbReceivedFiFOQueue = new LinkedBlockingQueue<byte[]>(Integer.MAX_VALUE); //USB data Queue
    BlockingQueue<double[]> RF_modeFiFOQueue = new LinkedBlockingQueue<double[]>(Integer.MAX_VALUE); //RF-mode data Queue
    BlockingQueue<double[]> FindMaxFiFOQueue = new LinkedBlockingQueue<double[]>(Integer.MAX_VALUE); //FindMax data Queue
    BlockingQueue<int[]> M_modeFiFOQueue = new LinkedBlockingQueue<int[]>(Integer.MAX_VALUE);//Every //M-mode data Queue
    private byte[] UsbFIFOData = new byte[PacketSize];
    private double[] RF_modeFIFOData = new double[dataSize];
    private double[] FindMaxFIFOData = new double[dataSize];

    private int[] M_modeFIFOData = new int[dataSize];
    byte[] dataSend;

//    BlockingQueue<double[]> SaveRawDataQueue = new LinkedBlockingQueue<double[]>(Integer.MAX_VALUE);
    private Handler handler;

    private long timeStart,timeEnd, executiveTime;
    private int maxvalueloc=100;
    private DataSaveToFile dataSaveToFile;
    private DataSaveToFile imageSaveToFile;
    private double[] SaveRawData = new double[dataSize];
    private LineChart mChart,TrackChart;
    private ImageView M_modeImage, NeedleTipPosition,TrackingMmode;


//    private EditText max_editText, min_editText, max_XeditText;
    private SeekBar gain_seekBar, Depth_seekBar, Amplitude_seekBar, M_modeSeekBar;
    private Button ScreenShot;
    private float displayTime = 0.f;
    private float Depth = 5.f;
    private float gain = 6.f;
    private float GainText = (float) (20 * Math.log10(gain));
    public double contrast = 1.5;
    private int contrast_i = 6; // *0.25 之前
    private float Amplitude = (float) (500 * 1.8 / 4096);
    private TextView GainDisplay, DepthDisplay, AmplitudeDisplay,M_modeSeekBarDisplay ;
    private TextView depth1, depth2, depth3, depth4, depth5;
    private TextView Time2, Time3, Time4, Time5;
    private int DepthX;
    private String FileFolderNameDate;

    private int Amplitude_p = 500;
    private int M_modeImageWidth = 900; //原本是900
    private int M_modeImageHeight = 2048;
    int[][] M_modeArray = new int[M_modeImageHeight][M_modeImageWidth];
    int DisplayLines;

    private Button dataSaveButton;
    private Button Gainplus,Gainsub,Depthplus,Depthsub,Ampplus,Ampsub,Mmodecontrastplus,Mmodecontrastsub; //新增調整介面的按鈕資料
    AtomicBoolean RecordOn = new AtomicBoolean(false);
    AtomicBoolean TrackingOn = new AtomicBoolean(false);

    private Button Tracking;



    @Override // 使UI為全螢幕顯示
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN //隱藏狀態列
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION //隱藏導航欄
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        this.getSupportActionBar().hide();
        initialSetup();
        // 取得傳過來的bundle
        Bundle bundle = getIntent().getExtras();
        // 取得bundle中的name這個字串
        String name_ = bundle.getString("name");

        //連結介面檔中的元件
        TextView INFO_name = findViewById(R.id.Name_keyin);; // 顯示受試者資料
        INFO_name.setText(name_);

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initialSetup(){
        toggle_ReceiveData = findViewById(R.id.toggleButton); // 建立一個可以切換模式的按鈕


        //gain值調整介面
        GainDisplay = (TextView) findViewById(R.id.Gain_textview);
        Gainplus = findViewById(R.id.gainplus);
        Gainsub = findViewById(R.id.gainsub);
        //raw data 儲存按鈕
        dataSaveButton = (Button) findViewById(R.id.button);

        //深度調整介面
        Depthplus = findViewById(R.id.Depthplus);
        Depthsub = findViewById(R.id.Depthsub);
        DepthDisplay = (TextView) findViewById(R.id.max_Xtextview);

        //振幅調整介面
        Ampplus = findViewById(R.id.Ampplus);
        Ampsub = findViewById(R.id.Ampsub);
        AmplitudeDisplay = (TextView) findViewById(R.id.max_Ytextview);

        //M-mode對比調整介面
        Mmodecontrastplus = findViewById(R.id.Mmodecontrastplus);
        Mmodecontrastsub = findViewById(R.id.Mmodecontrastsub);

        // 設定韌體指令
        dataSend = new byte[2];
        dataSend[0] = 0x00;
        dataSend[1] = 0X25; // 告知韌體設定成20MHZ

        //截圖按鈕
        ScreenShot=(Button) findViewById(R.id.Screenshotbutton);
        //追蹤按鈕
        Tracking=(Button) findViewById(R.id.TrackingButton);
        //RF-mode顯示介面
        mChart = (LineChart) findViewById(R.id.chart_line);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setHorizontalScrollBarEnabled(true);
        mChart.getLegend().setEnabled(false); // Remove Legend
        mChart.getDescription().setEnabled(false); //remove description
        //tracking顯示介面
        TrackChart = (LineChart) findViewById(R.id.chart_line_tracking);
        TrackChart.setDragEnabled(true);
        TrackChart.setScaleEnabled(true);
        TrackChart.setHorizontalScrollBarEnabled(true);
        TrackChart.getLegend().setEnabled(false); // Remove Legend
        TrackChart.getDescription().setEnabled(false); //remove description

//        RecordTime = (TextView) findViewById(R.id.RecordTime);
        //M-mode深度座標
        depth1 = (TextView) findViewById(R.id.M_modeDepth0mm);
        depth1.setText(0 + " mm --");
        depth1.setTextColor(Color.rgb(255,255, 255));
        depth2 = (TextView) findViewById(R.id.M_modeDepth2mm);
        depth2.setText(Depth / 4 * 1 + " mm --");
        depth2.setTextColor(Color.rgb(255,255, 255));
        depth3 = (TextView) findViewById(R.id.M_modeDepth4mm);
        depth3.setText(Depth / 4 * 2 + " mm --");
        depth3.setTextColor(Color.rgb(255,255, 255));
        depth4 = (TextView) findViewById(R.id.M_modeDepth6mm);
        depth4.setText(Depth / 4 * 3 + " mm --");
        depth4.setTextColor(Color.rgb(255,255, 255));
        depth5 = (TextView) findViewById(R.id.M_modeDepth8mm);
        depth5.setText(Depth + " mm --");
        depth5.setTextColor(Color.rgb(255,255, 255));

        //M-mode時間座標
        Time2 = (TextView) findViewById(R.id.M_modeTime2nds);
        Time3 = (TextView) findViewById(R.id.M_modeTime3rds);
        Time4 = (TextView) findViewById(R.id.M_modeTime4ths);
        Time5 = (TextView) findViewById(R.id.M_modeTime5ths);

        //M-mode影像顯示介面
        M_modeImage = (ImageView) findViewById(R.id.M_mode_Bitmap);
        NeedleTipPosition = (ImageView) findViewById(R.id.NeedleTipPosition);
        TrackingMmode = findViewById(R.id.TrackingMmode);

        //USB設定
        device = getIntent().getParcelableExtra("Device");
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        usbManager.requestPermission(device, mPermissionIntent);
        toggle_ReceiveData.setTextColor(Color.rgb(215,73, 20));

        //存檔設定
        dataSaveToFile = new DataSaveToFile(this);
        imageSaveToFile = new DataSaveToFile(this);
        SaveRawDataSetup();
        TrackingSetup(); //初始化設定 去確保按鈕是否有按下


        Log.i(TAG, "Get counter, New thread START");

        deviceConnection = usbManager.openDevice(device);
        Log.i(TAG, "Device connected.");
        usbInterface = device.getInterface(0);
        endpointOut = usbInterface.getEndpoint(0);
        endpointIn = usbInterface.getEndpoint(1);
        deviceConnection.claimInterface(usbInterface, forceClaim);
        Log.i(TAG, "deviceConnection.claimInterface");

        isRunning = true;
        Thread usbrecieveThread = new Thread(new UsbReceiveThread());
        usbrecieveThread.start(); //接收USB data執行緒
        DataProcessingThread(); //data前處理執行緒
        Signal_MaximumThread(); //尋找最大值執行緒
        RFChartingThread(); //RF-mode執行緒
        M_modeDisplayThread(); //M-mode執行緒




        //KEYINNAME = findViewById(alert.R.id.EditName);
        //INFO.setText(KEYINNAME.getText());

        toggle_ReceiveData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    isDataProcessRunning = !isDataProcessRunning;
                    if(!isDataProcessRunning){
                        toggle_ReceiveData.setTextColor(Color.rgb(1,172,3));
                    }else{
                        toggle_ReceiveData.setTextColor(Color.rgb(215,73, 20));
                    }

            }
        });

        //截圖按鈕按下後的指令
        ScreenShot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Bitmap RF_modeScreenShot = getScreenShot();
                String RF_modeImageName = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".jpg");
                imageSaveToFile.extelnalPrivateCreateFolerImageCapture("Screenshot",RF_modeImageName,RF_modeScreenShot);
            }
        });

        /////////////////////////////Gain 按鈕設定區域//////////////////////////////////////////
        Gainplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(gain<10){
                    gain++;
                    GainText = (float)(20*Math.log10(gain));
                    GainDisplay.setText( Math.round(GainText * 10.0) / 10.0 + " dB");
                    GainDisplay.setTextColor(Color.rgb(255,255, 255));
                }else{
                    GainText = (float)(20*Math.log10(gain));
                    GainDisplay.setText( Math.round(GainText * 10.0) / 10.0 + " dB");
                    GainDisplay.setTextColor(Color.rgb(255,255, 255));
                }
            }
        });
        Gainsub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(gain>1){
                    gain--;
                    GainText = (float)(20*Math.log10(gain));
                    GainDisplay.setText( Math.round(GainText * 10.0) / 10.0 + " dB");
                    GainDisplay.setTextColor(Color.rgb(255,255, 255));
                }else{
                    GainText = (float)(20*Math.log10(gain));
                    GainDisplay.setText( Math.round(GainText * 10.0) / 10.0 + " dB");
                    GainDisplay.setTextColor(Color.rgb(255,255, 255));
                }
            }
        });
        GainText = (float)(20*Math.log10(gain));
        GainDisplay.setText( Math.round(GainText * 10.0) / 10.0 + " dB");
        GainDisplay.setTextColor(Color.rgb(255,255, 255));


        /////////////////////////////Gain 按鈕設定區域//////////////////////////////////////////

        NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));
        /////////////////////////////Depth 按鈕設定區域//////////////////////////////////////////

        Depthplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(Depth<12){
                    Depth++;
                    DepthDisplay.setText(Depth + " mm");
                    DepthDisplay.setTextColor(Color.rgb(255,255, 255));
                    depth1.setText(0.0 + " mm --");
                    depth2.setText(Depth / 4 * 1 + " mm --");
                    depth3.setText(Depth / 4 * 2 + " mm --");
                    depth4.setText(Depth / 4 * 3 + " mm --");
                    depth5.setText(Depth + " mm --");
                    NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));

                }else{
                    DepthDisplay.setText(Depth + " mm");
                    DepthDisplay.setTextColor(Color.rgb(255,255, 255));
                    depth1.setText(0.0 + " mm --");
                    depth2.setText(Depth / 4 * 1 + " mm --");
                    depth3.setText(Depth / 4 * 2 + " mm --");
                    depth4.setText(Depth / 4 * 3 + " mm --");
                    depth5.setText(Depth + " mm --");
                    NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));

                }
            }
        });
        Depthsub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(Depth>4){
                    Depth--;
                    DepthDisplay.setText(Depth + " mm");
                    DepthDisplay.setTextColor(Color.rgb(255,255, 255));
                    depth1.setText(0.0 + " mm --");
                    depth2.setText(Depth / 4 * 1 + " mm --");
                    depth3.setText(Depth / 4 * 2 + " mm --");
                    depth4.setText(Depth / 4 * 3 + " mm --");
                    depth5.setText(Depth + " mm --");
                    NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));

                }else{
                    DepthDisplay.setText(Depth + " mm");
                    DepthDisplay.setTextColor(Color.rgb(255,255, 255));
                    depth1.setText(0.0 + " mm --");
                    depth2.setText(Depth / 4 * 1 + " mm --");
                    depth3.setText(Depth / 4 * 2 + " mm --");
                    depth4.setText(Depth / 4 * 3 + " mm --");
                    depth5.setText(Depth + " mm --");
                    NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));

                }
            }
        });

        DepthDisplay.setText(Depth + " mm");
        DepthDisplay.setTextColor(Color.rgb(255,255, 255));
        depth1.setText(0.0 + " mm --");
        depth2.setText(Depth / 4 * 1 + " mm --");
        depth3.setText(Depth / 4 * 2 + " mm --");
        depth4.setText(Depth / 4 * 3 + " mm --");
        depth5.setText(Depth + " mm --");


        DepthX = (int) Math.round(Depth / 6.16 * 1000);

        /////////////////////////////Depth 按鈕設定區域//////////////////////////////////////////

        /////////////////////////////Amplitude 按鈕設定區域/////////////////////////////////////

        Ampplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(Amplitude_p<2300){
                    Amplitude_p=Amplitude_p+200;
                    Amplitude= (float) (Amplitude_p* 1.8 / 4096);
                    AmplitudeDisplay.setText(Math.round(Amplitude * 100.0) / 100.0 + "V");
                    AmplitudeDisplay.setTextColor(Color.rgb(255,255, 255));
                }else{
                    Amplitude= (float) (Amplitude_p* 1.8 / 4096);
                    AmplitudeDisplay.setText(Math.round(Amplitude * 100.0) / 100.0 + "V");
                    AmplitudeDisplay.setTextColor(Color.rgb(255,255, 255));
                }
            }
        });
        Ampsub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(Amplitude_p>300){
                    Amplitude_p=Amplitude_p-200;
                    Amplitude= (float) (Amplitude_p* 1.8 / 4096);
                    AmplitudeDisplay.setText(Math.round(Amplitude * 100.0) / 100.0 + "V");
                    AmplitudeDisplay.setTextColor(Color.rgb(255,255, 255));
                }else{
                    Amplitude= (float) (Amplitude_p* 1.8 / 4096);
                    AmplitudeDisplay.setText(Math.round(Amplitude * 100.0) / 100.0 + "V");
                    AmplitudeDisplay.setTextColor(Color.rgb(255,255, 255));
                }
            }
        });
        Amplitude= (float) (Amplitude_p* 1.8 / 4096);
        AmplitudeDisplay.setText(Math.round(Amplitude * 100.0) / 100.0 + "V");

        /////////////////////////////Amplitude 按鈕設定區域//////////////////////////////////////////

        //M-mode對比度調整指令

        Mmodecontrastplus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(contrast_i<11){
                    contrast_i = contrast_i+1;
                    contrast = contrast_i*0.25;
                }else{
                    contrast = contrast_i*0.25;
                }
            }
        });
        Mmodecontrastsub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(contrast_i>1){
                    contrast_i = contrast_i-1;
                    contrast = contrast_i*0.25;
                }else{
                    contrast = contrast_i*0.25;
                }
            }
        });
        contrast = contrast_i*0.25;





    }

    //接收USB data執行緒
    public class UsbReceiveThread implements Runnable {

        public void run() {
            while(isRunning){
                CommandSend(); //發送指令給pulser
                byte[] receiveData = ReceiveData(); //接收data
                UsbReceivedFiFOQueue.add(receiveData); //建立給data前處理執行緒的Queue
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Delay the duration of handshake time
            }
        }
    }

    //發送指令給pulser
    private void CommandSend(){
        int ret = deviceConnection.bulkTransfer(endpointOut, dataSend, dataSend.length, TIMEOUT);
        Log.i(TAG, "CommandSend: "+ Arrays.toString(dataSend));
        Log.i(TAG, "bulkTransfer out, ret:" + ret);
    }

    //接收data (Every Received is 16384 bytes)
    private byte[] ReceiveData(){
        //timeStart = System.currentTimeMillis();
        // bulk transfer
        int ret = deviceConnection.bulkTransfer(endpointIn, PacketBuffer, PacketBuffer.length, TIMEOUT);
        Log.i(TAG, "usbrecieveThread ReceiveData ret:" + ret);

        return PacketBuffer;
    }

    //data前處理執行緒
    public void DataProcessingThread(){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                int i = 0; //raw data 儲存檔名順序
                while (isRunning) {
                    //從Queue取得data
                    try {
                        UsbFIFOData = UsbReceivedFiFOQueue.take();
                        Log.i(TAG, "run: UsbFrameFiFOQueue.take();");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //raw data前處理
                    if (UsbFIFOData != null) {
                        double[] RawData = DataProcessing(UsbFIFOData); //RF-mode raw data前處理

                        if (isDataProcessRunning) {
                            RF_modeFiFOQueue.add(RawData); //建立給RF-mode執行緒的Queue
                            FindMaxFiFOQueue.add(RawData); //建立給FindMax使用的Queue
                            int[] GrayScaleData = M_modeDataProcessing(RawData); //M-mode data前處理
                            M_modeFiFOQueue.add(GrayScaleData); //建立給M-mode執行緒的Queue
                            // 判斷儲存功能啟動
                            if (isRecord) {
                                SaveRawDataOn(i, RawData); //儲存raw data
                                i++;
                            } else if (!isRecord) {
                                i = 0; //raw data 儲存檔名順序歸零
                            }
                        }

                    }
                    try {
                        Thread.sleep(20); //20可
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    //RF-mode raw data前處理
    @RequiresApi(api = Build.VERSION_CODES.N)
    private double[] DataProcessing(byte[] FrameData){
        int[] CheckedData = ParserAndHeadCheck(FrameData); //trigger
        double[] convData = Arrays.copyOfRange(convolutionFilter(CheckedData), 131, displayDataSize+131 ); //前處理濾波
        //代入gain值
        double[] RFmodeData  = new double[displayDataSize];
        for(int i =0; i<displayDataSize; i++) {
            RFmodeData [i] = (double) (convData[i]+13) * gain;
        }
        return RFmodeData; //回傳raw data
    }

    //trigger
    private int[] ParserAndHeadCheck(byte[] PacketData){
        int[] DataBuferr = new int[dataSize];
        int[] CheckedData = new int[dataSize];
        int head = 0;
        for(int i =0; i<dataSize; i++) {
            DataBuferr[i] = ((PacketData[i * 2] & 0xFF)) | ((PacketData[i * 2 + 1] & 0xFF) << 8);
            if(DataBuferr[i]==0xffff){
                head = i;
            }
        }
        if( head != 0 ){
            System.arraycopy(DataBuferr, head, CheckedData, 0, dataSize-head);
            System.arraycopy(DataBuferr, 0, CheckedData, dataSize-head, head);
        }else {
            CheckedData = DataBuferr;
        }
        return CheckedData;
    }

    double[] BPF15_30 = {0.003344942,-0.000977696,-0.001311405,-0.000232548,-0.000952622,-0.005074854,-0.00628393,
            0.001705211,0.012026327,0.010237207,-0.004361026,-0.01442579,-0.008223024,0.003473931,0.00488987,-0.000277791,
            0.002216285,0.01007731,0.006862005,-0.008274433,-0.015379445,-0.005296733,0.005051356,0.00187589,-0.001676316,
            0.009019923,0.018813798,0.005125798,-0.019709593,-0.022410024,-0.002115955,0.008010333,-0.001655737,-0.00020017,
            0.024440028,0.032636101,-0.004035874,-0.044237703,-0.033627814,0.00575005,0.010252487,-0.011548001,0.0138818,
            0.083119215,0.077770982,-0.059769871,-0.187560199,-0.118468401,0.107343062,0.233333068,0.107343062,-0.118468401,
            -0.187560199,-0.059769871,0.077770982,0.083119215,0.0138818,-0.011548001,0.010252487,0.00575005,-0.033627814,
            -0.044237703,-0.004035874,0.032636101,0.024440028,-0.00020017,-0.001655737,0.008010333,-0.002115955,-0.022410024,
            -0.019709593,0.005125798,0.018813798,0.009019923,-0.001676316,0.00187589,0.005051356,-0.005296733,-0.015379445,
            -0.008274433,0.006862005,0.01007731,0.002216285,-0.000277791,0.00488987,0.003473931,-0.008223024,-0.01442579,
            -0.004361026,0.010237207,0.012026327,0.001705211,-0.00628393,-0.005074854,-0.000952622,-0.000232548,-0.001311405,-0.000977696,0.003344942};//15-30BPF


    //濾波
    @RequiresApi(api = Build.VERSION_CODES.N)
    private double[] convolutionFilter(int[] inputs){
        double[] doubles = Arrays.stream(inputs).asDoubleStream().toArray();
        //判斷適合的濾波器
        //由於都是20MHz因此不需要有判斷式了
        return MathArrays.convolve(doubles, BPF15_30);
    }

    //M-mode data前處理
    @RequiresApi(api = Build.VERSION_CODES.N)
    private int[] M_modeDataProcessing(double[] RawData){
        double[] envelopeData = Demodulation(RawData); //包絡峰處理
        int[] Log10toGrayScaleData = Log10toGrayScale(envelopeData); //log壓縮 8bits data
        return Log10toGrayScaleData; //回傳Ｍ-mode 8bits data
    }


    double[] sineModulation = {0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,
            0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,
            -0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1
    };
    double[] cosineModulation = {1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,-0,-1,0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-
            0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,
            0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,
            -1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,
            -1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,0,1,0,-1,-0,1,-0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,
            1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,
            -1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,
            0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,
            -0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0,1,0,-1,-0
    };
    double[] demodulationFilter = {0.0136763496502696,-0.00462367708994205,-0.0110424821959952,-0.00795589268795076,0.000477471970939969,0.000998669568356882,-0.00599176092820712,
            -0.00651231381030082,0.00222446844834954,0.00558798958370901,-0.00282542699184286,-0.00814489399302558,0.000340121332136294,0.0085950401119161,0.00121427293559214,
            -0.00974048777398372,-0.00400179039887473,0.00984032827916623,0.00679128580313259,-0.00960591731283685,-0.0101635411110049,0.00840816143370031,0.0136608471643519,
            -0.0063313483510353,-0.0173752602100055,0.00303092387778522,0.021066032110272,0.00169470523726904,-0.0246586230823006,-0.00828492237063172,0.0279821011408734,0.017483301927242,
            -0.0309159042548833,-0.0309070416799635,0.033335791464369,0.0528982729741412,-0.0351370179422017,-0.0994990459330031,0.0362641112141453,0.316079074515808,0.463362278364336,
            0.316079074515808,0.0362641112141453,-0.0994990459330031,-0.0351370179422017,0.0528982729741412,0.033335791464369,-0.0309070416799635,-0.0309159042548833,0.017483301927242,
            0.0279821011408734,-0.00828492237063172,-0.0246586230823006,0.00169470523726904,0.021066032110272,0.00303092387778522,-0.0173752602100055,-0.0063313483510353,0.0136608471643519,
            0.00840816143370031,-0.0101635411110049,-0.00960591731283685,0.00679128580313259,0.00984032827916623,-0.00400179039887473,-0.00974048777398372,0.00121427293559214,
            0.0085950401119161,0.000340121332136294,-0.00814489399302558,-0.00282542699184286,0.00558798958370901,0.00222446844834954,-0.00651231381030082,-0.00599176092820712,
            0.000998669568356882,0.000477471970939969,-0.00795589268795076,-0.0110424821959952,-0.00462367708994205,0.0136763496502696
    };

    //包絡峰處理
    @NotNull
    @RequiresApi(api = Build.VERSION_CODES.N)
    private double[] Demodulation(double[] convData) {
        int DisplayDepth = (int)Math.round(Depth/6.16*1000);
        double[] sineModulatedＷave = MathArrays.ebeMultiply(convData,Arrays.copyOfRange(sineModulation, 0,  displayDataSize)); //raw data * sin
        double[] sineFilterWave = MathArrays.convolve(sineModulatedＷave, demodulationFilter); //sinData濾波
        double[] sineLPF = Arrays.copyOfRange(sineFilterWave, 42, displayDataSize+42 ); //頭尾去0

        double[] cosineModulatedＷave = MathArrays.ebeMultiply(convData,Arrays.copyOfRange(cosineModulation, 0,  displayDataSize)); //raw data * cos
        double[] cosineFilterWave = MathArrays.convolve(cosineModulatedＷave, demodulationFilter); //cosData濾波
        double[] cosineLPF = Arrays.copyOfRange(cosineFilterWave, 42, displayDataSize+42 ); //頭尾去0
        //取絕對值
        double[] demodulatedWave = new double[displayDataSize];
        for(int i = 0; i<DisplayDepth; i++) {
            demodulatedWave[i] = Math.hypot(sineLPF[i], cosineLPF[i]) * 2;
        }
        return demodulatedWave; //回傳包絡峰data
    }

    //log壓縮 8bits data
    @NotNull
    @RequiresApi(api = Build.VERSION_CODES.N)
    private int[] Log10toGrayScale(double[] envelopeData) {
//        int DisplayDepth = (int)Math.round(Depth/6.16*1000);
        int[] logData = new int[displayDataSize]; //取log
        //壓縮 8bits data
        for(int i = 0; i<displayDataSize; i++) {
            int a = (int)Math.round((Math.log10(envelopeData[i]) - contrast)*255/contrast);
            if (a > 255 || a == 255){
                logData[i] = 255;
            }
            else if(a < 0){
                logData[i] = 0;
            }
            else{
                logData[i] = a;
            }
        }
        return logData;
    }

    //RF-mode執行緒
    public void RFChartingThread(){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                int i = 0;

                while(isRunning)
                {
                    timeStart = System.currentTimeMillis();
                    //從Queue取得RF-mode data
                    try {
                        RF_modeFIFOData = RF_modeFiFOQueue.take();
                        Log.i(TAG, "run: UsbFrameFiFOQueue.take();");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //畫出RF-mode訊號圖
                    if (RF_modeFIFOData != null & isDataProcessRunning) {
                        MPCharting(i, RF_modeFIFOData,maxvalueloc);
                        mChart.invalidate();
                    }
                    else if (i == 10){
                        i = 0;
                    }
                    i++;
                    timeEnd = System.currentTimeMillis();
                    executiveTime = timeEnd-timeStart;
                    Log.i("RF_modeDisplayThread", "Frame Rate:"+ (float)(1/(executiveTime*0.001)) + " hz");

                }
            }
        }).start();
    }


    public void Signal_MaximumThread(){
        new Thread(new Runnable() {

            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                while (isRunning) {
                    timeStart = System.currentTimeMillis();
                    //從Queue取得RF-mode data
                    try {
                        FindMaxFIFOData = FindMaxFiFOQueue.take();
                        Log.i(TAG, "run: FindMaxRawData.take();");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //畫出RF-mode訊號圖Math.round((max_loc*6.16/1000)*100/100);
                    if (FindMaxFIFOData != null & isDataProcessRunning) {
                        maxvalueloc = (int) FindMaxloc(FindMaxFIFOData); //尋找最大值位置
                    }
                    timeEnd = System.currentTimeMillis();

                }
            }
        }).start();
    }


    private void MPCharting(int j, double[] gainData, int max_location){

        int width_i = 0; //初始設定第幾條Mmode要追蹤
        String[] stringTmp = new String[displayDataSize];
        final String[] xLable = new String[displayDataSize];


        ArrayList<Entry> line = new ArrayList<>();
        ArrayList<Entry> boxline = new ArrayList<>();//畫出追蹤線的data
        ArrayList<Entry> boxline_1 = new ArrayList<>();//畫出追蹤線的data


        /**輸入資料↓*/
        for(int i =0; i<displayDataSize; i++) {
            float value = (float) (gainData[i] * 1.8 / 4096); //double轉float
            line.add(new Entry(i,value)); //輸入x,y值，x=點數, y=振幅
            //Log.i(TAG, "DataPaser TMP: "+DataBuferr);
            //stringTmp[i] = Integer.toString(CheckedData[i]);
            stringTmp[i] = Float.toString(value); //y軸振幅轉字串
            float xfloat = (float) (i*6.16/1000); //x軸點數轉距離
            xLable[i] = String.valueOf((float) Math.round(xfloat*100)/100); //x軸距離座標轉字串
        }

        for(int i=0; i<displayDataSize; i++){
            if (i<max_location-50 || i>max_location+50){
                boxline.add(new Entry(i, (float) 0));
                boxline_1.add(new Entry(i, (float) 0));
            }else{
                boxline.add(new Entry(i, (float) Amplitude));
                boxline_1.add(new Entry(i, (float) -Amplitude));
            }
        }


        /**輸入資料↑*/

        LineDataSet set1 = new LineDataSet(line,"AMP DATA");
        set1.setFillAlpha(110); //設定曲線下區域的顏色
        set1.setLineWidth(1f); //設定線寬
        set1.setColor(Color.WHITE); //設定曲線顏色
        set1.setDrawCircles(false); //設定是否顯示座標點的小圓圈
        set1.setDrawFilled(false);//使用範圍背景填充(預設不使用)
        ArrayList<ILineDataSet> dataSets= new  ArrayList<>(); //
        dataSets.add(set1); //

        width_i =width_i+1;

        if (isTracking){
            LineDataSet set2 = new LineDataSet(boxline, "trackingbox_upper");
            set2.setFillAlpha(110); //設定曲線下區域的顏色
            set2.setLineWidth(0f); //設定線寬
            set2.setColor(Color.GREEN); //設定曲線顏色
            set2.setDrawFilled(true);//使用範圍背景填充(預設不使用)
            set2.setFillColor(Color.rgb(188, 203, 176));
            set2.setDrawCircles(false);
            dataSets.add(set2);

            LineDataSet set3 = new LineDataSet(boxline_1, "trackingbox_lower");
            set3.setFillAlpha(110); //設定曲線下區域的顏色
            set3.setLineWidth(0f); //設定線寬
            set3.setColor(Color.GREEN); //設定曲線顏色
            set3.setDrawFilled(true);//使用範圍背景填充(預設不使用)
            set3.setFillColor(Color.rgb(188, 203, 176));
            set3.setDrawCircles(false);
            dataSets.add(set3);
        }


        /**設定圖表框架↓*/
        float minYLabel, maxYLabel, maxXLabel;

        YAxis leftAxis = mChart.getAxisLeft();//設置Y軸(左)
        YAxis rightAxis = mChart.getAxisRight();//設置Y軸(右)
        rightAxis.setEnabled(false);//讓右邊Y消失
        XAxis xAxis = mChart.getXAxis();//設定X軸
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);//將x軸表示字移到底下
        xAxis.setLabelCount(3,false);//設定X軸上要有幾個標籤
        mChart.getDescription().setEnabled(false);//讓右下角文字消失
//      xAxis.setEnabled(false);//去掉X軸數值
//      xAxis.setLabelRotationAngle(-45f);//讓字變成斜的
        xAxis.setDrawGridLines(false);//將X軸格子消失掉


        minYLabel = -Amplitude;
        maxYLabel = Amplitude;
        maxXLabel = (int)Math.round(Depth/6.16*1000);



        xAxis.setAxisMaximum(maxXLabel);
        xAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(minYLabel); // 設定y軸最小值
        leftAxis.setAxisMaximum(maxYLabel); // 設定y軸最大值
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setGranularity(1f); // interval 1
        leftAxis.setLabelCount(9, true); // force 9 labels
        /**設定圖表框架↑*/


        if (j == 5){
                LimitLine NeedleTipPosition = new LimitLine(195,"Needle Tip");
                NeedleTipPosition.setLineColor(Color.RED);
                NeedleTipPosition.setLineWidth(2f);
                NeedleTipPosition.enableDashedLine(10f,10f,0f);
                NeedleTipPosition.setTextSize(Color.RED);
                NeedleTipPosition.setTextSize(16);
                xAxis.addLimitLine(NeedleTipPosition);
                xAxis.setDrawLimitLinesBehindData(true);


        }

        IAxisValueFormatter iAxisValueFormatter = new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, AxisBase axisBase) {
                int index = (int) v;
                if (index < 0 || index >= displayDataSize) {
                    return "";
                } else {
                    return xLable[Math.abs((int)v)];
                }
            }
        };

        xAxis.setValueFormatter(iAxisValueFormatter);
        xAxis.setLabelCount(11, true);

        LineData lineData = new LineData(dataSets);
        mChart.setData(lineData);
        mChart.getData().setHighlightEnabled(false);
        //mChart.setVisibleXRangeMaximum(AdjustLength.get());
        mChart.setVisibleXRangeMaximum(displayDataSize);

    }

    //M-mode執行緒
    public void  M_modeDisplayThread(){
        handler = new Handler();
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                int i = 0; //M-mode顯示條數計數
                while (isRunning) {
                    timeStart = System.currentTimeMillis();
                    //從Queue取得Ｍ-mode 8bits data
                    try {
                        M_modeFIFOData = M_modeFiFOQueue.take();
                        Log.i(TAG, "run:MmodeUsbFrameFiFOQueue.take();");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //顯示M-mode影像
                    if(M_modeFIFOData != null & isDataProcessRunning ){

                        final Bitmap resizedBitmap = M_modeDisplay(i, M_modeFIFOData); //將8bits data放入bitmap
                        int maxvalueloc_mm = (int) (maxvalueloc*6.16/1000);
                        final Bitmap MmodeTrackingBitmap = MmodeTracking(i,maxvalueloc_mm,Depth);
                        DisplayLines = i;
                        handler.post(new Runnable() {
                            public void run() {
                                M_modeImage.setImageBitmap(resizedBitmap); //將bitmap放入imageview
                                TrackingMmode.setImageBitmap(MmodeTrackingBitmap);
                                //時間軸顯示
                                if (DisplayLines == M_modeImageWidth / 4) {
                                    Time2.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                    Time2.setTextColor(Color.rgb(255,255, 255));
                                } else if (DisplayLines == M_modeImageWidth / 4 * 2) {
                                    Time3.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                    Time3.setTextColor(Color.rgb(255,255, 255));
                                } else if (DisplayLines == M_modeImageWidth / 4 * 3) {
                                    Time4.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                    Time4.setTextColor(Color.rgb(255,255, 255));
                                } else if (DisplayLines == M_modeImageWidth) {
                                    Time5.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                    Time5.setTextColor(Color.rgb(255,255, 255));
                                }
                            }
                        });
                        if (i % M_modeImageWidth==0) {
                            //判斷儲存功能啟動，M-mode顯示匡滿時擷取螢幕影像
                            if (isRecord) {
                                Bitmap M_modeScreenShot = getScreenShot();
                                String M_modeImageName = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + Depth + "mm_" + Math.round(displayTime * 10.0) / 10.0 + "s.jpg");
                                imageSaveToFile.extelnalPrivateCreateFolerImageCapture(FileFolderNameDate, M_modeImageName, M_modeScreenShot);
                            }
                            i++;
                        }else{
                            i++;
                        }

                    }else{
                        Log.i("M_modeDisplayThread", "dead!");
                    }
                    timeEnd = System.currentTimeMillis();
                    executiveTime = timeEnd-timeStart;
                    displayTime = displayTime + (float) (executiveTime * 0.001);
                    Log.i("M_modeDisplayThread", "Frame Rate:"+ (float)(1/(executiveTime*0.001)) + " hz");


                }
            }
        }).start();
    }

    //將8bits data放入bitmap
    @NotNull
    @RequiresApi(api = Build.VERSION_CODES.N)
    private Bitmap M_modeDisplay(int i, int[] grayScaleData) {
        //將8bits data放入二維0矩陣
//
        if (i>=M_modeImageWidth){
            for(int k = 0; k<M_modeImageWidth-1; k++){
                for (int j = 0;j<M_modeImageHeight; j++) {
                    M_modeArray[j][(k)] = M_modeArray[j][(k+1)];
                }
            }
            for (int j = 0;j<M_modeImageHeight; j++) {
                M_modeArray[j][(M_modeImageWidth-1)] = grayScaleData[j];
            }
        }else{
            for (int j = 0;j<M_modeImageHeight; j++) {
                M_modeArray[j][(i%M_modeImageWidth)] = grayScaleData[j];
            }
        }

        Bitmap bitmap = ArrayToBitmap(M_modeArray); //將矩陣轉成bitmap
        return resizedBitmap(bitmap); //回傳bitmap
    }

    private Bitmap MmodeTracking(int width_i, int loc,float depth){
        int maxloc = (int)Math.round(140/depth*loc);// x 為 depth調整後的深度 loc 為 最大值位置;
        int height = 140;
        int width = 450;

        //設定maxloc範圍 避免error
        if (maxloc<5){
            maxloc = 6;
        }else if (maxloc>height){
            maxloc = 135;
        }

        int width_normalized = (int)Math.round(width_i/(M_modeImageWidth/width)); // 由於Mmode的矩陣大小和tracking不一樣 所以要normalized

        int[] DottedLine_ = new int[width * height]; //可以將矩陣大小縮小，以避免計算量過大

        if (isTracking){
            if (width_normalized>=width){
                for (int i = width-9; i < width+1; i++){
                    for(int j = maxloc-5; j < maxloc+5; j++){
                        DottedLine_[width * j + i] = (100<< 24) | (0 << 16) | (255 << 8) | 0;
                    }
                }//若目前i到最大值，則固定出現在最右邊
            }else{
                for (int i = width_normalized-5; i < width_normalized+5; i++){
                    for(int j = maxloc-5; j < maxloc+5; j++){
                        DottedLine_[width * j + i] = (100 << 24) | (0 << 16) | (255 << 8) | 0;
                    }
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(DottedLine_, 0, width, 0, 0, width, height );
        return bitmap;
    }
    //將矩陣轉成bitmap
    private Bitmap ArrayToBitmap(int[][] m_modeArray) {
        //設定bitmap尺寸
        int M_modeHight;
        M_modeHight= (int)Math.round(Depth/6.16*1000);
        int width = M_modeImageWidth;
        int height = M_modeHight;
        int[] pixels = new int[width * height];
        //設定RGB值
        int alpha = 255;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grayscale = m_modeArray[i][j];
                int GrayScale = (alpha << 24) | (grayscale << 16) | (grayscale << 8) | grayscale;
                pixels[width * i +j] = GrayScale;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height );
        return bitmap;
    }

    //壓縮M-mode影像
    private Bitmap resizedBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = 400;
        int newHeight = 140;
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        //matrix.postRotate(90);
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
    }

    //截圖功能
    private Bitmap getScreenShot(){
        //將螢幕畫面存成一個View
        View view = getWindow().getDecorView();
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap fullBitmap = view.getDrawingCache();
        //取得系統狀態欄高度
        Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        //取得手機長、寬
        int phoneWidth = getWindowManager().getDefaultDisplay().getWidth();
        int phoneHeight = getWindowManager().getDefaultDisplay().getHeight();
        //將螢幕快取到的圖片修剪尺寸(去掉status bar)後，存成Bitmap
        Bitmap bitmap = Bitmap.createBitmap(fullBitmap,0,statusBarHeight,phoneWidth
                ,phoneHeight-statusBarHeight);
        //清除螢幕截圖快取，避免內存洩漏
        view.destroyDrawingCache();
        return bitmap;
    }

    private Bitmap NeedleTipPositionDottedLine(int x){
        int tip;
        tip = (int)Math.round(140/x*1.2);
//        if (isTuohy){
//            tip = (int)Math.round(140/x*1.2);
//        }
////        else if (isStraight){
////            tip = (int)Math.round(140/x*0.4);
////        }
////        else{
////            tip = 0;
////        }
        int width = 400;
        int height = 140;
        int[] DottedLine = new int[width * height];
        for (int i = 0; i < width; i++){
            if ((i/10)%2 == 0){
                for(int j = tip; j < tip+10; j++){
                    DottedLine[width * j + i] = (50 << 24) | (255 << 16) | (0 << 8) | 0;
                }
            }
            else{
                for(int j = 0; j < height; j++)
                    DottedLine[width * j + i] = (0 << 24) | (0 << 16) | (0 << 8) | 0;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(DottedLine, 0, width, 0, 0, width, height );
        return bitmap;
    }


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }
        }
    };



    //raw data 儲存功能設定
    private void SaveRawDataSetup() {
        dataSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (RecordOn.get() == false) {
                    RecordOn.set(true);
                    isRecord = true;
                    dataSaveButton.setText("RECORD OFF"); //儲存按鈕文字變化
                    dataSaveButton.setTextColor(Color.rgb(255, 0, 0)); //儲存按鈕顏色變化（紅）
                    FileFolderNameDate = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); //新增儲存路徑資料夾
                } else {
                    RecordOn.set(false);
                    isRecord = false;
                    dataSaveButton.setText("RECORD ON"); //儲存按鈕文字變化
                    dataSaveButton.setTextColor(Color.rgb(0, 0, 0)); //儲存按鈕顏色變化（黑）
                }
            }
        });
    }



    //raw data 儲存
    private void SaveRawDataOn(int j, double[] saveRawData) {
        int RecordLength ;
        RecordLength= (int)Math.round(Depth/6.16*1000); //儲存深度

        String[] stringTmp = new String[RecordLength]; //轉string
        for(int i =0; i<RecordLength; i++) {
            float value = (float) saveRawData[i];
            stringTmp[i] = Float.toString(value);
        }
        String rawDataFileName = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+"_RF(" + j + ")"); //檔名設定
        dataSaveToFile.extelnalPrivateCreateFoler(FileFolderNameDate,rawDataFileName,  Arrays.toString(stringTmp)); //存檔

    }

    private void TrackingSetup(){
        Tracking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TrackingOn.get() == false) {
                    TrackingOn.set(true);
                    isTracking = true;
                    Tracking.setTextColor(Color.rgb(255, 0, 0)); //儲存按鈕顏色變化（紅）
                } else {
                    TrackingOn.set(false);
                    isTracking = false;
                    Tracking.setTextColor(Color.rgb(0, 0, 0)); //儲存按鈕顏色變化（黑）
                }
            }
        });
    }

    private double FindMaxloc(double[] array){
        double max_value=0;
        double max_value_loc=0;
        int length = (int) Math.round(Depth / 6.16 * 1000);
        for (int i = 150; i < length-100; i++) {
            if (array[i] > max_value) {
                max_value = array[i];
                max_value_loc=i;
            }
        }
        return max_value_loc;
    }



}