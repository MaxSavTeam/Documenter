package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

public class DocumentList extends AppCompatActivity {
	private ArrayList<Document> mDocuments;
	private Category mCategory;
	private View decorView;
	private Map<String, Document> mDocumentMap = new HashMap<>(  );
	private Map<String, Document> documentsToInclude = new HashMap<>();
	private ArrayList<String> documentsWhichWillBeExcluded = new ArrayList<>();

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(mCategory.getName());
			Utils.applyDefaultActionBarStyle( actionBar );
		}
	}

	private void backPressed(){
		setResult( ResultCodes.RESULT_CODE_OK );
		finish();
	}

	private void setupRecyclerView(){
		try {
			mDocuments = ParseSeparate.parseCategoryWithId( mCategory.getId() );
		}catch (Exception e){
			e.printStackTrace();
			Toast.makeText( this, e.toString() + "\n\nsetup", Toast.LENGTH_LONG ).show();
			return;
		}
		RecyclerView recyclerView = findViewById(R.id.category_list_view);
		if( mDocuments.isEmpty()){
			recyclerView.setVisibility(View.GONE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.VISIBLE);
		}else {
			LinearLayoutManager lay = new LinearLayoutManager(this);
			lay.setOrientation(RecyclerView.VERTICAL);
			if(mDocuments.size() > 1)
				Collections.sort( mDocuments );
			DocumentsAdapter mAdapter = new DocumentsAdapter( mDocuments, mOnClickListener);
			recyclerView.setLayoutManager(lay);
			recyclerView.setAdapter(mAdapter);
			recyclerView.setVisibility(View.VISIBLE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.GONE);
		}
	}

	private View.OnClickListener mOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			Intent intent = new Intent(DocumentList.this, EntriesList.class);
			TextView t = v.findViewById(R.id.lblHiddenCategoryId);
			String id = t.getText().toString();
			intent.putExtra("id", id);
			startActivity(intent);
		}
	};

	@Override
	public void onBackPressed() {
		backPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()){
			case R.id.item_edit_documents_list:
				prepareChooseRecyclerView();
				break;
			case android.R.id.home:
				backPressed();
				break;
			case R.id.item_delete:
				try {
					if ( MainData.finallyDeleteCategoryWithId( mCategory.getId() ) ) {
						setResult( ResultCodes.NEED_TO_REFRESH );
						finish();
					} else {
						Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText( this, e.toString() + "\nonOptionsItemSelected", Toast.LENGTH_LONG ).show();
				}
				break;
			case R.id.menu_edit_name:
				AlertDialog alertDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Edit category name").setMessage("Edit category name here");
				//View view = new View(this);
				final EditText editText = new EditText(this);
				editText.setText(mCategory.getName());
				editText.append("");
				editText.requestFocus();
				ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				editText.setLayoutParams(layoutParams);
				builder.setView(editText).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String text = editText.getText().toString();
						if(!text.isEmpty() && !text.equals(mCategory.getName())){
							MainData.removeCategoryWithId(mCategory.getId());
							ArrayList<Category> categories = MainData.getCategoriesList();
							mCategory = new Category(mCategory.getId(), text);
							applyTheme();
							categories.add(mCategory);
							MainData.setCategoriesList(categories);
							Utils.saveCategoriesList(categories);
							setResult( ResultCodes.NEED_TO_REFRESH );
						}
						dialog.cancel();
					}
				}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				alertDialog = builder.create();
				alertDialog.show();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void restartActivity() {
		setResult( ResultCodes.RESULT_CODE_RESTART_ACTIVITY, new Intent().putExtra( "id", mCategory.getId() ) );
		this.finish();
	}

	private void prepareChooseRecyclerView(){
		setContentView( R.layout.layout_choose_documents );
		mDocumentMap.clear();

		View.OnClickListener cancel = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				restartActivity();
			}
		};
		Button btnCancel = findViewById( R.id.btnCancel );
		btnCancel.setOnClickListener( cancel );

		View.OnClickListener apply = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ArrayList<Document> documents = new ArrayList<>();
				/*for(String key : mDocumentMap.keySet()){
					documents.add( mDocumentMap.get( key ) );
				}*/
				for(String key : documentsToInclude.keySet()){
					Document document = documentsToInclude.get( key );
					documents.add( document );
					try {
						//MainData.addCategoryToIncludedInXml( document.getId(), mCategory.getId() );
						document.addCategoryToIncludedInXml( mCategory.getId() );
					} catch (Exception e) {
						e.printStackTrace();
						Toast.makeText( DocumentList.this, e.toString() + "\napply", Toast.LENGTH_SHORT ).show();
						return;
					}
				}
				for(String key : documentsWhichWillBeExcluded){
					Document document = MainData.getDocumentWithId( key );
					documents.remove( document );
					try {
						document.removeCategoryFromIncludedXml( mCategory.getId() );
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				try {
					Utils.saveCategoryDocuments( mCategory.getId(), documents );
				}catch (Exception e){
					Toast.makeText( DocumentList.this, e.toString(), Toast.LENGTH_LONG ).show();
				}
				//Toast.makeText( DocumentList.this, Integer.toString( documents.size() ), Toast.LENGTH_SHORT ).show();
				restartActivity();
				//Toast.makeText( DocumentList.this, documentsToInclude.toString(), Toast.LENGTH_LONG ).show();
			}
		};
		Button btnApply = findViewById( R.id.btnApply );
		btnApply.setOnClickListener( apply );

		RecyclerView recyclerView = findViewById( R.id.recyclerViewChangeList );
		LinearLayoutManager layoutManager = new LinearLayoutManager( this );
		layoutManager.setOrientation( RecyclerView.VERTICAL );
		recyclerView.setLayoutManager( layoutManager );

		ArrayList<Document> documents = null;
		try {
			documents = MainData.getDocumentsFromThisCategory( mCategory.getId() );
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText( this, e.toString() + "\nprepareChooseRecyclerView", Toast.LENGTH_LONG ).show();
			return;
		}
		for(Document document : documents){
			mDocumentMap.put( document.getId(), document );
		}
		documentsToInclude = mDocumentMap;

		View.OnClickListener adapterOnClickListener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CheckBox checkBox = v.findViewById( R.id.checkBoxInCheckboxItem );
				checkBox.setChecked( !checkBox.isChecked() );
				String id = ((TextView) v.findViewById( R.id.checkbox_item_hidden_id )).getText().toString();
				if(checkBox.isChecked()){
					documentsToInclude.put( id, MainData.getDocumentWithId( id ) );
					if(mDocumentMap.containsKey( id )){
						documentsWhichWillBeExcluded.remove( id );
					}
				}else{
					if(mDocumentMap.containsKey( id )){
						documentsWhichWillBeExcluded.add( id );
					}
					documentsToInclude.remove( id );
					//Toast.makeText( DocumentList.this, Boolean.toString( documentsToInclude.containsKey( id ) ), Toast.LENGTH_SHORT ).show();
				}
				//Toast.makeText( DocumentList.this, Integer.toString( documentsToInclude.size() ) + "\n" + Integer.toString( documentsWhichWillBeExcluded.size() ), Toast.LENGTH_LONG ).show();
			}
		};
		// TODO: 20.12.2019 add list sorting
		ChangeListAdapter changeListAdapter = new ChangeListAdapter(
				MainData.getDocumentsList(),
				adapterOnClickListener );

		recyclerView.setAdapter( changeListAdapter );
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

		try {
			mCategory = MainData.getCategoryWithId(id);
			mDocuments = ParseSeparate.parseCategoryWithId(id);
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();
			return;
		}

		applyTheme();

		setupRecyclerView();
		FloatingActionButton fab = findViewById(R.id.fabCreateDocument);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivityForResult(new Intent(DocumentList.this, CreateDocument.class).putExtra("parent_id", mCategory.getId()), RequestCodes.REQUEST_CODE_CREATE_DOCUMENT );
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult( requestCode, resultCode, data );
		if ( requestCode == RequestCodes.REQUEST_CODE_CREATE_DOCUMENT ) {
			if(resultCode == ResultCodes.NEED_TO_REFRESH){
				setupRecyclerView();
			}
		}
	}

	class DocumentsAdapter extends RecyclerView.Adapter<DocumentsAdapter.VH>{
		private ArrayList<Document> mData;
		private View.OnClickListener onClickListener;

		DocumentsAdapter(ArrayList<Document> data, View.OnClickListener onClickListener) {
			mData = data;
			this.onClickListener = onClickListener;
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
				itemView.setOnClickListener( DocumentsAdapter.this.onClickListener );
			}
		}
	}

	class ChangeListAdapter extends RecyclerView.Adapter<ChangeListAdapter.ViewHolder>{
		private ArrayList<Document> allDocumentsList;
		private View.OnClickListener mOnClickListener;

		ChangeListAdapter(ArrayList<Document> allDocumentsList, View.OnClickListener onClickListener) {
			this.allDocumentsList = allDocumentsList;
			mOnClickListener = onClickListener;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			LayoutInflater layoutInflater = LayoutInflater.from( DocumentList.this );
			View view = layoutInflater.inflate( R.layout.check_box_list_item, parent, false );
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			if(mDocumentMap.containsKey( allDocumentsList.get( position ).getId() )){
				holder.mCheckBox.setChecked( true );
			}else{
				holder.mCheckBox.setChecked( false );
			}
			holder.mName.setText( allDocumentsList.get( position ).getName() );
			holder.mId.setText( allDocumentsList.get( position ).getId() );
		}

		@Override
		public int getItemCount() {
			return allDocumentsList.size();
		}

		class ViewHolder extends RecyclerView.ViewHolder {
			TextView mName, mId;
			CheckBox mCheckBox;

			ViewHolder(@NonNull View itemView) {
				super( itemView );
				mName = itemView.findViewById( R.id.lblNameInCheckbox );
				mId = itemView.findViewById( R.id.checkbox_item_hidden_id );
				mCheckBox = itemView.findViewById( R.id.checkBoxInCheckboxItem );
				itemView.setOnClickListener( mOnClickListener );
			}
		}
	}
}
