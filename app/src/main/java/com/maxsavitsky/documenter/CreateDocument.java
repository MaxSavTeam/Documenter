package com.maxsavitsky.documenter;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.adapters.DefaultChooseAdapter;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import java.util.ArrayList;
import java.util.Date;

public class CreateDocument extends ThemeActivity {
	private String categoryId;
	private ArrayList<Entry> mEntriesToInclude = new ArrayList<>(  );

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle("Create Document");
			Utils.applyDefaultActionBarStyle( actionBar );
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
			setResult( Results.OK );
			finish();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_documents);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		applyTheme();
		categoryId = getIntent().getStringExtra( "parent_id");

		FloatingActionButton fab = findViewById(R.id.fabSaveDocument);
		fab.setOnClickListener(saveDocument);
		EditText editText = findViewById(R.id.editTextTextPersonName);
		Utils.showKeyboard( editText, this );
		setupRecyclerView();
	}

	private void setupRecyclerView(){
		final ArrayList<Entry> mEntries = MainData.getEntriesList();
		if(mEntries.isEmpty()){
			findViewById(R.id.layout_including).setVisibility(View.GONE);
		}else{
			RecyclerView rv = findViewById( R.id.recyclerViewChooseDocuments );
			LinearLayoutManager layoutManager = new LinearLayoutManager( this );
			layoutManager.setOrientation( RecyclerView.VERTICAL );
			rv.setLayoutManager( layoutManager );
			TextView t = findViewById( R.id.textViewTypeToBeIncluded );
			t.setText( R.string.entries_to_be_included_in_category );

			View.OnClickListener onItemClick = new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					CheckBox checkBox = v.findViewById( R.id.checkBoxInCheckboxItem );
					checkBox.setChecked( !checkBox.isChecked() );
					TextView t = v.findViewById( R.id.checkbox_item_hidden_id );
					String id = t.getText().toString();
					if(checkBox.isChecked()){
						mEntriesToInclude.add( MainData.getEntryWithId( id ) );
					}else{
						for(int i = 0; i < mEntriesToInclude.size(); i++){
							if(mEntriesToInclude.get( i ).getId().equals( id )){
								mEntriesToInclude.remove( i );
								break;
							}
						}
					}
				}
			};
			DefaultChooseAdapter adapter = new DefaultChooseAdapter( mEntries, onItemClick, this );
			rv.setAdapter( adapter );
			rv.setVisibility( View.VISIBLE );
		}
	}

	View.OnClickListener saveDocument = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			EditText editText = findViewById(R.id.editTextTextPersonName);
			String name = editText.getText().toString();
			if(!name.isEmpty()){
				String id = Utils.generateUniqueId() + "_doc";

				Document thisDocument = new Document(id, name);
				ArrayList<Document> documents;
				try {
					thisDocument.setAndSaveInfo( new Info( (int) new Date().getTime() ) );
					//documents = MainData.getCategoryWithId( categoryId ).getDocuments();
					documents = ParseSeparate.parseCategoryWithId( categoryId );
				}catch (Exception e){
					Toast.makeText( CreateDocument.this, "saveDocument", Toast.LENGTH_SHORT ).show();
					Utils.getErrorDialog( e, CreateDocument.this ).show();
					return;
				}
				ArrayList<Document> allDocuments = MainData.getDocumentsList();
				allDocuments.add(thisDocument);
				documents.add( thisDocument );
				try {
					Utils.createAllNecessaryForDocument( id );
					thisDocument.addCategoryToIncludedInXml( categoryId );
				}catch (Exception e){
					e.printStackTrace();
					Toast.makeText( CreateDocument.this, "saveDocument 1", Toast.LENGTH_LONG ).show();
					Utils.getErrorDialog( e, CreateDocument.this ).show();
					return;
				}
				MainData.setDocumentsList(allDocuments);
				Utils.saveDocumentsList(allDocuments);
				ArrayList<Category> categories = new ArrayList<>(  );
				try {
					Utils.saveCategoryDocuments( categoryId, documents );
					categories.add( MainData.getCategoryWithId( categoryId ) );
				}catch (Exception e){
					Toast.makeText( CreateDocument.this, "saveDocument1.5" + e.toString(), Toast.LENGTH_LONG ).show();
					Utils.getErrorDialog( e, CreateDocument.this ).show();
				}
				try {
					thisDocument.saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText( CreateDocument.this, "saveDocument2", Toast.LENGTH_LONG ).show();
					Utils.getErrorDialog( e, CreateDocument.this ).show();
				}

				try {
					for (Entry entry : mEntriesToInclude) {
						entry.addDocumentToIncluded( id );
					}
				}catch (Exception e){
					e.printStackTrace();
					Utils.getErrorDialog( e, CreateDocument.this ).show();
				}

				Utils.saveDocumentEntries( id, mEntriesToInclude );
				setResult( Results.NEED_TO_REFRESH );
				finish();
			}else{
				editText.requestFocus();
			}
		}
	};

}
