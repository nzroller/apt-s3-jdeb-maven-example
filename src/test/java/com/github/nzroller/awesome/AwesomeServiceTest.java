package com.github.nzroller.awesome;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.nzroller.awesome.resources.PingPongResource;
import com.yammer.dropwizard.config.Environment;
import org.junit.Ignore;
import org.junit.Test;

public class AwesomeServiceTest {
	private final Environment environment = mock(Environment.class);
	private final AwesomeService service = new AwesomeService();
	private final AwesomeConfiguration config = new AwesomeConfiguration();

	//	@Before
	//    public void setup() throws Exception {
	//        config.setMyParam("yay");
	//    }

	@Test
	public void buildsAuthResource() throws Exception {
		service.run(config, environment);

		verify(environment).addResource(any(PingPongResource.class));
	}
}
