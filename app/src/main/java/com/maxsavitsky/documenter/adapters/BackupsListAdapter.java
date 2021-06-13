package com.maxsavitsky.documenter.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ui.CloudBackupsListActivity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class BackupsListAdapter extends RecyclerView.Adapter<BackupsListAdapter.ViewHolder> {

	private ArrayList<CloudBackupsListActivity.BackupEntity> mBackupEntities;
	private AdapterCallback callback;

	public BackupsListAdapter(ArrayList<CloudBackupsListActivity.BackupEntity> backupEntities, AdapterCallback callback) {
		mBackupEntities = backupEntities;
		this.callback = callback;
	}

	public interface AdapterCallback {
		void onBackupClick(CloudBackupsListActivity.BackupEntity backupEntity);
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from( parent.getContext() );
		return new ViewHolder( inflater.inflate( R.layout.backup_entity, parent, false ) );
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		CloudBackupsListActivity.BackupEntity entity = mBackupEntities.get( position );

		DateFormat format = DateFormat.getDateTimeInstance();
		holder.time.setText( format.format( new Date( entity.time ) ) );

		if(entity.description == null){
			holder.description.setVisibility( View.GONE );
		}else{
			holder.description.setVisibility( View.VISIBLE );
			String d = String.format( "%s: %s", holder.itemView.getContext().getString( R.string.description ), entity.description );
			holder.description.setText( d );
		}

		holder.itemView.setOnClickListener( v->callback.onBackupClick( entity ) );
	}

	@Override
	public int getItemCount() {
		return mBackupEntities.size();
	}

	protected static class ViewHolder extends RecyclerView.ViewHolder {
		public final TextView time, description;

		public ViewHolder(@NonNull View itemView) {
			super( itemView );

			time = itemView.findViewById( R.id.backup_entity_text_view_time );
			description = itemView.findViewById( R.id.backup_entity_description );
		}
	}

}
