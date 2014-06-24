package com.badlogicgames.partyroom.mvc;

public class UserRoomData {
	public Item song;
	public volatile long lastUpdate; // used for heartbeat in room
	public volatile int lastVote; 
}
