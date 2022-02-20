package com.railflow.ahnnath.railflowcommandlinetool;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class RailflowCliCommandTest {

	/** TODO: add documentation 
	 
	private static Logger logger = LoggerFactory.getLogger(RailflowCliCommandTest.class);

	@Test
	public void testWithCommandLineOption() throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		// run command-line app and assert that it returns correct outcome after passing
		// argument
		try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
			String[] args = new String[] { "-v" };
			PicocliRunner.run(RailflowCliCommand.class, ctx, args);
			logger.info("baos.toString()");
			// railflow-cli
			assertTrue(baos.toString().contains("Hi!"));
		}
	}

	// TODO: add documentation 
	@Test
	public void testWithParseMethod() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		// run command-line app and assert that it returns correct outcome after passing
		// argument
		try (ApplicationContext ctx = ApplicationContext.run(Environment.CLI, Environment.TEST)) {
			String[] args = new String[] { "parse", "C:/Users/Usuario/Downloads/junit.xml" };
			PicocliRunner.run(RailflowCliCommand.class, ctx, args);

			// railflow-cli
			assertTrue(baos.toString().contains("Parsing..."));
		}
	}
	**/
}
