package com.badlogicgames.partyroom.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.badlogicgames.partyroom.mvc.MessageRequest;
import com.badlogicgames.partyroom.mvc.Room;
import com.badlogicgames.partyroom.mvc.RoomDescriptor;
import com.badlogicgames.partyroom.mvc.Rooms;
import com.badlogicgames.partyroom.mvc.User;
import com.badlogicgames.partyroom.mvc.Users;
import com.sun.jersey.spi.resource.Singleton;

@Singleton
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {
	private final Rooms rooms;
	private final Users users;
	
	public RoomResource(Rooms rooms, Users users) {
		this.rooms = rooms;
		this.users = users;
	}
	
	@GET
	@Path("hello")
	public String hello() {
		return "gugu World";
	}
	
	@POST
	@Path("join")
	public Room join(JoinRequest params) {
		User user = users.getUser(params.userId);
		if(user == null) throw new WebApplicationException(Status.FORBIDDEN);
		return rooms.join(params.roomName, user);
	}
	
	@POST
	@Path("list")
	public List<RoomDescriptor> list(String userId) {
		User user = users.getUser(userId);
		// TODO user may be null if the user isn't logged in
		// could react to that
		return rooms.listRooms();
	}
	
	@POST
	@Path("update")
	public Room update(UpdateRequest req) {
		User user = users.getUser(req.userId);
		if(user == null) throw new WebApplicationException(Status.FORBIDDEN);				
		return rooms.getStatus(user, req.roomName);
	}
	
	@POST
	@Path("message")
	public Room message(MessageRequest req) {
		User user = users.getUser(req.userId);
		if(user == null) throw new WebApplicationException(Status.FORBIDDEN);
		return rooms.sendMessage(req.roomName, req.message, user);
	}
	
	@POST
	@Path("song")
	public void setSong(SongRequest req) {
		User user = users.getUser(req.userId);
		rooms.setSong(user, req.roomName, req.playList);
	}
	
	@POST
	@Path("vote")
	public void vote(VoteRequest req) {
		User user = users.getUser(req.userId);
		rooms.vote(user, req.roomName, req.vote);
	}
}
