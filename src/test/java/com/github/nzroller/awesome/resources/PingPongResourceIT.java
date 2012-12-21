package com.github.nzroller.awesome.resources;

import static org.fest.assertions.api.Assertions.assertThat;

import com.yammer.dropwizard.testing.ResourceTest;
import org.junit.Test;

public class PingPongResourceIT extends ResourceTest {
	@Override
	protected void setUpResources() {
		addResource(new PingPongResource());
	}

	@Test
	public void shouldPongUponPing() throws Exception {
		String result = client().resource("/ping").get(String.class);

		assertThat(result).isEqualTo("pong");
	}
}
