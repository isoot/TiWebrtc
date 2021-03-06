package com.hhtc.dialer.main.recent;

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.hhtc.dialer.R;
import com.hhtc.dialer.adapter.FavoriteItemDecoration;
import com.hhtc.dialer.data.bean.CollectFavorite;
import com.hhtc.dialer.data.bean.RecentCallLog;
import com.hhtc.dialer.main.holder.CollectsViewHolder;
import com.hhtc.dialer.main.holder.EmptyHolder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int LOADING_TYPE = 0;
    public static final int EMPTY_TYPE = 1;
    public static final int NORMAL_TYPE = 2;

    public static final int HEAD_DATA = 3;
    public static final int NORMAL_DATA = 4;

    private int type = LOADING_TYPE;

    @IntDef({HEAD_DATA,NORMAL_DATA})
    @Retention(RetentionPolicy.SOURCE)
    public @interface HeadOrNormal {
    }

    private Context context;

    private List<RecentModel> models = new ArrayList<>();

    private RecyclerView collect_view;

    public RecentAdapter(Context context) {
        this.context = context;
        models.add(null);
    }

    @Override
    public int getItemViewType(int position) {
        if (type == NORMAL_TYPE) {
            return models.get(position).getType();
        } else {
            return type;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        RecyclerView.ViewHolder holder = null;
        switch (viewType) {
            case LOADING_TYPE:
                holder = new EmptyHolder(inflater.inflate(R.layout.data_loading_layout, viewGroup, false));
                break;
            case EMPTY_TYPE:
                holder = new EmptyHolder(inflater.inflate(R.layout.recent_data_empty_layout, viewGroup, false));
                break;
            case HEAD_DATA: //下面有两种
                holder = new HeadViewHolder(inflater.inflate(R.layout.recent_data_head_layout, viewGroup, false));
                break;
            case NORMAL_DATA: //下面有两种
                collect_view.setBackgroundColor(viewGroup.getContext().getColor(R.color.dialer_recent_recycler_view_background));
                holder = new ItemViewHolder(inflater.inflate(R.layout.recent_data_itme_layout, viewGroup, false));
                break;
            default:
                throw new RuntimeException("recent adapter view type unaware!!");
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof HeadViewHolder) {
            ((HeadViewHolder) viewHolder).bindData(models.get(position));
        } else if (viewHolder instanceof ItemViewHolder) {
            ((ItemViewHolder) viewHolder).bindData(models.get(position));
        }
    }


    public void setModels(List<RecentModel> model) {
        models.clear();
        if (Objects.isNull(model)) {
            type = EMPTY_TYPE;
            models.add(null);
        } else {
            type = NORMAL_TYPE;
            this.models.addAll(model);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public void bindRecycler(RecyclerView collect_view) {
        this.collect_view = collect_view;
        collect_view.addItemDecoration(new FavoriteItemDecoration(1, 20, true));
        collect_view.setAdapter(this);
    }

}
