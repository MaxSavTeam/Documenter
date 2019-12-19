package com.maxsavitsky.documenter;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.adapters.CategoryListAdapter;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.MainData;

import java.util.ArrayList;

public class CategoryList extends AppCompatActivity {
	private ArrayList<Category> mCategories;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle("Category List");
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
	protected void onResume() {
		super.onResume();
		//if(mCategories.size() != MainData.getCategoriesList().size())
			setupRecyclerView();
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
			startActivity(intent);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_category_list);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		applyTheme();

		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				goToActivity(CreateCategory.class);
			}
		});
		/*try{
			XMLParser xmlParser = new XMLParser();
			mCategories = xmlParser.parseCategories();
		}catch (Exception e){
			e.printStackTrace();
			mCategories = new ArrayList<>();
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}*/

		setupRecyclerView();
	}

	private void goToActivity(Class <?> cls){
		Intent goTo = new Intent(this, cls);
		startActivity(goTo);
	}
}
