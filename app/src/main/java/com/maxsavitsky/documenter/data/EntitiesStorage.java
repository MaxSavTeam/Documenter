package com.maxsavitsky.documenter.data;

import android.util.Log;

import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.EntryEntity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EntitiesStorage {

	private static final String TAG = MainActivity.TAG + " EntitiesStorage";
	private static EntitiesStorage instance;

	public static EntitiesStorage get() {
		if ( instance == null ) {
			instance = new EntitiesStorage();
		}
		return instance;
	}

	private ArrayList<Group> mGroups = new ArrayList<>();
	private ArrayList<EntryEntity> mEntryEntities = new ArrayList<>();

	public void setGroups(ArrayList<Group> groups) {
		mGroups = groups;
	}

	public void setEntryEntities(ArrayList<EntryEntity> entryEntities) {
		mEntryEntities = entryEntities;
	}

	public Optional<Group> getGroup(String id) {
		for (Group g : mGroups)
			if ( g.getId().equals( id ) ) {
				return Optional.of( g );
			}
		return Optional.empty();
	}

	public Optional<EntryEntity> getEntry(String id) {
		for (var e : mEntryEntities) {
			if ( e.getId().equals( id ) ) {
				return Optional.of( e );
			}
		}
		return Optional.empty();
	}

	public boolean isGroupNameExists(String name) {
		return mGroups.stream().anyMatch( g->g.getName().equals( name ) );
	}

	public boolean isEntryNameExists(String name) {
		return mEntryEntities.stream().anyMatch( e->e.getName().equals( name ) );
	}

	public synchronized void save() {
		JSONObject result = new JSONObject();

		String outString;

		try {
			result.put( "groups", describeEntities( mGroups ) );
			result.put( "entries", describeEntities( mEntryEntities ) );

			JSONObject groupsContent = new JSONObject();
			for (Group group : mGroups) {
				if ( group.getContainingEntities().size() > 0 ) {
					JSONArray jsonArray = new JSONArray();
					for (Entity entity : group.getContainingEntities()) {
						jsonArray.put( entity.getId() );
					}
					groupsContent.put( group.getId(), jsonArray );
				}
			}
			result.put( "groupsContent", groupsContent );

			outString = result.toString();
		} catch (JSONException e) {
			Log.i( TAG, "save: " + e );
			return;
		}

		FileOutputStream fos = null;
		File file = new File( Utils.getExternalStoragePath().getPath() + "/data.json" );
		try {
			if ( !file.exists() ) {
				file.createNewFile();
			}
			fos = new FileOutputStream( file );
			fos.write( outString.getBytes( StandardCharsets.UTF_8 ) );
			fos.flush();
			fos.close();
		} catch (IOException e) {
			Log.i( TAG, "save: " + e );

			if ( fos != null ) {
				try {
					fos.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	private JSONArray describeEntities(ArrayList<? extends Entity> entities) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (Entity entity : entities) {
			jsonArray.put(
					new JSONObject()
							.put( "id", entity.getId() )
							.put( "name", entity.getName() )
							.put( "timestamp", entity.getCreationTimestamp() )
			);
		}
		return jsonArray;
	}

	public boolean addEntityTo(Entity e, String groupId) {
		Optional<Group> op = getGroup( groupId );
		if ( !op.isPresent() ) {
			return false;
		}
		Group g = op.get();
		if ( g.addMember( e ) ) {
			save();
			return true;
		}
		return false;
	}

	public boolean moveEntityTo(Entity e, String groupId) {
		Optional<Group> op = getGroup( groupId );
		if ( !op.isPresent() ) {
			return false;
		}
		Group g = op.get();
		if ( g.addMember( e ) ) {
			ArrayList<String> parents = new ArrayList<>( e.getParents() );
			for (String p : parents) {
				if ( !p.equals( g.getId() ) ) {
					getGroup( p ).ifPresent( group->group.removeContainingEntity( e.getId() ) );
					e.removeParent( p );
				}
			}
			save();
			return true;
		}
		return false;
	}

	public void removeEntityFrom(Entity entity, Group parentGroup){
		if(parentGroup.getId().equals( "root" ) && entity.getParents().size() == 1 && entity.getParents().get( 0 ).equals( "root" ))
			return;
		parentGroup.removeContainingEntity( entity.getId() );
		entity.removeParent( parentGroup.getId() );
		if(entity.getParents().isEmpty()){
			getGroup( "root" )
					.ifPresent( g->{
						if(g.addMember( entity ))
							entity.addParent( g.getId() );
					} );
		}
		save();
	}

	public void createGroup(String name, String parentId) {
		String id = Utils.generateUniqueId() + "1";
		Group g = new Group( id, name );
		mGroups.add( g );
		getGroup( parentId ).ifPresent( group->{
			group.addMember( g );
			g.addParent( parentId );
		} );
		save();
	}

	public EntryEntity createEntry(String name, String parentId){
		String id = Utils.generateUniqueId() + "0";
		EntryEntity e = new EntryEntity( id, name );
		mEntryEntities.add( e );
		getGroup( parentId )
				.ifPresent( g->{
					if(g.addMember( e ))
						e.addParent( g.getId() );
				} );
		save();
		return e;
	}

	public void deleteEntity(String id) {
		for (Group g : mGroups) {
			if ( g.getId().equals( id ) ) {
				for (var e : g.getContainingEntities())
					e.removeParent( id );
				for (String p : g.getParents()) {
					getGroup( p ).ifPresent( group->group.removeContainingEntity( id ) );
				}
				mGroups.remove( g );
				break;
			}
		}
		for (EntryEntity e : mEntryEntities) {
			if ( e.getId().equals( id ) ) {
				for (String p : e.getParents()) {
					getGroup( p ).ifPresent( group->group.removeContainingEntity( id ) );
				}
				deleteEntry( e );
				break;
			}
		}
		save();
	}

	private void deleteEntry(EntryEntity e) {
		Utils.deleteDirectory( new File( App.appDataPath + "/entries/" + e.getId() ) );
		mEntryEntities.remove( e );
	}

	/**
	 * @param mode Mode of deletion. Can be 0 (delete group and members, which included only in that group),
	 *             1 (delete group and all members),
	 *             2 (delete group and move members to upper level),
	 *             3 (delete group and move members to the root (unlink))
	 */
	public void deleteGroup(String id, int mode) {
		Optional<Group> op = getGroup( id );
		if ( !op.isPresent() ) {
			return;
		}
		Group g = op.get();
		Map<String, Boolean> map = new HashMap<>();
		indexGroupMembers( g, map );
		for (Entity m : g.getContainingEntities()) {
			m.removeParent( g.getId() );
			if ( mode == 1 || mode == 0 && checkEntityBeforeDeletion( m, map ) ) {
				if ( m instanceof EntryEntity ) {
					deleteEntry( (EntryEntity) m );
				} else if ( m instanceof Group ) {
					deleteGroupInternal( (Group) m, mode, map );
				}
			}
			if ( mode == 2 ) {
				for (String p : g.getParents()) {
					if ( addEntityTo( m, p ) ) {
						m.addParent( p );
					}
				}
			}
			if ( mode == 3 && m.getParents().size() == 0 ) {
				addEntityTo( m, "root" );
				m.addParent( "root" );
			}
		}
		g.unlink();
		mGroups.remove( g );

		save();
	}

	private void deleteGroupInternal(Group g, int mode, Map<String, Boolean> map) {
		g.unlink();
		for (Entity e : g.getContainingEntities()) {
			if ( mode == 1 || mode == 0 && checkEntityBeforeDeletion( e, map ) ) {
				if ( e instanceof Group ) {
					deleteGroupInternal( (Group) e, mode, map );
				} else if ( e instanceof EntryEntity ) {
					deleteEntry( (EntryEntity) e );
				}
			}
		}
		mGroups.remove( g );
	}

	private boolean checkEntityBeforeDeletion(Entity e, Map<String, Boolean> map) {
		boolean f = true;
		for (String p : e.getParents()) {
			Boolean b = map.getOrDefault( p, false );
			f &= b != null && b;
		}
		return f;
	}

	private void indexGroupMembers(Group g, Map<String, Boolean> map) {
		map.put( g.getId(), true );
		for (Entity e : g.getContainingEntities()) {
			if ( e instanceof Group ) {
				Boolean b = map.getOrDefault( e.getId(), false );
				if ( b != null && !b ) {
					indexGroupMembers( (Group) e, map );
				}
			} else {
				map.put( e.getId(), true );
			}
		}
	}

}
