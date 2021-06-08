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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.ui.widget.CustomScrollView;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONException;

import java.io.IOException;
import java.util.Optional;

public class EntryViewer extends ThemeActivity {

	private static final String TAG = MainActivity.TAG + " EntryViewer";
	private Entry mEntry;
	private Group mParentGroup;
	private SharedPreferences sp;
	private CustomScrollView mScrollView;

	private final ActivityResultLauncher<Intent> mCopyMoveLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Results.ACCEPTED ) {
					Intent data = result.getData();
					if ( data == null ) {
						return;
					}
					String groupId = data.getStringExtra( "groupId" );
					if ( groupId != null ) {
						if ( data.getIntExtra( "mode", 0 ) == 0 ) {
							if ( EntitiesStorage.get().addEntityTo( mEntry, groupId ) ) {
								Toast.makeText( this, R.string.success, Toast.LENGTH_SHORT ).show();
							} else {
								Toast.makeText( this, R.string.move_add_fail_reason, Toast.LENGTH_LONG ).show();
							}
						} else {
							if ( EntitiesStorage.get().moveEntityTo( mEntry, groupId ) ) {
								Toast.makeText( this, R.string.success, Toast.LENGTH_SHORT ).show();
							} else {
								Toast.makeText( this, R.string.move_add_fail_reason, Toast.LENGTH_LONG ).show();
							}
						}
					}
				}
			}
	);

	private final ActivityResultLauncher<Intent> mEntryEditorLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Results.REOPEN ) {
					setResult( result.getResultCode(), result.getData() );
					onBackPressed();
				} else if ( result.getResultCode() == Results.OK ) {
					if ( result.getData() != null ) {
						int scrollPos = result.getData().getIntExtra( "scroll_position", -1 );
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
	);

	private final ActivityResultLauncher<Intent> mChooseEntryLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if(result.getResultCode() == Results.ACCEPTED && result.getData() != null){
					String id = result.getData().getStringExtra( "id" );
					Intent intent = new Intent(this, EntryEditor.class);
					intent.putExtra( "type", "copy" );
					intent.putExtra( "from_id", mEntry.getId() );
					intent.putExtra( "to_id", id );
					mEntryEditorLauncher.launch( intent );
				}
			}
	);

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
			AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
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
			mEntryEditorLauncher.launch( intent );
		} else if ( itemId == R.id.item_copy_content ) {
			prepareCopyToLayout();
		} else if ( itemId == R.id.item_menu_add_to ) {
			mCopyMoveLauncher.launch(
					new Intent( this, CopyMoveActivity.class )
							.putExtra( "mode", 0 )
			);
		} else if ( itemId == R.id.item_menu_move_to ) {
			mCopyMoveLauncher.launch(
					new Intent( this, CopyMoveActivity.class )
							.putExtra( "mode", 1 )
			);
		} else if ( itemId == R.id.item_menu_remove_from_parent ) {
			mEntry.removeParent( mParentGroup.getId() );
			mParentGroup.removeContainingEntity( mEntry.getId() );
			if ( mEntry.getParents().size() == 0 ) {
				EntitiesStorage.get().addEntityTo( mEntry, "root" );
			}
			EntitiesStorage.get().save();
			sendBroadcast( new Intent( BuildConfig.APPLICATION_ID + ".REFRESH_ENTITIES_LISTS" ) );
		} else if(itemId == R.id.item_debug_show_id){
			Toast.makeText( this, mEntry.getId(), Toast.LENGTH_SHORT ).show();
		}
		return super.onOptionsItemSelected( item );
	}

	private void prepareCopyToLayout() {
		mChooseEntryLauncher.launch( new Intent( this, ChooseEntryActivity.class ) );
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
			int r = ( mScrollView.getMaxVerticalScroll() - mScrollView.getScrollY() ) / speed * 20;
			mScrollAnimator.setDuration( r );
			mScrollAnimator.setInterpolator( new LinearInterpolator() );
			mScrollAnimator.start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.entry_menu, menu );
		if ( mParentGroup == null || mParentGroup.getId().equals( "root" ) && mEntry.getParents().size() == 1 ) {
			menu.findItem( R.id.item_menu_remove_from_parent ).setVisible( false );
		}
		menu.findItem( R.id.item_debug_show_id ).setVisible( BuildConfig.DEBUG );
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

		Optional<Entry> op = EntitiesStorage.get().getEntry( intent.getStringExtra( "id" ) );
		if ( op.isPresent() ) {
			mEntry = op.get();
		} else {
			Toast.makeText( this, "Entry not found", Toast.LENGTH_SHORT ).show();
			onBackPressed();
			return;
		}

		EntitiesStorage.get().getGroup( getIntent().getStringExtra( "parentId" ) )
				.ifPresent( g->mParentGroup = g );

		applyTheme();
		invalidateOptionsMenu();

		sp = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );

		TextView textView = findViewById( R.id.textViewContent );
		textView.setTextSize( TypedValue.COMPLEX_UNIT_DIP, mEntry.getProperties().getTextSize() );
		textView.setTextColor( mEntry.getProperties().getDefaultTextColor() );

		mScrollView = findViewById( R.id.viewEntryScrollView );
		mScrollView.setOnScrollChangeListener( (v, scrollX, scrollY, oldScrollX, oldScrollY)->{
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
		} );
		mScrollView.setOnTouchListener( (v, event)->{
			if ( event.getAction() == MotionEvent.ACTION_DOWN ) {
				if ( mScrollAnimator != null ) {
					mScrollAnimator.cancel();
				}
			} else if ( event.getAction() == MotionEvent.ACTION_UP ) {
				startScroll( mScrollSpeed );
			}
			mEntry.getProperties().setScrollPosition( mScrollView.getScrollY() ); // just remember
			return false;
		} );
		findViewById( R.id.fabUpView ).setOnClickListener( v->mScrollView.smoothScrollTo( 0, 0 ) );

		setupSpeedSeekBar();
		findViewById( R.id.speed_button_close ).setOnClickListener( view->{
			findViewById( R.id.speedLayout ).setVisibility( View.GONE );
			startScroll( 0 );
		} );

		try {
			mEntry.loadProperties();
		} catch (IOException | JSONException e) {
			e.printStackTrace();
			Utils.getErrorDialog( e, this ).show();
		}

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