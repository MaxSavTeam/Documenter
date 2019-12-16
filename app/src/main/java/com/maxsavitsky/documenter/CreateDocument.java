package com.maxsavitsky.documenter;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.Utils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import java.util.ArrayList;

public class CreateDocument extends AppCompatActivity {

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle("Create Document");
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_32dp);
			actionBar.setHomeButtonEnabled(true);
			actionBar.setBackgroundDrawable(getDrawable(R.drawable.black));
		}
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
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

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(saveDocument);
		EditText editText = findViewById(R.id.editTextTextPersonName);
		editText.requestFocus();
	}

	View.OnClickListener saveDocument = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			EditText editText = findViewById(R.id.editTextTextPersonName);
			String name = editText.getText().toString();
			if(!name.isEmpty()){
				String id = Utils.generateUniqueId() + "_doc";

				Document thisDocument = new Document(id, name);

				ArrayList<Document> documents = MainData.getDocumentsList();
				documents.add(thisDocument);

				MainData.setDocumentsList(documents);
				Utils.saveDocumentsList(documents);

			}else{
				editText.requestFocus();
			}
		}
	};

}
