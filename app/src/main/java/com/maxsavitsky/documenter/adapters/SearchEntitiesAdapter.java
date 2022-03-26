package com.maxsavitsky.documenter.adapters;

import android.view.View;

import androidx.annotation.NonNull;

import com.maxsavitsky.documenter.data.types.Entity;

import java.util.ArrayList;

public class SearchEntitiesAdapter extends EntitiesAdapter {

	public SearchEntitiesAdapter(AdapterCallback callback) {
		super( new ArrayList<>(), callback );
	}

	private final ArrayList<String> paths = new ArrayList<>();

	public void add(Entity entity, String path){
		mEntities.add( entity );
		paths.add( path );
		notifyItemInserted( mEntities.size() - 1 );
	}

	public void clear(){
		notifyItemRangeRemoved( 0, mEntities.size() );
		mEntities.clear();
		paths.clear();
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		super.onBindViewHolder( holder, position );

		String path = paths.get( position );
		holder.getSubtext().setVisibility( path == null || path.isEmpty() ? View.GONE : View.VISIBLE );
		holder.getSubtext().setText( path );
	}
}
