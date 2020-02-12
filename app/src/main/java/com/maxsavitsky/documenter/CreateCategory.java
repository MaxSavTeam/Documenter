package com.maxsavitsky.documenter;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.adapters.DefaultChooseAdapter;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Info;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.ResultCodes;
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
		if(!documents.isEmpty()) {
			LinearLayoutManager lay = new LinearLayoutManager(CreateCategory.this);
			lay.setOrientation(RecyclerView.VERTICAL);
			rv.setLayoutManager(lay);
			DefaultChooseAdapter adapter = new DefaultChooseAdapter(documents, itemClicked, this);
			rv.setAdapter(adapter);
		}else{
			rv.setVisibility(View.GONE);
		}
		EditText editText = findViewById(R.id.editTextTextPersonName);
		Utils.showKeyboard( editText, this );
	}

	View.OnClickListener saveCategory = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			EditText editText = findViewById(R.id.editTextTextPersonName);
			String name = editText.getText().toString();
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

				setResult( ResultCodes.NEED_TO_REFRESH );
				finish();
			}else{
				editText.requestFocus();
			}
		}
	};

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
