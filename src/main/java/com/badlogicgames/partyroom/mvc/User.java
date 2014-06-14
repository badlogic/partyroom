package com.badlogicgames.partyroom.mvc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;


public class User {
	public String name;
	public String imageUrl;
	private String password;
	public volatile String roomName;
	public Item song;	
	
	@JsonIgnore
	public String getPassword () {
		return password;
	}
	
	@JsonProperty
	public void setPassword (String password) {
		this.password = password;
	}
	
	@Override
	public int hashCode () {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((imageUrl == null) ? 0 : imageUrl.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}
	@Override
	public boolean equals (Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		User other = (User)obj;
		if (imageUrl == null) {
			if (other.imageUrl != null) return false;
		} else if (!imageUrl.equals(other.imageUrl)) return false;
		if (name == null) {
			if (other.name != null) return false;
		} else if (!name.equals(other.name)) return false;
		return true;
	}
}
