package com.badlogicgames.partyroom.mvc;

public class Item {
	public String user;
	public String url;
	public String thumbnail;
	public String title;
	public int duration; // duration in seconds

	@Override
	public String toString () {
		return "Item [user=" + user + ", url=" + url + ", thumbnail=" + thumbnail + ", title=" + title + ", duration=" + duration
			+ "]";
	}
}
