package com.badlogicgames.partyroom;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.badlogicgames.partyroom.assets.StaticAssetsConfiguration;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yammer.dropwizard.config.Configuration;

public class PartyRoomServiceConfiguration extends Configuration implements com.badlogicgames.partyroom.assets.StaticAssetsBundleConfiguration {
	@JsonProperty
	@Valid
	@NotNull
	public StaticAssetsConfiguration assets = new StaticAssetsConfiguration();
	
	@JsonProperty
	@Valid
	@NotNull
	public String youtubeKey;
	
	@JsonProperty
	@Valid
	public long heartBeat = 10000000000l; // 10 seconds
	
	@Override
	public StaticAssetsConfiguration getAssetsConfiguration () {
		return assets;
	}
}
