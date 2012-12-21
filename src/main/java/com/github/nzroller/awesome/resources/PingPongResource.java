package com.github.nzroller.awesome.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/ping")
public class PingPongResource {

	@GET
	public String pong() {
		return "pong";
	}
}
