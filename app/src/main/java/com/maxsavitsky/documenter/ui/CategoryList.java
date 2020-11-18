package com.maxsavitsky.documenter.ui;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
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
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.adapters.ListAdapter;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Type;
import com.maxsavitsky.documenter.ui.widget.FabButton;
import com.maxsavitsky.documenter.updates.UpdatesChecker;
import com.maxsavitsky.documenter.updates.UpdatesDownloader;
import com.maxsavitsky.documenter.updates.VersionInfo;
import com.maxsavitsky.documenter.utils.ApkInstaller;
import com.maxsavitsky.documenter.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CategoryList extends ThemeActivity {
	private int mSortOrder = 1; //1 - по возрастанию - стрелка вверх; -1 - по убыванию
	private SharedPreferences sp;

	private void applyTheme(){
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle( R.string.title_activity_category_list);
			Utils.applyDefaultActionBarStyle(actionBar);
			actionBar.setDisplayHomeAsUpEnabled( false );
		}
	}

	private void onMyBackPressed(){
		setResult( Results.EXIT );
		finish();
	}

	private void finishActivity() {
		finish();
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
			case R.id.item_common_invert:
				mSortOrder = -mSortOrder;
				if(mSortOrder == 1)
					item.setIcon( R.drawable.ic_sort_ascending );
				else
					item.setIcon( R.drawable.ic_sort_descending );
				setupRecyclerView();
				break;
			case R.id.item_common_sort_mode:
				AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
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
		getMenuInflater().inflate( R.menu.common_menu, menu );
		menu.findItem( R.id.item_common_parameters ).setVisible( false );
		menu.findItem( R.id.item_common_change_name ).setVisible( false );
		return super.onCreateOptionsMenu( menu );
	}

	private final Comparator<Category> mCategoryComparator = new Comparator<Category>() {
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
		ArrayList<Category> categories = MainData.getCategoriesList();
		if( categories.size() == 0){
			recyclerView.setVisibility(View.GONE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.VISIBLE);
		}else {
			if( categories.size() > 1)
				Collections.sort( categories, mCategoryComparator );
			ListAdapter adapter = new ListAdapter(this, categories, mAdapterCallback );
			recyclerView.setLayoutManager(lay);
			recyclerView.setAdapter(adapter);
			recyclerView.setVisibility(View.VISIBLE);
			TextView textView = findViewById(R.id.textViewNothingFound);
			textView.setVisibility(View.GONE);
		}
	}

	private final ListAdapter.AdapterCallback mAdapterCallback = new ListAdapter.AdapterCallback() {
		@Override
		public void onClick(Type type) {
			Intent intent = new Intent(CategoryList.this, DocumentList.class);
			intent.putExtra("category_uid", type.getId());
			//startActivity(intent);
			startActivityForResult( intent, Requests.DOCUMENT_LIST );
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if(requestCode == Requests.DOCUMENT_LIST ){
			if(resultCode == Results.RESTART_ACTIVITY ){
				Intent intent = new Intent( this, DocumentList.class );
				if ( data != null ) {
					intent.putExtra( "category_uid", data.getStringExtra( "id" ) );
				}
				startActivityForResult( intent, Requests.DOCUMENT_LIST );
			}
		}
		if ( resultCode == Results.RESTART_APP ) {
			setResult( resultCode );
			finishActivity();
		}
		if(resultCode == Results.NEED_TO_REFRESH){
			setupRecyclerView();
		}
		if(resultCode == Results.EXIT){
			onMyBackPressed();
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	private final UpdatesChecker.CheckResults mCheckResults = new UpdatesChecker.CheckResults() {
		@Override
		public void noUpdates() {

		}

		@Override
		public void onUpdateAvailable(final VersionInfo versionInfo) {
			runOnUiThread( ()->{
				AlertDialog.Builder builder = new AlertDialog.Builder( CategoryList.this, CategoryList.super.mAlertDialogStyle );
				if(versionInfo.isImportant()){
					builder.setTitle( String.format( getString(R.string.necessary_update_title), versionInfo.getVersionName() ) );
					builder.setMessage( R.string.necessary_update_text )
							.setCancelable( false )
							.setPositiveButton( "OK", (dialog, which)->{
								download( versionInfo );
								dialog.cancel();
							} );
				}else{
					builder.setTitle( String.format( getString(R.string.update_available), versionInfo.getVersionName() ) )
							.setCancelable( false )
							.setMessage( R.string.would_you_like_to_download_and_install )
							.setPositiveButton( R.string.yes, (dialog, which)->{
								download( versionInfo );
								dialog.cancel();
							} )
							.setNegativeButton( R.string.no, (dialog, which)->dialog.cancel() )
							.setNeutralButton( R.string.ignore_this_update, (dialog, which)->sp.edit().putInt( "ignore_update", versionInfo.getVersionCode() ).apply() );
				}
				builder.create().show();
			} );
		}

		@Override
		public void onDownloaded(File path) {
			if(mDownloadPd != null)
				mDownloadPd.dismiss();

			ApkInstaller.installApk( CategoryList.this, path );
		}

		@Override
		public void onException(Exception e) {

		}

		@Override
		public void onDownloadProgress(final int bytesCount, final int totalBytesCount) {
			runOnUiThread( ()->{
				mDownloadPd.setIndeterminate( false );
				mDownloadPd.setMax( 100 );
				mDownloadPd.setProgress( bytesCount * 100 / totalBytesCount );
			} );
		}
	};


	private ProgressDialog mDownloadPd = null;
	private Thread downloadThread;

	private void download(VersionInfo versionInfo){
		final UpdatesDownloader downloader = new UpdatesDownloader( versionInfo, mCheckResults );
		mDownloadPd = new ProgressDialog(this);
		mDownloadPd.setMessage( getString( R.string.downloading ) );
		mDownloadPd.setCancelable( false );
		mDownloadPd.setProgressStyle( ProgressDialog.STYLE_HORIZONTAL );
		mDownloadPd.setIndeterminate( true );
		mDownloadPd.setButton( ProgressDialog.BUTTON_NEGATIVE, getResources().getString( R.string.cancel ), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, int which) {
				downloadThread.interrupt();
				runOnUiThread( dialog::cancel );
			}
		} );
		mDownloadPd.show();
		downloadThread = new Thread( downloader::download );
		downloadThread.start();
	}

	public static void initializeFabButtons(final Activity activity){
		activity.findViewById( R.id.fabFreeEntries).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(activity, EntriesList.class);
				intent.putExtra( "free_mode", true );
				activity.startActivityForResult( intent, Requests.FREE_ENTRIES );
			}
		} );
		activity.findViewById( R.id.fabSettings ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent( activity, SettingsActivity.class );
				activity.startActivityForResult( intent, Requests.SETTINGS );
			}
		} );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		sp = Utils.getDefaultSharedPreferences();
		setContentView(R.layout.activity_category_list);
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FabButton fab = findViewById(R.id.fabCreateNew);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent( CategoryList.this, CreateCategory.class );
				startActivityForResult( intent, Requests.CREATE_CATEGORY );
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
									setResult( Results.OK );
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
		initializeFabButtons( this );
		findViewById( R.id.fabSettings ).setOnLongClickListener( new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				setResult( Results.LOOK_STARTUP );
				finish();
				return true;
			}
		} );

		if(!isMemoryAccessGranted()){
			if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
				requestPermissions( new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, 1 );
			}
		}else {
			setupRecyclerView();
			if(sp.getBoolean( "check_updates", true )) {
				final UpdatesChecker checker = new UpdatesChecker( mCheckResults );
				new Thread( new Runnable() {
					@Override
					public void run() {
						checker.runCheck();
					}
				} ).start();
			}
		}
	}

	private boolean isMemoryAccessGranted(){
		boolean write = ContextCompat.checkSelfPermission( this, Manifest.permission.WRITE_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
		boolean read = ContextCompat.checkSelfPermission( this, Manifest.permission.READ_EXTERNAL_STORAGE ) == PackageManager.PERMISSION_GRANTED;
		return write && read;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		if(requestCode == 1){
			if(grantResults[0] != PackageManager.PERMISSION_GRANTED || grantResults[1] != PackageManager.PERMISSION_GRANTED){
				AlertDialog.Builder builder = new AlertDialog.Builder( this )
						.setTitle( getResources().getString( R.string.you_will_not_pass ) )
						.setMessage( R.string.warning_when_memory_denied )
						.setCancelable( false )
						.setNeutralButton( "Request", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
								if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
									requestPermissions( new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE }, 1 );
								}
							}
						} );
				builder.create().show();
			}else{
				setupRecyclerView();
			}
		}
	}
}
