package com.github.nzroller.awesome;

import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.core.HealthCheck.Result;

import com.yammer.metrics.core.HealthCheck;
import com.yammer.metrics.core.HealthCheck.Result;

import com.github.nzroller.awesome.resources.PingPongResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

/**
 * The AirZettle point-of-entry
 *
 * @author tim
 */
public class AwesomeService extends Service<AwesomeConfiguration> {

	public static void main(String[] args) throws Exception {
		new AwesomeService().run(args);
	}

	@Override
	public void initialize(Bootstrap<AwesomeConfiguration> bootstrap) {
		bootstrap.setName("awesomeapp");
	}

	@Override
	public void run(AwesomeConfiguration config, Environment environment) throws Exception {
		environment.addResource(new PingPongResource());

		// ping pong is always healthy
		environment.addHealthCheck(new HealthCheck("pingpong") {

			@Override
			protected Result check() throws Exception {
				return Result.healthy();
			}
		});
	}
}
