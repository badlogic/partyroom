package com.badlogicgames.partyroom.mvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class Room {
	public String name;
	public List<User> users = new Vector<User>(); // to lazy for more lightweight synching :)
	public Map<String, UserRoomData> songsPerUser = new HashMap<String, UserRoomData>();
	public List<Message> messages = new Vector<Message>(); // -- "" --
	public int currentUser;
	public Item currentSong;
	public long startTime; // start time in UTC
	public long playedTime; // number of seconds played of the song
	public long switchTime; // time at which we switch to the next user/song, server system time
	public String youtubeKey;
	public int positiveVotes;
	public int negativeVotes;
	public long lastUpdate;
}
