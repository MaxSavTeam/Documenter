package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class EntriesList extends AppCompatActivity {
	private Document mDocument;
	private ArrayList<Entry> mEntries;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(mDocument.getName());
			Utils.applyDefaultActionBarStyle(actionBar);
		}
	}

	private void setupRecyclerView(){
		try {
			mEntries = ParseSeparate.parseDocumentWithId( mDocument.getId() );
		}catch (Exception e){
			e.printStackTrace();
			Toast.makeText( this, "setupRecyclerView EntriesList\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
			return;
		}
		RecyclerView recyclerView = findViewById( R.id.category_list_view );
		if(mEntries.isEmpty()){
			recyclerView.setVisibility(View.GONE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.VISIBLE);
		}else{
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

		Intent intent = getIntent ();
		mDocument = MainData.getDocumentWithId( intent.getStringExtra ( "id" ) );
		try {
			mEntries = ParseSeparate.parseDocumentWithId( mDocument.getId() );
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText( this, e.toString() + "\nonCreate 85", Toast.LENGTH_LONG ).show();
		}
		applyTheme();

		FloatingActionButton fab = findViewById( R.id.fabCreateEntry );
		fab.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent1 = new Intent( EntriesList.this, CreateEntry.class );
				intent1.putExtra( "id", mDocument.getId() );
				startActivityForResult( intent1, RequestCodes.CREATE_ENTRY );
			}
		} );

		setupRecyclerView();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch ( item.getItemId() ) {
			case android.R.id.home:
				setResult( ResultCodes.OK );
				finish();
				break;
			case R.id.item_delete_document:
				try {
					if(MainData.finallyDeleteDocumentWithId( mDocument.getId() )){
						setResult( ResultCodes.NEED_TO_REFRESH );
						finish();
					}else{
						Toast.makeText( this, "Failed", Toast.LENGTH_SHORT ).show();
					}
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();
				}
				break;
			case R.id.item_change_document_name:
				AlertDialog alertDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder( this ).setTitle( "Edit document name" ).setMessage( "Edit document name here" );
				final EditText editText = new EditText( this );
				editText.setText( mDocument.getName() );
				editText.append( "" );
				editText.requestFocus();
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
				} );
				alertDialog = builder.create();
				alertDialog.show();
				break;
			case R.id.item_edit_entries_list:

				break;
		}
		return super.onOptionsItemSelected( item );
	}

	private void prepareChooseLayout(){

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(resultCode == ResultCodes.NEED_TO_REFRESH)
			setupRecyclerView();
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.document_menu, menu );
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
