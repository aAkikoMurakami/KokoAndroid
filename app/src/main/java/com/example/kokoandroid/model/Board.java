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

package com.example.kokoandroid.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Board implements Parcelable {

    public String mId;
    public String mTitle;
    public ArrayList<Post> mPosts;

    public Board(String id, String title, ArrayList<Post> contents) {
        mId = id;
        mTitle = title;
        mPosts = contents;
    }

    public Board() {
        mId = "0";
        mTitle = "Board title";

        ArrayList<Post> postList = new ArrayList<Post>();
        for (int i = 0; i < 10; i++) {
            postList.add(new Post());
        }
        mPosts = postList;
    }

    public Board(Parcel in) {
        mId = in.readString();
        mTitle = in.readString();
        mPosts = in.createTypedArrayList(Post.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mId);
        out.writeString(mTitle);
        out.writeTypedList(mPosts);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Board> CREATOR = new Creator<Board>() {
        public Board createFromParcel(Parcel in) {
            return new Board(in);
        }

        public Board[] newArray(int size) {
            return new Board[size];
        }
    };
}
