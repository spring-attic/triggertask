/*
 * Copyright 2016-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.stream.app.triggertask.source;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.cloud.task.launcher.TaskLaunchRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Triggertask source tests.
 * @author Glenn Renfro
 * @author Thomas Risberg
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
public abstract class TriggertaskSourceTests {

	private static final String FIXED_DELAY = "fixedDelay=2";

	private static final String INITIAL_DELAY = "initialDelay=1";

	private static final String CRON_DELAY = "cron=0/2 * * * * *";

	private static final String URI_KEY = "triggertask.uri=";

	private static final String BASE_URI = "hello.world";

	private static final String CRON_URI = BASE_URI + "_CRON";

	private static final String []PARAMS = {"param1=test", "param2=another test", "param3=boo"};

	private static final String COMMAND_LINE_ARGS_KEY = "triggertask.commandLineArgs=";

	private static final String COMMAND_LINE_ARGS = "param1='test' param2='another test' param3=boo";

	private static final String ENVIRONMENT_PROPERTIES_KEY = "triggertask.environmentProperties=";

	private static final String ENVIRONMENT_PROPERTIES ="prop.1=foo, prop.2=bar,prop.3=baz";

	private static final String DEPLOYMENT_PROPERTIES_KEY = "triggertask.deploymentProperties=";

	private static final String DEPLOYMENT_PROPERTIES ="prop.1=aaa, prop.2=bbb,prop.3=ccc";

	private static final String PROP_PREFIX = "prop.";

	private static final String APP_NAME_KEY = "triggertask.applicationName=";

	private static final String APP_NAME = "foo";

	private static final Map<String, String> environmentPropertyMap = new HashMap<>();
	static
	{
		environmentPropertyMap.put(PROP_PREFIX + "1", "foo");
		environmentPropertyMap.put(PROP_PREFIX + "2", "bar");
		environmentPropertyMap.put(PROP_PREFIX + "3", "baz");

	}

	private static final Map<String, String> deploymentPropertyMap = new HashMap<>();
	static
	{
		deploymentPropertyMap.put(PROP_PREFIX + "1", "aaa");
		deploymentPropertyMap.put(PROP_PREFIX + "2", "bbb");
		deploymentPropertyMap.put(PROP_PREFIX + "3", "ccc");

	}

	private static final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	protected Source triggerSource;

	@Autowired
	protected MessageCollector messageCollector;

	@TestPropertySource(properties = {FIXED_DELAY, INITIAL_DELAY, URI_KEY + BASE_URI,
			COMMAND_LINE_ARGS_KEY + COMMAND_LINE_ARGS,
			ENVIRONMENT_PROPERTIES_KEY + ENVIRONMENT_PROPERTIES,
			DEPLOYMENT_PROPERTIES_KEY + DEPLOYMENT_PROPERTIES,
			APP_NAME_KEY + APP_NAME})
	public static class FixedDelayTest extends TriggertaskSourceTests {

		@Test
		public void fixedDelayTest() throws InterruptedException, IOException {
			String result = (String) messageCollector.forChannel(triggerSource.output()).poll(2100, TimeUnit.MILLISECONDS).getPayload();
			TaskLaunchRequest tlr = objectMapper.readValue(result, TaskLaunchRequest.class);
			assertThat(tlr.getUri()).isEqualTo(BASE_URI);
			checkConfigurationSize(3, 3, 3, tlr);

			for (int i = 0; i < 3; i++) {
				assertThat(tlr.getEnvironmentProperties().get(PROP_PREFIX + i)).isEqualTo(environmentPropertyMap.get(PROP_PREFIX + i ));

				assertThat(tlr.getDeploymentProperties().get(PROP_PREFIX + i)).isEqualTo(deploymentPropertyMap.get(PROP_PREFIX + i ));

				assertThat(tlr.getCommandlineArguments().get(i)).isEqualTo(PARAMS[i]);
			}
		}
	}

	@TestPropertySource(properties = {FIXED_DELAY, INITIAL_DELAY, URI_KEY + BASE_URI})
	public static class FixedDelayTestNoArgumentsTest extends TriggertaskSourceTests {

		@Test
		public void fixedDelayNoCommandLineArgumentsTest() throws InterruptedException, IOException {
			String result = (String) messageCollector.forChannel(triggerSource.output()).poll(2100, TimeUnit.MILLISECONDS).getPayload();
			TaskLaunchRequest tlr = objectMapper.readValue(result, TaskLaunchRequest.class);
			assertThat(tlr.getUri()).isEqualTo(BASE_URI);
			checkConfigurationSize(0, 0, 0, tlr);
		}
	}

	@TestPropertySource(properties = {FIXED_DELAY, INITIAL_DELAY})
	public static class MissingURITest extends TriggertaskSourceTests {

		@Test(expected = NullPointerException.class)
		public void MissingURITest() throws InterruptedException {
			messageCollector.forChannel(triggerSource.output()).poll(2100, TimeUnit.MILLISECONDS).getPayload();
		}
	}

	@TestPropertySource(properties = {"trigger.cron = 0/2 * * * * *", CRON_DELAY, URI_KEY + CRON_URI})
	public static class CronTriggerTest extends TriggertaskSourceTests {

		@Test
		public void cronTriggerTest() throws InterruptedException, IOException {
			String result = (String) messageCollector.forChannel(triggerSource.output()).poll(2100, TimeUnit.MILLISECONDS).getPayload();
			TaskLaunchRequest tlr = objectMapper.readValue(result, TaskLaunchRequest.class);
			assertThat(tlr.getUri()).isEqualTo(CRON_URI);
			result = (String) messageCollector.forChannel(triggerSource.output()).poll(2100, TimeUnit.MILLISECONDS).getPayload();
			tlr = objectMapper.readValue(result, TaskLaunchRequest.class);
			assertThat(tlr.getUri()).isEqualTo(CRON_URI);
			checkConfigurationSize(0, 0, 0, tlr);
		}
	}

	@SpringBootApplication
	public static class TriggerTaskSourceApplication {

	}

	private static void checkConfigurationSize(int commandLineArgSize, int environmentPropertySize, int deploymentPropertySize, TaskLaunchRequest tlr) {
		assertThat(tlr.getCommandlineArguments().size()).isEqualTo(commandLineArgSize);
		assertThat(tlr.getEnvironmentProperties().size()).isEqualTo(environmentPropertySize);
		assertThat(tlr.getDeploymentProperties().size()).isEqualTo(deploymentPropertySize);
	}
}
