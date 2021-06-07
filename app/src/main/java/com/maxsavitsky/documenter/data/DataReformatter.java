package com.maxsavitsky.documenter.data;

import com.maxsavitsky.documenter.App;
import com.maxsavitsky.documenter.data.types.EntryEntity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavitsky.documenter.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class DataReformatter {

	private static JSONObject prepareInfoJSON() throws Exception {
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

		ArrayList<EntryEntity> entriesList = new ArrayList<>();
		File file = new File( App.appStoragePath, "entries.xml" );
		if ( file.exists() ) {
			EntityHandler entityHandler = new EntityHandler( "entry" );
			saxParser.parse( file, entityHandler );
			for (Entity entity : entityHandler.getEntities())
				entriesList.add( new EntryEntity( entity.getId(), entity.getName() ) );
		}
		for (EntryEntity e : entriesList) {
			File infoFile = new File( App.appStoragePath, "entries/" + e.getId() + "/info.xml" );
			if ( infoFile.exists() ) {
				TimestampHandler handler = new TimestampHandler();
				saxParser.parse( infoFile, handler );
				e.setCreationTimestamp( handler.timestamp );
			}
		}

		ArrayList<Group> documentsList = new ArrayList<>();
		file = new File( App.appStoragePath, "documents.xml" );
		if ( file.exists() ) {
			EntityHandler entityHandler = new EntityHandler( "document" );
			saxParser.parse( file, entityHandler );
			for (Entity entity : entityHandler.getEntities())
				documentsList.add( new Group( entity.getId(), entity.getName() ) );
		}

		for (Group document : documentsList) {
			File infoFile = new File( App.appStoragePath, "documents/" + document.getId() + "/info.xml" );
			if ( infoFile.exists() ) {
				TimestampHandler handler = new TimestampHandler();
				saxParser.parse( infoFile, handler );
				document.setCreationTimestamp( handler.timestamp );
			}

			File includedEntriesFile = new File( App.appStoragePath, "documents/" + document.getId() + "/" + document.getId() + ".xml" );
			if ( includedEntriesFile.exists() ) {
				EntityHandler entityHandler = new EntityHandler( "entry" );
				saxParser.parse( includedEntriesFile, entityHandler );
				ArrayList<EntryEntity> includedEntries = new ArrayList<>();
				for (Entity p : entityHandler.getEntities()) {
					for (EntryEntity entryEntity : entriesList) {
						if ( entryEntity.getId().equals( p.getId() ) ) {
							includedEntries.add( entryEntity );
							entryEntity.addParent( document.getId() );
						}
					}
				}
				document.setContainingEntities( includedEntries );
			}
		}

		ArrayList<Group> categoriesList = new ArrayList<>();
		file = new File( App.appStoragePath, "categories.xml" );
		if ( file.exists() ) {
			EntityHandler entityHandler = new EntityHandler( "category" );
			saxParser.parse( file, entityHandler );
			for (Entity p : entityHandler.getEntities()) {
				categoriesList.add( new Group( p.getId(), p.getName() ) );
			}
		}

		for (Group category : categoriesList) {
			File infoFile = new File( App.appStoragePath, "categories/" + category.getId() + "/info.xml" );
			if ( infoFile.exists() ) {
				TimestampHandler handler = new TimestampHandler();
				saxParser.parse( infoFile, handler );
				category.setCreationTimestamp( handler.timestamp );
			}

			File includedDocsFile = new File( App.appStoragePath, "categories/" + category.getId() + "/" + category.getId() + ".xml" );
			if ( includedDocsFile.exists() ) {
				EntityHandler entityHandler = new EntityHandler( "document" );
				saxParser.parse( includedDocsFile, entityHandler );
				ArrayList<Group> includedEntries = new ArrayList<>();
				for (Entity p : entityHandler.getEntities()) {
					for (Group entity : documentsList) {
						if ( entity.getId().equals( p.getId() ) ) {
							includedEntries.add( entity );
							entity.addParent( category.getId() );
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

		Group rootGroup = new Group( "root", "root" );
		ArrayList<com.maxsavitsky.documenter.data.types.Entity> entities = new ArrayList<>();
		for(Group g : allGroups){
			if(g.getParents().size() == 0){
				entities.add( g );
				g.addParent( rootGroup.getId() );
			}
		}
		for(EntryEntity e : entriesList){
			if(e.getParents().size() == 0){
				entities.add( e );
				e.addParent( rootGroup.getId() );
			}
		}
		rootGroup.setContainingEntities( entities );
		allGroups.add( rootGroup );

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

	public static void runReformat() throws Exception {
		JSONObject jsonObject = prepareInfoJSON();
		reformatEntriesProperties( jsonObject );
		reformatEntriesAdditional( jsonObject );
		deleteUnusedEntriesDirs( jsonObject );
		deleteUnusedEntriesFiles( jsonObject );
		File dataDir = new File( App.appStoragePath, "data" );
		if(!dataDir.exists())
			dataDir.mkdirs();
		copyEntriesDir();
		deleteUnusedRootFiles();
		File file = new File( dataDir, "data.json" );
		if ( !file.exists() ) {
			file.createNewFile();
		}
		String reformattedData = jsonObject.toString();
		try (FileOutputStream fos = new FileOutputStream( file )) {
			fos.write( reformattedData.getBytes( StandardCharsets.UTF_8 ) );
		}
	}

	private static void deleteUnusedRootFiles(){
		File file = new File( App.appStoragePath );
		File[] files = file.listFiles();
		if(files != null){
			for(File f : files){
				if(f.isDirectory()){
					if(!f.getName().equals( "data" ) && !f.getName().equals( "backups" ))
						Utils.deleteDirectory( f );
				}else{
					f.delete();
				}
			}
		}
	}

	private static void copyEntriesDir() throws IOException {
		Utils.copy( new File( App.appStoragePath, "entries" ), new File( App.appStoragePath, "data" ) );
	}

	private static void deleteUnusedEntriesFiles(JSONObject jsonObject) throws JSONException {
		JSONArray jsonArray = jsonObject.getJSONArray( "entries" );
		for (int i = 0; i < jsonArray.length(); i++) {
			String id = jsonArray.getJSONObject( i ).getString( "id" );
			File dir = new File( App.appStoragePath, "entries/" + id );
			if(dir.exists()){
				File[] files = dir.listFiles();
				if(files  != null){
					for(File f : files){
						String n = f.getName();
						if(n.endsWith( ".xml" ) || n.equals( "alignment" ) || n.equals( "relative_spans" )){
							f.delete();
						}
					}
				}
			}
		}
	}

	private static void deleteUnusedEntriesDirs(JSONObject jsonObject) throws JSONException {
		Map<String, Boolean> map = new HashMap<>();
		JSONArray jsonArray = jsonObject.getJSONArray( "entries" );
		for (int i = 0; i < jsonArray.length(); i++) {
			String id = jsonArray.getJSONObject( i ).getString( "id" );
			map.put( id, true );
		}
		File file = new File( App.appStoragePath, "entries" );
		if(file.exists()){
			File[] files = file.listFiles();
			if(files != null){
				for(File f : files){
					if(!map.containsKey( f.getName() ))
						Utils.deleteDirectory( f );
				}
			}
		}
	}

	private static void reformatEntriesProperties(JSONObject jsonObject) throws JSONException, ParserConfigurationException, SAXException, IOException {
		SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();
		JSONArray jsonArray = jsonObject.getJSONArray( "entries" );
		for (int i = 0; i < jsonArray.length(); i++) {
			String id = jsonArray.getJSONObject( i ).getString( "id" );
			File propsFile = new File( App.appStoragePath, "entries/" + id + "/properties.xml" );
			EntryEntity.Properties properties;
			if ( propsFile.exists() ) {
				EntryPropertiesHandler handler = new EntryPropertiesHandler();
				saxParser.parse( propsFile, handler );
				properties = handler.getProperties();
			} else {
				properties = new EntryEntity.Properties();
			}
			propsFile = new File( App.appStoragePath, "entries/" + id + "/properties.json" );
			if ( !propsFile.exists() ) {
				propsFile.createNewFile();
			}
			try (FileOutputStream fos = new FileOutputStream( propsFile )) {
				fos.write( properties.convertToJSON().toString().getBytes( StandardCharsets.UTF_8 ) );
			}
		}
	}

	private static void reformatEntriesAdditional(JSONObject jsonObject) throws JSONException, IOException {
		JSONArray jsonArray = jsonObject.getJSONArray( "entries" );
		for (int i = 0; i < jsonArray.length(); i++) {
			String id = jsonArray.getJSONObject( i ).getString( "id" );
			String pathDir = App.appStoragePath + "/entries/" + id + "/";
			JSONObject out = new JSONObject();
			File file = new File( pathDir + "alignment" );
			JSONArray alignments = new JSONArray();
			if ( file.exists() ) {
				try (BufferedReader br = new BufferedReader( new FileReader( file ) )) {
					String line;
					while ( br.ready() ) {
						line = br.readLine();

						String[] strings = line.split( " " );
						alignments.put(
								new JSONObject()
										.put( "name", strings[ 0 ] )
										.put( "start", Integer.parseInt( strings[ 1 ] ) )
										.put( "end", Integer.parseInt( strings[ 2 ] ) )
						);
					}
				}
			}
			out.put( "alignments", alignments );

			file = new File( pathDir + "relative_spans" );
			JSONArray relativeSpans = new JSONArray();
			if ( file.exists() ) {
				try (BufferedReader br = new BufferedReader( new FileReader( file ) )) {
					String line;
					while ( br.ready() ) {
						line = br.readLine();

						String[] strings = line.split( " " );
						relativeSpans.put(
								new JSONObject()
										.put( "value", Float.parseFloat( strings[ 0 ] ) )
										.put( "start", Integer.parseInt( strings[ 1 ] ) )
										.put( "end", Integer.parseInt( strings[ 2 ] ) )
						);
					}
				}
			}
			out.put( "relativeSpans", relativeSpans );

			file = new File( pathDir + "additional.json" );
			if(!file.exists())
				file.createNewFile();
			try(FileOutputStream fos = new FileOutputStream(file)){
				fos.write( out.toString().getBytes( StandardCharsets.UTF_8 ) );
			}
		}
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

	private static class TimestampHandler extends DefaultHandler{
		private int timestamp;

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			if(qName.equals( "timestamp" )){
				timestamp = Integer.parseInt( attributes.getValue( "value" ) );
			}
		}
	}

	private static class EntityHandler extends DefaultHandler {
		private final String name;
		private final ArrayList<Entity> mEntities = new ArrayList<>();

		public EntityHandler(String name) {
			this.name = name;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
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

	private static class EntryPropertiesHandler extends DefaultHandler {
		private final EntryEntity.Properties mProperties = new EntryEntity.Properties();

		public EntryEntity.Properties getProperties() {
			return mProperties;
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) {
			switch ( qName ) {
				case "textSize":
					mProperties.setTextSize( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "bgColor":
					mProperties.setBgColor( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "textColor":
					mProperties.setTextColor( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "scrollPosition":
					mProperties.setScrollPosition( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "textAlignment":
					mProperties.setTextAlignment( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
				case "saveLastPos":
					mProperties.setSaveLastPos( Boolean.parseBoolean( attributes.getValue( "value" ) ) );
					break;
				case "defaultColor":
					mProperties.setDefaultTextColor( Integer.parseInt( attributes.getValue( "value" ) ) );
					break;
			}
		}
	}

}
