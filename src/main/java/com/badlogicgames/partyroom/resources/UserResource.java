package com.badlogicgames.partyroom.resources;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.badlogicgames.partyroom.mvc.User;
import com.badlogicgames.partyroom.mvc.Users;
import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
	private final Users users;
	
	public UserResource(Users users) {
		this.users = users;
	}
	
	@POST
	@Path("signup")
	public String signup(User user) {
		return users.signup(user.name, user.imageUrl, user.getPassword());
	}
	
	@POST
	@Path("login")
	public String login(User user) {
		return users.login(user.name, user.getPassword());
	}
	
	@POST
	@Path("logout")
	public void logout(String userId) {
		// FIXME
	}
	
	@POST
	@Path("getUser")
	public User getUser(String userId) {
		return users.getUser(userId);
	}		
}
