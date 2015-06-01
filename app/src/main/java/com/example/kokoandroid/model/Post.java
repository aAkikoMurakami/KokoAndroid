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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


public class Post implements Parcelable {

    public String mId;
    public String mText;
    public Date mDate;

    static int counter = 0;

    public Post(String id, String text, String date) {
        mId = id;
        mText = text;

        //date : "2015-05-26T02:47:59.223Z"
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, parseIntSubstr(date, 0, 4));
        cal.set(Calendar.MONTH, parseIntSubstr(date, 5, 7) - 1);
        cal.set(Calendar.DAY_OF_MONTH, parseIntSubstr(date, 8, 10));
        cal.set(Calendar.HOUR_OF_DAY, parseIntSubstr(date, 11, 13));
        cal.set(Calendar.MINUTE, parseIntSubstr(date, 14, 16));
        cal.set(Calendar.SECOND, parseIntSubstr(date, 17, 19));

        cal.setTimeInMillis(cal.getTimeInMillis() + TimeZone.getDefault().getRawOffset());

        mDate = cal.getTime();


    }

    private int parseIntSubstr(String str, int start, int end) {
        return Integer.parseInt(str.substring(start, end));
    }

    public Post() {
        mId = String.valueOf(counter++);
        mText = "Post text";
        mDate = new Date();
    }

    public Post(Parcel in) {
        mId = in.readString();
        mText = in.readString();
        mDate = new Date(in.readString());
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mId);
        out.writeString(mText);
        out.writeString(mDate.toString());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Post> CREATOR = new Parcelable.Creator<Post>() {
        public Post createFromParcel(Parcel in) {
            return new Post(in);
        }

        public Post[] newArray(int size) {
            return new Post[size];
        }
    };

}
