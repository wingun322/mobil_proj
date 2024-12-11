package com.example.bitchat.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.example.bitchat.R;
import com.example.bitchat.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CryptoAdapter extends BaseAdapter {
    private Context context;
    private List<HashMap<String, String>> cryptoList;
    private OnItemClickListener onItemClickListener;

    private static class ViewHolder {
        TextView cryptoName;
        TextView cryptoPrice;
        Button favoriteButton;
    }

    public interface OnItemClickListener {
        void onItemClick(String cryptoId);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public CryptoAdapter(Context context, List<HashMap<String, String>> cryptoList) {
        this.context = context;
        this.cryptoList = cryptoList;
    }

    public void updateData(List<HashMap<String, String>> newData) {
        this.cryptoList = newData;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return cryptoList.size();
    }

    @Override
    public HashMap<String, String> getItem(int position) {
        return cryptoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        HashMap<String, String> crypto = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_crypto, parent, false);
            holder = new ViewHolder();
            holder.cryptoName = convertView.findViewById(R.id.crypto_name);
            holder.cryptoPrice = convertView.findViewById(R.id.crypto_price);
            holder.favoriteButton = convertView.findViewById(R.id.favorite_button);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String name = crypto.get("name");
        String price = crypto.get("price");
        String priceColor = crypto.get("color");
        String market = crypto.get("market");
        boolean isFavorite = ((MainActivity)context).isFavorite(market);
        
        holder.cryptoName.setText(name);
        holder.cryptoPrice.setText(price);

        if ("red".equals(priceColor)) {
            holder.cryptoPrice.setTextColor(Color.RED);
        } else if ("blue".equals(priceColor)) {
            holder.cryptoPrice.setTextColor(Color.BLUE);
        } else {
            holder.cryptoPrice.setTextColor(Color.BLACK);
        }

        holder.favoriteButton.setVisibility(View.VISIBLE);
        holder.favoriteButton.setText(isFavorite ? "★" : "☆");
        holder.favoriteButton.setTextColor(isFavorite ? Color.YELLOW : Color.GRAY);
        holder.favoriteButton.setOnClickListener(v -> {
            ((MainActivity)context).toggleFavorite(market);
        });
        
        convertView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(market);
            }
        });

        return convertView;
    }
}

