package com.maxsavitsky.documenter;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.Info;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class CreateDocument extends AppCompatActivity {
	private String categoryId;

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
			setResult( ResultCodes.RESULT_CODE_OK );
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
		editText.requestFocus();

		// TODO: 19.12.2019 when add entries support, activate recycler view

		findViewById(R.id.recyclerViewChooseDocuments).setVisibility(View.GONE);
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
					Toast.makeText( CreateDocument.this, "saveDocument:\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
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
					Toast.makeText( CreateDocument.this, "saveDocument 1\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
					return;
				}
				MainData.setDocumentsList(allDocuments);
				Utils.saveDocumentsList(allDocuments);
				ArrayList<Category> categories = new ArrayList<>(  );
				try {
					Utils.saveCategoryDocuments( categoryId, documents );
					categories.add( MainData.getCategoryWithId( categoryId ) );
				}catch (Exception e){
					Toast.makeText( CreateDocument.this, "saveDocument: \n\n" + e.toString(), Toast.LENGTH_LONG ).show();
				}
				try {
					thisDocument.saveInWhichCategoriesDocumentWithIdIncludedIn( categories );
				} catch (IOException e) {
					e.printStackTrace();
					Toast.makeText( CreateDocument.this, e.toString() + "\nsaveDocument", Toast.LENGTH_LONG ).show();
				}
				// TODO: 19.12.2019 change to store included entries
				Utils.saveDocumentEntries( id, new ArrayList<Entry>() );
				setResult( ResultCodes.NEED_TO_REFRESH );
				finish();
			}else{
				editText.requestFocus();
			}
		}
	};

}
