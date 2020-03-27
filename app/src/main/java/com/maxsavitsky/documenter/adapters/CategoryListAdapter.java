package com.maxsavitsky.documenter.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.types.Category;

import java.util.ArrayList;

public class CategoryListAdapter extends RecyclerView.Adapter<CategoryListAdapter.ViewHolder> {
	private final ArrayList<Category> mData;
	private final LayoutInflater mLayoutInflater;
	private final View.OnClickListener mOnClickListener;

	public CategoryListAdapter(Context context, ArrayList<Category> data, View.OnClickListener onClickListener){
		this.mData = data;
		this.mLayoutInflater = LayoutInflater.from(context);
		mOnClickListener = onClickListener;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = mLayoutInflater.inflate(R.layout.list_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		holder.name.setText(mData.get(position).getName());
		holder.id.setText(mData.get(position).getId());
	}

	@Override
	public int getItemCount() {
		return mData.size();
	}

	class ViewHolder extends RecyclerView.ViewHolder{
		final TextView name;
		final TextView id;

		ViewHolder(@NonNull View itemView) {
			super(itemView);
			name = itemView.findViewById(R.id.lblCategoryName);
			id = itemView.findViewById(R.id.lblHiddenCategoryId);
			itemView.setOnClickListener(mOnClickListener);
		}
	}
}
