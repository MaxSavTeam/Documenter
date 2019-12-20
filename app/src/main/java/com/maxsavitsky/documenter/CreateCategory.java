package com.maxsavitsky.documenter;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Info;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class CreateCategory extends AppCompatActivity {

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

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(saveCategory);
		applyTheme();

		ArrayList<Document> documents = MainData.getDocumentsList();
		RecyclerView rv = findViewById(R.id.recyclerViewChooseDocuments);
		if(!documents.isEmpty()) {
			LinearLayoutManager lay = new LinearLayoutManager(CreateCategory.this);
			lay.setOrientation(RecyclerView.VERTICAL);
			rv.setLayoutManager(lay);
			Adapter adapter = new Adapter(documents, itemClicked);
			rv.setAdapter(adapter);
		}else{
			rv.setVisibility(View.GONE);
		}
		EditText editText = findViewById(R.id.editTextTextPersonName);
		editText.requestFocus();
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
					Toast.makeText( CreateCategory.this, "Failed\n\n" + e.toString(), Toast.LENGTH_LONG ).show();
					return;
				}
				categories.add(newCategory);

				MainData.setCategoriesList(categories);
				Utils.saveCategoriesList(categories);
				Utils.saveCategoryDocuments(uid, documentsToIncludeInThisCategory);

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

	class Adapter extends RecyclerView.Adapter<Adapter.VH>{
		private ArrayList<Document> mDocuments;
		private LayoutInflater mLayoutInflater;
		private View.OnClickListener mOnClickListener;

		Adapter(ArrayList<Document> documents, View.OnClickListener onClickListener) {
			mDocuments = documents;
			mLayoutInflater = LayoutInflater.from(CreateCategory.this);
			mOnClickListener = onClickListener;
		}

		@NonNull
		@Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = mLayoutInflater.inflate(R.layout.check_box_list_item, parent, false);
			return new VH(view);
		}

		@Override
		public void onBindViewHolder(@NonNull VH holder, int position) {
			holder.name.setText(mDocuments.get(position).getName());
			holder.id.setText(mDocuments.get(position).getId());
		}

		@Override
		public int getItemCount() {
			return mDocuments.size();
		}

		class VH extends RecyclerView.ViewHolder {
			TextView name, id;

			VH(@NonNull View itemView) {
				super(itemView);
				id = itemView.findViewById(R.id.checkbox_item_hidden_id);
				name = itemView.findViewById(R.id.lblNameInCheckbox );
				itemView.setOnClickListener(mOnClickListener);
			}
		}
	}
}
