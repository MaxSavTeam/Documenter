package com.maxsavitsky.documenter;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparateCategory;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class DocumentList extends AppCompatActivity {
	private ArrayList<Document> documents;
	private static final int MENU_EDIT_TEXT_ID = 101;
	private Category mCategory;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(mCategory.getName());
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_32dp);
			actionBar.setHomeButtonEnabled(true);
			actionBar.setBackgroundDrawable(getDrawable(R.drawable.black));
		}
	}

	private void setupRecyclerView(){
		documents = MainData.getDocumentsList();
		RecyclerView recyclerView = findViewById(R.id.category_list_view);
		if(documents.isEmpty()){
			recyclerView.setVisibility(View.GONE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.VISIBLE);
		}else {
			LinearLayoutManager lay = new LinearLayoutManager(this);
			lay.setOrientation(RecyclerView.VERTICAL);
			DocumentsAdapter mAdapter = new DocumentsAdapter(documents);
			recyclerView.setLayoutManager(lay);
			recyclerView.setAdapter(mAdapter);
			recyclerView.setVisibility(View.VISIBLE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onResume() {
		if(documents.size() != MainData.getDocumentsList().size()){
			setupRecyclerView();
		}
		super.onResume();
	}

	@Override
	public void onBackPressed() {
		finish();
		super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()){
			case android.R.id.home:
				finish();
				break;
			case R.id.item_delete:
				if(!MainData.finallyDeleteCategoryWithId(mCategory.getId())){
					Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
				}else
					finish();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.category_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_document_list2);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Intent intent = getIntent();
		String id = intent.getStringExtra("category_uid");
		mCategory = MainData.getCategoryWithId(id);

		applyTheme();

		try {
			documents = ParseSeparateCategory.parseCategoryWithId(id);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}

		setupRecyclerView();
		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(DocumentList.this, CreateDocument.class).putExtra("parent_id", mCategory.getId()));
			}
		});
	}

	class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.VH>{
		private ArrayList<Document> mData;

		DocumentsAdapter(ArrayList<Document> data) {
			mData = data;
		}

		@NonNull
		@Override
		public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new VH(LayoutInflater.from(DocumentList.this).inflate(R.layout.list_item, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull VH holder, int position) {
			holder.name.setText(mData.get(position).getName());
			holder.id.setText(mData.get(position).getId());
		}

		@Override
		public int getItemCount() {
			return mData.size();
		}

		class VH extends RecyclerView.ViewHolder{
			TextView name, id;

			VH(@NonNull View itemView) {
				super(itemView);
				name = itemView.findViewById(R.id.lblCategoryName);
				id = itemView.findViewById(R.id.lblHiddenCategoryId);
			}
		}
	}

}
