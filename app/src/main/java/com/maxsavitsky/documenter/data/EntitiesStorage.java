package com.maxsavitsky.documenter.data;

import android.util.Log;

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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public ArrayList<? extends Entity> getRootEntities() {
		return Stream.concat(
				mGroups.stream()
						.filter( Entity::isRoot ),
				mEntryEntities.stream()
						.filter( Entity::isRoot ) )
				.collect( Collectors.toCollection( ArrayList::new ) );
	}

	public boolean isGroupNameExists(String name){
		return mGroups.stream().anyMatch( g->g.getName().equals( name ) );
	}

	public synchronized void save(){
		JSONObject result = new JSONObject();

		String outString;

		try {
			result.put( "groups", describeEntities( mGroups ) );
			result.put( "entries", describeEntities( mEntryEntities ) );

			JSONObject groupsContent = new JSONObject();
			for(Group group : mGroups){
				if(group.getContainingEntities().size() > 0){
					JSONArray jsonArray = new JSONArray();
					for(Entity entity : group.getContainingEntities()){
						jsonArray.put( entity.getId() );
					}
					groupsContent.put( group.getId(), jsonArray );
				}
			}
			result.put( "groupsContent", groupsContent );

			outString = result.toString();
		}catch (JSONException e){
			Log.i( TAG, "save: " + e );
			return;
		}

		FileOutputStream fos = null;
		File file = new File(Utils.getExternalStoragePath().getPath() + "/data.json"  );
		try{
			if(!file.exists())
				file.createNewFile();
			fos = new FileOutputStream( file );
			fos.write( outString.getBytes( StandardCharsets.UTF_8 ) );
			fos.flush();
			fos.close();
		}catch (IOException e){
			Log.i( TAG, "save: " + e );

			if(fos != null) {
				try {
					fos.close();
				} catch (IOException ignored) {}
			}
		}
	}

	private JSONArray describeEntities(ArrayList<? extends Entity> entities) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for(Entity entity : entities){
			jsonArray.put(
					new JSONObject()
							.put( "id", entity.getId() )
							.put( "name", entity.getName() )
					.put( "timestamp", entity.getCreationTimestamp() )
			);
		}
		return jsonArray;
	}

}
