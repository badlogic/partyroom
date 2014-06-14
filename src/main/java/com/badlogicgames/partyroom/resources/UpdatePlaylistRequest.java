package com.badlogicgames.partyroom.resources;

import java.util.ArrayList;
import java.util.List;

import com.badlogicgames.partyroom.mvc.Item;

public class UpdatePlaylistRequest {
	public String userId;
	public List<Item> playlist = new ArrayList<>();
}
