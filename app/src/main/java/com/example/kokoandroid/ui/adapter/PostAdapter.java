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

package com.example.kokoandroid.ui.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.kokoandroid.R;
import com.example.kokoandroid.model.Post;

import java.text.SimpleDateFormat;


public class PostAdapter extends ArrayAdapter<Post> {

    private LayoutInflater mInflater;

    public PostAdapter(Context context) {
        super(context, R.layout.list_item_post);
        mInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
    }

    class ViewHolder {
//        TextView id;
        TextView title;
        TextView date;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Post post = getItem(position);
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_item_post, null);
            holder = new ViewHolder();
//            holder.id = (TextView) convertView.findViewById(R.id.list_item_post_id);
            holder.title = (TextView) convertView.findViewById(R.id.list_item_post_text);
            holder.date = (TextView) convertView.findViewById(R.id.list_item_post_date);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

//        holder.id.setText(post.mId);
        holder.title.setText(post.mText);
        holder.date.setText(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(post.mDate));

        return convertView;
    }

    @Override
    public boolean isEnabled(int position){
        return false;
    }
}