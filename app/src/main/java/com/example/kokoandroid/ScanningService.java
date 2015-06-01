package com.example.kokoandroid;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ScanningService extends Service {
    // stopLeScan, startLeScan, getRunningTasksはAndroid5.0以降では使えない。
    // 今回使用する端末がAndroid4.3なので使っている。

    public static final String ACTION = "ScanningService Action";

    final static String TAG = "ScanningService";

    private Timer startTimer;
    private boolean isTimer = false;
    private Timer mBroadcastTimer;

    // 検出したbeaconのuuidを格納する
    HashMap<String, Integer> mBeaconList = new HashMap<String, Integer>();

    // ソートしたbeacon情報を格納する
    // [ {uuid -> distance}, {uuid -> distance}, ...]
    ArrayList<String> mSortBeaconsList = new ArrayList<String>();

    // これまでに見つけたことのあるbeaconリスト
    LinkedHashSet<String> mFoundBeaconSet = new LinkedHashSet<String>();

    // 最初にbroadcastするタイミング
    private static final long BROADCAST_FIRST = 0;
    // broadcast周期
    private static final long BROADCAST_PERIOD = 3000;

    private static final int NOTIFY_ID = 0;
    private NotificationManager mNotificationManager;

    // BluetoothManagerからのBluetoothAdapter取得
    BluetoothManager mBluetoothManager = null;
    BluetoothAdapter mBluetoothAdapter = null;

    public ScanningService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        Log.d(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // サービスを開始したら、beacon検知を始め、
        // BROADCAST_PERIOD時間毎にbroadcastする。

        Log.d(TAG, "onStartCommand");

//        startTimer = new Timer();

        startScan();
        Log.e("===========", "startScan");

        mBroadcastTimer = new Timer();
        isTimer = true;

        mBroadcastTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                // mBeaconListのソート
                // mSortBeaconsList[0]が最も近いbeacon
                mSortBeaconsList = sortBeacons(mBeaconList);
                Log.d("finalList", "" + mSortBeaconsList);

                // broadcastする
                broadcastBeaconInfo();
                Log.e("==========", "broadcast");

                // mBeaconList を破棄する
                mBeaconList.clear();


            }
        }, BROADCAST_FIRST, BROADCAST_PERIOD);

//        startTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                Timer stopTimer = new Timer();
//                isTimer = true;
//
//                startScan();
//                Log.e("============", "start");
//
//                stopTimer.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        stopScan();
//
//                        broadcastBeaconInfo();
//                        Log.e("============", "stop");
//                    }
//                }, 5000);
//            }
//        }, 0, 20000);


        return START_STICKY;
    }

    private void broadcastBeaconInfo() {
        Intent beaconInfo = new Intent(ACTION);
        beaconInfo.putStringArrayListExtra("beacon_uuid_list", mSortBeaconsList);
        sendBroadcast(beaconInfo);
        mBeaconList.clear();

        Log.d(TAG, "beaconInfo sent");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mBroadcastTimer.cancel();

        stopScan();
        Log.e("==========", "stopScan");

        broadcastBeaconInfo();
        Log.e("==========", "broadcast");

        // Notification削除
        deleteNotification();

        Log.d(TAG, "onDestroy");
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            // デバイスが検出される度に呼び出さる処理
            // BLE 端末のscanRecord byte数は必ず30以上
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

                    // running task情報
                    ActivityManager runningTask = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
                    // foreground アプリの名前
                    String foregroundApp = runningTask.getRunningTasks(1).get(0).topActivity.getPackageName();

                    // 新しいbeaconを発見、かつKokoがforegroundでない場合のみ通知する
                    if (!mFoundBeaconSet.contains(uuid)) {
                        // 履歴に新しいbeaconを追加
                        mFoundBeaconSet.add(uuid);

                        // Koko がforeground かどうか判定
                        // foreground でない場合に通知する
                        if (!foregroundApp.equals(getApplicationContext().getPackageName())) {
                            Log.d("foregroundApp", foregroundApp);

                            notifyNewBeacon();
                        }
                    }

                    // Kokoがforegroundの場合、Notificationを削除する
                    if (foregroundApp.equals(getApplicationContext().getPackageName())) {
                        deleteNotification();
                    }

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

    // 既に存在するKokoのNotificationを削除する
    private void deleteNotification() {
        if (mNotificationManager != null)
            mNotificationManager.cancel(NOTIFY_ID);
    }


    private void notifyNewBeacon() {
        // 新しいbeaconを見つけた通知する
        // TODO

        // 既にあるNotification削除
        deleteNotification();

        Notification newBeacon = new Notification.Builder(this)
                .setContentTitle("Koko")
                .setContentText("この場所にメッセージを残すことができます。")
                .setTicker("新しい掲示板を見つけました")
                .setContentIntent(
                        PendingIntent.getActivity(this, 0,
                                new Intent(this, com.example.kokoandroid.ui.MainActivity.class),
                                PendingIntent.FLAG_CANCEL_CURRENT)
                )
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        // Notification の登録
        mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(NOTIFY_ID, newBeacon);
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
        ArrayList<Map.Entry<String, Integer>> sortList = new ArrayList<Map.Entry<String, Integer>>(beaconList.entrySet());

        Collections.sort(sortList, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                Map.Entry entry1 = (Map.Entry) o1;
                Map.Entry entry2 = (Map.Entry) o2;

                return ((Integer) entry1.getValue()).compareTo((Integer) entry2.getValue());
            }
        });

        ArrayList<String> sortedUUIDList = new ArrayList<String>();
        for (Map.Entry<String, Integer> entry : sortList) {
            sortedUUIDList.add(entry.getKey());
        }

        Log.d("sortBeaconsList", "" + sortedUUIDList);

        return sortedUUIDList;
    }

    // スキャンを開始する
    public boolean startScan() {
        return mBluetoothAdapter.startLeScan(mLeScanCallback);
    }

    // スキャンを停止する
    public void stopScan() {
        mBluetoothAdapter.stopLeScan(mLeScanCallback);

        // mBeaconList のソート
        // mSortBeaconsList[0]が最も近いbeacon
        mSortBeaconsList = sortBeacons(mBeaconList);
        Log.d("finalList", "" + mSortBeaconsList);
    }

}
