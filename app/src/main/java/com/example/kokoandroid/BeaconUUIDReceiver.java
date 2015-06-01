package com.example.kokoandroid;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.kokoandroid.ui.MainActivity;

import java.util.ArrayList;

public class BeaconUUIDReceiver extends BroadcastReceiver {

    private MainActivity mActivity;
    private ArrayList<String> mBeaconUUIDList = new ArrayList<String>();

    public ArrayList<String> getBeaconUUIDList() {
        return mBeaconUUIDList;
    }

    public void setActivity(MainActivity activity) {
        mActivity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(getClass().getSimpleName(), "onReceive");

        if (mActivity == null) {
            return;
        }

        if (intent == null) {
            return;
        }

        mBeaconUUIDList = intent.getStringArrayListExtra("beacon_uuid_list");
        mActivity.onBeaconUUIDReceived(mBeaconUUIDList);
    }

    @Override
    public IBinder peekService(Context myContext, Intent service) {

        Log.d(getClass().getSimpleName(), "peekService");
        return super.peekService(myContext, service);
    }
}
