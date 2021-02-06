package in.dev.android.demoauth.adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.dev.android.demoauth.databinding.RowUserBinding;
import in.dev.android.demoauth.model.User;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.MyViewHolder> {

    Activity activity;
    List<User> userList = new ArrayList<>();
    List<String> userKey = new ArrayList<>();

    public UserAdapter(Activity activity, List<User> userList, List<String> userKey) {
        this.activity = activity;
        this.userList = userList;
        this.userKey = userKey;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowUserBinding userBinding = RowUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new MyViewHolder(userBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.tvEmail.setText(userList.get(position).getEmail());
        holder.tvName.setText(userKey.get(position));
    }

    @Override
    public int getItemCount() {
        if (userList != null)
            return userList.size();
        return 0;
    }

    public void removeItem(int position) {
        userList.remove(position);
        userKey.remove(position);
        notifyItemRemoved(position);
        notifyDataSetChanged();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public RelativeLayout viewForeground;
        public TextView tvEmail;
        public TextView tvName;

        public MyViewHolder(@NonNull RowUserBinding binding) {
            super(binding.getRoot());

            viewForeground = binding.viewForeground;
            tvEmail = binding.tvEmail;
            tvName = binding.tvName;
        }
    }
}
