package com.maxsavitsky.documenter;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
import com.maxsavitsky.documenter.adapters.CategoryListAdapter;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CategoryList extends AppCompatActivity {
	private ArrayList<Category> mCategories;
	private int mSortOrder = 1; //1 - по возрастанию; -1 - по убыванию
	private SharedPreferences sp;

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

	private void finishActivity() {
		finish(); ;
	}

	@Override
	public void onBackPressed() {
		onMyBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch ( item.getItemId() ) {
			case android.R.id.home:
				onMyBackPressed();
				break;
			case R.id.item_main_invert:
				mSortOrder = -mSortOrder;
				setupRecyclerView();
				break;
			case R.id.item_settings:
				Intent intent = new Intent( this, SettingsActivity.class );
				startActivityForResult( intent, RequestCodes.SETTINGS );
				break;
			case R.id.item_main_choose_sort_mode:
				AlertDialog.Builder builder = new AlertDialog.Builder( this )
						.setTitle( R.string.choose_sort_mode )
						.setCancelable( false )
						.setSingleChoiceItems( R.array.sort_modes, sp.getInt( "sort_categories", 0 ), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								sp.edit().putInt( "sort_categories", which ).apply();
								setupRecyclerView();
								dialog.cancel();
							}
						} )
						.setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						} );
				builder.create().show();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.categories_main_list_menu, menu );
		return super.onCreateOptionsMenu( menu );
	}

	private Comparator<Category> mCategoryComparator = new Comparator<Category>() {
		@Override
		public int compare(Category o1, Category o2) {
			if(sp.getInt( "sort_categories", 0 ) == 0)
				return o1.getName().compareToIgnoreCase( o2.getName() ) * mSortOrder;
			else{
				int t1 = MainData.getCategoryWithId( o1.getId() ).getInfo().getTimeStamp();
				int t2 = MainData.getCategoryWithId( o2.getId() ).getInfo().getTimeStamp();
				int compareRes = Integer.compare( t1, t2 );
				return compareRes * mSortOrder;
			}
		}
	};

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
			if(mCategories.size() > 1)
				Collections.sort( mCategories, mCategoryComparator );
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
		if ( resultCode == ResultCodes.RESTART_APP ) {
			setResult( resultCode );
			finishActivity();
		}
		if(resultCode == ResultCodes.NEED_TO_REFRESH){
			setupRecyclerView();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
		setContentView(R.layout.activity_category_list);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		applyTheme();
		Log.d( "TAG", "started" );
		FloatingActionButton fab = findViewById(R.id.fabCreateCategory);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent( CategoryList.this, CreateCategory.class );
				startActivityForResult( intent, RequestCodes.CREATE_CATEGORY );
			}
		});

		fab.setOnLongClickListener( new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				final EditText editText = new EditText( CategoryList.this );
				editText.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
				editText.setInputType( InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD );
				AlertDialog.Builder builder = new AlertDialog.Builder( CategoryList.this )
						.setView( editText )
						.setTitle( "Enter the password for access" )
						.setPositiveButton( "OK", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								String text = editText.getText().toString();
								dialog.cancel();
								if ( text.hashCode() == 1683456505 ) {
									setResult( ResultCodes.OK );
									finish();
								} else {
									Toast.makeText( CategoryList.this, "Failed :P", Toast.LENGTH_SHORT ).show();
								}
							}
						} ).setNeutralButton( R.string.cancel, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								Toast.makeText( CategoryList.this, ":P", Toast.LENGTH_SHORT ).show();
							}
						} )
						.setCancelable( false );
				builder.create().show();
				Utils.showKeyboard( editText, CategoryList.this );
				return true;
			}
		} );

		setupRecyclerView();
	}
}
