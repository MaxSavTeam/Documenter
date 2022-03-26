package com.maxsavitsky.documenter.utils;

import com.maxsavitsky.documenter.data.types.Entity;
import com.maxsavitsky.documenter.data.types.Group;
import com.maxsavteam.stringalgorithms.StringAlgorithms;

import java.util.ArrayList;
import java.util.List;

public class SearchUtil {

	private SearchUtil(){}

	public interface SearchCallback{
		void onEntityFound(Entity entity, List<Entity> path);

		void onFinish();
	}

	public static void search(Group group, String query, SearchCallback callback){
		String norm = query.trim().toLowerCase();
		searchRecursive( group, norm, callback, new ArrayList<>() );
		callback.onFinish();
	}

	private static void searchRecursive(Group group, String q, SearchCallback callback, ArrayList<Entity> currentPath){
		currentPath.add( group );
		for(Entity e : group.getContainingEntities()){
			if(matches( e.getName(), q ))
				callback.onEntityFound( e, currentPath );
			if(e instanceof Group) {
				searchRecursive( (Group) e, q, callback, currentPath );
			}
		}
		currentPath.remove( currentPath.size() - 1 );
	}

	private static boolean matches(String s, String q){
		return StringAlgorithms.indexOf( s.toLowerCase(), q ) != -1;
	}

}
