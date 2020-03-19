package com.maxsavitsky.documenter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.adapters.DefaultChooseAdapter;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class EntriesList extends ThemeActivity {
	private Document mDocument;
	private ArrayList<Entry> mEntries;
	private int mSortOrder = 1;
	private SharedPreferences sp;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(mDocument.getName());
			Utils.applyDefaultActionBarStyle(actionBar);
		}
	}

	private Comparator<Entry> mEntryComparator = new Comparator<Entry>() {
		@Override
		public int compare(Entry o1, Entry o2) {
			if(o1 == null || o2 == null)
				return 0;
			if(sp.getInt( "sort_entries", 0 ) == 0)
				return o1.getName().compareToIgnoreCase( o2.getName() ) * mSortOrder;
			else{
				int t1 = MainData.getEntryWithId( o1.getId() ).getInfo().getTimeStamp();
				int t2 = MainData.getEntryWithId( o2.getId() ).getInfo().getTimeStamp();
				int compareRes = Integer.compare( t1, t2 );
				return compareRes * mSortOrder;
			}
		}
	};

	private void setupRecyclerView(){
		try {
			mEntries = ParseSeparate.parseDocumentWithId( mDocument.getId() );
		}catch (Exception e){
			e.printStackTrace();
			Toast.makeText( this, "setupRecyclerView EntriesList", Toast.LENGTH_LONG ).show();
			Utils.getErrorDialog( e, this ).show();
			return;
		}
		RecyclerView recyclerView = findViewById( R.id.category_list_view );
		if(mEntries.isEmpty()){
			recyclerView.setVisibility(View.GONE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.VISIBLE);
		}else{
			if(mEntries.size() > 1)
				Collections.sort( mEntries, mEntryComparator );

			LinearLayoutManager linearLayoutManager = new LinearLayoutManager( this );
			linearLayoutManager.setOrientation( RecyclerView.VERTICAL );

			RVAdapter rvAdapter = new RVAdapter( mEntries );

			recyclerView.setLayoutManager( linearLayoutManager );
			recyclerView.setAdapter( rvAdapter );
			recyclerView.setVisibility(View.VISIBLE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.GONE);
		}
	}

	View.OnClickListener onItemClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent( EntriesList.this, ViewEntry.class );
			String id = ((TextView) v.findViewById( R.id.lblHiddenCategoryId )).getText().toString();
			intent.putExtra( "id",  id);
			startActivityForResult( intent, RequestCodes.VIEW_ENTRY );
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_entries_list );
		Toolbar toolbar = findViewById ( R.id.toolbar );
		setSupportActionBar ( toolbar );
		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
		final Intent intent = getIntent ();
		mDocument = MainData.getDocumentWithId( intent.getStringExtra ( "id" ) );
		try {
			mDocument.readProperties();
			invalidateOptionsMenu();
			mEntries = ParseSeparate.parseDocumentWithId( mDocument.getId() );
		} catch (Exception e) {
			e.printStackTrace();
			Utils.getErrorDialog( e, this ).show();
		}
		applyTheme();

		FloatingActionButton fab = findViewById( R.id.fabCreateEntry );
		fab.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent1 = new Intent( EntriesList.this, EntryEditor.class );
				intent1.putExtra( "id", mDocument.getId() );
				intent1.putExtra( "type", "create" );
				startActivityForResult( intent1, RequestCodes.CREATE_ENTRY );
			}
		} );

		setupRecyclerView();

	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch ( item.getItemId() ) {
			case android.R.id.home:
				finish();
				break;
			case R.id.item_invert_in_document:
				mSortOrder = -mSortOrder;
				setupRecyclerView();
				break;
			case R.id.item_choose_entry_sort_mode:
				AlertDialog.Builder chooserBuilder = new AlertDialog.Builder( this )
						.setTitle( R.string.choose_sort_mode )
						.setCancelable( false )
						.setSingleChoiceItems( R.array.sort_modes, sp.getInt( "sort_entries", 0 ), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								sp.edit().putInt( "sort_entries", which ).apply();
								setupRecyclerView();
								dialog.cancel();
							}
						} )
						.setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						} );
				chooserBuilder.create().show();
				break;
			case R.id.item_delete_document:
				AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
						.setMessage( R.string.delete_confirmation_text )
						.setTitle( R.string.confirmation )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								try {
									if ( MainData.finallyDeleteDocumentWithId( mDocument.getId() ) ) {
										setResult( ResultCodes.NEED_TO_REFRESH );
										finish();
									} else {
										Toast.makeText(EntriesList.this, "Failed", Toast.LENGTH_SHORT).show();
									}
								} catch (Exception e) {
									e.printStackTrace();
									Utils.getErrorDialog( e, EntriesList.this ).show();
								}
							}
						} ).setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						} ).setCancelable( false );
				deletionBuilder.create().show();
				break;
			case R.id.item_change_document_name:
				AlertDialog alertDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder( this ).setTitle( "Edit document name" ).setMessage( "Edit document name here" );
				final EditText editText = new EditText( this );
				editText.setText( mDocument.getName() );
				editText.append( "" );
				editText.requestFocus();
				editText.setTextColor( getResources().getColor( super.mEditTextColor ) );
				ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
				editText.setLayoutParams( layoutParams );
				builder.setView( editText ).setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = editText.getText().toString();
						if ( !text.isEmpty() && !text.equals( mDocument.getName() ) ) {
							ArrayList<Document> documents = MainData.getDocumentsList();
							documents.remove( mDocument );
							mDocument = new Document( mDocument.getId(), text );
							documents.add( mDocument );
							applyTheme();
							MainData.setDocumentsList( documents );
							Utils.saveDocumentsList( documents );
							setResult( ResultCodes.NEED_TO_REFRESH );
						}
					}
				} ).setNegativeButton( getResources().getString( R.string.cancel ), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				} ).setCancelable( false );
				alertDialog = builder.create();
				alertDialog.show();
				break;
			case R.id.item_edit_entries_list:
				try {
					prepareChooseLayout();
				}catch (Exception e){
					Utils.getErrorDialog( e, this ).show();
					e.printStackTrace();
					restartActivity();
				}
				break;
		}
		return super.onOptionsItemSelected( item );
	}

	private void restartActivity(){
		setResult( ResultCodes.RESTART_ACTIVITY, new Intent(  ).putExtra( "id", mDocument.getId() ) );
		finish();
	}

	private ArrayList<Entry> entriesToChange = new ArrayList<>(  );
	private Map<String, Entry> mDocumentEntriesMap = new HashMap<>(  );

	private void prepareChooseLayout() throws Exception{
		final ArrayList<Entry> entries = MainData.getEntriesList();
		if(!entries.isEmpty()){
			setContentView( R.layout.layout_choose_documents);

			ArrayList<Entry> documentEntries = ParseSeparate.parseDocumentWithId( mDocument.getId() );
			for(Entry entry : documentEntries){
				mDocumentEntriesMap.put( entry.getId(), entry );
			}
			entriesToChange = documentEntries;

			View.OnClickListener cancel = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					restartActivity();
				}
			};
			Button btn = findViewById( R.id.btnCancel );
			btn.setOnClickListener( cancel );

			View.OnClickListener apply = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					Utils.saveDocumentEntries( mDocument.getId(), entriesToChange );
					restartActivity();
				}
			};
			btn = findViewById( R.id.btnApply );
			btn.setOnClickListener( apply );

			View.OnClickListener onItemClick = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox checkBox = v.findViewById( R.id.checkBoxInCheckboxItem );
					checkBox.setChecked( !checkBox.isChecked() );
					String id = ((TextView) v.findViewById( R.id.checkbox_item_hidden_id )).getText().toString();
					if(checkBox.isChecked()){
						Entry entry = MainData.getEntryWithId( id );
						entriesToChange.add( entry );
					}else{
						for(int i = 0; i < entriesToChange.size(); i++){
							if(entriesToChange.get( i ).getId().equals( id )){
								entriesToChange.remove( i );
								break;
							}
						}
					}
				}
			};

			ChooseAdapter adapter = new ChooseAdapter( entries, onItemClick, this );
			RecyclerView recyclerView = findViewById( R.id.recyclerViewChangeList );
			LinearLayoutManager layoutManager = new LinearLayoutManager( this );
			layoutManager.setOrientation( RecyclerView.VERTICAL );
			recyclerView.setLayoutManager( layoutManager );

			recyclerView.setAdapter( adapter );
		}
	}

	class ChooseAdapter extends DefaultChooseAdapter{
		ArrayList<Entry> mElements;

		ChooseAdapter(ArrayList<Entry> elements, @Nullable View.OnClickListener onClickListener, Context context) {
			super( elements, onClickListener, context );
			mElements = elements;
		}

		@Override
		public void onBindViewHolder(@NonNull VH holder, int position) {
			holder.mCheckBox.setChecked( mDocumentEntriesMap.containsKey( mElements.get( position ).getId() ) );
			holder.mId.setText( mElements.get( position ).getId() );
			holder.mName.setText( mElements.get( position ).getName() );
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(resultCode == ResultCodes.NEED_TO_REFRESH)
			setupRecyclerView();
		if(requestCode == RequestCodes.VIEW_ENTRY || requestCode == RequestCodes.CREATE_ENTRY){
			if(resultCode == ResultCodes.REOPEN){
				setupRecyclerView();
				Intent intent = new Intent( this, ViewEntry.class );
				if(data != null)
					intent.putExtras( data );
				startActivityForResult( intent, RequestCodes.VIEW_ENTRY );
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.document_menu, menu );
		/*getMenuInflater().inflate( R.menu.common_menu, menu );

		MenuItem item = menu.findItem(R.id.item_common_remember_pos);
		item.setChecked( mDocument.getProperties().isSaveLastPos() );
		item.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				boolean isChecked = !item.isChecked();
				item.setChecked( isChecked );
				try{
					mDocument.applySaveLastPosState( isChecked );
				} catch (SAXException | IOException e) {
					e.printStackTrace();
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							Utils.getErrorDialog( e, EntriesList.this ).show();
						}
					} );
				}
				return true;
			}
		} );*/
		return super.onCreateOptionsMenu( menu );
	}

	class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder>{
		private ArrayList<Entry> mData;

		RVAdapter(ArrayList<Entry> data) {
			mData = data;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder( LayoutInflater.from( EntriesList.this ).inflate( R.layout.list_item, parent, false ) );
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			holder.id.setText( mData.get( position ).getId() );
			holder.name.setText( mData.get( position ).getName() );
		}

		@Override
		public int getItemCount() {
			return mData.size();
		}

		 class ViewHolder extends RecyclerView.ViewHolder {
			TextView id, name;

			ViewHolder(@NonNull View itemView) {
				super(itemView);
				id = itemView.findViewById( R.id.lblHiddenCategoryId );
				name = itemView.findViewById( R.id.lblCategoryName );
				itemView.setOnClickListener( onItemClick );
			}
		}
	}
}
