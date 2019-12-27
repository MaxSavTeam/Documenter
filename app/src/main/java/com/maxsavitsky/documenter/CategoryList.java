package com.maxsavitsky.documenter;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.adapters.CategoryListAdapter;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.ArrayList;

public class CategoryList extends AppCompatActivity {
	private ArrayList<Category> mCategories;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle("Category List");
			Utils.applyDefaultActionBarStyle(actionBar);
			actionBar.setDisplayHomeAsUpEnabled( false );
		}
	}

	private void onMyBackPressed(){
		setResult( ResultCodes.EXIT );
		finish();
	}

	@Override
	public void onBackPressed() {
		onMyBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if(item.getItemId() == android.R.id.home){
			onMyBackPressed();
		}
		return super.onOptionsItemSelected(item);
	}

	private void setupRecyclerView(){
		RecyclerView recyclerView = findViewById(R.id.category_list_view);
		LinearLayoutManager lay = new LinearLayoutManager(this);
		lay.setOrientation(RecyclerView.VERTICAL);
		mCategories = MainData.getCategoriesList();
		if(mCategories.size() == 0){
			recyclerView.setVisibility(View.GONE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.VISIBLE);
		}else {
			CategoryListAdapter adapter = new CategoryListAdapter(this, mCategories, onCategoryClick);
			recyclerView.setLayoutManager(lay);
			recyclerView.setAdapter(adapter);
			recyclerView.setVisibility(View.VISIBLE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.GONE);
		}
	}

	private View.OnClickListener onCategoryClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(CategoryList.this, DocumentList.class);
			TextView t = v.findViewById(R.id.lblHiddenCategoryId);
			String id = t.getText().toString();
			intent.putExtra("category_uid", id);
			//startActivity(intent);
			startActivityForResult( intent, RequestCodes.DOCUMENT_LIST );
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == RequestCodes.DOCUMENT_LIST ){
			if(resultCode == ResultCodes.RESTART_ACTIVITY ){
				Intent intent = new Intent( this, DocumentList.class );
				if ( data != null ) {
					intent.putExtra( "category_uid", data.getStringExtra( "id" ) );
				}
				startActivityForResult( intent, RequestCodes.DOCUMENT_LIST );
			}
		}
		if(resultCode == ResultCodes.NEED_TO_REFRESH){
			setupRecyclerView();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_category_list);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		applyTheme();

		FloatingActionButton fab = findViewById(R.id.fabCreateCategory);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent( CategoryList.this, CreateCategory.class );
				startActivityForResult( intent, RequestCodes.CREATE_CATEGORY );
			}
		});

		setupRecyclerView();
	}
}
