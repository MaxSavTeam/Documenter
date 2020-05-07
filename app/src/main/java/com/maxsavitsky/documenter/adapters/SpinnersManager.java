package com.maxsavitsky.documenter.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Spinner;

import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressLint("StaticFieldLeak")
public class SpinnersManager {
	private Spinner spinnerCat;
	private Spinner spinnerDoc;
	private Context mContext;
	private String mType;
	private View mView;

	private String selectedCat = "all", selectedDoc = "all", selectedEnt = "all";
	private ArrayList<Category> mCategories;
	private ArrayList<Document> mDocuments;
	private ArrayList<Entry> mEntries;

	public SpinnersManager(Context context, String type) {
		mContext = context;
		mType = type;
	}

	public void createWith(View rootView) {
		mView = rootView;
		spinnerCat = rootView.findViewById( R.id.spinnerCat );
		spinnerDoc = rootView.findViewById( R.id.spinnerDoc );
		initializeSpinnerCat();
	}

	private void reloadData(){
		addDocuments();
		addEntries();
		RecyclerView recyclerView = mView.findViewById( R.id.recyclerViewChangeList );
		DefaultChooseAdapter oldAdapter = (DefaultChooseAdapter) recyclerView.getAdapter();
		/*DefaultChooseAdapter newAdapter = new DefaultChooseAdapter( mEntries, oldAdapter.getOnClickListener(), mContext );
		newAdapter.setStartElements( oldAdapter.getStartElements() );
		recyclerView.setAdapter( newAdapter );*/
		oldAdapter.reload( mEntries );
	}

	private void initializeSpinnerCat(){
		mCategories = new ArrayList<>();
		mCategories.add( new Category( "all", mContext.getString( R.string.all ) ) );
		mCategories.addAll( MainData.getCategoriesList() );

		spinnerCat.setAdapter( new SpinnerAdapter( mCategories, mContext ) );
		spinnerCat.setSelection( 0 );
		spinnerCat.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				selectedCat = mCategories.get( position ).getId();
				reloadData();
				if(selectedCat.equals( "all" ))
					mView.findViewById( R.id.layoutDoc ).setVisibility( View.GONE );
				else if(mType.equals( "ent" )) {
					mView.findViewById( R.id.layoutDoc ).setVisibility( View.VISIBLE );
					initializeSpinnerDoc();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		} );
	}

	private void initializeSpinnerDoc(){
		addDocuments();
		spinnerDoc.setAdapter( new SpinnerAdapter( mDocuments, mContext ) );
		spinnerDoc.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				selectedDoc = mDocuments.get( position ).getId();
				reloadData();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		} );
	}

	private void addEntries(){
		ArrayList<Entry> entries = new ArrayList<>();
		if(selectedCat.equals( "all" )){
			if(selectedDoc.equals( "all" )) {
				entries.addAll( MainData.getEntriesList() );
			} else {
				entries.addAll( MainData.getDocumentWithId( selectedDoc ).getEntries() );
			}
		}else{
			Category category = MainData.getCategoryWithId( selectedCat );
			if(selectedDoc.equals( "all" )){
				for(Document document : category.getDocuments()){
					entries.addAll( document.getEntries() );
				}
			}else{
				Document document = MainData.getDocumentWithId( selectedDoc );
				entries.addAll( document.getEntries() );
			}
		}
		// check for duplicates
		mEntries = new ArrayList<>();
		Map<String, Boolean> map = new HashMap<>();
		for(Entry entry : entries){
			if(!map.containsKey( entry.getId() )){
				mEntries.add( entry );
				map.put( entry.getId(), true );
			}
		}

		Collections.sort( mEntries, Utils.getSortByNamesComparator() );
	}

	private void addDocuments(){
		ArrayList<Document> documents = new ArrayList<>();
		documents.add( new Document( "all", mContext.getString( R.string.all ) ) );
		if(selectedCat.equals( "all" ))
			documents.addAll( MainData.getDocumentsList() );
		else{
			Category category = MainData.getCategoryWithId( selectedCat );
			documents.addAll( category.getDocuments() );
		}

		mDocuments = documents;
	}

}
