package com.badlogicgames.partyroom.mvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

public class Users {
	private final Map<String, User> users = new HashMap<String, User>();
	private final Map<String, List<Playlist>> idToPlaylists = new ConcurrentHashMap<String, List<Playlist>>();
	private final Map<String, User> idToUsers = new ConcurrentHashMap<String, User>();
	private final Map<String, String> namesToIds = new HashMap<String, String>();	
	
	/**
	 * Creates a (temporary) user with the given username and (optional) image url.
	 * Returns a unique id by which the user will be identified.
	 */
	public String signup(String userName, String password) {
		Objects.requireNonNull(userName, "user name must not be null");
		Objects.requireNonNull(password, "password must not be null");
		
		synchronized(users) {
			if(users.containsKey(userName)) {
				throw new WebApplicationException(Status.FORBIDDEN);
			} else {
				User user = new User();
				user.name = userName;				
				user.setPassword(password);
				String id = UUID.randomUUID().toString();
				users.put(userName, user);
				idToPlaylists.put(id, new ArrayList<Playlist>());
				idToUsers.put(id, user);
				namesToIds.put(userName, id);
				return id;
			}
		}
	}
	
	public String login(String userName, String password) {
		Objects.requireNonNull(userName, "user name must not be null");
		Objects.requireNonNull(password, "password must not be null");
		
		synchronized(users) {
			if(!users.containsKey(userName)) throw new WebApplicationException(Status.FORBIDDEN);
			if(password.equals(users.get(userName).getPassword())) return namesToIds.get(userName);
			else throw new WebApplicationException(Status.FORBIDDEN);
		}
	}
	
	/**
	 * Returns the user with the given id or null if she doesn't exist
	 */
	public User getUser(String id) {
		return idToUsers.get(id);		
	}

	public List<Playlist> getPlaylists (String userId) {	
		List<Playlist> playlists = idToPlaylists.get(userId);
		if(playlists == null) throw new WebApplicationException(Status.FORBIDDEN);
		return playlists;
	}

	public void updatePlaylist (String userId, Playlist list) {
		Objects.requireNonNull(userId, "user id must not be null");
		Objects.requireNonNull(list, "playlist may not be null");
		Objects.requireNonNull(list.name, "playlist name must not be null");
		Objects.requireNonNull(list.items, "playlist items must not be null");
		
		List<Playlist> playlists = idToPlaylists.get(userId);
		if(playlists == null) throw new WebApplicationException(Status.FORBIDDEN);
		synchronized(playlists) {
			for(int i = 0; i < playlists.size(); i++) {
				Playlist otherPlaylist = playlists.get(i);
				if(otherPlaylist.name.equals(list.name)) {
					playlists.set(i, list);
					return;
				}
			}
			
			playlists.add(list);
		}
	}
}
