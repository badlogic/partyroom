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
	private static final long HEART_BEAT = 10000000000l;
	private static final long DELAY_BETWEEN_SONGS = 4000000000l;
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
					
					Message msg = new Message();
					msg.message = "User " + user.name + " quit";
					msg.utcTimeStamp = new Date().getTime();
					msg.userName = null;
					oldRoom.messages.add(msg);
					
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
			// time to switch the song, cause the current one has ended or +50% of users downvoted it 
			if(room.switchTime < System.nanoTime() || room.negativeVotes > Math.ceil(room.users.size() / 2f)) {
				// check if there are any users that haven't been calling us
				// for a while (10 seconds == HEART_BEAT) and remove them
				// from the room if that's the case. We do this here, so a
				// user may get disconnected only after the current song is 
				// done/skipped!
				List<User> usersToRemove = new ArrayList<>();				
				for(User user: room.users) {					
					user.lastVote = 0;
					if(System.nanoTime() - user.lastUpdate > HEART_BEAT) {
						usersToRemove.add(user);
					}
				}
				for(User user: usersToRemove) {
					leave(user);
				}		
				
				// reset the votes for the new song
				room.positiveVotes = 0;
				room.negativeVotes = 0;
				
				// move to the next user, this assumes that there's at least one user in the room!
				room.currentUser++;
				if(room.currentUser >= room.users.size()) room.currentUser = 0;
				
				// get the current song of the current user
				room.currentSong = room.users.get(room.currentUser).song;
				room.users.get(room.currentUser).song = null;
				
				// calculate when the song has started in UTC, as well as when to switch to the next song, in server time
				// we use a delay of 4 seconds between songs.
				room.startTime = room.currentSong == null? 0: new Date().getTime();
				if(room.currentSong != null) {
					room.switchTime = System.nanoTime() + room.currentSong.duration * 1000000000l + DELAY_BETWEEN_SONGS;
				} else {
					room.switchTime = System.nanoTime() + 1000000000;
				}
				
				// if this song was skipped, add a message to the chat
				if(room.negativeVotes > Math.ceil(room.users.size() / 2f)) {
					Message msg = new Message();
					msg.message = "Skipped song due to vote";
					msg.utcTimeStamp = new Date().getTime();
					msg.userName = null;
					room.messages.add(msg);
				}										
			}
			room.playedTime = (new Date().getTime() - room.startTime) / 1000;
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
