package com.badlogicgames.partyroom.mvc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the creation and update of rooms. Returned rooms
 * are read only.
 * 
 * The synching is a bit meh in this class. We try to avoid
 * some race conditions while locking at a very fine-grained level.
 */
public class Rooms {	
	private final Map<String, Room> rooms = new ConcurrentHashMap<>();
	private final String youtubeKey;
	
	public Rooms(String youtubeKey) {
		this.youtubeKey = youtubeKey;
		final Thread t = new Thread(() -> {
			while(true) {
				synchronized(rooms) {
					for(Entry<String, Room> entry: rooms.entrySet()) {						
						if(entry.getValue() != null && entry.getValue().users.size() == 0) {
							rooms.remove(entry.getKey());
							System.out.println("removed room " + entry.getKey());
						}
					}
				}
				
				try {
					Thread.sleep(10000);
				} catch (Exception e) {
				}
			}
		});
		t.setDaemon(true);
		t.start();
	}
	
	/**
	 * Creates a new room for the given user, or lets the given
	 * user join the room.
	 * 
	 * @return the created/joined room
	 */
	public Room join(String roomName, User user) {
		Objects.requireNonNull(roomName, "room name must not be null");
		Objects.requireNonNull(user, "user must not be null");
		
		Room room = null;
		synchronized(rooms) {
			room = rooms.get(roomName);
			if(room == null) {
				room = new Room();
				room.youtubeKey = youtubeKey;
				room.name = roomName;
				rooms.put(roomName, room);
			}			
		}
		
		boolean found = false;
		for(User otherUser: room.users) {
			if(otherUser.name.equals(user.name)) {
				found = true;
				break;
			}
		}
		if(!found) {
			leave(user);
			user.roomName = roomName;
			user.lastVote = 0;
			synchronized(room) {
				room.users.add(user);				
			}
		}
		Message msg = new Message();
		msg.message = "User " + user.name + " joined";
		msg.utcTimeStamp = new Date().getTime();
		msg.userName = null;
		room.messages.add(msg);
		return room;
	}
	
	/**
	 * Removes the user from any room she's currently in.
	 */
	public void leave(User user) {
		user.song = null;
		if(user.roomName != null) {
			Room oldRoom = rooms.get(user.roomName);
			if(oldRoom != null) {
				synchronized(oldRoom) {
					oldRoom.users.remove(user);
					if(oldRoom.users.size() == 0) {
						synchronized(rooms) {
							rooms.remove(oldRoom.name);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Returns the current status of the room
	 */
	public Room getStatus(String roomName) {
		Objects.requireNonNull(roomName, "room name may not be null");

		Room room = rooms.get(roomName);
		synchronized(room) {
			if(room.switchTime < System.nanoTime() || room.negativeVotes > Math.ceil(room.users.size() / 2f)) {
				room.currentUser++;
				if(room.currentUser >= room.users.size()) room.currentUser = 0;
				room.currentSong = room.users.get(room.currentUser).song;
				room.users.get(room.currentUser).song = null;
				room.startTime = room.currentSong == null? 0: new Date().getTime();
				if(room.currentSong != null) {
					room.switchTime = System.nanoTime() + room.currentSong.duration * 1000000000l + 4000000000l;
				} else {
					room.switchTime = System.nanoTime() + 1000000000;
				}
				
				if(room.negativeVotes > Math.ceil(room.users.size() / 2f)) {
					Message msg = new Message();
					msg.message = "Skipped song due to vote";
					msg.utcTimeStamp = new Date().getTime();
					msg.userName = null;
					room.messages.add(msg);
				}
				
				for(User user: room.users) {
					user.lastVote = 0;
				}
				room.positiveVotes = 0;
				room.negativeVotes = 0;
			}
		}
		return room;
	}
	
	/**
	 * Adds a new message from the given user to the room's chat log
	 */
	public Room sendMessage(String roomName, String message, User user) {
		Objects.requireNonNull(roomName, "room name may not be null");
		Objects.requireNonNull(message, "message may not be null");
		Objects.requireNonNull(user, "user may not be null");
		
		Room room = rooms.get(roomName);
		Objects.requireNonNull(room, "room with name '" + roomName + "' doesn't exist");		
		Message msg = new Message();
		msg.message = message;
		msg.utcTimeStamp = new Date().getTime();
		msg.userName = user.name;
		room.messages.add(msg);
		return room;
	}
	
	/**
	 * Returns a list of room names and number of users in each room
	 */
	public List<RoomDescriptor> listRooms() {
		List<RoomDescriptor> descriptors = new ArrayList<>();
		for(Map.Entry<String, Room> entry: rooms.entrySet()) {
			RoomDescriptor desc = new RoomDescriptor();
			desc.name = entry.getKey();
			desc.numUsers = entry.getValue().users.size();
			descriptors.add(desc);
		}
		return descriptors;
	}

	/**
	 * Sets the song to be played for the next user. Synchs on the room
	 * the user is in, hence why it's in this class.
	 */
	public void setSong (User user, Item song) {
		Objects.requireNonNull(user, "user may not be null");
		
		Room room = rooms.get(user.roomName);
		synchronized(room) {
			user.song = song;
		}
	}
	
	/**
	 * Sets the vote for the user in the room she's in.
	 */
	public void vote(int vote, User user) {
		Objects.requireNonNull(user, "user may not be null");
		Room room = rooms.get(user.roomName);
		synchronized(room) {
			vote = (int)Math.signum(vote);
			if(user.lastVote != vote) {
				if(user.lastVote < 0) {
					room.negativeVotes--;					
				}
				if(user.lastVote > 0) {
					room.positiveVotes--;
				}
				if(vote < 0) {
					room.negativeVotes++;
					if(room.currentSong != null && room.currentSong.user.equals(user.name)) {
						room.negativeVotes = room.users.size() * 2;
					}
				}
				if(vote > 0) {
					room.positiveVotes++;
				}
			}
			user.lastVote = vote;
		}
	}
}
