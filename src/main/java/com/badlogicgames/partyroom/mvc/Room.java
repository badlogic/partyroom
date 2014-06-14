package com.badlogicgames.partyroom.mvc;

import java.util.List;
import java.util.Vector;


public class Room {
	public String name;
	public List<User> users = new Vector<User>(); // to lazy for more lightweight synching :)
	public List<Message> messages = new Vector<Message>(); // -- "" --
}
