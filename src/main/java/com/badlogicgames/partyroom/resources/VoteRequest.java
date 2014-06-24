package com.badlogicgames.partyroom.resources;

public class VoteRequest {
	public String userId;
	public String roomName;
	public int vote; // -1 no, 0 neutral, 1 yes 
}
