package com.maxsavitsky.documenter.ui;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.maxsavitsky.documenter.BuildConfig;
import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.adapters.EntitiesAdapter;
import com.maxsavitsky.documenter.codes.Results;
import com.maxsavitsky.documenter.data.EntitiesStorage;
import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.ui.widget.CustomRadioGroup;
import com.maxsavitsky.documenter.ui.widget.FabButton;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

public class EntitiesListActivity extends ThemeActivity {

	private static final String TAG = MainActivity.TAG + " EntitiesList";
	protected boolean isRoot;

	protected Group mGroup;
	protected Group mParentGroup;

	protected ArrayList<? extends Entity> mEntities;

	private boolean selectionMode = false;

	private int mSortOrder = 0, mSortingMode = 0, mAscendingDescendingOrder = 0;

	protected EntitiesAdapter mEntitiesAdapter;

	private final ArrayList<Entity> mSelectedEntities = new ArrayList<>();

	private int mFlexboxLayoutHeight;

	private final ActivityResultLauncher<Intent> mEntitiesListLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Results.NEED_TO_REFRESH ) {
					refresh();
				}
			}
	);

	private final ActivityResultLauncher<Intent> mCreateGroupLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Results.NEED_TO_REFRESH ) {
					refresh();
				}
			}
	);

	private ActivityResultLauncher<Intent> mEntryViewerLauncher;

	{
		mEntryViewerLauncher = registerForActivityResult(
				new ActivityResultContracts.StartActivityForResult(),
				result->{
					if ( result.getResultCode() == Results.NEED_TO_REFRESH ) {
						refresh();
					}
					if ( result.getResultCode() == Results.REOPEN ) {
						Intent intent = new Intent( this, EntryViewer.class );
						if ( result.getData() != null ) {
							intent.putExtras( result.getData() );
						}
						mEntryViewerLauncher.launch( intent );
					}
				}
		);
	}

	private final ActivityResultLauncher<Intent> mCreateEntryLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result -> {
				if ( result.getResultCode() == Results.REOPEN ) {
					Intent intent = new Intent( this, EntryViewer.class );
					if ( result.getData() != null ) {
						intent.putExtras( result.getData() );
					}
					mEntryViewerLauncher.launch( intent );
				}
			}
	);

	private final ActivityResultLauncher<Intent> mSettingsLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(),
			result->{
				if ( result.getResultCode() == Results.RESTART_APP ) {
					setResult( Results.RESTART_APP );
					onBackPressed();
				}
			}
	);

	private final BroadcastReceiver mNeedToRefreshReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refresh();
		}
	};

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
						boolean success = true;
						int mode = data.getIntExtra( "mode", 0 );
						for(Entity e : mSelectedEntities) {
							if ( mode == 0 ) {
								success &= EntitiesStorage.get().addEntityTo( e, groupId );
							} else {
								success &= EntitiesStorage.get().moveEntityTo( e, groupId );
							}
						}
						if ( success ) {
							Toast.makeText( this, R.string.success, Toast.LENGTH_SHORT ).show();
						} else {
							if ( mSelectedEntities.size() == 1 )
								Toast.makeText( this, R.string.move_add_fail_reason, Toast.LENGTH_LONG ).show();
							else {
								Toast.makeText( this, R.string.some_entities_were_not_moved_added, Toast.LENGTH_LONG ).show();
							}
						}
						sendRefreshIntent();
					}
					if( selectionMode )
						exitSelectionMode();
					mSelectedEntities.clear();
				}
			}
	);

	private final EntitiesAdapter.AdapterCallback ADAPTER_CALLBACK = new EntitiesAdapter.AdapterCallback() {
		@Override
		public void onEntityClick(String id, Entity.Type type, int index) {
			if( selectionMode ){
				boolean contains = mSelectedEntities.contains( mEntities.get( index ) );
				if(contains)
					mSelectedEntities.remove( mEntities.get( index ) );
				else
					mSelectedEntities.add( mEntities.get( index ) );
				mEntitiesAdapter.setCheckBox( index, !contains );
				CheckBox checkBox = findViewById( R.id.checkbox_toggle_all );
				checkBox.setChecked( mSelectedEntities.size() == mEntities.size() );
			}else {
				if ( type == Entity.Type.GROUP ) {
					mEntitiesListLauncher.launch( new Intent( EntitiesListActivity.this, EntitiesListActivity.class ).putExtra( "groupId", id ).putExtra( "parentId", mGroup.getId() ) );
				} else {
					mEntryViewerLauncher.launch( new Intent( EntitiesListActivity.this, EntryViewer.class ).putExtra( "id", id ).putExtra( "parentId", mGroup.getId() ) );
				}
			}
		}

		@Override
		public void onLongClick(int index) {
			if(!selectionMode ) {
				TransitionManager.beginDelayedTransition( (ViewGroup) getWindow().getDecorView() );
				mEntitiesAdapter.showCheckBoxes();
				selectionMode = true;

				mEntitiesAdapter.setCheckBox( index, true );
				mSelectedEntities.add( mEntities.get( index ) );

				findViewById( R.id.flexboxLayout ).setVisibility( View.VISIBLE );
				findViewById( R.id.fab_menu ).setVisibility( View.GONE );

				((CheckBox) findViewById( R.id.checkbox_toggle_all )).setChecked( mEntities.size() == 1 );
			}
		}

		@Override
		public View getViewAt(int position) {
			return ((RecyclerView) findViewById( R.id.recycler_view_entities )).getLayoutManager().findViewByPosition( position );
		}
	};

	@Override
	public void onBackPressed() {
		if( selectionMode ){
			exitSelectionMode();
		}else{
			super.onBackPressed();
		}
	}

	private void exitSelectionMode(){
		selectionMode = false;
		TransitionManager.beginDelayedTransition( (ViewGroup) getWindow().getDecorView() );
		mEntitiesAdapter.hideCheckBoxes();
		findViewById( R.id.flexboxLayout ).setVisibility( View.GONE );
		findViewById( R.id.fab_menu ).setVisibility( View.VISIBLE );
		mSelectedEntities.clear();
	}

	private void refresh(){
		sortAndUpdateList();
		invalidateOptionsMenu();
	}

	@Override
	protected void onDestroy() {
		try {
			unregisterReceiver( mNeedToRefreshReceiver );
		}catch (IllegalArgumentException e){
			Log.i( TAG, "onDestroy: " + e );
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		int itemId = item.getItemId();
		if ( itemId == android.R.id.home ) {
			onBackPressed();
		} else if ( itemId == R.id.item_menu_sort_mode ) {
			showSortOptionsDialog();
		} else if ( itemId == R.id.item_menu_change_name ) {
			AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
					.setTitle( R.string.edit_name );
			final EditText editText = new EditText( this );
			editText.setText( mGroup.getName() );
			editText.append( "" );
			editText.setTextColor( getColor( super.mTextColor ) );
			editText.requestFocus();
			ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
			editText.setLayoutParams( layoutParams );
			builder.setView( editText ).setPositiveButton( "OK", (dialog, which)->{
				String text = editText.getText().toString();
				text = text.trim();
				if ( !text.isEmpty() && !text.equals( mGroup.getName() ) ) {
					if ( EntitiesStorage.get().isGroupNameExists( text ) ) {
						Toast.makeText( this, R.string.this_name_already_exist, Toast.LENGTH_SHORT ).show();
						return;
					}
					mGroup.rename( text );
					setTitle( text );
					sendRefreshIntent();
					EntitiesStorage.get().save();
				} else {
					Toast.makeText( this, R.string.invalid_name, Toast.LENGTH_SHORT ).show();
				}
				dialog.cancel();
			} ).setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() ).setCancelable( false );
			builder.show();
		} else if ( itemId == R.id.item_menu_delete ) {
			showDeletionDialog();
		} else if ( itemId == R.id.item_menu_add_to ) {
			mSelectedEntities.add( mGroup );
			openCopyMoveActivity( 0 );
		} else if ( itemId == R.id.item_menu_move_to ) {
			mSelectedEntities.add( mGroup );
			openCopyMoveActivity( 1 );
		} else if(itemId == R.id.item_menu_remove_from_parent){
			mGroup.removeParent( mParentGroup.getId() );
			mParentGroup.removeContainingEntity( mGroup.getId() );
			if(mGroup.getParents().size() == 0){
				EntitiesStorage.get().addEntityTo( mGroup, "root" );
			}
			EntitiesStorage.get().save();
			sendRefreshIntent();
		}
		return super.onOptionsItemSelected( item );
	}

	private void openCopyMoveActivity(int mode){
		mCopyMoveLauncher.launch(
				new Intent( this, CopyMoveActivity.class )
						.putExtra( "mode", mode )
		);
	}

	private void sendRefreshIntent(){
		sendBroadcast( new Intent( BuildConfig.APPLICATION_ID + ".REFRESH_ENTITIES_LISTS" ) );
	}

	private void showDeletionDialog() {
		View view = getLayoutInflater().inflate( R.layout.group_deletion_menu, null );
		CustomRadioGroup radioGroup = view.findViewById( R.id.radio_group_delete );
		radioGroup.setCheckedIndex( 0 );

		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle );
		builder
				.setView( view )
				.setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() )
				.setPositiveButton( "OK", (dialog, which)->{
					int deletionMode = radioGroup.getCheckedItemIndex();
					EntitiesStorage.get().deleteGroup( mGroup.getId(), deletionMode );
					sendRefreshIntent();
					onBackPressed();
				} )
				.setCancelable( false );
		builder.show();
	}

	private void showSortOptionsDialog() {
		View view = LayoutInflater.from( this ).inflate( R.layout.sort_menu, null );
		CustomRadioGroup radioOrder = view.findViewById( R.id.radio_group_order );
		radioOrder.setCheckedIndex( mSortOrder );
		CustomRadioGroup radioSorting = view.findViewById( R.id.radio_group_sorting );
		radioSorting.setCheckedIndex( mSortingMode );
		CustomRadioGroup radioAscendingOrder = view.findViewById( R.id.radio_group_ascending_descending );
		radioAscendingOrder.setCheckedIndex( mAscendingDescendingOrder );
		AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle );
		builder
				.setCancelable( false )
				.setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() )
				.setPositiveButton( R.string.apply, (dialog, which)->{
					mSortOrder = radioOrder.getCheckedItemIndex();
					mSortingMode = radioSorting.getCheckedItemIndex();
					mAscendingDescendingOrder = radioAscendingOrder.getCheckedItemIndex();
					sortAndUpdateList();
				} )
				.setView( view );
		builder.show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate( R.menu.entities_list_menu, menu );
		if ( isRoot ) {
			for(int i = 0; i < menu.size(); i++)
				menu.getItem( i ).setVisible( false );
			menu.findItem( R.id.item_menu_sort_mode ).setVisible( true );
		}
		if(mParentGroup != null && mParentGroup.getId().equals( "root" ) && mGroup.getParents().size() == 1)
			menu.findItem( R.id.item_menu_remove_from_parent ).setVisible( false );
		return super.onCreateOptionsMenu( menu );
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_entities_list );

		Toolbar toolbar = findViewById( R.id.toolbar );
		setSupportActionBar( toolbar );

		String groupId = getIntent().getStringExtra( "groupId" );
		if ( groupId == null ) {
			groupId = "root";
		}
		isRoot = groupId.equals( "root" );

		EntitiesStorage.get().getGroup( getIntent().getStringExtra( "parentId" ) ).ifPresent( g->mParentGroup = g );

		ActionBar actionBar = getSupportActionBar();
		if ( actionBar != null ) {
			actionBar.setDisplayHomeAsUpEnabled( !isRoot );
		}

		Optional<Group> op = EntitiesStorage.get().getGroup( groupId );
		if ( !op.isPresent() ) {
			Toast.makeText( this, "Group not found", Toast.LENGTH_SHORT ).show();
			onBackPressed();
			return;
		}
		mGroup = op.get();

		if(!isRoot)
			setTitle( mGroup.getName() );

		initializeRecyclerView( mGroup.getContainingEntities() );
		sortAndUpdateList();

		FabButton fab = findViewById( R.id.fabSettings );
		fab.setOnClickListener( v->{
			mSettingsLauncher.launch( new Intent( this, SettingsActivity.class ) );
		} );

		findViewById( R.id.fabCreateGroup ).setOnClickListener( v->{
			Intent intent = new Intent( this, CreateGroupActivity.class );
			intent.putExtra( "parentId", mGroup.getId() );
			mCreateGroupLauncher.launch( intent );
		} );

		findViewById( R.id.fabCreateEntry ).setOnClickListener( v->{
			Intent intent = new Intent(this, EntryEditor.class);
			intent.putExtra( "parentId", mGroup.getId() );
			intent.putExtra( "type", "create" );
			mCreateEntryLauncher.launch( intent );
		} );

		registerReceiver( mNeedToRefreshReceiver, new IntentFilter( BuildConfig.APPLICATION_ID + ".REFRESH_ENTITIES_LISTS" ) );

		findViewById( R.id.layout_add_to ).setOnClickListener( v->{
			if(mSelectedEntities.size() > 0){
				openCopyMoveActivity( 0 );
			}
		} );
		findViewById( R.id.layout_move_to ).setOnClickListener( v->{
			if(mSelectedEntities.size() > 0){
				openCopyMoveActivity( 1 );
			}
		} );
		findViewById( R.id.layout_remove ).setOnClickListener( v->{
			if(mSelectedEntities.size() > 0){
				for(Entity e : mSelectedEntities){
					EntitiesStorage.get().removeEntityFrom( e, mGroup );
				}
				exitSelectionMode();
				sendRefreshIntent();
			}
		} );
		findViewById( R.id.layout_delete ).setOnClickListener( v->{
			if(mSelectedEntities.size() > 0){
				AlertDialog.Builder deletionBuilder = new AlertDialog.Builder( this, super.mAlertDialogStyle )
						.setMessage( R.string.delete_confirmation_text )
						.setTitle( R.string.confirmation )
						.setPositiveButton( "OK", (dialog, which)->{
							dialog.cancel();
							prepareMultipleDeletion();
						} )
						.setNeutralButton( R.string.cancel, (dialog, which)->dialog.cancel() ).setCancelable( false );
				deletionBuilder.create().show();
			}
		} );

		CheckBox checkBox = findViewById( R.id.checkbox_toggle_all );
		checkBox.setOnClickListener( v->{
			boolean state = checkBox.isChecked();
			for(int i = 0; i < mEntitiesAdapter.getItemCount(); i++)
				mEntitiesAdapter.setCheckBox( i, state );
			mSelectedEntities.clear();
			if(state){
				mSelectedEntities.addAll(mEntities);
			}
		} );
	}

	private void prepareMultipleDeletion(){
		boolean areThereGroups = false;
		for(Entity e : mSelectedEntities){
			if(e instanceof Group){
				areThereGroups = true;
				break;
			}
		}
		if(areThereGroups){
			View view = getLayoutInflater().inflate( R.layout.group_deletion_menu, null );
			CustomRadioGroup radioGroup = view.findViewById( R.id.radio_group_delete );
			radioGroup.setCheckedIndex( 0 );

			LinearLayout layout = (LinearLayout) view;
			TextView textView = new TextView(this);
			textView.setText( R.string.multiple_deletion_note );
			MarginLayoutParams params = new MarginLayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT );
			params.topMargin = (int) TypedValue.applyDimension( TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics() );
			textView.setLayoutParams( params );

			TypedValue typedValue = new TypedValue();
			getTheme().resolveAttribute( R.attr.textColor, typedValue, true );
			textView.setTextColor( typedValue.data );
			textView.setTextSize( 18 );
			layout.addView(textView);

			AlertDialog.Builder builder = new AlertDialog.Builder( this, super.mAlertDialogStyle );
			builder
					.setView( layout )
					.setNegativeButton( R.string.cancel, (dialog, which)->dialog.cancel() )
					.setPositiveButton( "OK", (dialog, which)->{
						int deletionMode = radioGroup.getCheckedItemIndex();
						EntitiesListActivity.this.deleteMultipleEntities( deletionMode );
					} )
					.setCancelable( false );
			builder.show();
		}else{
			deleteMultipleEntities( 0 );
		}
	}

	private void deleteMultipleEntities(int groupDeletionMode){
		for(Entity e : mSelectedEntities){
			if(e instanceof Group){
				EntitiesStorage.get()
						.deleteGroup( e.getId(), groupDeletionMode );
			}else{
				EntitiesStorage.get()
						.deleteEntry( e.getId() );
			}
		}
		exitSelectionMode();
		mSelectedEntities.clear();
		sendRefreshIntent();
	}

	private void initializeRecyclerView(ArrayList<? extends Entity> entities) {
		RecyclerView recyclerView = findViewById( R.id.recycler_view_entities );
		LinearLayoutManager manager = new LinearLayoutManager( this );
		manager.setOrientation( RecyclerView.VERTICAL );
		recyclerView.setLayoutManager( manager );
		mEntitiesAdapter = new EntitiesAdapter( entities, ADAPTER_CALLBACK );
		recyclerView.setAdapter( mEntitiesAdapter );
	}

	private void sortAndUpdateList() {
		ArrayList<? extends Entity> entities;
		entities = mGroup.getContainingEntities();
		entities.sort( (Comparator<Entity>) (o1, o2)->{
			if ( o1.getType() != o2.getType() ) {
				if ( mSortOrder == 0 ) {
					return o1.getType() == Entity.Type.GROUP ? -1 : 1;
				} else if ( mSortOrder == 1 ) {
					return o1.getType() == Entity.Type.GROUP ? 1 : -1;
				}
			}
			int comp;
			if ( mSortingMode == 0 ) {
				comp = o1.getName().compareToIgnoreCase( o2.getName() );
			} else {
				comp = Long.compare( o1.getCreationTimestamp(), o2.getCreationTimestamp() );
			}
			if ( mAscendingDescendingOrder == 1 ) {
				comp = -comp;
			}
			return comp;
		} );
		updateList( entities );
	}

	private void updateList(ArrayList<? extends Entity> entities) {
		mEntitiesAdapter.setElements( entities );
		mEntities = entities;
	}
}