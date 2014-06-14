package com.badlogicgames.partyroom.mvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

public class Users {
	private final Map<String, User> users = new HashMap<String, User>();
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
}
