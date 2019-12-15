package com.maxsavitsky.documenter;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.datatypes.Category;
import com.maxsavitsky.documenter.datatypes.Document;
import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.XMLParser;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
	XMLParser mXMLParser = new XMLParser();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		FloatingActionButton fab = findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

			}
		});

		Utils.setContext(getApplicationContext());

		ArrayList<Document> documents = new ArrayList<>();
		try{
			documents = mXMLParser.parseDocuments();
			MainData.setDocumentsList(documents);
		}catch (Exception e){
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}

		try {
			MainData.readAllCategories();
		} catch (IOException | SAXException e) {
			e.printStackTrace();
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
		}
	}

	public void getCategories(View v){
		ArrayList<Category> categories = new ArrayList<>();
		try {
			categories = mXMLParser.parseCategories();
		} catch (Exception e){
			Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
		}
		String msg = "";
		for(int i = 0 ; i < categories.size(); i++){
			msg = String.format("%s id=%s name=%s\n", msg, categories.get(i).getId(), categories.get(i).getName());
		}
		Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
	}

	public void getDocuments(View v){
		ArrayList<Document> documents = new ArrayList<>();
		try{
			documents = mXMLParser.parseDocuments();
		}catch (Exception e){
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}
		String msg = "";
		for(int i = 0;  i < documents.size(); i++){
			msg = String.format("%sid=%s name=%s\n", msg, documents.get(i).getId(), documents.get(i).getName());
		}
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	public void viewCategoryList(View v){
		Intent intent = new Intent(this, CategoryList.class);
		startActivity(intent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
