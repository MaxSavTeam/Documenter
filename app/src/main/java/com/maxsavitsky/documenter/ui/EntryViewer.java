package com.maxsavitsky.documenter.ui;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.adapters.ListAdapter;
import com.maxsavitsky.documenter.codes.Requests;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.MainData;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.EntryEntity;
import com.maxsavitsky.documenter.data.types.Type;
import com.maxsavitsky.documenter.ui.widget.CustomScrollView;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

public class EntryViewer extends ThemeActivity {

	private static final String TAG = MainActivity.TAG + " EntryViewer";
	private EntryEntity mEntry;
	private SharedPreferences sp;
	private CustomScrollView mScrollView;

	private void applyTheme() {
		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			Utils.applyDefaultActionBarStyle( actionBar );
			actionBar.setTitle( mEntry.getName() );
		}
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if ( itemId == android.R.id.home ) {
			onBackPressed();
		} else if ( itemId == R.id.item_auto_scroll ) {
			findViewById( R.id.speedLayout ).setVisibility( View.VISIBLE );
			startScroll( ( (SeekBar) findViewById( R.id.speedSeekBar ) ).getProgress() );
		} else if ( itemId == R.id.item_edit_entry_name ) {
			AlertDialog changeNameDialog;
			final EditText editText = new EditText( this );
			editText.setLayoutParams( new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT ) );
			editText.setText( mEntry.getName() );
			editText.requestFocus();
			editText.setTextColor( getColor( super.mTextColor ) );
			AlertDialog.Builder builder = new AlertDialog.Builder( this )
					.setTitle( R.string.edit_entry_name )
					.setView( editText )
					.setPositiveButton( "OK", (dialog, which)->{
						String newName = editText.getText().toString();
						newName = newName.trim();
						if ( !newName.isEmpty() && !newName.equals( mEntry.getName() ) ) {
							if ( EntitiesStorage.get().isEntryNameExists( newName ) ) {
								Toast.makeText( EntryViewer.this, R.string.this_name_already_exist, Toast.LENGTH_SHORT ).show();
								return;
							}
							mEntry.rename( newName );
							applyTheme();
							setResult( Results.NEED_TO_REFRESH );
						} else {
							Toast.makeText( EntryViewer.this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
						}
					} )
					.setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() ).setCancelable( false );
			changeNameDialog = builder.create();
			changeNameDialog.show();
		} else if ( itemId == R.id.item_delete_entry ) {
			AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this )
					.setMessage( R.string.delete_confirmation_text )
					.setTitle( R.string.confirmation )
					.setPositiveButton( "OK", (dialog, which)->{
						dialog.cancel();
						EntitiesStorage.get().deleteEntity( mEntry.getId() );
						setResult( Results.NEED_TO_REFRESH );
						onBackPressed();
					} )
					.setNeutralButton( R.string.cancel, (dialog, which)->dialog.cancel() ).setCancelable( false );
			deletionBuilder.create().show();
		} else if ( itemId == R.id.item_edit_entry_text ) {
			mEntry.getProperties().setScrollPosition( mScrollView.getScrollY() );
			try {
				mEntry.saveProperties();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Intent intent = new Intent( this, EntryEditor.class );
			intent.putExtra( "type", "edit" );
			intent.putExtra( "id", mEntry.getId() );
			intent.putExtra( "scroll_position", mScrollView.getScrollY() );
			startActivityForResult( intent, Requests.EDIT_ENTRY );
		} else if ( itemId == R.id.item_copy_content ) {
			prepareCopyToLayout();
		}
		return super.onOptionsItemSelected( item );
	}

	private void prepareCopyToLayout() {
		setContentView( R.layout.layout_copy_to );
		getWindow().getDecorView().setBackgroundColor( super.BACKGROUND_COLOR );
		ArrayList<Type> entryArrayList = new ArrayList<>();
		ArrayList<Entry> mainEntries = MainData.getEntriesList();
		for (int i = 0; i < mainEntries.size(); i++) {
			Type entry = mainEntries.get( i );
			if ( !entry.getId().equals( mEntry.getId() ) ) {
				entryArrayList.add( entry );
			}
		}
		findViewById( R.id.btnCopyCancel ).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent data = new Intent();
				data.putExtra( "id", mEntry.getId() );
				setResult( Results.REOPEN, data );
				finish();
			}
		} );
		RecyclerView rc = findViewById( R.id.rcCopyTo );
		if ( entryArrayList.size() == 0 ) {
			rc.setVisibility( View.GONE );
			findViewById( R.id.txtCopyNothingToShow ).setVisibility( View.VISIBLE );
		} else {
			Collections.sort( entryArrayList, Utils.getSortByNamesComparator() );
			ListAdapter.AdapterCallback adapterCallback = new ListAdapter.AdapterCallback() {
				@Override
				public void onClick(Type type) {
					Intent intent = new Intent();
					String id = type.getId();
					intent.putExtra( "type", "copy" );
					intent.putExtra( "from_id", mEntry.getId() );
					intent.putExtra( "to_id", id );
					setResult( Results.COPY_TO_ACTION, intent );
					finish();
				}
			};

			LinearLayoutManager linearLayoutManager = new LinearLayoutManager( this );
			linearLayoutManager.setOrientation( RecyclerView.VERTICAL );

			ListAdapter listAdapter = new ListAdapter( this, entryArrayList, adapterCallback );
			rc.setLayoutManager( linearLayoutManager );
			rc.setAdapter( listAdapter );
		}
	}

	private void setupSpeedSeekBar() {
		SeekBar seekBar = findViewById( R.id.speedSeekBar );
		seekBar.setProgress( 1 );
		seekBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				startScroll( progress );
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}
		} );
	}

	private ObjectAnimator mScrollAnimator;
	private int mScrollSpeed = 0;

	private void startScroll(int speed) {
		if ( mScrollAnimator != null ) {
			mScrollAnimator.cancel();
		}
		mScrollSpeed = speed;
		if ( speed != 0 ) {
			mScrollAnimator = ObjectAnimator.ofInt( mScrollView, "scrollY", mScrollView.getMaxVerticalScroll() );
			int r = (int) ( ( mScrollView.getMaxVerticalScroll() - mScrollView.getScrollY() ) / speed * 20 );
			mScrollAnimator.setDuration( r );
			mScrollAnimator.setInterpolator( new LinearInterpolator() );
			mScrollAnimator.start();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		if ( requestCode == Requests.EDIT_ENTRY ) {
			if ( resultCode == Results.REOPEN ) {
				setResult( resultCode, data );
				finish();
			} else if ( resultCode == Results.OK ) {
				if ( data != null ) {
					int scrollPos = data.getIntExtra( "scroll_position", -1 );
					if ( scrollPos != -1 ) {
						mScrollView.post( new Runnable() {
							@Override
							public void run() {
								mScrollView.scrollTo( 0, scrollPos );
							}
						} );
					}
				}
			}
		}
		super.onActivityResult( requestCode, resultCode, data );
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if ( menu != null ) {
			menu.clear();
		}
		getMenuInflater().inflate( R.menu.entry_menu, menu );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onResume() {
		super.onResume();
		if ( sp.getBoolean( "keep_screen_on", true ) ) {
			getWindow().addFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		getWindow().clearFlags( WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON );
	}

	private ProgressDialog mProgressDialog;

	private interface TextLoaderCallback {
		void exceptionOccurred(Exception e);

		void loaded(Spannable spannable);
	}

	private static final int SCROLL_ANIMATION_SPEED = 500;

	private final TextLoaderCallback mCallback = new TextLoaderCallback() {

		@Override
		public void loaded(final Spannable spannable) {
			final TextView t = findViewById( R.id.textViewContent );
			runOnUiThread( ()->{
				t.setText( spannable );
				int scrollPos = getIntent().getIntExtra( "scroll_position", -1 );
				if ( scrollPos == -1 ) {
					if ( mEntry.getProperties().isSaveLastPos() ) {
						mScrollView.post( ()->{
							ObjectAnimator.ofInt( mScrollView, "scrollY", mEntry.getProperties().getScrollPosition() ).setDuration( SCROLL_ANIMATION_SPEED ).start();
							//mScrollView.smoothScrollTo( 0, mEntry.getProperties().getScrollPosition());
						} );
					}
				} else {
					mScrollView.post( ()->{
						ObjectAnimator.ofInt( mScrollView, "scrollY", scrollPos ).setDuration( SCROLL_ANIMATION_SPEED ).start();
						//mScrollView.smoothScrollTo( 0, scrollPos );
					} );
				}
				mProgressDialog.dismiss();
			} );
		}

		@Override
		public void exceptionOccurred(Exception e) {
			Utils.getErrorDialog( e, EntryViewer.this ).show();
		}
	};

	private void hideUpButton() {
		FloatingActionButton fab = findViewById( R.id.fabUpView );
		fab.animate().setDuration( 500 ).scaleX( 0 ).scaleY( 0 ).start();
		fab.setEnabled( false );
	}

	private void showUpButton() {
		FloatingActionButton fab = findViewById( R.id.fabUpView );
		fab.animate().setDuration( 500 ).scaleX( 1 ).scaleY( 1 ).start();
		fab.setEnabled( true );
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_view_entry );
		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );
		Intent intent = getIntent();

		Optional<EntryEntity> op = EntitiesStorage.get().getEntry( intent.getStringExtra( "id" ) );
		if ( op.isPresent() ) {
			mEntry = op.get();
		} else {
			Toast.makeText( this, "Entry not found", Toast.LENGTH_SHORT ).show();
			onBackPressed();
			return;
		}

		applyTheme();
		invalidateOptionsMenu();

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		TextView textView = findViewById( R.id.textViewContent );
		textView.setTextSize( TypedValue.COMPLEX_UNIT_DIP, mEntry.getProperties().getTextSize() );
		textView.setTextColor( mEntry.getProperties().getDefaultTextColor() );
		//textView.setMovementMethod( LinkMovementMethod.getInstance() );

		mScrollView = findViewById( R.id.viewEntryScrollView );
		mScrollView.setOnScrollChangeListener( new View.OnScrollChangeListener() {
			@Override
			public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
				if ( oldScrollY > scrollY && scrollY > 5 ) {
					showUpButton();
				} else if ( scrollY <= 5 || oldScrollY < scrollY ) {
					hideUpButton();
				}
				mEntry.getProperties().setScrollPosition( mScrollView.getScrollY() );
				try {
					mEntry.saveProperties();
				} catch (Exception e) {
					e.printStackTrace();
					Log.i( TAG, "onScrollChange: " + e );
				}
			}
		} );
		mScrollView.setOnTouchListener( new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if ( event.getAction() == MotionEvent.ACTION_DOWN ) {
					if ( mScrollAnimator != null ) {
						mScrollAnimator.cancel();
					}
				} else if ( event.getAction() == MotionEvent.ACTION_UP ) {
					startScroll( mScrollSpeed );
				}
				mEntry.getProperties().setScrollPosition( mScrollView.getScrollY() ); // just remember
				return false;
			}
		} );
		findViewById( R.id.fabUpView ).setOnClickListener( v->mScrollView.smoothScrollTo( 0, 0 ) );

		setupSpeedSeekBar();
		findViewById( R.id.speed_button_close ).setOnClickListener( view->{
			findViewById( R.id.speedLayout ).setVisibility( View.GONE );
			startScroll( 0 );
		} );

		getWindow().getDecorView().setBackgroundColor( mEntry.getProperties().getBgColor() );

		final Thread loadThread = new Thread( ()->{
			try {
				mCallback.loaded( mEntry.loadText( Utils.getScreenSize().x ) );
			} catch (IOException | JSONException e) {
				e.printStackTrace();
				mCallback.exceptionOccurred( e );
			}
		} );
		mProgressDialog = new ProgressDialog( this );
		mProgressDialog.setTitle( R.string.loading );
		mProgressDialog.setMessage( getResources().getString( R.string.entry_is_loading ) );
		mProgressDialog.setCancelable( false );
		mProgressDialog.setButton( ProgressDialog.BUTTON_NEUTRAL,
				getResources().getString( R.string.cancel ),
				(dialog, which)->{
					loadThread.interrupt();
					dialog.cancel();
					onBackPressed();
				} );
		mProgressDialog.show();
		loadThread.start();
	}
}