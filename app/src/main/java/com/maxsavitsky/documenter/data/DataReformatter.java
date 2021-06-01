package com.maxsavitsky.documenter.data;

import com.maxsavitsky.documenter.data.types.EntryEntity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.utils.Utils;
import com.maxsavitsky.documenter.xml.handlers.InfoHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class DataReformatter {

	public static JSONObject runReformat() throws Exception {
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

		ArrayList<EntryEntity> entriesList = new ArrayList<>();
		File file = new File( Utils.getExternalStoragePath().getPath() + "/entries.xml" );
		if ( file.exists() ) {
			EntityHandler entityHandler = new EntityHandler( "entry" );
			saxParser.parse( file, entityHandler );
			for (Entity entity : entityHandler.getEntities())
				entriesList.add( new EntryEntity( entity.getId(), entity.getName() ) );
		}
		for (EntryEntity e : entriesList) {
			File infoFile = new File( Utils.getExternalStoragePath() + "/entries/" + e.getId() + "/info.xml" );
			if ( infoFile.exists() ) {
				InfoHandler infoHandler = new InfoHandler();
				saxParser.parse( infoFile, infoHandler );
				e.setCreationTimestamp( infoHandler.getInfo().getTimeStamp() );
			}
		}

		ArrayList<Group> documentsList = new ArrayList<>();
		file = new File( Utils.getExternalStoragePath().getPath() + "/documents.xml" );
		if ( file.exists() ) {
			EntityHandler entityHandler = new EntityHandler( "document" );
			saxParser.parse( file, entityHandler );
			for (Entity entity : entityHandler.getEntities())
				documentsList.add( new Group( entity.getId(), entity.getName() ) );
		}

		for (Group document : documentsList) {
			File infoFile = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + document.getId() + "/info.xml" );
			if ( infoFile.exists() ) {
				InfoHandler infoHandler = new InfoHandler();
				saxParser.parse( infoFile, infoHandler );
				document.setCreationTimestamp( infoHandler.getInfo().getTimeStamp() );
			}

			File includedEntriesFile = new File( Utils.getExternalStoragePath().getPath() + "/documents/" + document.getId() + "/" + document.getId() + ".xml" );
			if ( includedEntriesFile.exists() ) {
				EntityHandler entityHandler = new EntityHandler( "entry" );
				saxParser.parse( includedEntriesFile, entityHandler );
				ArrayList<EntryEntity> includedEntries = new ArrayList<>();
				for (Entity p : entityHandler.getEntities()) {
					for (EntryEntity entryEntity : entriesList) {
						if ( entryEntity.getId().equals( p.getId() ) ) {
							includedEntries.add( entryEntity );
						}
					}
				}
				document.setContainingEntities( includedEntries );
			}
		}

		ArrayList<Group> categoriesList = new ArrayList<>();
		file = new File( Utils.getExternalStoragePath().getPath() + "/categories.xml" );
		if ( file.exists() ) {
			EntityHandler entityHandler = new EntityHandler( "category" );
			saxParser.parse( file, entityHandler );
			for (Entity p : entityHandler.getEntities()) {
				categoriesList.add( new Group( p.getId(), p.getName() ) );
			}
		}

		for (Group category : categoriesList) {
			File infoFile = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + category.getId() + "/info.xml" );
			if ( infoFile.exists() ) {
				InfoHandler infoHandler = new InfoHandler();
				saxParser.parse( infoFile, infoHandler );
				category.setCreationTimestamp( infoHandler.getInfo().getTimeStamp() );
			}

			File includedDocsFile = new File( Utils.getExternalStoragePath().getPath() + "/categories/" + category.getId() + "/" + category.getId() + ".xml" );
			if ( includedDocsFile.exists() ) {
				EntityHandler entityHandler = new EntityHandler( "document" );
				saxParser.parse( includedDocsFile, entityHandler );
				ArrayList<Group> includedEntries = new ArrayList<>();
				for (Entity p : entityHandler.getEntities()) {
					for (Group entity : documentsList) {
						if ( entity.getId().equals( p.getId() ) ) {
							includedEntries.add( entity );
						}
					}
				}
				category.setContainingEntities( includedEntries );
			}
		}

		JSONObject jsonObject = new JSONObject();

		ArrayList<Group> allGroups = new ArrayList<>();
		allGroups.addAll( documentsList );
		allGroups.addAll( categoriesList );
		jsonObject.put( "groups", getDescriptionList( allGroups ) );

		jsonObject.put( "entries", getDescriptionList( entriesList ) );

		JSONObject groupsContent = new JSONObject();
		for (Group group : allGroups) {
			if ( group.getContainingEntities().size() > 0 ) {
				JSONArray containing = new JSONArray();
				for (var e : group.getContainingEntities()) {
					containing.put( e.getId() );
				}
				groupsContent.put( group.getId(), containing );
			}
		}

		jsonObject.put( "groupsContent", groupsContent );
		return jsonObject;
	}

	private static JSONArray getDescriptionList(ArrayList<? extends com.maxsavitsky.documenter.data.types.Entity> entities) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (var e : entities) {
			jsonArray.put(
					new JSONObject()
							.put( "id", e.getId() )
							.put( "name", e.getName() )
							.put( "timestamp", e.getCreationTimestamp() )
			);
		}
		return jsonArray;
	}

	private static class EntityHandler extends DefaultHandler {
		private final String name;
		private final ArrayList<Entity> mEntities = new ArrayList<>();

		public EntityHandler(String name) {
			this.name = name;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if ( qName.equals( name ) ) {
				mEntities.add( new Entity( attributes.getValue( "id" ), attributes.getValue( "name" ) ) );
			}
		}

		public ArrayList<Entity> getEntities() {
			return mEntities;
		}
	}

	private static class Entity {
		private final String id, name;

		public Entity(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

}
