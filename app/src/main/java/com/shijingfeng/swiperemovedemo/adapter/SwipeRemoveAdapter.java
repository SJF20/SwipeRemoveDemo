package com.shijingfeng.swiperemovedemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.shijingfeng.swiperemovedemo.R;
import com.shijingfeng.swiperemovedemo.bean.SwipeRemoveBean;

import java.util.List;

public class SwipeRemoveAdapter extends RecyclerView.Adapter<SwipeRemoveAdapter.SwipeRemoveViewHolder> {

    private Context mContext;
    private List<SwipeRemoveBean> mDataList;

    public SwipeRemoveAdapter(Context context, List<SwipeRemoveBean> dataList) {
        this.mContext = context;
        this.mDataList = dataList;
    }

    @NonNull
    @Override
    public SwipeRemoveViewHolder onCreateViewHolder(@NonNull ViewGroup container, int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.adapter_item_swipe_remove, container, false);
        return new SwipeRemoveViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SwipeRemoveViewHolder holder, int position) {
        final SwipeRemoveBean bean = mDataList.get(position);

        Glide.with(mContext).load(bean.imageUrl).into(holder.ivImg);
        holder.tvName.setText(bean.name);

        holder.llContent.setOnClickListener(view -> Toast.makeText(mContext, "llContent OnClick点击了！", Toast.LENGTH_SHORT).show());
        holder.llContent.setOnLongClickListener(view -> {
            Toast.makeText(mContext, "llContent OnLongClick点击了！", Toast.LENGTH_SHORT).show();
            return true;
        });

        holder.btnTop.setOnClickListener(view -> Toast.makeText(mContext, "置顶了！", Toast.LENGTH_SHORT).show());
        holder.btnUnread.setOnClickListener(view -> Toast.makeText(mContext, "标记未读了！", Toast.LENGTH_SHORT).show());
        holder.btnDelete.setOnClickListener(view -> Toast.makeText(mContext, "删除了！", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        if (mDataList == null) {
            return 0;
        }
        return mDataList.size();
    }

    static class SwipeRemoveViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout llContent;
        private ImageView ivImg;
        private TextView tvName;
        private Button btnTop;
        private Button btnUnread;
        private Button btnDelete;

        private SwipeRemoveViewHolder(@NonNull View itemView) {
            super(itemView);
            llContent = itemView.findViewById(R.id.ll_content);
            ivImg = itemView.findViewById(R.id.iv_img);
            tvName = itemView.findViewById(R.id.tv_name);
            btnTop = itemView.findViewById(R.id.btn_top);
            btnUnread = itemView.findViewById(R.id.btn_unread);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

    }

}
