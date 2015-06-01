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
import android.app.Fragment;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kokoandroid.BeaconUUIDReceiver;
import com.example.kokoandroid.KokoClientCommunicator;
import com.example.kokoandroid.ScanningService;
import com.example.kokoandroid.model.Board;
import com.example.kokoandroid.ui.adapter.BoardAdapter;
import com.example.kokoandroid.R;

import java.util.ArrayList;
import java.util.List;


// このアクティビティでは、ナビゲーションドロワーを実装するために Google のサンプルコードを
// 参考にしています。ナビゲーションドロワーのより詳しい説明については
// https://developer.android.com/training/implementing-navigation/nav-drawer.html
// を参照してください。

public class MainActivity extends AppCompatActivity implements
        BoardFragment.OnFragmentInteractionListener,
        KokoClientCommunicator.AsyncGetCallback {

    private static final int BOARD_ORDER_NEAREST = 0;

    private KokoClientCommunicator mCommunicator;

    // ドロワー本体のビュー
    private View mLeftDrawer;

    // ナビゲーションドロワー本体
    private DrawerLayout mDrawerLayout;

    // ナビゲーションドロワー内のメニュー項目
    private ListView mDrawerListView;


    private View mDrawerBlankView;
    private TextView mDrawerBlankTextView;


    private BoardAdapter mBoardAdapter;

    // ナビゲーションドロワーに対するアクションが発生した場合の反応
    private ActionBarDrawerToggle mDrawerToggle;

    private BeaconUUIDReceiver mReceiver;

    private int mPreviousSelectedDrawerPosition = -1;
    private Board openedBoard = null;
    private boolean activityOnForeground = false;

    private String mDrawerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Activityの初期化処理
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Material Design に対応するために、このアクティビティでは Activity クラスではなく
        // AppCompatActivity クラスを継承しています。そのため、アクションバーのインスタンスを
        // 取得する場合は android.app.ActionBar#getActionBar メソッドではなく
        // android.support.v7.app.ActionBar#getSupportActionBar メソッドを用います。
        final ActionBar actionBar = getSupportActionBar();

        // アクションバーのホームアイコンを、押下可能なボタンにします。これにより、ホームアイコンを
        // 押下することでナビゲーションドロワーを開閉したり、前の画面に戻ったりすることを可能にします。
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // HTTPクライアントを用意
        mCommunicator = new KokoClientCommunicator();

        // Beaconの周期スキャンのためのバックグラウンドサービス起動
        startScanningService();

        // バックグラウンドサービスからUUIDを受け取るためのレシーバを生成・登録
        mReceiver = new BeaconUUIDReceiver();
        mReceiver.setActivity(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScanningService.ACTION);
//        unregisterReceiver(mReceiver);
        registerReceiver(mReceiver, filter);

        // 上部タイトル・ツールバー
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        mDrawerTitle = getString(R.string.app_name);

        // 左開閉メニュー
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLeftDrawer = findViewById(R.id.left_drawer);
        mDrawerBlankView = findViewById(R.id.left_drawer_blank_view);
        mDrawerBlankTextView = (TextView) findViewById(R.id.drawer_blank_text_view);
        mDrawerListView = (ListView) findViewById(R.id.list_view_left_drawer);

        // ナビゲーションドロワー内の ListView のそれぞれの項目をタップした時の反応を定義する
        // OnItemClickListener オブジェクトを生成して、それを ListView へ反映させます。
        mDrawerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectItem(position);
            }
        });

        // ナビゲーションドロワー内の ListView に表示させる項目の実体となるアダプターを生成します。
        mBoardAdapter = new BoardAdapter(this);

//        View footerSettingItemView = getLayoutInflater().inflate(R.layout.list_item_drawer_footer_item, null);
//        ((TextView) footerSettingItemView.findViewById(R.id.text_view_drawer_footer_item)).setText(R.string.settings);
//        mDrawerListView.addFooterView(footerSettingItemView);


        // ビーコンのデバッグ用画面へ遷移するメニューを追加します。
        View footerBeaconTestItemView = getLayoutInflater().inflate(R.layout.list_item_drawer_footer_item, null);
        ((TextView) footerBeaconTestItemView.findViewById(R.id.text_view_drawer_footer_item)).setText("SCANNING CONTROLLER");
        mDrawerListView.addFooterView(footerBeaconTestItemView);

        // 生成したアダプターを ListView へ反映させます。
        mDrawerListView.setAdapter(mBoardAdapter);


        // デザイン作成のためのダミーデータ追加
//        for (int i = 0; i < 3; i++) {
//            mBoardAdapter.add(new Board());
//        }

        // ナビゲーションドロワーに対して何らかの操作を行った場合に、ナビゲーションドロワーが
        // どのように反応するかを定義するオブジェクトを生成します。
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* 親のアクティビティ（ここでは「この」アクティビティ）のインスタンス */
                mDrawerLayout,         /* DrawerLayout オブジェクト */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            @Override
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                actionBar.setTitle(mDrawerTitle);

//                Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
//                if (fragment instanceof BoardFragment) {
//                    ((BoardFragment) fragment).showPostButton();
//                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                actionBar.setTitle(getString(R.string.app_name));

//                Fragment fragment = getFragmentManager().findFragmentById(R.id.content_frame);
//                if (fragment instanceof BoardFragment) {
//                    BoardFragment boardFragment = (BoardFragment) fragment;
//                    boardFragment.hidePostButton();
//                    boardFragment.forceStopRefreshLayout();
//                }
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // 初期化処理完了後、最近傍のBeaconの掲示板を自動で開きます。
//        openNearestBoard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityOnForeground = true;
    }

    private void startScanningService() {

        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfoList = am.getRunningServices(Integer.MAX_VALUE);

        boolean isRunning = false;

        for (ActivityManager.RunningServiceInfo rsi : serviceInfoList) {
            // クラス名を比較
            if (rsi.service.getClassName().equals(ScanningService.class.getName())) {
                isRunning = true;
                break;
            }
        }

        if (!isRunning) {
            startService(new Intent(getApplicationContext(), ScanningService.class));
        }
    }

    @Override
    protected void onDestroy() {
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // 画面回転等でこの画面が再生成された場合に、ナビゲーションドロワーの開閉状態を同期します。
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

//    @Override
//    public boolean onPrepareOptionsMenu(Menu menu) {
//        // If the nav drawer is open, hide action items related to the content view
////        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerListView);
//        return super.onPrepareOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onBeaconUUIDReceived(ArrayList<String> uuidList) {

        Log.d(getClass().getSimpleName(), "onBeaconUUIDReceived: " + uuidList);

        KokoClientCommunicator.AsyncGetRequestWithUUID task =
                mCommunicator.new AsyncGetRequestWithUUID(MainActivity.this, this);

        String[] uuidArray = new String[uuidList.size()];
        for (int i = 0; i < uuidList.size(); i++) {
            uuidArray[i] = uuidList.get(i);
        }

        task.execute(uuidArray);
//        task.execute("E02CC25E-0049-4185-832C-3A65DB755D01", "AAAAAAAA-0049-4185-832C-3A65DB755D01");
    }

    private void showBlankView(String message) {
        mDrawerBlankTextView.setText(message);
        mDrawerBlankView.setVisibility(View.VISIBLE);
    }

    private void hideBlankView() {
        mDrawerBlankTextView.setText(null);
        mDrawerBlankView.setVisibility(View.GONE);
    }

    // メニューから選択したActivityやFragmentに遷移
    private void selectItem(int position) {

        // BeaconTestActivityに遷移
        if (position == mDrawerListView.getCount() - 1) {

            startActivity(new Intent(getApplicationContext(), BeaconTestActivity.class));
            mDrawerListView.setItemChecked(position, false);

            if (mPreviousSelectedDrawerPosition != -1) {
                mDrawerListView.setItemChecked(mPreviousSelectedDrawerPosition, true);
            }

//        } else if (position == mDrawerListView.getCount() - 2) {
//            // Settingsに遷移
//
//            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
//            mDrawerListView.setItemChecked(position, false);
//
//            if (mPreviousSelectedDrawerPosition != -1) {
//                mDrawerListView.setItemChecked(mPreviousSelectedDrawerPosition, true);
//            }

        } else {
            // 選択した掲示板に遷移

            openSelectedBoard(position);
        }

        mDrawerLayout.closeDrawer(mLeftDrawer);
    }

    // メニューもしくはBoardAdapterにおけるposition番目の掲示板を開きます。
    // positionは0始まりで、追加時に距離によってソートされているので、0が最近傍です。
    protected void openSelectedBoard(int position) {
        // 新しい Fragment を生成して、パラメーターを設定します。
        Fragment fragment = new BoardFragment();
        Board board = (Board) mDrawerListView.getItemAtPosition(position);
        Bundle args = new Bundle(2);
        args.putString("board_id", board.mId);
        args.putParcelableArrayList("posts", board.mPosts);
        fragment.setArguments(args);

        mDrawerTitle = board.mTitle;

        // アクションバーのインスタンスを再取得しないと反映されない。
        getSupportActionBar().setTitle(mDrawerTitle);

        // これまで Fragment を表示していた領域を、生成した Fragment で置き換えます。
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
        openedBoard = board;

        // 表示した Fragment に対応するメニュー内の項目を選択状態にします。
        mDrawerListView.setItemChecked(position, true);

        mPreviousSelectedDrawerPosition = position;
    }

    // 最近傍BeaconのBoardを開きます。
    protected void openNearestBoard() {
        openSelectedBoard(BOARD_ORDER_NEAREST);
        mDrawerLayout.closeDrawer(mLeftDrawer);
        hideBlankView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityOnForeground = false;
    }

    @Override
    public void onPreExecute() {

    }

    @Override
    public void onProgressUpdate(int progress) {
    }

    @Override
    public void onPostExecute(String[] result) {

        // 掲示板リストを先にクリア
        mBoardAdapter.clear();

        if (mDrawerListView.isItemChecked(mPreviousSelectedDrawerPosition)) {
            mDrawerListView.setItemChecked(mPreviousSelectedDrawerPosition, false);
        }

        boolean isInOpenedBoardRange = false;

        // エラー時のメッセージ内容。
        String errorMessage = getString(R.string.no_beacons_found);

        // 最新のBeacon取得結果を元に掲示板リストに掲示板を追加
        for (int i = 0; i < result.length; i++) {

            if (result[i].equals(KokoClientCommunicator.HTTP_ERROR)) {
                errorMessage = getString(R.string.server_error);
                break;
            } else if (result[i].equals(KokoClientCommunicator.NETWORK_PROBLEM)) {
                errorMessage = getString(R.string.no_internet_connection);
                break;
            }

            Board board = mCommunicator.parseJsonBoardInfo(result[i]);
            if (board != null)
                mBoardAdapter.add(board);

            if (mPreviousSelectedDrawerPosition != -1 &&
                    openedBoard != null &&
                    board.mId.equals(openedBoard.mId)) {
                mDrawerListView.setItemChecked(i, true);
                mPreviousSelectedDrawerPosition = i;
            }

            if (openedBoard != null && board.mId == openedBoard.mId) isInOpenedBoardRange = true;
        }

        // すでに開いていた掲示板（openedBoard）のBeaconから離れた場合に、Toastで通知
        if (activityOnForeground
                && openedBoard != null && !isInOpenedBoardRange) {
            Toast lostBoardToast = Toast.makeText(this, "またね！【掲示板から離れました】", Toast.LENGTH_LONG);
            lostBoardToast.show();
            // 開いている掲示板を閉じる。
            getFragmentManager()
                    .beginTransaction()
                    .replace(R.id.content_frame, new PlaceholderFragment())
                    .commit();
            openedBoard = null;
        }

        // 最近傍のBeaconの掲示板を開きます。
        if (activityOnForeground
                && openedBoard == null) {
            // アクティビティを初めて立ち上げた時,見ていた掲示板から離れた時に実行されます。
            if (this.mBoardAdapter.getCount() > 0)
                openNearestBoard();
            else {
                showBlankView(getString(R.string.no_beacons_found));
                mDrawerLayout.openDrawer(mLeftDrawer);
            }
        }


//        View footerSettingItemView = getLayoutInflater().inflate(R.layout.list_item_drawer_footer_item, null);
//        ((TextView) footerSettingItemView.findViewById(R.id.text_view_nav_footer_item)).setText(R.string.settings);
//        mDrawerListView.addFooterView(footerSettingItemView);
//
//
//        // ビーコンのデバッグ用画面へ遷移するメニューを追加します。
//        View footerBeaconTestItemView = getLayoutInflater().inflate(R.layout.list_item_drawer_footer_item, null);
//        ((TextView) footerBeaconTestItemView.findViewById(R.id.text_view_nav_footer_item)).setText(R.string.activity_beacon_test);
//        mDrawerListView.addFooterView(footerBeaconTestItemView);

        // 生成したアダプターを ListView へ反映させます。
//        mDrawerListView.setAdapter(mBoardAdapter);
    }

    @Override
    public void onCancelled() {

    }

    @Override
    public void onFragmentInteraction(String id) {
        Log.d(getClass().getSimpleName(), "onFragmentInteraction called.");
    }
}
