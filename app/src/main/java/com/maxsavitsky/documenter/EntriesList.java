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
import android.widget.LinearLayout;
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
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.IOException;
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
	private boolean isFreeEntriesMode = false;

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			actionBar.setTitle( mDocument.getName() );
			Utils.applyDefaultActionBarStyle( actionBar );
		}
	}

	private void backPressed() {
		finish();
	}

	@Override
	public void onBackPressed() {
		backPressed();
	}

	private final Comparator<Entry> mEntryComparator = new Comparator<Entry>() {
		@Override
		public int compare(Entry o1, Entry o2) {
			if ( o1 == null || o2 == null ) {
				return 0;
			}
			if ( sp.getInt( "sort_entries", 0 ) == 0 ) {
				return o1.getName().compareToIgnoreCase( o2.getName() ) * mSortOrder;
			} else {
				int t1 = MainData.getEntryWithId( o1.getId() ).getInfo().getTimeStamp();
				int t2 = MainData.getEntryWithId( o2.getId() ).getInfo().getTimeStamp();
				int compareRes = Integer.compare( t1, t2 );
				return compareRes * mSortOrder;
			}
		}
	};

	private void setupRecyclerView() {
		try {
			mEntries = XMLParser.newInstance().parseDocumentWithId( mDocument.getId() );
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			Utils.getErrorDialog( e, this ).show();
			return;
		}
		RecyclerView recyclerView = findViewById( R.id.category_list_view );
		if ( mEntries.isEmpty() ) {
			recyclerView.setVisibility( View.GONE );
			TextView textView = findViewById( R.id.textViewNothingFound );
			textView.setVisibility( View.VISIBLE );
		} else {
			if ( mEntries.size() > 1 ) {
				Collections.sort( mEntries, mEntryComparator );
			}

			LinearLayoutManager linearLayoutManager = new LinearLayoutManager( this );
			linearLayoutManager.setOrientation( RecyclerView.VERTICAL );

			RVAdapter rvAdapter = new RVAdapter( mEntries );

			recyclerView.setLayoutManager( linearLayoutManager );
			recyclerView.setAdapter( rvAdapter );
			recyclerView.setVisibility( View.VISIBLE );
			TextView textView = findViewById( R.id.textViewNothingFound );
			textView.setVisibility( View.GONE );
		}
	}

	final View.OnClickListener onItemClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent( EntriesList.this, ViewEntry.class );
			String id = ( (TextView) v.findViewById( R.id.lblHiddenCategoryId ) ).getText().toString();
			intent.putExtra( "id", id );
			intent.putExtra( "free_mode", isFreeEntriesMode );
			startActivityForResult( intent, Requests.VIEW_ENTRY );
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_entries_list );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
		final Intent intent = getIntent();
		isFreeEntriesMode = intent.getBooleanExtra( "free_mode", false );
		FloatingActionButton floatingActionButton = findViewById( R.id.fabFreeEntries );
		if ( !isFreeEntriesMode ) {
			mDocument = MainData.getDocumentWithId( intent.getStringExtra( "id" ) );
			try {
				mDocument.readProperties();
				mEntries = XMLParser.newInstance().parseDocumentWithId( mDocument.getId() );
			} catch (Exception e) {
				e.printStackTrace();
				Utils.getErrorDialog( e, this ).show();
			}
		} else {
			mDocument = new Document( "free_entries", getString( R.string.free_entries ) );
			floatingActionButton.setVisibility( View.GONE );
		}
		invalidateOptionsMenu();
		applyTheme();

		FloatingActionButton fab = findViewById( R.id.fabCreateEntry );
		fab.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent1 = new Intent( EntriesList.this, EntryEditor.class );
				intent1.putExtra( "id", mDocument.getId() );
				intent1.putExtra( "type", "create" );
				intent1.putExtra( "without_doc", isFreeEntriesMode );
				startActivityForResult( intent1, Requests.CREATE_ENTRY );
			}
		} );
		floatingActionButton.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent( EntriesList.this, EntriesList.class );
				intent.putExtra( "free_mode", true );
				startActivityForResult( intent, Requests.FREE_ENTRIES );
			}
		} );
		findViewById( R.id.fabSettings ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent( EntriesList.this, SettingsActivity.class );
				startActivityForResult( intent, Requests.SETTINGS );
			}
		} );

		setupRecyclerView();

	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch ( item.getItemId() ) {
			case android.R.id.home:
				backPressed();
				//finish();
				break;
			case R.id.item_common_invert:
				mSortOrder = -mSortOrder;
				if ( mSortOrder == 1 ) {
					item.setIcon( R.drawable.ic_sort_ascending );
				} else {
					item.setIcon( R.drawable.ic_sort_descending );
				}
				setupRecyclerView();
				break;
			case R.id.item_common_sort_mode:
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
				final CheckBox checkBoxDelEntries = new CheckBox( this );
				LinearLayout layout = new LinearLayout( this );
				layout.setOrientation( LinearLayout.VERTICAL );
				layout.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );

				checkBoxDelEntries.setText( R.string.delete_document__with_entries );
				checkBoxDelEntries.setChecked( false );

				if ( super.isDarkTheme ) {
					checkBoxDelEntries.setTextColor( getColor( super.mTextColor ) );
				}

				layout.addView( checkBoxDelEntries );

				AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
						.setMessage( R.string.delete_confirmation_text )
						.setTitle( R.string.confirmation )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								try {
									if ( MainData.finallyDeleteDocumentWithId( mDocument.getId(), checkBoxDelEntries.isChecked() ) ) {
										setResult( Results.NEED_TO_REFRESH );
										finish();
									} else {
										Toast.makeText( EntriesList.this, "Failed", Toast.LENGTH_SHORT ).show();
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
						} ).setCancelable( false )
						.setView( layout );
				deletionBuilder.create().show();
				break;
			case R.id.item_common_change_name:
				AlertDialog alertDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder( this ).setTitle( R.string.edit_document_name );
				final EditText editText = new EditText( this );
				editText.setText( mDocument.getName() );
				editText.append( "" );
				editText.requestFocus();
				editText.setTextColor( getColor( super.mTextColor ) );
				ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
				editText.setLayoutParams( layoutParams );
				builder.setView( editText ).setPositiveButton( "OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = editText.getText().toString();
						text = text.trim();
						if ( !text.isEmpty() && !text.equals( mDocument.getName() ) ) {
							if ( Utils.isNameExist( text, "doc" ) ) {
								Toast.makeText( EntriesList.this, R.string.this_name_already_exist, Toast.LENGTH_SHORT ).show();
								return;
							}
							ArrayList<Document> documents = MainData.getDocumentsList();
							documents.remove( mDocument );
							mDocument = new Document( mDocument.getId(), text );
							documents.add( mDocument );
							applyTheme();
							MainData.setDocumentsList( documents );
							Utils.saveDocumentsList( documents );
							setResult( Results.NEED_TO_REFRESH );
						} else {
							Toast.makeText( EntriesList.this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
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
				prepareChooseLayout();
				break;
		}
		return super.onOptionsItemSelected( item );
	}

	private void restartActivity() {
		setResult( Results.RESTART_ACTIVITY, new Intent().putExtra( "id", mDocument.getId() ) );
		finish();
	}

	private ArrayList<Entry> entriesToChange = new ArrayList<>();
	private final Map<String, Entry> mDocumentEntriesMap = new HashMap<>();

	private void prepareChooseLayout() {
		final ArrayList<Entry> entries = MainData.getEntriesList();
		if ( !entries.isEmpty() ) {
			setContentView( R.layout.layout_choose_documents );

			entriesToChange = mDocument.getEntries();
			for (Entry entry : entriesToChange) {
				mDocumentEntriesMap.put( entry.getId(), entry );
			}

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
					ArrayList<Entry> documentEntries = mDocument.getEntries();
					for (Entry entry : documentEntries) {
						try {
							entry.removeDocumentFromIncluded( mDocument.getId() );
						} catch (IOException | SAXException e) {
							e.printStackTrace();
							Utils.getErrorDialog( e, EntriesList.this ).show();
						}
					}
					Utils.saveDocumentEntries( mDocument.getId(), entriesToChange );
					for (Entry entry : entriesToChange) {
						try {
							entry.addDocumentToIncluded( mDocument.getId() );
						} catch (IOException | SAXException e) {
							e.printStackTrace();
							Utils.getErrorDialog( e, EntriesList.this ).show();
						}
					}
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
					String id = ( (TextView) v.findViewById( R.id.checkbox_item_hidden_id ) ).getText().toString();
					if ( checkBox.isChecked() ) {
						Entry entry = MainData.getEntryWithId( id );
						entriesToChange.add( entry );
					} else {
						for (int i = 0; i < entriesToChange.size(); i++) {
							if ( entriesToChange.get( i ).getId().equals( id ) ) {
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

	private class ChooseAdapter extends DefaultChooseAdapter {
		final ArrayList<Entry> mElements;

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
		if ( resultCode == Results.NEED_TO_REFRESH ) {
			setupRecyclerView();
		}
		if ( requestCode == Requests.VIEW_ENTRY || requestCode == Requests.CREATE_ENTRY ) {
			if ( resultCode == Results.REOPEN ) {
				setupRecyclerView();
				Intent intent = new Intent( this, ViewEntry.class );
				if ( data != null ) {
					intent.putExtras( data );
				}
				startActivityForResult( intent, Requests.VIEW_ENTRY );
			}
		}
		if ( resultCode == Results.RESTART_APP ) {
			setResult( Results.RESTART_APP );
			finish();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if ( isFreeEntriesMode ) {
			getMenuInflater().inflate( R.menu.common_menu, menu );
			menu.setGroupVisible( 0, false );
			try {
				if(MainData.getFreeEntries().size() != 0) {
					menu.findItem( R.id.item_common_invert ).setVisible( true );
					menu.findItem( R.id.item_common_sort_mode ).setVisible( true );
				}
			} catch (IOException | SAXException e) {
				e.printStackTrace();
				Utils.getErrorDialog( e, this ).show();
			}
		} else {
			getMenuInflater().inflate( R.menu.common_menu, menu );
			getMenuInflater().inflate( R.menu.document_menu, menu );
			if(mDocument.getEntries().size() == 0){
				menu.findItem( R.id.item_common_invert ).setVisible( false );
				menu.findItem( R.id.item_common_sort_mode ).setVisible( false );
				menu.findItem( R.id.item_common_parameters ).setVisible( false );
			}
		}
		return super.onCreateOptionsMenu( menu );
	}

	class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> {
		private final ArrayList<Entry> mData;

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
			final TextView id;
			final TextView name;

			ViewHolder(@NonNull View itemView) {
				super( itemView );
				id = itemView.findViewById( R.id.lblHiddenCategoryId );
				name = itemView.findViewById( R.id.lblCategoryName );
				itemView.setOnClickListener( onItemClick );
			}
		}
	}
}
