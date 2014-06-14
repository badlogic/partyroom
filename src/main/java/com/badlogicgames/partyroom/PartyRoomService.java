package com.badlogicgames.partyroom;

import com.badlogicgames.partyroom.assets.StaticAssetsBundle;
import com.badlogicgames.partyroom.assets.TemplateProcessor;
import com.badlogicgames.partyroom.mvc.Rooms;
import com.badlogicgames.partyroom.mvc.Users;
import com.badlogicgames.partyroom.resources.RoomResource;
import com.badlogicgames.partyroom.resources.UserResource;
import com.bazaarvoice.dropwizard.redirect.RedirectBundle;
import com.bazaarvoice.dropwizard.redirect.UriRedirect;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class PartyRoomService extends Service<PartyRoomServiceConfiguration> {
	@Override
	public void initialize (Bootstrap<PartyRoomServiceConfiguration> bootstrap) {
		bootstrap.setName("partyroom");
		bootstrap.addBundle(new StaticAssetsBundle("/", new TemplateProcessor("htm", "html")));
		bootstrap.addBundle(new RedirectBundle(new UriRedirect("/", "/index.html")));
	}

	@Override
	public void run (PartyRoomServiceConfiguration config, Environment env) throws Exception {
		Users users = new Users();
		Rooms rooms = new Rooms(config.youtubeKey);
		
		env.addResource(new UserResource(users));
		env.addResource(new RoomResource(rooms, users));
	}
	
	public static void main (String[] args) throws Exception {
		new PartyRoomService().run(args);
	}
}
