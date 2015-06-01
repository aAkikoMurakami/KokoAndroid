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

package com.example.kokoandroid;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.example.kokoandroid.model.Board;
import com.example.kokoandroid.model.Post;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;

public class KokoClientCommunicator {

    public static final String HTTP_ERROR = "HTTP_ERROR";
    public static final String NETWORK_PROBLEM = "NETWORK_PROBLEM";

    public interface AsyncGetCallback {
        void onPreExecute();
        void onProgressUpdate(int progress);
        void onPostExecute(String[] result);
        void onCancelled();
    }

    public class AsyncGetRequestWithUUID extends AsyncTask<String, Integer, String[]> {

        private AsyncGetCallback mAsyncGetCallback = null;
        private Context mContext;

        public AsyncGetRequestWithUUID(Context context, AsyncGetCallback asyncGetCallback) {
            mContext = context;
            mAsyncGetCallback = asyncGetCallback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mAsyncGetCallback.onPreExecute();
        }

        @Override
        protected String[] doInBackground(String... uuid) {

            String jsonData[] = new String[uuid.length];

            for (int i = 0; i < uuid.length; i++) {
                URL url = getBoardUrlFromUUID(uuid[i]);
                jsonData[i] = getResponseBody(url);
            }

            return jsonData;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mAsyncGetCallback.onProgressUpdate(values[0]);
        }

        @Override
        protected void onPostExecute(String[] jsonData) {
            super.onPostExecute(jsonData);
            mAsyncGetCallback.onPostExecute(jsonData);
        }

        @Override
        protected void onCancelled() {
            this.cancel(true);
            mAsyncGetCallback.onCancelled();
        }

        Context getContext() {
            return mContext;
        }
    }

    public class AsyncGetRequestWithBoardID extends AsyncGetRequestWithUUID {

        public AsyncGetRequestWithBoardID(Context context, AsyncGetCallback asyncGetCallback){
            super(context, asyncGetCallback);
        }

        @Override
        protected String[] doInBackground(String... boardId){

            String jsonData[] = new String[boardId.length];

            for (int i = 0; i < boardId.length; i++) {
                URL url = getBoardUrlFromBoardID(boardId[i]);
                jsonData[i] = getResponseBody(url);
            }

            return jsonData;
        }
    }

    public interface AsyncPostCallback {
        void onPreExecute();
        void onProgressUpdate(int progress);
        void onPostExecute(boolean isPosted);
        void onCancelled();
    }

    public class AsyncPostRequest extends AsyncTask<String, Integer, Boolean> {

        private AsyncPostCallback mAsyncPostCallback = null;
        private Context mContext;

        public AsyncPostRequest(Context context, AsyncPostCallback asyncPostCallback) {
            mContext = context;
            mAsyncPostCallback = asyncPostCallback;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mAsyncPostCallback.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            byte[] contentBytes = null;
            int byteLength = 0;

            String newContent = null;

            DataOutputStream dos = null;

            try {

                newContent = "post[content]=" +
                        params[0] + "&post[board_id]=" +
                        params[1];

                //            contentBytes = setContent.toString().getBytes("UTF-8");
                contentBytes = newContent.getBytes("UTF-8");
                byteLength = contentBytes.length;

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            try {
                URL url = new URL("https://koko-server.herokuapp.com/posts.json");

                // Begin post process
                HttpURLConnection postConnection = null;
                postConnection = (HttpURLConnection) url.openConnection();
                postConnection.setChunkedStreamingMode(0);
                postConnection.setUseCaches(false);
                postConnection.setRequestMethod("POST");


                postConnection.setRequestProperty("Connection", "Keep-Alive");
                // postConnection.setRequestProperty("Content-type", "application/json; charset=utf-8");
                postConnection.setRequestProperty("Content-type", "application/x-www-form-urlencoded; charset=utf-8");
                postConnection.setRequestProperty("accept", "application/json");
                postConnection.setRequestProperty("Content-Length", String.valueOf(byteLength));
                //postConnection.setDoInput(true);
                postConnection.setDoOutput(true);

                dos = new DataOutputStream(postConnection.getOutputStream());
                dos.write(newContent.getBytes("UTF-8"));
                dos.flush();
                if (postConnection.getResponseCode() == 200 || postConnection.getResponseCode() == 201) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(postConnection.getInputStream()));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        Log.e(getClass().getSimpleName(), line);
                    }
                    return true;
                } else {
                    Log.e(getClass().getSimpleName(), "Connection Failed:" + postConnection.getResponseCode());
                }

            } catch (MalformedURLException me){
                me.printStackTrace();
            } catch (ProtocolException pe){
                pe.printStackTrace();
            } catch (IOException ioe){
                ioe.printStackTrace();
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            mAsyncPostCallback.onProgressUpdate(values[0]);
        }

        @Override
        protected void onPostExecute(Boolean isPosted) {
            super.onPostExecute(isPosted);
            mAsyncPostCallback.onPostExecute(isPosted);
        }

        @Override
        protected void onCancelled() {
            this.cancel(true);
            mAsyncPostCallback.onCancelled();
        }

        Context getContext() {
            return mContext;
        }
    }

    private String getResponseBody(URL url) {

        String data = "";
        HttpURLConnection connection = null;

        try {
            // URL先に接続する処理の準備
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            if (connection.getResponseCode() == 200 || connection.getResponseCode() == 201) {

                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder sb = new StringBuilder();

                data = br.toString();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

                br.close();

                data = sb.toString();

            } else {

                Log.d(getClass().getSimpleName(), "connection failed.");
                data = HTTP_ERROR;
            }

        } catch (IOException e) {

//            e.printStackTrace();
            data = NETWORK_PROBLEM;

        } finally {

            if (connection != null) {
                connection.disconnect();
            }
        }

        return data;
    }

    private URL getBoardUrlFromUUID(String uuid){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://koko-server.herokuapp.com/beacons/find/");
        stringBuilder.append(uuid);
        stringBuilder.append(".json");

        URL url = null;

        try {
            url = new URL(stringBuilder.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    private URL getBoardUrlFromBoardID(String boardid){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("https://koko-server.herokuapp.com/boards/");
        stringBuilder.append(boardid);
        stringBuilder.append(".json");

        URL url = null;

        try {
            url = new URL(stringBuilder.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return url;
    }

    public Board parseJsonBoardInfo(String jsonData) {

        Log.d(getClass().getSimpleName(), "parseJsonBoardInfo");

        try {
            JSONObject jObject = new JSONObject(jsonData);

            JSONArray jPostArray = new JSONArray(jObject.getString("posts"));
            ArrayList<Post> postArray = new ArrayList<Post>();

            for (int i = 0; i < jPostArray.length(); i++) {
                JSONObject obj = jPostArray.getJSONObject(i);
                Log.d(getClass().getSimpleName(), "parseJsonBoard" + i);
                Post post = new Post(obj.getString("id"), obj.getString("content"), obj.getString("created_at"));
                Log.d(getClass().getSimpleName(), "parseJsonBoard" + i + "_");
                postArray.add(post);
            }


            return new Board(jObject.getString("board_id"), jObject.getString("title"), postArray);

        } catch (JSONException e) {
            e.getStackTrace();
        } catch (Exception e) {
            e.getStackTrace();
        }

        return null;
    }
}
