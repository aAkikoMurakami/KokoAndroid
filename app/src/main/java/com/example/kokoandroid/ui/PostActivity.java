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

import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.kokoandroid.KokoClientCommunicator;
import com.example.kokoandroid.R;

public class PostActivity extends AppCompatActivity implements TextWatcher {

    private Button mSendButton;
    private EditText mPostEditText;

    private String mBoardId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        if (getIntent() != null) {
            mBoardId = getIntent().getStringExtra("board_id");
        }

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        setTitle(R.string.post_activity_title);

        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.setEnabled(false);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSendButtonClick(v);
            }
        });

        mPostEditText = (EditText) findViewById(R.id.edit_text_post);
        mPostEditText.addTextChangedListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

        if (TextUtils.isEmpty(mPostEditText.getText())) {
            mSendButton.setEnabled(false);

        } else {
            mSendButton.setEnabled(true);
        }
    }

    private void onSendButtonClick(View v) {

        if (TextUtils.isEmpty(mPostEditText.getText())) {
            Toast.makeText(this, R.string.post_is_empty, Toast.LENGTH_SHORT);
            return;
        }

        KokoClientCommunicator communicator = new KokoClientCommunicator();

        KokoClientCommunicator.AsyncPostRequest task =
                communicator.new AsyncPostRequest(this, new KokoClientCommunicator.AsyncPostCallback() {
            @Override
            public void onPreExecute() {
            }

            @Override
            public void onProgressUpdate(int progress) {
            }

            @Override
            public void onPostExecute(boolean isPosted) {

                Log.d(getClass().getSimpleName(), "isPosted: " + isPosted);

                if (isPosted) {
                    setResult(RESULT_OK);
                } else {
                    setResult(RESULT_CANCELED);
                }

                finish();
            }

            @Override
            public void onCancelled() {
            }
        });
        task.execute(mPostEditText.getText().toString(), mBoardId);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
