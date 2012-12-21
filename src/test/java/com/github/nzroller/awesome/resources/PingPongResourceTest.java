package com.github.nzroller.awesome.resources;

import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Test;

public class PingPongResourceTest {
	@Test
	public void shouldPongUponPing() throws Exception {
		String result = new PingPongResource().pong();

		assertThat(result).isEqualTo("pong");
	}
}
