package com.example.bitchat;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

public class CryptoAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<HashMap<String, String>> cryptoData;

    public CryptoAdapter(Context context, ArrayList<HashMap<String, String>> cryptoData) {
        this.context = context;
        this.cryptoData = cryptoData;
    }

    public void updateData(ArrayList<HashMap<String, String>> newData) {
        cryptoData.clear();
        cryptoData.addAll(newData);
        notifyDataSetChanged(); // 데이터 갱신 알림
    }

    public ArrayList<HashMap<String, String>> getData() {
        return cryptoData; // 어댑터의 데이터 반환
    }

    @Override
    public int getCount() {
        return cryptoData.size();
    }

    @Override
    public Object getItem(int position) {
        return cryptoData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.crypto_item, parent, false);
        }

        TextView cryptoName = convertView.findViewById(R.id.crypto_name);
        TextView cryptoPrice = convertView.findViewById(R.id.crypto_price);

        // 데이터 가져오기
        HashMap<String, String> crypto = cryptoData.get(position);
        String name = crypto.get("name");
        String price = crypto.get("price");
        String priceColor = crypto.get("color");

        // 텍스트 설정
        cryptoName.setText(name);
        cryptoPrice.setText(price);

        // 가격 텍스트 색상 변경
        if ("red".equals(priceColor)) {
            cryptoPrice.setTextColor(Color.RED);
        } else if ("blue".equals(priceColor)) {
            cryptoPrice.setTextColor(Color.BLUE);
        } else if ("black".equals(priceColor)) {
            cryptoPrice.setTextColor(Color.BLACK);
        }

        return convertView;
    }
}

