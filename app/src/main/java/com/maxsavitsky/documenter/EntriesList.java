package com.maxsavitsky.documenter;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.Entry;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.xml.ParseSeparate;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;

public class EntriesList extends AppCompatActivity {
	private Document mDocument;
	private ArrayList<Entry> mEntries;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle(mDocument.getName());
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_32dp);
			actionBar.setHomeButtonEnabled(true);
			actionBar.setBackgroundDrawable(getDrawable(R.drawable.black));
		}
	}

	private void setupRecyclerView(){
		mEntries = MainData.getDocumentWithId( mDocument.getId() ).getEntries();
		RecyclerView recyclerView = findViewById( R.id.category_list_view );
		if(mEntries.isEmpty()){
			recyclerView.setVisibility(View.GONE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.VISIBLE);
		}else{
			LinearLayoutManager linearLayoutManager = new LinearLayoutManager( this );
			linearLayoutManager.setOrientation( RecyclerView.VERTICAL );

			RVAdapter rvAdapter = new RVAdapter( mEntries );

			recyclerView.setLayoutManager( linearLayoutManager );
			recyclerView.setAdapter( rvAdapter );
			recyclerView.setVisibility(View.VISIBLE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.GONE);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate ( savedInstanceState );
		setContentView ( R.layout.activity_entries_list );
		Toolbar toolbar = findViewById ( R.id.toolbar );
		setSupportActionBar ( toolbar );

		Intent intent = getIntent ();
		mDocument = MainData.getDocumentWithId( intent.getStringExtra ( "id" ) );
		try {
			mEntries = ParseSeparate.parseDocumentWithId( mDocument.getId() );
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();
		}
		applyTheme();

		setupRecyclerView();
	}

	class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder>{
		private ArrayList<Entry> mData;

		RVAdapter(ArrayList<Entry> data) {
			mData = data;
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			return new ViewHolder( LayoutInflater.from( EntriesList.this ).inflate( R.layout.list_item, parent, false ) );
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
			holder.id.setText( mData.get( position ).getId() );
			holder.name.setText( mData.get( position ).getName() );
		}

		@Override
		public int getItemCount() {
			return mData.size();
		}

		 class ViewHolder extends RecyclerView.ViewHolder {
			TextView id, name;

			ViewHolder(@NonNull View itemView) {
				super(itemView);
				id = itemView.findViewById( R.id.lblHiddenCategoryId );
				name = itemView.findViewById( R.id.lblCategoryName );
			}
		}
	}
}
