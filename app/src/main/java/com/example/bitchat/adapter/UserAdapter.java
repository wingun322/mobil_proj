package com.example.bitchat.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.bitchat.R;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<String> users;

    public UserAdapter(List<String> users) {
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        String username = users.get(position);
        holder.username.setText(username);
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public void updateUsers(List<String> newUsers) {
        users.clear();
        users.addAll(newUsers);
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView username;

        UserViewHolder(View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.username);
        }
    }
} 