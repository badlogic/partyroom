package com.badlogicgames.partyroom.resources;

import java.io.InputStream;
import java.net.URL;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;

import com.sun.jersey.spi.resource.Singleton;

/**
 * Allows to get a directory listing from a remote server. Pretty freaking dangerous stuff :)
 * @author badlogic
 *
 */
@Singleton
@Path("/proxy")
@Produces(MediaType.APPLICATION_JSON)
public class ProxyResource {
	@POST
	@Path("fetch")
	public String fetch(String url) {
		try(InputStream in = new URL(url).openStream()) {
			return IOUtils.toString(in);
		} catch(Exception e) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
	}
}
