package com.maxsavitsky.documenter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
	private AdapterCallback mAdapterCallback;
	private boolean isSelectionMode = false;
	private ArrayList<Boolean> isItemSelected;

	private final ArrayList<ViewHolder> viewHolders = new ArrayList<>();

	public interface AdapterCallback {
		void onEntityClick(String id, Entity.Type type, int index);

		void onLongClick(int index);

		View getViewAt(int position);
	}

	public EntitiesAdapter(ArrayList<? extends Entity> entities, AdapterCallback callback) {
		mEntities = copy( entities );
		mAdapterCallback = callback;
		initHoldersList();
	}

	private void initHoldersList() {
		if ( viewHolders.size() < mEntities.size() ) {
			while ( viewHolders.size() != mEntities.size() ) {
				viewHolders.add( null );
			}
		} else {
			while ( viewHolders.size() != mEntities.size() ) {
				viewHolders.remove( viewHolders.size() - 1 );
			}
		}
	}

	public void setElements(ArrayList<? extends Entity> entities) {
		DiffResult diffResult = DiffUtil.calculateDiff( new DiffUtilCallback( mEntities, entities ) );
		mEntities = copy( entities );
		initHoldersList();
		diffResult.dispatchUpdatesTo( this );
	}

	public void setAdapterCallback(AdapterCallback adapterCallback) {
		mAdapterCallback = adapterCallback;
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

	public void showCheckBoxes() {
		isSelectionMode = true;
		isItemSelected = new ArrayList<>();
		for (int i = 0; i < mEntities.size(); i++)
			isItemSelected.add( false );
		for (int i = 0; i < getItemCount(); i++) {
			View view = mAdapterCallback.getViewAt( i );
			if(view == null)
				continue;
			ViewHolder holder = new ViewHolder( view );
			holder.checkBox.setVisibility( View.VISIBLE );
			holder.checkBox.setChecked( isItemSelected.get( i ) );
		}
	}

	public void hideCheckBoxes() {
		isSelectionMode = false;
		isItemSelected.clear();
		for (int i = 0; i < getItemCount(); i++) {
			View view = mAdapterCallback.getViewAt( i );
			if(view == null)
				continue;
			ViewHolder holder = new ViewHolder( view );
			holder.checkBox.setVisibility( View.GONE );
			holder.checkBox.setChecked( false );
		}
	}

	public void setCheckBox(int index, boolean state) {
		isItemSelected.set( index, state );
		View view = mAdapterCallback.getViewAt( index );
		if(view != null)
			new ViewHolder( view ).checkBox.setChecked( state );
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from( parent.getContext() ).inflate( R.layout.entity_layout, parent, false );
		return new ViewHolder( view );
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		viewHolders.set( position, holder );
		Entity entity = mEntities.get( position );
		holder.textView.setText( entity.getName() );
		if ( entity.getType() == Entity.Type.GROUP ) {
			holder.imageView.setImageResource( R.drawable.ic_folder );
		} else {
			holder.imageView.setImageResource( R.drawable.ic_document );
		}

		holder.itemView.setOnClickListener( v->{
			if ( mAdapterCallback != null ) {
				mAdapterCallback.onEntityClick( entity.getId(), entity.getType(), holder.getAdapterPosition() );
			}
		} );

		holder.itemView.setOnLongClickListener( v->{
			mAdapterCallback.onLongClick( holder.getAdapterPosition() );
			return true;
		} );

		if ( isSelectionMode ) {
			holder.checkBox.setVisibility( View.VISIBLE );
			holder.checkBox.setChecked( isItemSelected.get( position ) );
		} else {
			holder.checkBox.setVisibility( View.GONE );
			holder.checkBox.setChecked( false );
		}
	}

	@Override
	public int getItemCount() {
		return mEntities.size();
	}

	public static class ViewHolder extends RecyclerView.ViewHolder {
		private final TextView textView;
		private final ImageView imageView;
		private final CheckBox checkBox;

		public ViewHolder(@NonNull View itemView) {
			super( itemView );

			textView = itemView.findViewById( R.id.entity_layout_text );
			imageView = itemView.findViewById( R.id.entity_layout_icon );
			checkBox = itemView.findViewById( R.id.entity_layout_check_box );
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
