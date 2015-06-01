/*
 * Copyright 2015 株式会社ACCESS 32期新卒チーム
 *
 * Apache License Version 2.0（「本ライセンス」）に基づいてライセンスされます。
 * あなたがこのファイルを使用するためには、本ライセンスに従わなければなりません。
 * 本ライセンスのコピーは下記の場所から入手できます。
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * 適用される法律または書面での同意によって命じられない限り、本ライセンスに基づいて頒布される
 * ソフトウェアは、明示黙示を問わず、いかなる保証も条件もなしに「現状のまま」頒布されます。
 * 本ライセンスでの権利と制限を規定した文言については、本ライセンスを参照してください。
 */

package com.example.kokoandroid.ui;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.kokoandroid.BeaconUUIDReceiver;
import com.example.kokoandroid.R;
import com.example.kokoandroid.ScanningService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BeaconTestActivity extends AppCompatActivity {
    /*
    stopLeScan, startLeScanはAndroid5.0以降では使えない。
    今回使用する端末がAndroid4.3なので使っている。
     */

    // 検出したbeaconのuuidを格納する
    HashMap<String, Integer> mBeaconList = new HashMap<String, Integer>();

    // ソートしたbeacon情報を格納する
    // [ {uuid -> distance}, {uuid -> distance}, ...]
    ArrayList mSortBeaconsList = new ArrayList();

    // スキャン時間
    private static final long SCAN_LENGTH = 5000;
    // スキャン周期
    private static final long SCAN_PERIOD = 20000;

    // スキャン時間操作用ハンドラ
    private Handler mLengthHandler = new Handler();
    // スキャン周期操作用ハンドラ
    private Handler mPeriodHandler = new Handler();

    private Runnable scanExit;
    private Runnable periodicalScan;

    private BeaconUUIDReceiver mReceiver;

    boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beacon_test);

//        mReceiver = new BeaconUUIDReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ScanningService.ACTION);
////        unregisterReceiver(mReceiver);
//        registerReceiver(mReceiver, filter);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        setTitle("Scanning Controller");

        // BluetoothManagerからのBluetoothAdapter取得
        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();

        // テスト用のボタンを生成します。
        Button button1 = (Button) findViewById(R.id.button_beacon_test_1);
        Button button2 = (Button) findViewById(R.id.button_beacon_test_2);
        Button button3 = (Button) findViewById(R.id.button_beacon_test_3);
        Button button4 = (Button) findViewById(R.id.button_beacon_test_4);

        // テスト用のテキストを表示するビューを生成します。
        final TextView textView1 = (TextView) findViewById(R.id.text_view_beacon_test_1);
        final TextView textView2 = (TextView) findViewById(R.id.text_view_beacon_test_2);
        final TextView textView3 = (TextView) findViewById(R.id.text_view_beacon_test_3);

        button1.setText("Force stop service");
        button2.setText("Toggle normal scan");
        button3.setText("Toggle periodical scan");
        button4.setText("Toggle service");

        textView1.setText("Normal scan status: Stopped");
        textView2.setText("Periodical scan status: Stopped");


        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfoList = am.getRunningServices(Integer.MAX_VALUE);


        for (ActivityManager.RunningServiceInfo rsi : serviceInfoList) {
            // クラス名を比較
            if (rsi.service.getClassName().equals(ScanningService.class.getName())) {
                isServiceRunning = true;
                break;
            }
        }

        if (isServiceRunning) {
            textView3.setText("Service status: Running");
        } else {
            textView3.setText("Service status: Stopped");
        }


        // スキャン停止のためのオブジェクト
        scanExit = new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);

                // mBeaconList のソート
                // mSortBeaconsList[0] が最も近いbeacon
                mSortBeaconsList = sortBeacons(mBeaconList);
                Log.d("finalList", "" + mSortBeaconsList);
            }
        };

        // 周期的にスキャンするためのオブジェクト
        periodicalScan = new Runnable() {
            @Override
            public void run() {
                mLengthHandler.postDelayed(scanExit, SCAN_LENGTH);
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                mPeriodHandler.postDelayed(periodicalScan, SCAN_PERIOD);
            }
        };

//        // 5秒後にスキャン停止
//        mLengthHandler.postDelayed(scanExit, SCAN_LENGTH);
//
//        // スキャン開始
//        // Android4.3以降、5.0未満対応
//        mBluetoothAdapter.startLeScan(mLeScanCallback);


        // button1 タップでサービスを強制停止
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopService(new Intent(getApplicationContext(), ScanningService.class));
                textView3.setText("Service status: Stopped");
                isServiceRunning = false;
            }
        });

        // button2 タップでスキャン開始・停止
        button2.setOnClickListener(new View.OnClickListener() {
            boolean isScanning = false;

            @Override
            public void onClick(View view) {
                if (!isScanning) {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);
                    textView1.setText("Normal scan status: Started");
                } else {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);

                    // mBeaconList のソート
                    // mSortBeaconsList[0]が最も近いbeacon
                    mSortBeaconsList = sortBeacons(mBeaconList);
                    Log.d("finalList", "" + mSortBeaconsList);
                    textView1.setText("Normal scan status: Stopped");
                }

                isScanning = !isScanning;
            }
        });

        // button3 タップで周期的スキャン開始・停止
        button3.setOnClickListener(new View.OnClickListener() {
            boolean isScanning = false;

            @Override
            public void onClick(View view) {
                if (!isScanning) {
                    periodicalScan.run();
                    textView2.setText("Periodical scan status: Started");

                } else {
                    mPeriodHandler.removeCallbacks(periodicalScan);
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    textView2.setText("Periodical scan status: Stopped");
                }

                isScanning = !isScanning;
            }

        });

        //button4 タップでバックグラウンドスキャン開始
        button4.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (!isServiceRunning) {
                    startService(new Intent(getApplicationContext(), ScanningService.class));
                    textView3.setText("Service status: Running");

                } else {
                    stopService(new Intent(getApplicationContext(), ScanningService.class));
                    textView3.setText("Service status: Stopped");
                }

                isServiceRunning = !isServiceRunning;

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            // デバイスが検出される度に呼び出さる処理
            if(scanRecord.length > 30)
            {
                Log.d("onLeScan", "scanRecord.length > 30");

                // iBeaconの場合、6byte目から9byte目は値が決まっている
                // iBeaconである否かの判定
                if((scanRecord[5] == (byte)0x4c) && (scanRecord[6] == (byte)0x00) &&
                   (scanRecord[7] == (byte)0x02) && (scanRecord[8] == (byte)0x15))
                {
                    // iBeaconの場合のみ、以下の処理を行う。
                    Log.d("onLeScan", "find iBeacon");

                    // scanRecord の中でuuid を示すのは10byte目から25byte目
                    String uuidFormat = "XXXX-XX-XX-XX-XXXXXX";

                    int scanRecordIndex = 9;
                    String uuid = "";

                    int i;

                    for(i = 0; i < uuidFormat.length(); i++) {
                        if (uuidFormat.charAt(i) == '-')
                            uuid += '-';
                        else
                            uuid += changeIntToHex(scanRecord[scanRecordIndex++]);
                    }

                    Log.d("uuid:", uuid);

                    // majorとminorの取得
                    String major = changeIntToHex(scanRecord[25]) + changeIntToHex(scanRecord[26]);
                    String minor = changeIntToHex(scanRecord[27]) + changeIntToHex(scanRecord[28]);

                    Log.d("major", major);
                    Log.d("minor", minor);

                    // beaconまでの距離概算
                    int distance = calcDistance(rssi);

                    if (mBeaconList.containsKey(uuid)) {
                        // 検出済みのbeaconだった場合、距離の平均を求める
                        distance = (mBeaconList.get(uuid) + distance) / 2;
                    }

                    // mBeaconListにuuidとdistanceを追加する
                    mBeaconList.put(uuid, distance);

                    Log.d("mBeaconList", "" + mBeaconList);

                }
            }
        }
    };

    @Override
    protected void onDestroy() {
//        if (mReceiver != null) {
//            unregisterReceiver(mReceiver);
//        }
        super.onDestroy();
    }

    // intから2桁16進数への変換
    public String changeIntToHex(int i) {
        char hex[] = { Character.forDigit((i >> 4) & 0x0f, 16), Character.forDigit(i & 0x0f, 16) };
        String hex_str = new String(hex);

        return hex_str.toUpperCase();
    }

    // 検知したbeaconまでの距離概算
    public int calcDistance(int rssi) {
        return 1 - (rssi + 59) * 10 / 20;
    }

    // mBeaconListを距離（value）でソート
    // ソートした配列をsortListに格納し返す
    public ArrayList sortBeacons(HashMap beaconList) {
        // ソート後の配列
        ArrayList sortList = new ArrayList(beaconList.entrySet());

        Collections.sort(sortList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Map.Entry entry1 = (Map.Entry) o1;
                Map.Entry entry2 = (Map.Entry) o2;

                return ((Integer) entry1.getValue()).compareTo((Integer) entry2.getValue());
            }
        });

        Log.d("sortBeaconsList", "" + sortList);

        return sortList;
    }

//    // スキャンを開始する
//    public boolean startScan() {
//        return mBluetoothAdapter.startLeScan(mLeScanCallback);
//    }
//
//    // スキャンを停止する
//    public void stopScan() {
//        mBluetoothAdapter.stopLeScan(mLeScanCallback);
//    }

}
