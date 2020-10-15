package com.maxsavitsky.documenter.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.types.Type;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VHolder> {
	private final ArrayList<? extends Type> mElements;
	private final LayoutInflater mLayoutInflater;
	private final AdapterCallback mCallback;

	public interface AdapterCallback{
		void onClick(Type type);
	}

	public ListAdapter(Context context, ArrayList<? extends Type> data, AdapterCallback adapterCallback){
		mElements = data;
		mLayoutInflater = LayoutInflater.from( context );
		mCallback = adapterCallback;
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

		holder.itemView.setOnClickListener( v->mCallback.onClick( mElements.get( position ) ) );
	}

	static class VHolder extends RecyclerView.ViewHolder{
		final TextView id;
		final TextView name;

		public VHolder(@NonNull View itemView) {
			super( itemView );
			id = itemView.findViewById( R.id.lblHiddenTypeId );
			name = itemView.findViewById( R.id.lblTypeName );
		}
	}

	@Override
	public int getItemCount() {
		return mElements.size();
	}
}
