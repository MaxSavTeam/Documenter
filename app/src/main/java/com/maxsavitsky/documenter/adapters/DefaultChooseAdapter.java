package com.maxsavitsky.documenter.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.datatypes.Type;

import java.util.ArrayList;

public class DefaultChooseAdapter extends RecyclerView.Adapter<DefaultChooseAdapter.VH>{
	private ArrayList<? extends Type> mElements;
	private LayoutInflater mLayoutInflater;
	private View.OnClickListener mOnClickListener;
	private Runnable mRunnable;

	public DefaultChooseAdapter(ArrayList<? extends Type> elements, @Nullable View.OnClickListener onClickListener, Context context) {
		mElements = elements;
		mLayoutInflater = LayoutInflater.from(context);
		mOnClickListener = onClickListener;
	}

	@NonNull
	@Override
	public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = mLayoutInflater.inflate( R.layout.check_box_list_item, parent, false);
		return new VH(view);
	}

	@Override
	public void onBindViewHolder(@NonNull VH holder, int position) {
		if(mRunnable != null)
			mRunnable.run();
		holder.mName.setText( mElements.get(position).getName() );
		holder.mId.setText( mElements.get(position).getId() );
	}

	@Override
	public int getItemCount() {
		return mElements.size();
	}

	public class VH extends RecyclerView.ViewHolder {
		public TextView mName, mId;
		public CheckBox mCheckBox;

		VH(@NonNull View itemView) {
			super(itemView);
			mId = itemView.findViewById(R.id.checkbox_item_hidden_id);
			mName = itemView.findViewById(R.id.lblNameInCheckbox );
			mCheckBox = itemView.findViewById( R.id.checkBoxInCheckboxItem );
			itemView.setOnClickListener(mOnClickListener);
		}
	}
}
