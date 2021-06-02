package com.maxsavitsky.documenter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.DiffResult;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.types.Entity;

import java.util.ArrayList;

public class EntitiesAdapter extends RecyclerView.Adapter<EntitiesAdapter.ViewHolder> {

	private ArrayList<? extends Entity> mEntities;
	private final AdapterCallback mAdapterCallback;

	public interface AdapterCallback {
		void onEntityClick(String id, Entity.Type type);
	}

	public EntitiesAdapter(ArrayList<? extends Entity> entities, AdapterCallback callback) {
		mEntities = copy( entities );
		mAdapterCallback = callback;
	}

	public void setElements(ArrayList<? extends Entity> entities) {
		DiffResult diffResult = DiffUtil.calculateDiff( new DiffUtilCallback( mEntities, entities ) );
		mEntities = copy( entities );
		diffResult.dispatchUpdatesTo( this );
	}

	private ArrayList<? extends Entity> copy(ArrayList<? extends Entity> entities) {
		ArrayList<Entity> res = new ArrayList<>();
		for (Entity e : entities) {
			res.add( new Entity( e.getId(), e.getName() ) {
				@Override
				public Type getType() {
					return e.getType();
				}
			} );
		}
		return res;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.entity_layout, parent, false );
		return new ViewHolder( view );
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Entity entity = mEntities.get( position );
		holder.textView.setText( entity.getName() );
		if ( entity.getType() == Entity.Type.GROUP ) {
			holder.imageView.setImageResource( R.drawable.ic_folder );
		} else {
			holder.imageView.setImageResource( R.drawable.ic_document );
		}

		holder.itemView.setOnClickListener( v->{
			if ( mAdapterCallback != null ) {
				mAdapterCallback.onEntityClick( entity.getId(), entity.getType() );
			}
		} );
	}

	@Override
	public int getItemCount() {
		return mEntities.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		private final TextView textView;
		private final ImageView imageView;

		public ViewHolder(@NonNull View itemView) {
			super( itemView );

			textView = itemView.findViewById( R.id.entity_layout_text );
			imageView = itemView.findViewById( R.id.entity_layout_icon );
		}
	}

	private static class DiffUtilCallback extends DiffUtil.Callback {

		private final ArrayList<? extends Entity> oldList, newList;

		public DiffUtilCallback(ArrayList<? extends Entity> oldList, ArrayList<? extends Entity> newList) {
			this.oldList = oldList;
			this.newList = newList;
		}

		@Override
		public int getOldListSize() {
			return oldList.size();
		}

		@Override
		public int getNewListSize() {
			return newList.size();
		}

		@Override
		public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			return oldList.get( oldItemPosition ).getId().equals( newList.get( newItemPosition ).getId() );
		}

		@Override
		public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			return oldList.get( oldItemPosition ).getName()
					.equals( newList.get( newItemPosition ).getName() ) &&
					oldList.get( oldItemPosition ).getType() == newList.get( newItemPosition ).getType();
		}
	}

}
