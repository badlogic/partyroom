package com.badlogicgames.partyroom.mvc;

import java.util.ArrayList;
import java.util.List;

public class UserRoomData {
	public List<Item> playList = new ArrayList<Item>();
	public volatile long lastUpdate; // used for heartbeat in room
	public volatile int lastVote; 
}
