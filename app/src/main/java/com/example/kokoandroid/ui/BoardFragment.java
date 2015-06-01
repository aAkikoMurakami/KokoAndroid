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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.example.kokoandroid.KokoClientCommunicator;
import com.example.kokoandroid.ui.adapter.PostAdapter;
import com.example.kokoandroid.R;
import com.example.kokoandroid.model.Post;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BoardFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class BoardFragment extends Fragment implements
        SwipeRefreshLayout.OnRefreshListener, AbsListView.OnScrollListener {

    private static final int PARAMS_FILL = ViewGroup.LayoutParams.MATCH_PARENT;

    private View mPlaceholderView;

    // 「引っ張って更新」を実現するためのビュー
    private ListSwipeRefreshLayout mRefreshLayout;

    // 投稿の一覧
    private ListView mListView;

    // 投稿の一覧に表示させる項目の実体
    private PostAdapter mAdapter;

    // 投稿を新規に行う画面へ遷移するためのボタン
    private FrameLayout mPostButtonLayout;

    private View mBoardPlaceholderView;

    // 投稿を新規に行う画面へ遷移するためのボタン
    private Button mPostButton;

    // 投稿の一覧の最下部に表示する、ページャによる読込中であることを示すビュー
    private View mLoadingItemView;

    // ページャによる読み込み処理が実行されているかどうかを示すフラグ
    private boolean mRefreshTaskIsRunning = false;

    private int previousFirstVisibleItem = 0;

    private OnFragmentInteractionListener mListener;

    private KokoClientCommunicator mCommunicator;

    private String mBoardId;


    public BoardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mLoadingItemView = inflater.inflate(R.layout.list_item_loading, null, false);

        View view = inflater.inflate(R.layout.fragment_board, container, false);

        mBoardPlaceholderView = view.findViewById(R.id.board_empty_placeholder);

        mListView = (ListView) view.findViewById(R.id.list_view_post);

        // 投稿の一覧をスクロールした場合に実行される処理を設定します。
        mListView.setOnScrollListener(this);

        mAdapter = new PostAdapter(getActivity());

//        // ActionBar の高さ分のマージンを持つ View を生成して、ListView の上部マージンとして利用します。
//        View headerView = new View(getActivity());
//        headerView.setMinimumHeight(((MainActivity) getActivity()).getSupportActionBar().getHeight());
//        mListView.addHeaderView(headerView);

        // ListView 内の最下部に「読み込み中」の表示をします。
        //mListView.addFooterView(mLoadingItemView);

        mListView.setAdapter(mAdapter);

        if (getArguments() != null) {
            mBoardId = getArguments().getString("board_id");

            // 投稿のオブジェクトをインテントから取得して、アダプターへ追加します。
            ArrayList<Post> postList = getArguments().getParcelableArrayList("posts");
            insertPosts(postList);
        }

        if (mAdapter.getCount() != 0) {
            mBoardPlaceholderView.setVisibility(View.GONE);
        } else {
            mBoardPlaceholderView.setVisibility(View.VISIBLE);
        }

        mPostButtonLayout = (FrameLayout) view.findViewById(R.id.button_post_layout);

        mPostButton = (Button) view.findViewById(R.id.button_post);
        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), PostActivity.class);
                intent.putExtra("board_id", mBoardId);
                startActivityForResult(intent, 100);
            }
        });

        // 「引っ張って更新」を実現するためのビューを生成して、その中に、この画面内に
        // 表示するべきビューを配置します。
        mRefreshLayout = new ListSwipeRefreshLayout(getActivity(), mListView);
        mRefreshLayout.addView(view, PARAMS_FILL, PARAMS_FILL);

        // 生成したビューを画面に対してどのように配置するかを示すパラメーターを明示的に指定します。
        mRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(PARAMS_FILL, PARAMS_FILL));

        // TODO: データ更新中に表示されるプログレスバーの配色を指定します。
//         mRefreshLayout.setColorSchemeResources();

        // 「引っ張って更新」を実行した場合に、実際に行われる処理を設定します。
        mRefreshLayout.setOnRefreshListener(this);

        mCommunicator = new KokoClientCommunicator();

        return mRefreshLayout;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String id) {
        if (mListener != null) {
            mListener.onFragmentInteraction(id);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        // この画面が非表示になった場合に、次にこの画面が表示された時にキャッシュの内容が
        // 再び描画されたりすることを防ぐために、データを解放します。
        mListener = null;
        mListView.setAdapter(null);
    }

    public void showPostButton() {
        mPostButtonLayout.setVisibility(View.VISIBLE);
    }
    public void hidePostButton() {
        mPostButtonLayout.setVisibility(View.GONE);
    }

    private void insertPosts(ArrayList<Post> postList) {
        mAdapter.clear();
        for (Post post : postList) {
            mAdapter.insert(post, 0);
            mBoardPlaceholderView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRefresh() {
        refreshPosts();
    }

    public void refreshPosts() {
        Log.d(getClass().getSimpleName(), "refreshPosts called.");
        mCommunicator = new KokoClientCommunicator();
        mCommunicator.new AsyncGetRequestWithBoardID(getActivity(), new KokoClientCommunicator.AsyncGetCallback() {
            @Override
            public void onPreExecute() {

            }

            @Override
            public void onProgressUpdate(int progress) {

            }

            @Override
            public void onPostExecute(String[] result) {
                Log.d(getClass().getSimpleName(), result[0]);
                insertPosts(mCommunicator.parseJsonBoardInfo(result[0]).mPosts);

                mRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled() {

            }
        }).execute(mBoardId);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(String id);
    }

    private class ListSwipeRefreshLayout extends SwipeRefreshLayout {

        // 親クラスの変数名と被らないようにするため、異なった命名規則を適用しています。
        private ListView _listView;

        public ListSwipeRefreshLayout(Context context, ListView listView) {
            super(context);
            _listView = listView;
        }

        @Override
        public boolean canChildScrollUp() {
            return _listView.getVisibility() == View.VISIBLE
                    && ViewCompat.canScrollVertically(_listView, -1);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {

        // スクロールの発生を検知します。
        if (previousFirstVisibleItem != firstVisibleItem) {

            // スクロールする前に ListView の先頭に表示していた項目のインデックスよりも、スクロール
            // によって次に ListView の先頭に表示される項目のインデックスが後ろにある場合は、
            // 下方向にスクロールしたと判断して、新規投稿ボタンを非表示にします。
            if (previousFirstVisibleItem < firstVisibleItem) {
                hidePostButton();

            } else {
                // そうでない場合は、上方向にスクロールしたと判断して、新規投稿ボタンを表示します。
                showPostButton();
            }
            previousFirstVisibleItem = firstVisibleItem;
        }

//        // アダプターのインスタンスが生成前の場合、または既に再読み込み処理が実行中の場合は、
//        // 新たに再読み込み処理を実行せずに終了します。
//        if (mAdapter == null || mRefreshTaskIsRunning) {
//            return;
//        }
//
//        // ListView 内のアイテムのうち、最後から数えて 2 番目のアイテムが画面内に表示された場合は、
//        // 再読み込み処理を実行します。
//        if (totalItemCount - (firstVisibleItem + visibleItemCount) < 3) {
//            mRefreshTaskIsRunning = true;
//
//            // TODO: 実際のデータを取得して追加するための処理を記述します。
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//
//                    // ダミーの投稿を追加します。
//                    for (int i = 0; i < 10; i++) {
//                        mAdapter.add(new Post());
//                    }
//
//                    // ListView 内の最下部に表示した「読み込み中」の表示を消去します。
////                    mListView.removeFooterView(mLoadingItemView);
////                    mListView.invalidateViews();
//
//                    mRefreshTaskIsRunning = false;
//                }
//            }, 2000);
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d(getClass().getSimpleName(), "onActivityResult called.");

        if (requestCode != 100) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            refreshPosts();
            Log.d(getClass().getSimpleName(), "Refreshing...........");
        }
    }
}
