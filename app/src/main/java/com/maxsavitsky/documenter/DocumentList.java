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
import android.widget.CompoundButton;
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
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
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

public class DocumentList extends ThemeActivity {
	private ArrayList<Document> mDocuments;
	private Category mCategory;
	private SharedPreferences sp;
	private final Map<String, Document> mDocumentMap = new HashMap<>();
	private Map<String, Document> documentsToInclude = new HashMap<>();
	private final ArrayList<String> documentsWhichWillBeExcluded = new ArrayList<>();
	private int mSortOrder = 1; // 1 - по возрастанию; -1 -

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			actionBar.setTitle( mCategory.getName() );
			Utils.applyDefaultActionBarStyle( actionBar );
		}
	}

	private void backPressed() {
		finish();
	}

	final Comparator<Document> mDocumentComparator = new Comparator<Document>() {
		@Override
		public int compare(Document o1, Document o2) {
			if ( sp.getInt( "sort_documents", 0 ) == 0 ) {
				return o1.getName().compareToIgnoreCase( o2.getName() ) * mSortOrder;
			} else {
				int t1 = MainData.getDocumentWithId( o1.getId() ).getInfo().getTimeStamp();
				int t2 = MainData.getDocumentWithId( o2.getId() ).getInfo().getTimeStamp();
				int compared = Integer.compare( t1, t2 );
				return compared * mSortOrder;
			}

		}
	};

	private void setupRecyclerView() {
		mDocuments = mCategory.getDocuments();
		RecyclerView recyclerView = findViewById( R.id.category_list_view );
		if ( mDocuments.isEmpty() ) {
			recyclerView.setVisibility( View.GONE );
			TextView textView = findViewById( R.id.textViewNothingFound );
			textView.setVisibility( View.VISIBLE );

		} else {
			LinearLayoutManager lay = new LinearLayoutManager( this );
			lay.setOrientation( RecyclerView.VERTICAL );
			if ( mDocuments.size() > 1 ) {
				Collections.sort( mDocuments, mDocumentComparator );
			}
			DocumentsAdapter mAdapter = new DocumentsAdapter( mDocuments, mOnClickListener );
			recyclerView.setLayoutManager( lay );
			recyclerView.setAdapter( mAdapter );
			recyclerView.setVisibility( View.VISIBLE );
			TextView textView = findViewById( R.id.textViewNothingFound );
			textView.setVisibility( View.GONE );
		}
	}

	private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent( DocumentList.this, EntriesList.class );
			TextView t = v.findViewById( R.id.lblHiddenCategoryId );
			String id = t.getText().toString();
			intent.putExtra( "id", id );
			startActivityForResult( intent, Requests.ENTRIES_LIST );
		}
	};

	@Override
	public void onBackPressed() {
		backPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if ( itemId == R.id.item_edit_documents_list ) {
			prepareChooseRecyclerView();
		} else if ( itemId == android.R.id.home ) {
			backPressed();
		} else if ( itemId == R.id.item_delete ) {
			final CheckBox checkBoxDelDocs = new CheckBox( this );
			final CheckBox checkBoxDelEntries = new CheckBox( this );
			LinearLayout layout = new LinearLayout( this );
			layout.setOrientation( LinearLayout.VERTICAL );
			layout.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );

			checkBoxDelDocs.setText( R.string.delete_category__with_docs );
			checkBoxDelDocs.setChecked( false );
			checkBoxDelDocs.setOnCheckedChangeListener( new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					checkBoxDelEntries.setEnabled( isChecked );
					if ( !isChecked ) {
						checkBoxDelEntries.setChecked( false );
					}
				}
			} );
			checkBoxDelEntries.setText( R.string.delete_category__with_entries );
			checkBoxDelEntries.setChecked( false );
			checkBoxDelEntries.setEnabled( false );

			if ( super.isDarkTheme ) {
				checkBoxDelDocs.setTextColor( getColor( super.mTextColor ) );
				checkBoxDelEntries.setTextColor( getColor( super.mTextColor ) );
			}

			layout.addView( checkBoxDelDocs );
			layout.addView( checkBoxDelEntries );

			AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
					.setMessage( R.string.delete_confirmation_text )
					.setTitle( R.string.confirmation )
					.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							try {
								if ( checkBoxDelDocs.isChecked() ) {
									for (Document document : mCategory.getDocuments()) {
										if ( document.getCategoriesInWhichIncludedDocument().size() == 1 ) {
											MainData.finallyDeleteDocumentWithId( document.getId(), checkBoxDelEntries.isChecked() );
										}
									}
								}
								if ( MainData.finallyDeleteCategoryWithId( mCategory.getId() ) ) {
									setResult( Results.NEED_TO_REFRESH );
									finish();
								} else {
									Toast.makeText( DocumentList.this, "Failed", Toast.LENGTH_SHORT ).show();
								}
							} catch (Exception e) {
								e.printStackTrace();
								Utils.getErrorDialog( e, DocumentList.this ).show();
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
		} else if ( itemId == R.id.item_common_change_name ) {
			AlertDialog alertDialog;
			AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
					.setTitle( R.string.edit_category_name );
			//View view = new View(this);
			final EditText editText = new EditText( this );
			editText.setText( mCategory.getName() );
			editText.append( "" );
			editText.setTextColor( getColor( super.mTextColor ) );
			editText.requestFocus();
			ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
			editText.setLayoutParams( layoutParams );
			builder.setView( editText ).setPositiveButton( "OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String text = editText.getText().toString();
					text = text.trim();
					if ( !text.isEmpty() && !text.equals( mCategory.getName() ) ) {
						if ( Utils.isNameExist( text, "cat" ) ) {
							Toast.makeText( DocumentList.this, R.string.this_name_already_exist, Toast.LENGTH_SHORT ).show();
							return;
						}
						MainData.removeCategoryWithId( mCategory.getId() );
						ArrayList<Category> categories = MainData.getCategoriesList();
						mCategory = new Category( mCategory.getId(), text );
						applyTheme();
						categories.add( mCategory );
						MainData.setCategoriesList( categories );
						Utils.saveCategoriesList( categories );
						setResult( Results.NEED_TO_REFRESH );
					} else {
						Toast.makeText( DocumentList.this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
					}
					dialog.cancel();
				}
			} ).setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			} ).setCancelable( false );
			alertDialog = builder.create();
			alertDialog.show();
		} else if ( itemId == R.id.item_common_sort_mode ) {
			AlertDialog.Builder builder;
			AlertDialog chooseSortType;
			String[] items = getResources().getStringArray( R.array.sort_modes );
			int pos = sp.getInt( "sort_documents", 0 );
			builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
					.setTitle( R.string.choose_sort_mode )
					.setSingleChoiceItems( items, pos, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							sp.edit().putInt( "sort_documents", which ).apply();
							setupRecyclerView();
							dialog.cancel();
						}
					} )
					.setCancelable( false )
					.setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					} );
			chooseSortType = builder.create();
			chooseSortType.show();
		} else if ( itemId == R.id.item_common_invert ) {
			mSortOrder = -mSortOrder;

			if ( mSortOrder == 1 ) {
				item.setIcon( getDrawable(R.drawable.ic_sort_ascending) );
			} else {
				item.setIcon( getDrawable(R.drawable.ic_sort_descending) );
			}
			setupRecyclerView();
		}
		return super.onOptionsItemSelected( item );
	}

	private void restartActivity() {
		setResult( Results.RESTART_ACTIVITY, new Intent().putExtra( "id", mCategory.getId() ) );
		this.finish();
	}

	private ArrayList<Document> documentsToChange;

	private void prepareChooseRecyclerView() {
		if ( MainData.getEntriesList().size() != 0 ) {
			documentsToChange = mCategory.getDocuments();
			mDocumentMap.clear();
			for (Document document : documentsToChange) {
				mDocumentMap.put( document.getId(), document );
			}

			setContentView( R.layout.layout_choose_documents );

			View.OnClickListener cancel = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					restartActivity();
				}
			};
			Button btnCancel = findViewById( R.id.btnCancel );
			btnCancel.setOnClickListener( cancel );

			View.OnClickListener apply = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					ArrayList<Document> oldDocuments = mCategory.getDocuments();
					for (Document document : oldDocuments) {
						try {
							document.removeCategoryFromIncludedXml( mCategory.getId() );
						} catch (IOException | SAXException e) {
							e.printStackTrace();
							Utils.getErrorDialog( e, DocumentList.this ).show();
						}
					}
					Utils.saveCategoryDocuments( mCategory.getId(), documentsToChange );
					for (Document document : documentsToChange) {
						try {
							document.addCategoryToIncludedInXml( mCategory.getId() );
						} catch (IOException | SAXException e) {
							e.printStackTrace();
							Utils.getErrorDialog( e, DocumentList.this ).show();
						}
					}

					restartActivity();
					//Toast.makeText( DocumentList.this, documentsToInclude.toString(), Toast.LENGTH_LONG ).show();
				}
			};
			Button btnApply = findViewById( R.id.btnApply );
			btnApply.setOnClickListener( apply );

			RecyclerView recyclerView = findViewById( R.id.recyclerViewChangeList );
			LinearLayoutManager layoutManager = new LinearLayoutManager( this );
			layoutManager.setOrientation( RecyclerView.VERTICAL );
			recyclerView.setLayoutManager( layoutManager );

			View.OnClickListener adapterOnClickListener = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox checkBox = v.findViewById( R.id.checkBoxInCheckboxItem );
					checkBox.setChecked( !checkBox.isChecked() );
					String id = ( (TextView) v.findViewById( R.id.checkbox_item_hidden_id ) ).getText().toString();
					if ( checkBox.isChecked() ) {
						documentsToChange.add( MainData.getDocumentWithId( id ) );
					} else {
						for (int i = 0; i < documentsToChange.size(); i++) {
							if ( documentsToChange.get( i ).getId().equals( id ) ) {
								documentsToChange.remove( i );
								break;
							}
						}
					}

				}
			};
			ChangeListAdapter2 changeListAdapter = new ChangeListAdapter2(
					MainData.getDocumentsList(),
					adapterOnClickListener, this, null );

			recyclerView.setAdapter( changeListAdapter );
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.common_menu, menu );
		getMenuInflater().inflate( R.menu.category_menu, menu );
		MenuItem m = menu.findItem( R.id.item_common_remember_pos );
		m.setChecked( mCategory.getProperties().isSaveLastPos() );
		m.setOnMenuItemClickListener( new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				boolean isChecked = !item.isChecked();
				item.setChecked( isChecked );
				try {
					mCategory.applySaveLastPos( isChecked );
				} catch (final IOException | SAXException e) {
					e.printStackTrace();
					runOnUiThread( new Runnable() {
						@Override
						public void run() {
							Utils.getErrorDialog( e, DocumentList.this ).show();
						}
					} );
				}
				return true;
			}
		} );
		if ( mCategory.getDocuments().size() == 0 ) {
			menu.findItem( R.id.item_common_invert ).setVisible( false );
			menu.findItem( R.id.item_common_sort_mode ).setVisible( false );
			menu.findItem( R.id.item_common_parameters ).setVisible( false );
		}
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_document_list2 );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
		invalidateOptionsMenu();

		Intent intent = getIntent();
		String id = intent.getStringExtra( "category_uid" );
		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
		try {
			mCategory = MainData.getCategoryWithId( id );
			mDocuments = XMLParser.newInstance().parseCategoryWithId( id );
		} catch (Exception e) {
			e.printStackTrace();
			Utils.getErrorDialog( e, this ).show();
			return;
		}

		applyTheme();

		FloatingActionButton fab = findViewById( R.id.fabCreateDocument );
		fab.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivityForResult( new Intent( DocumentList.this, CreateDocument.class ).putExtra( "parent_id", mCategory.getId() ), Requests.CREATE_DOCUMENT );
			}
		} );
		findViewById( R.id.fabFreeEntries ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent( DocumentList.this, EntriesList.class );
				intent.putExtra( "free_mode", true );
				startActivityForResult( intent, Requests.FREE_ENTRIES );
			}
		} );
		findViewById( R.id.fabSettings ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent( DocumentList.this, SettingsActivity.class );
				startActivityForResult( intent, Requests.SETTINGS );
			}
		} );

		try {
			mCategory.readProperties();
		} catch (IOException | SAXException e) {
			e.printStackTrace();
			Utils.getErrorDialog( e, this ).show();
		}
	}

	@Override
	protected void onPostCreate(@Nullable Bundle savedInstanceState) {
		super.onPostCreate( savedInstanceState );
		setupRecyclerView();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if ( resultCode == Results.NEED_TO_REFRESH ) {
			setupRecyclerView();
		}
		if ( requestCode == Requests.ENTRIES_LIST ) {
			if ( resultCode == Results.RESTART_ACTIVITY ) {
				Intent intent = new Intent( this, EntriesList.class );
				//intent.putExtra( "id", data.getStringExtra( "id" ) );
				if ( data != null ) {
					intent.putExtras( data );
				}
				startActivityForResult( intent, Requests.ENTRIES_LIST );
			}
		}
		if ( resultCode == Results.RESTART_APP ) {
			setResult( Results.RESTART_APP );
			finish();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.VH> {
		private final ArrayList<Document> mData;
		private final View.OnClickListener onClickListener;

		DocumentsAdapter(ArrayList<Document> data, View.OnClickListener onClickListener) {
			mData = data;
			if ( mData.size() > 1 ) {
				Collections.sort( mData, mDocumentComparator );
			}
			this.onClickListener = onClickListener;
		}

		@NonNull
		@Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new VH( LayoutInflater.from( DocumentList.this ).inflate( R.layout.list_item, parent, false ) );
		}

		@Override
		public void onBindViewHolder(@NonNull VH holder, int position) {
			holder.name.setText( mData.get( position ).getName() );
			holder.id.setText( mData.get( position ).getId() );
		}

		@Override
		public int getItemCount() {
			return mData.size();
		}

		class VH extends RecyclerView.ViewHolder {
			final TextView name;
			final TextView id;

			VH(@NonNull View itemView) {
				super( itemView );
				name = itemView.findViewById( R.id.lblCategoryName );
				id = itemView.findViewById( R.id.lblHiddenCategoryId );
				itemView.setOnClickListener( DocumentsAdapter.this.onClickListener );
			}
		}
	}

	class ChangeListAdapter2 extends DefaultChooseAdapter {
		final ArrayList<Document> mDocuments;

		public ChangeListAdapter2(ArrayList<Document> elements, @Nullable View.OnClickListener onClickListener, Context context, @Nullable Runnable onBindRun) {
			super( elements, onClickListener, context );
			mDocuments = elements;
		}

		@Override
		public void onBindViewHolder(@NonNull DefaultChooseAdapter.VH holder, int position) {
			if ( mDocumentMap.containsKey( mDocuments.get( position ).getId() ) ) {
				holder.mCheckBox.setChecked( true );
			} else {
				holder.mCheckBox.setChecked( false );
			}
			holder.mName.setText( mDocuments.get( position ).getName() );
			holder.mId.setText( mDocuments.get( position ).getId() );
		}
	}
}
