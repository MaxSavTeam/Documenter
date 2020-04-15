package com.maxsavitsky.documenter.adapters;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Type;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VHolder> {
	private ArrayList<? extends Type> mElements;
	private LayoutInflater mLayoutInflater;
	private View.OnClickListener mOnClickListener;

	public ListAdapter(Context context, ArrayList<? extends Type> data, View.OnClickListener onClickListener){
		mElements = data;
		mLayoutInflater = LayoutInflater.from( context );
		mOnClickListener = onClickListener;
	}

	@NonNull
	@Override
	public VHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = mLayoutInflater.inflate( R.layout.list_item, parent, false );
		return new VHolder( view );
	}

	@Override
	public void onBindViewHolder(@NonNull VHolder holder, int position) {
		holder.id.setText( mElements.get( position ).getId() );
		holder.name.setText( mElements.get( position ).getName() );
	}

	class VHolder extends RecyclerView.ViewHolder{
		final TextView id;
		final TextView name;

		public VHolder(@NonNull View itemView) {
			super( itemView );
			id = itemView.findViewById( R.id.lblHiddenTypeId );
			name = itemView.findViewById( R.id.lblTypeName );
			itemView.setOnClickListener( mOnClickListener );
		}
	}

	@Override
	public int getItemCount() {
		return mElements.size();
	}
}
