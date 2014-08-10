package com.badlogicgames.partyroom.mvc;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Users {
	private Map<String, User> users = new HashMap<String, User>();
	private Map<String, List<Playlist>> idToPlaylists = new ConcurrentHashMap<String, List<Playlist>>();
	private Map<String, User> idToUsers = new ConcurrentHashMap<String, User>();
	private Map<String, String> namesToIds = new HashMap<String, String>();	
	private final String dbDir;
	private final File userFile;
	private final File idsFile;
	private final File playlistFile;
	private final File passFile;
	
	public Users(String dbDir) {
		this.dbDir = dbDir;
		this.userFile = new File(dbDir, "users.json");
		this.idsFile = new File(dbDir, "ids.json");
		this.playlistFile = new File(dbDir, "playlists.json");
		this.passFile = new File(dbDir, "pass.json");
		load(dbDir);
	}
	
	private void load(String dbDir) {
		ObjectMapper mapper = new ObjectMapper();
		File dir = new File(dbDir);
		if(!dir.exists()) {
			dir.mkdirs();
		} else {			
			if(userFile.exists() && playlistFile.exists() && idsFile.exists()) {
				try {
					Map<String, User> users = mapper.readValue(userFile, new TypeReference<HashMap<String,User>>(){});
					Map<String, User> idToUsers = mapper.readValue(idsFile, new TypeReference<HashMap<String,User>>(){});
					Map<String, List<Playlist>> idToPlaylists = mapper.readValue(playlistFile, new TypeReference<HashMap<String,List<Playlist>>>(){});
					Map<String, String> passes = mapper.readValue(passFile, new TypeReference<HashMap<String, String>>() {});
					
					for(String key: users.keySet()) {
						this.users.put(key, users.get(key));
					}										
					
					for(String key: idToUsers.keySet()) {
						String name = idToUsers.get(key).name;
						User user = this.users.get(name);
						this.idToUsers.put(name, user);
						this.namesToIds.put(name, key);
					}
					
					for(String key: idToPlaylists.keySet()) {
						this.idToPlaylists.put(key, idToPlaylists.get(key));
					}
					
					for(String key: passes.keySet()) {
						this.idToUsers.get(key).setPassword(passes.get(key));
					}

				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private synchronized void save(String dbDir) {
		ObjectMapper mapper = new ObjectMapper();
		
		try {			
			mapper.writeValue(userFile, users);			
			mapper.writeValue(idsFile, idToUsers);
			mapper.writeValue(playlistFile, idToPlaylists);
			
			Map<String, String> passes = new HashMap<String, String>();
			for(String key: idToUsers.keySet()) {
				passes.put(key, idToUsers.get(key).getPassword());
			}
			mapper.writeValue(passFile, passes);
		} catch(Exception e) {
			// FIXME ...
			e.printStackTrace();
		}
	}
	
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
				save(dbDir);
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
			save(dbDir);
		}
	}
}
