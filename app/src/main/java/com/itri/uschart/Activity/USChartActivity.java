package com.itri.uschart.Activity;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

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


public class USChartActivity extends AppCompatActivity {
    private static final byte CR  = 0x0d;
    private static final byte LF  = 0x0a;
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

    boolean isFirst = true;

    ImageButton button_ReceiveData;


    private boolean forceClaim = true;
    static int TIMEOUT = 100;


    AtomicInteger AdjustLength = new AtomicInteger(4096);
    private int PacketSize = 8192;
    private int dataSize = PacketSize/2;
    private byte[] PacketBuffer = new byte[PacketSize];
    boolean isRunning = false;
    boolean isDataProcessRunning = false;
    boolean isRecord = false;
    boolean isSingleSave = false;
    BlockingQueue<byte[]> UsbReceivedFiFOQueue = new LinkedBlockingQueue<byte[]>(Integer.MAX_VALUE); //USB data Queue
    BlockingQueue<double[]> RF_modeFiFOQueue = new LinkedBlockingQueue<double[]>(Integer.MAX_VALUE); //RF-mode data Queue
    BlockingQueue<int[]> M_modeFiFOQueue = new LinkedBlockingQueue<int[]>(Integer.MAX_VALUE);//Every //M-mode data Queue
    private byte[] UsbFIFOData = new byte[PacketSize];
    private double[] RF_modeFIFOData = new double[dataSize];
    private double[] preRawData = new double[dataSize];
    private int[] M_modeFIFOData = new int[dataSize];
    private int displayDataSize = 2048;
    byte[] dataSend;

//    BlockingQueue<double[]> SaveRawDataQueue = new LinkedBlockingQueue<double[]>(Integer.MAX_VALUE);
    private Handler handler;

    private long timeStart,timeEnd, executiveTime;

    private DataSaveToFile dataSaveToFile;
    private DataSaveToFile imageSaveToFile;
    private double[] SaveRawData = new double[dataSize];
    private LineChart mChart;
    private ImageView M_modeImage, NeedleTipPosition;
    private Button TuohyNeedleTip, StraightNeedleTip;

    private RadioGroup radioGroup;

//    private EditText max_editText, min_editText, max_XeditText;
    private SeekBar gain_seekBar, Depth_seekBar, Amplitude_seekBar, M_modeSeekBar;
    private SeekBar tgc1,tgc2,tgc3,tgc4,tgc5,tgc6,tgc7;
    private Button TGCReset;
    private Button ScreenShot;
    private float displayTime = 0.f;
    private float Depth = 5.f;
    private float NeedleTipRF;
    private float NeedleTipM = 0.f;
    private float gain = 6.f;
    private float GainText = (float) (20 * Math.log10(gain));
    private double contrast = 1.5;
    private float Amplitude = (float) (500 * 1.8 / 4096);
    private TextView GainDisplay, DepthDisplay, AmplitudeDisplay;
//    private TextView RecordTime;
    private TextView depth1, depth2, depth3, depth4, depth5;
    private TextView Time2, Time3, Time4, Time5;
    private int DepthX;
    private String FileFolderNameDate;

    private int M_modeImageWidth = 900;
    private int M_modeImageHeight = 2048;
    int[][] M_modeArray = new int[M_modeImageHeight][M_modeImageWidth];
    int DisplayLines;

    private Button RF_modeDisplayButton;
    private Button M_modeDisplayButton;
    private Button dataSaveButton;
    AtomicBoolean RecordOn = new AtomicBoolean(false);
    AtomicBoolean SingleRecordOn = new AtomicBoolean(false);

    //    AtomicBoolean RF_modeOn = new AtomicBoolean(true);
    boolean isRF_mode = true;
    boolean isM_mode = false;
    private Button SaveSingle;
//    boolean isTuohy = false;
//    boolean isStraight = false;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        this.getSupportActionBar().hide();
        initialSetup();

    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initialSetup(){
        button_ReceiveData = findViewById(R.id.read_message);  //開始顯示RF/M-mode按鈕
//        max_editText = (EditText) findViewById(R.id.max_editText);
//        min_editText = (EditText) findViewById(R.id.min_editText);
//        max_XeditText = (EditText) findViewById(R.id.max_XeditText);
        //gain值調整介面
        gain_seekBar = (SeekBar) findViewById(R.id.Gain_seekBar);
        GainDisplay = (TextView) findViewById(R.id.Gain_textview);

        //raw data 儲存按鈕
        dataSaveButton = (Button) findViewById(R.id.button);

        //深度調整介面
        DepthDisplay = (TextView) findViewById(R.id.max_Xtextview);
        Depth_seekBar = (SeekBar) findViewById(R.id.Depth_seekBar);

        //振幅調整介面
        AmplitudeDisplay = (TextView) findViewById(R.id.max_Ytextview);
        Amplitude_seekBar = (SeekBar) findViewById(R.id.Amplitude_seekBar);

        //M-mode對比調整介面
        M_modeSeekBar = (SeekBar) findViewById(R.id.M_modeContrastSeekBar);

        //發射頻率調整介面
//        radioGroup = (RadioGroup)findViewById(R.id.radio_group);
//        RadioButton radioButton1 = (RadioButton) findViewById(R.id.radioButton1);
//        RadioButton radioButton2 = (RadioButton) findViewById(R.id.radioButton2);
//        RadioButton radioButton3 = (RadioButton) findViewById(R.id.radioButton3);
//        RadioButton radioButton4 = (RadioButton) findViewById(R.id.radioButton4);
//        radioGroup.setOnCheckedChangeListener(listener);
        dataSend = new byte[2];
        dataSend[0] = 0x00;
        dataSend[1] = 0X25; // 初始設定成20MHZ
//        radioGroup.check(R.id.radioButton3);

        //截圖按鈕
        ScreenShot=(Button) findViewById(R.id.Screenshotbutton);
        //存單筆按鈕
        SaveSingle=(Button) findViewById(R.id.SaveSingleButton);
        //RF-mode顯示介面
        mChart = (LineChart) findViewById(R.id.chart_line);
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setHorizontalScrollBarEnabled(true);
        mChart.getLegend().setEnabled(false); // Remove Legend
        mChart.getDescription().setEnabled(false); //remove description

//        RecordTime = (TextView) findViewById(R.id.RecordTime);
        //M-mode深度座標
        depth1 = (TextView) findViewById(R.id.M_modeDepth0mm);
        depth1.setText(0 + " mm --");
        depth2 = (TextView) findViewById(R.id.M_modeDepth2mm);
        depth2.setText(Depth / 4 * 1 + " mm --");
        depth3 = (TextView) findViewById(R.id.M_modeDepth4mm);
        depth3.setText(Depth / 4 * 2 + " mm --");
        depth4 = (TextView) findViewById(R.id.M_modeDepth6mm);
        depth4.setText(Depth / 4 * 3 + " mm --");
        depth5 = (TextView) findViewById(R.id.M_modeDepth8mm);
        depth5.setText(Depth + " mm --");

        //M-mode時間座標
        Time2 = (TextView) findViewById(R.id.M_modeTime2nds);
        Time3 = (TextView) findViewById(R.id.M_modeTime3rds);
        Time4 = (TextView) findViewById(R.id.M_modeTime4ths);
        Time5 = (TextView) findViewById(R.id.M_modeTime5ths);

        //M-mode影像顯示介面
        M_modeImage = (ImageView) findViewById(R.id.M_mode_Bitmap);
        NeedleTipPosition = (ImageView) findViewById(R.id.NeedleTipPosition);

        //
        TuohyNeedleTip = (Button)findViewById(R.id.TuohyNeedleTip);
        StraightNeedleTip = (Button)findViewById(R.id.StraightNeedleTip);

        //RF/M-mode顯示切換鈕
        RF_modeDisplayButton = (Button) findViewById(R.id.RF_modeButton);
        M_modeDisplayButton = (Button) findViewById(R.id.M_modeButton);

        //USB設定
        device = getIntent().getParcelableExtra("Device");
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        usbManager.requestPermission(device, mPermissionIntent);

        //RF/M-mode切換設定
        DisplaySetup();
        //存檔設定
        dataSaveToFile = new DataSaveToFile(this);
        imageSaveToFile = new DataSaveToFile(this);
        SaveRawDataSetup();
        SaveSingleRawDataSetup();
        //TGC設定
        TGCsetup();

        //顯示RF/M-mode按鈕按下後的指令
        button_ReceiveData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Get counter, New thread START");
                isDataProcessRunning = !isDataProcessRunning;

                if (isFirst) {
                    deviceConnection = usbManager.openDevice(device);
                    Log.i(TAG, "Device connected.");
                    usbInterface = device.getInterface(0);
                    endpointOut = usbInterface.getEndpoint(0);
                    endpointIn = usbInterface.getEndpoint(1);
                    deviceConnection.claimInterface(usbInterface, forceClaim);
                    Log.i(TAG, "deviceConnection.claimInterface");

                    isRunning = true;
                    isRF_mode = true;
                    isM_mode = false;
                    RF_modeDisplayButton.setTextColor(Color.rgb(255, 0, 0));
                    Thread usbrecieveThread = new Thread(new UsbReceiveThread());
                    usbrecieveThread.start(); //接收USB data執行緒
                    DataProcessingThread(); //data前處理執行緒
                    RFChartingThread(); //RF-mode執行緒
                    M_modeDisplayThread(); //M-mode執行緒
                    isFirst = false;
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
        //存單筆按鈕按下後的指令


        //gain值調整指令
        gain_seekBar.setMin(1);
        gain_seekBar.setMax(10);
        gain_seekBar.setProgress(6);
        GainDisplay.setText("Gain: " + Math.round(GainText * 10.0) / 10.0 + " dB");
        gain_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                gain = i;
                GainText = (float) (20 * Math.log10(i));
                GainDisplay.setText("Gain: " + Math.round(GainText * 10.0) / 10.0  + " dB");

            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //針尖位置顯示
//        TuohyNeedleTip.setOnClickListener(new View.OnClickListener(){
//            public void onClick(View v) {
//                isTuohy = true;
//                isM_mode = false;
//
//                TuohyNeedleTip.setTextColor(Color.rgb(255, 0, 0));
//                StraightNeedleTip.setTextColor(Color.rgb(0, 0, 0));
//                NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));
//            }
//        });
//        StraightNeedleTip.setOnClickListener(new View.OnClickListener(){
//            public void onClick(View v) {
//                isTuohy = false;
//                isStraight = true;
//
//                StraightNeedleTip.setTextColor(Color.rgb(255, 0, 0));
//                TuohyNeedleTip.setTextColor(Color.rgb(0, 0, 0));
//                NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));
//            }
//        });
        NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));

        //深度調整指令
        Depth_seekBar.setMin(4); //最小顯示深度
        Depth_seekBar.setMax(12); //最大顯示深度
        Depth_seekBar.setProgress(5); //初始顯示深度
        DepthDisplay.setText("Depth: "+ Depth + " mm");
        Depth_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Depth= i;
                DepthDisplay.setText("Depth: " + Depth + "mm");
                depth1.setText(0.0 + " mm --");
                depth2.setText(Depth / 4 * 1 + " mm --");
                depth3.setText(Depth / 4 * 2 + " mm --");
                depth4.setText(Depth / 4 * 3 + " mm --");
                depth5.setText(Depth + " mm --");
                NeedleTipPosition.setImageBitmap(NeedleTipPositionDottedLine((int)Math.round(Depth)));

                DepthX = (int) Math.round(Depth / 6.16 * 1000);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //振幅調整指令
        Amplitude_seekBar.setMin(300); //最小顯示振幅
        Amplitude_seekBar.setMax(2300); //最大顯示振幅
        Amplitude_seekBar.setProgress(500); //初始顯示振幅
        AmplitudeDisplay.setText("Amp: " + Math.round(Amplitude * 100.0) / 100.0 + "V");
        Amplitude_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Amplitude= (float) (i * 1.8 / 4096);
                AmplitudeDisplay.setText("Amp: " + Math.round(Amplitude * 100.0) / 100.0 + "V");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        //M-mode對比度調整指令
        M_modeSeekBar.setMin(1); //最小對比度
        M_modeSeekBar.setMax(11); //最大對比度
        M_modeSeekBar.setProgress(6); //初始對比度
        M_modeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                contrast = 0.25*i;
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
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
                        if (isSingleSave){
                            SaveSingleRawData(RawData);
                        }
                    }
                    try {
                        Thread.sleep(10);
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
                while (isRunning) {
                    timeStart = System.currentTimeMillis();
                    //從Queue取得RF-mode data
                    try {
                        RF_modeFIFOData = RF_modeFiFOQueue.take();
                        Log.i(TAG, "run: UsbFrameFiFOQueue.take();");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //畫出RF-mode訊號圖
                    if (RF_modeFIFOData != null & isDataProcessRunning & isRF_mode & !isM_mode) {
                        float x = NeedleTipRF;
                        MPCharting(i, RF_modeFIFOData);
                        mChart.invalidate();
                    }
                    else if (i == 5){
                        i = 0;
                    }
                    i++;
                    timeEnd = System.currentTimeMillis();
                    Log.i("RFChartingThread", "Success.");
                }
            }
        }).start();
    }

    private void MPCharting(int j, double[] gainData){

        String[] stringTmp = new String[displayDataSize];
        final String[] xLable = new String[displayDataSize];
        ArrayList<Entry> line = new ArrayList<>();

        for(int i =0; i<displayDataSize; i++) {
            float value = (float) (gainData[i] * 1.8 / 4096); //double轉float
            line.add(new Entry(i,value)); //輸入x,y值，x=點數, y=振幅
            //Log.i(TAG, "DataPaser TMP: "+DataBuferr);
            //stringTmp[i] = Integer.toString(CheckedData[i]);
            stringTmp[i] = Float.toString(value); //y軸振幅轉字串
            float xfloat = (float) (i*6.16/1000); //x軸點數轉距離
            xLable[i] = String.valueOf((float) Math.round(xfloat*100)/100); //x軸距離座標轉字串
        }

        LineDataSet set1 = new LineDataSet(line,"AMP DATA");
        set1.setFillAlpha(110); //設定曲線下區域的顏色
        set1.setLineWidth(1f); //設定線寬
        set1.setColor(Color.BLACK); //設定曲線顏色
        set1.setDrawCircles(false); //設定是否顯示座標點的小圓圈
        ArrayList<ILineDataSet> dataSets= new  ArrayList<>(); //
        dataSets.add(set1); //

        LineData data = new LineData(dataSets);
        XAxis xAxis = mChart.getXAxis();
        YAxis yAxis = mChart.getAxisLeft();
        YAxis yAxisR = mChart.getAxisRight();
        yAxisR.setEnabled(false);
        yAxis.setTextSize(11.5f);// set the text size
        xAxis.setTextSize(11.5f);
        float minYLabel, maxYLabel, maxXLabel;

        minYLabel = -Amplitude;
        maxYLabel = Amplitude;
        maxXLabel = (int)Math.round(Depth/6.16*1000);

        xAxis.setAxisMaximum(maxXLabel);
        yAxis.setAxisMinimum(minYLabel); // start at zero
        yAxis.setAxisMaximum(maxYLabel); // the axis maximum is 100
        yAxis.setTextColor(Color.BLACK);
        yAxis.setGranularity(1f); // interval 1
        yAxis.setLabelCount(9, true); // force 6 labels

        if (j == 5){
//            if (isTuohy && !isStraight){
//                LimitLine NeedleTipPosition = new LimitLine(163,"Needle Tip");//162 >> 1mm 45needle, 195 >> 1.2mm tuohy needle
//                NeedleTipPosition.setLineColor(Color.RED);
//                NeedleTipPosition.setLineWidth(2f);
//                NeedleTipPosition.enableDashedLine(10f,10f,0f);
//                NeedleTipPosition.setTextSize(Color.RED);
//                NeedleTipPosition.setTextSize(16);
//                xAxis.addLimitLine(NeedleTipPosition);
//                xAxis.setDrawLimitLinesBehindData(true);
//            }
//            else if (!isTuohy && isStraight){
//                LimitLine NeedleTipPosition = new LimitLine(65,"Needle Tip");//
//                NeedleTipPosition.setLineColor(Color.RED);
//                NeedleTipPosition.setLineWidth(2f);
//                NeedleTipPosition.enableDashedLine(10f,10f,0f);
//                NeedleTipPosition.setTextSize(Color.RED);
//                NeedleTipPosition.setTextSize(16);
//                xAxis.addLimitLine(NeedleTipPosition);
//                xAxis.setDrawLimitLinesBehindData(true);
//            }
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
        mChart.setData(data);
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
                        Log.i(TAG, "run: UsbFrameFiFOQueue.take();");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    //顯示M-mode影像
                    if(M_modeFIFOData != null & isDataProcessRunning & !isRF_mode & isM_mode){
                        final Bitmap resizedBitmap = M_modeDisplay(i, M_modeFIFOData); //將8bits data放入bitmap
                        DisplayLines = i;
                        handler.post(new Runnable() {
                            public void run() {
                                M_modeImage.setImageBitmap(resizedBitmap); //將bitmap放入imageview
                                //時間軸顯示
                                if (DisplayLines == M_modeImageWidth / 4) {
                                    Time2.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                } else if (DisplayLines == M_modeImageWidth / 4 * 2) {
                                    Time3.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                } else if (DisplayLines == M_modeImageWidth / 4 * 3) {
                                    Time4.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                } else if (DisplayLines == M_modeImageWidth) {
                                    Time5.setText(Math.round(displayTime * 10.0) / 10.0 + "s");
                                }
                            }
                        });
                        if (i == M_modeImageWidth) {
                            i = 0; //M-mode顯示匡滿時歸零，重新從最左邊顯示M-mode
                            //判斷儲存功能啟動，M-mode顯示匡滿時擷取螢幕影像
                            if (isRecord) {
                                Bitmap M_modeScreenShot = getScreenShot();
                                String M_modeImageName = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + "_" + Depth + "mm_" + Math.round(displayTime * 10.0) / 10.0 + "s.jpg");
                                imageSaveToFile.extelnalPrivateCreateFolerImageCapture(FileFolderNameDate, M_modeImageName, M_modeScreenShot);
                            }
                        //歸零
                        }
                        else if (i == 0) {
                            displayTime = 0.f;
                            i++;
                        }
                        else if(!isM_mode & isRF_mode){
                            i = 0;
                        }
                        else {
                            i++;
                        }
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

        for (int j = 0;j<M_modeImageHeight; j++) {
            M_modeArray[j][(i%M_modeImageWidth)] = grayScaleData[j];
        }
        Bitmap bitmap = ArrayToBitmap(M_modeArray); //將矩陣轉成bitmap
        return resizedBitmap(bitmap); //回傳bitmap
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
                for(int j = tip; j < tip+1; j++){
                    DottedLine[width * j + i] = (255 << 24) | (255 << 16) | (0 << 8) | 0;
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

    //發射頻率指令設定
//    private RadioGroup.OnCheckedChangeListener listener = new RadioGroup.OnCheckedChangeListener() {
//
//        @Override
//        public void onCheckedChanged(RadioGroup group, int checkedId) {
//            // TODO Auto-generated method stub
//            switch (checkedId) {
//                case R.id.radioButton1:
//
//                    dataSend[1] = 0x23;
//                    is5MHz = true;
//                    break;
//                case R.id.radioButton2:
//
//                    dataSend[1] = 0x24;
//                    //dataSend[1] = 0x11;
//                    is5MHz = true;
//                    break;
//                case R.id.radioButton3:
//
//                    dataSend[1] = 0x25;
//                    is5MHz = false;
//                    break;
//                case R.id.radioButton4:
//
//                    dataSend[1] = 0x26;
//                    is5MHz = false;
//                    break;
//            }
//        }
//
//    };

    //TGC調整指令設定
    private void TGCsetup(){
        TGCReset = (Button)findViewById(R.id.tgcreset);
        TGCReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dataSend[0] = 0x00;
                tgc1.setProgress(0);
                tgc2.setProgress(0);
                tgc3.setProgress(0);
                tgc4.setProgress(0);
                tgc5.setProgress(0);
                tgc6.setProgress(0);
                tgc7.setProgress(0);
            }
        });

        tgc1 = (SeekBar)findViewById(R.id.tgc1_seekBar);
        //tgc1.setMin(0);
        tgc1.setMax(15);
        tgc1.setProgress(0);
        tgc1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                dataSend[0] = (byte) (0x10 | (i & 0xFF));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tgc2 = (SeekBar)findViewById(R.id.tgc2_seekBar);
        //tgc2.setMin(0);
        tgc2.setMax(15);
        tgc2.setProgress(0);
        tgc2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dataSend[0] = (byte) ( 0x20 | (i & 0xFF));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tgc3 = (SeekBar)findViewById(R.id.tgc3_seekBar);
        //tgc3.setMin(0);
        tgc3.setMax(15);
        tgc3.setProgress(0);
        tgc3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dataSend[0] = (byte) ( 0x30 | (i & 0xFF));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tgc4 = (SeekBar)findViewById(R.id.tgc4_seekBar);
        //tgc4.setMin(0);
        tgc4.setMax(15);
        tgc4.setProgress(0);
        tgc4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dataSend[0] = (byte) ( 0x40 | (i & 0xFF));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tgc5 = (SeekBar)findViewById(R.id.tgc5_seekBar);
        //tgc5.setMin(0);
        tgc5.setMax(15);
        tgc5.setProgress(0);
        tgc5.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dataSend[0] = (byte) ( 0x50 | (i & 0xFF));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tgc6 = (SeekBar)findViewById(R.id.tgc6_seekBar);
        //tgc6.setMin(0);
        tgc6.setMax(15);
        tgc6.setProgress(0);
        tgc6.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dataSend[0] = (byte) ( 0x60 | (i & 0xFF));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        tgc7 = (SeekBar)findViewById(R.id.tgc7_seekBar);
        //tgc7.setMin(0);
        tgc7.setMax(15);
        tgc7.setProgress(0);
        tgc7.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                dataSend[0] = (byte) ( 0x70 | (i & 0xFF));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }
    //RF/M-mode顯示切換設定
    private void DisplaySetup(){
        RF_modeDisplayButton.setOnClickListener(new View.OnClickListener(){
           public void onClick(View v)
           {
               isRF_mode = true;
               isM_mode = false;
               RF_modeDisplayButton.setTextColor(Color.rgb(255, 0, 0));
               M_modeDisplayButton.setTextColor(Color.rgb(0, 0, 0));
           }
        });
        M_modeDisplayButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v)
            {
                isRF_mode = false;
                isM_mode = true;
                RF_modeDisplayButton.setTextColor(Color.rgb(0, 0, 0));
                M_modeDisplayButton.setTextColor(Color.rgb(255, 0, 0));
            }
        });
    }
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
    private void SaveSingleRawDataSetup(){
        SaveSingle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (SingleRecordOn.get() == false) {
                    SingleRecordOn.set(true);
                    isSingleSave = true;
                    FileFolderNameDate = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()); //新增儲存路徑資料夾
                } else {
                    SingleRecordOn.set(false);
                    isSingleSave = false;
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
            float value = (float) saveRawData[i]* gain;
            stringTmp[i] = Float.toString(value);
        }
        String rawDataFileName = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+"_RF(" + j + ")"); //檔名設定
        dataSaveToFile.extelnalPrivateCreateFoler(FileFolderNameDate,rawDataFileName,  Arrays.toString(stringTmp)); //存檔
    }
    //可以存單筆資料

    private void SaveSingleRawData(double[] saveRawData) {
        int RecordLength ;
        RecordLength= (int)Math.round(Depth/6.16*1000); //儲存深度

        String[] stringTmp = new String[RecordLength]; //轉string
        for(int i =0; i<RecordLength; i++) {
            float value = (float) saveRawData[i]* gain;
            stringTmp[i] = Float.toString(value);
        }
        String rawDataFileName = (new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())+"_RF"); //檔名設定
        dataSaveToFile.extelnalPrivateCreateFoler(FileFolderNameDate,rawDataFileName,  Arrays.toString(stringTmp)); //存檔
    }
}