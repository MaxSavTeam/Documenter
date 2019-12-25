package com.maxsavitsky.documenter;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.maxsavitsky.documenter.datatypes.MainData;
import com.maxsavitsky.documenter.utils.RequestCodes;
import com.maxsavitsky.documenter.utils.ResultCodes;
import com.maxsavitsky.documenter.utils.Utils;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		Utils.setContext(getApplicationContext());

		try {
			MainData.readAllCategories();
		} catch (Exception e) {
			e.printStackTrace();
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
		}

		try{
			MainData.readAllDocuments();
		}catch (Exception e){
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
		}

		try{
			MainData.readAllEntries();
		}catch (Exception e){
			Toast.makeText( this, e.toString(), Toast.LENGTH_SHORT ).show();
		}
		Intent intent = new Intent(this, CategoryList.class);
		startActivityForResult( intent, RequestCodes.CATEGORY_LIST );
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == RequestCodes.CATEGORY_LIST ){
			if(resultCode == ResultCodes.RESULT_CODE_EXIT){
				finishAndRemoveTask();
			}
			if(resultCode == ResultCodes.RESULT_CODE_RESTART_ACTIVITY){
				Intent intent = new Intent( this, CategoryList.class );
				startActivityForResult( intent, RequestCodes.CATEGORY_LIST );
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}
}
