package com.maxsavitsky.documenter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.adapters.DefaultChooseAdapter;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.Info;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.ArrayList;
import java.util.Date;

public class CreateCategory extends ThemeActivity {

	private ArrayList<Document> documentsToIncludeInThisCategory = new ArrayList<>();

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if(actionBar != null){
			Utils.applyDefaultActionBarStyle( actionBar );
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
		setContentView(R.layout.activity_create_category);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = findViewById(R.id.fabSaveCategory);
		fab.setOnClickListener(saveCategory);
		applyTheme();

		ArrayList<Document> documents = MainData.getDocumentsList();
		RecyclerView rv = findViewById(R.id.recyclerViewChooseDocuments);
		TextView t = findViewById( R.id.textViewTypeToBeIncluded );
		t.setText( R.string.documents_to_be_included_in_category );
		if(!documents.isEmpty()) {
			LinearLayoutManager lay = new LinearLayoutManager(CreateCategory.this);
			lay.setOrientation(RecyclerView.VERTICAL);
			rv.setLayoutManager(lay);
			DefaultChooseAdapter adapter = new DefaultChooseAdapter(documents, itemClicked, this);
			rv.setAdapter(adapter);
		}else{
			findViewById( R.id.layout_including ).setVisibility(View.GONE);
		}
		EditText editText = findViewById(R.id.editTextTextPersonName);
		Utils.showKeyboard( editText, this );
	}

	View.OnClickListener saveCategory = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			EditText editText = findViewById(R.id.editTextTextPersonName);
			final String name = editText.getText().toString();
			if(!name.isEmpty() && name.trim().equals( "" )){
				Toast.makeText( CreateCategory.this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
				return;
			}
			if(Utils.isNameExist( name, "cat" )){
				AlertDialog.Builder builder1 = new AlertDialog.Builder( CreateCategory.this )
						.setTitle( R.string.warning )
						.setMessage( getResources().getString( R.string.this_name_already_exist ) + "\n" +
								getResources().getString( R.string.do_you_want_to_continue ))
						.setPositiveButton( R.string.yes, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								createCategory( name );
								dialog.dismiss();
							}
						} )
						.setNegativeButton( R.string.no, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
							}
						} );
				builder1.create().show();
			}else{
				createCategory( name );
			}
		}
	};

	private void createCategory(String name){
		EditText editText = findViewById(R.id.editTextTextPersonName);
		name = name.trim();
		if(!name.isEmpty()){
			ProgressDialog pd = new ProgressDialog(CreateCategory.this);
			pd.setCancelable(false);
			pd.setMessage("We are saving new category");
			pd.show();

			String uid = Utils.generateUniqueId() + "_cat";

			Category newCategory = new Category( uid, name );

			ArrayList<Category> categories = MainData.getCategoriesList();
			Info info = new Info();
			info.setTimeStamp( (int) new Date().getTime() );
			try {
				newCategory.setAndSaveInfo( info );
			} catch (Exception e) {
				e.printStackTrace();
				pd.dismiss();
				//Toast.makeText( CreateCategory.this, "Failed\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
				Utils.getErrorDialog( e, CreateCategory.this ).show();
				return;
			}
			categories.add(newCategory);

			MainData.setCategoriesList(categories);
			Utils.saveCategoriesList(categories);
			try {
				for(Document document : documentsToIncludeInThisCategory){
					document.addCategoryToIncludedInXml( uid );
				}
				Utils.saveCategoryDocuments( uid, documentsToIncludeInThisCategory );
			}catch (Exception e){
				Utils.getErrorDialog( e, CreateCategory.this ).show();
			}

			pd.dismiss();

			setResult( Results.NEED_TO_REFRESH );
			finish();
		}else{
			editText.requestFocus();
		}
	}

	View.OnClickListener itemClicked = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			CheckBox checkBox = v.findViewById(R.id.checkBoxInCheckboxItem );
			checkBox.setChecked(!checkBox.isChecked());
			TextView textViewId = v.findViewById(R.id.checkbox_item_hidden_id);
			String id = textViewId.getText().toString();
			if(checkBox.isChecked()){
				documentsToIncludeInThisCategory.add(MainData.getDocumentWithId(id));
			}else{
				for(int i = 0; i < documentsToIncludeInThisCategory.size(); i++){
					if(documentsToIncludeInThisCategory.get(i).getId().equals(id)){
						documentsToIncludeInThisCategory.remove(i);
						return;
					}
				}
			}
		}
	};
}
