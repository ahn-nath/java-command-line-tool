package com.railflow.ahnnath.railflowcommandlinetool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.railflow.ahnnath.railflowcommandlinetool.api.JiraApiService;
import com.railflow.ahnnath.railflowcommandlinetool.commands.ParseCommand;
import com.railflow.ahnnath.railflowcommandlinetool.commands.RailflowCliCommand;
import com.railflow.ahnnath.railflowcommandlinetool.util.DomParser;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import picocli.CommandLine;
import picocli.CommandLine.RunLast;

/**
 * Class created to test that the main functionality is working properly and the
 * minimum requirements to get the application working are met.
 * 
 * @author ahn-nath
 * @version 1.0
 * @since 2.0
 *
 */

@SpringBootTest
class RailflowCommandLineToolApplicationTests {

	@Value("${jira.api.instance}")
	private String apiInstanceUrl;

	@Value("${jira.api.email}")
	private String apiEmail;

	@Value("${jira.api.custom-field-name}")
	private String apiRequiredCustomField;

	@Value("${jira.api.target.project}")
	private String targetProject;

	@Autowired
	DomParser parser;

	@Autowired
	JiraApiService apiService;

	/**
	 * Test asserts that the application returns the expected string when the option
	 * '-v'/'--verbose' is passed as an option.
	 */
	@Test
	void mainCommandReturnsApplicationInitialization() {
		// prepare command line and ByteArrayOutputStream object to check for returned
		// string
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		System.setOut(new PrintStream(baos));
		String[] args = new String[] { "-v" };

		CommandLine commandLine = new CommandLine(new RailflowCliCommand());
		commandLine.addSubcommand("parse", new ParseCommand());
		commandLine.parseWithHandler(new RunLast(), args);

		// construct expect string
		String expectedString = "Running railflow-cli application...";

		assertTrue(baos.toString().contains(expectedString));
	}

	/**
	 * Test asserts that the parser (DomParser.class) is able to handle the
	 * FileNotFoundException appropriately. It checks for the logs received or sent
	 * to the console once the error is thrown (as expected). It passes an
	 * inexistent file as input file.
	 * 
	 * @throws UnirestException
	 */
	@Test
	void domParserHandlesFileNotFoundException() throws UnirestException {

		// create and start a ListAppender to check for logged messages
		Logger parserLogger = (Logger) LoggerFactory.getLogger(DomParser.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		parserLogger.addAppender(listAppender);

		// call method under test
		File file = new File("C:/Users/Usuario/Downloads/juni.xml");
		parser.parseXMLDoc(file);

		// JUnit assertions to determine that the output is correct
		List<ILoggingEvent> logsList = listAppender.list;
		assertEquals("There was an error with the file specified. The file should exist and have a XML format",
				logsList.get(0).getMessage());
		assertEquals(Level.INFO, logsList.get(0).getLevel());

	}

	/**
	 * Test asserts that the parser (DomParser.class) is able to handle a
	 * SAXException appropriately. It checks for the logs received or sent to the
	 * console once the error is thrown (as expected). It passes an existent file
	 * with the wrong extension or format (not .xml) as input file.
	 * 
	 * @throws UnirestException
	 */
	@Test
	void domParserHandlesWrongFileFormatException() throws UnirestException {
		// create and start a ListAppender to check for logged messages
		Logger parserLogger = (Logger) LoggerFactory.getLogger(DomParser.class);
		ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
		listAppender.start();
		parserLogger.addAppender(listAppender);

		// call method under test and add a path to an existing file of the wrong format
		File file = new File("C:/Users/Usuario/Downloads/juni.xml");
		parser.parseXMLDoc(file);

		// JUnit assertions to determine that the output is correct
		List<ILoggingEvent> logsList = listAppender.list;
		assertEquals("There was an error with the file specified. The file should exist and have a XML format",
				logsList.get(0).getMessage());
		assertEquals(Level.INFO, logsList.get(0).getLevel());
	}

	/**
	 * Test asserts that the expected custom field used to store issue/defect
	 * metadata exist on specified Jira instance.
	 * 
	 * @throws UnirestException
	 */
	@Test
	void checkRequiredCustomFieldExist() throws UnirestException {
		// check that the expected field is present in Jira Instance
		boolean doesRequiredExist = apiService.doesRequiredCustomFieldExist(apiRequiredCustomField);

		assertTrue(doesRequiredExist);

	}

	/**
	 * Test asserts that the doesRequiredCustomFieldExist returns false for a
	 * property or custom field that actually does not exist.
	 * 
	 * @throws UnirestException
	 */
	@Test
	void checkWrongCustomFieldNotExist() throws UnirestException {
		// check that the method is working properly (false positive)
		boolean doesRequiredExist = apiService.doesRequiredCustomFieldExist("railflow-connection");

		assertFalse(doesRequiredExist);
	}

	/**
	 * Test asserts that a defect can be created and that the method is working
	 * properly. It creates a new defect or Jira issue of type 'bug' and then
	 * verifies its existing by trying to find an open issue with the id of the
	 * recently created defect.It finally deletes the test issue and asserts that it
	 * was deleted.
	 * 
	 * @throws UnirestException
	 * @throws JsonProcessingException
	 */
	@Test
	void testCreateDefectMethod() throws UnirestException, JsonProcessingException {
		// create issue/defect on Jira
		int n = new Random().nextInt(Integer.MAX_VALUE);
		String issueMetadata = String.format("test-create-defect-method-%s", n);
		String issueTitle = "Test Create Defect Method";
		String createdIssueId = apiService.createDefect(issueMetadata, issueTitle);

		// check the issue/defect was created on Jira and delete it
		String foundIssueId = apiService.findDefectByStatus(issueMetadata, 1);
		boolean wasDeleted = apiService.deleteDefect(foundIssueId);

		assertTrue(wasDeleted);
		assertEquals(createdIssueId, foundIssueId);
	}

	/**
	 * Test asserts that a newly created defect can be closed. It first creates a
	 * new defect, closes it, and then verifies that there's a closed issue/defect
	 * with that id. It finally asserts it was deleted.
	 * 
	 * @throws UnirestException
	 * @throws JsonProcessingException
	 */
	@Test
	void testCloseDefectMethod() throws UnirestException, JsonProcessingException {
		// create issue/defect on Jira
		int n = new Random().nextInt(Integer.MAX_VALUE);
		String issueMetadata = String.format("test-close-defect-method-%s", n);
		String issueTitle = "Test Close Defect Method";

		// check the issue/defect was closed on Jira
		String createdIssueId = apiService.createDefect(issueMetadata, issueTitle);
		apiService.closeIssue(createdIssueId);
		String foundIssueId = apiService.findDefectByStatus(issueMetadata, 0);

		assertEquals(createdIssueId, foundIssueId);

		// delete issue
		boolean wasDeleted = apiService.deleteDefect(foundIssueId);

		assertTrue(wasDeleted);

	}

	/**
	 * Test asserts that a comment can be created. It first creates a new defect,
	 * adds a comment to it, and then verifies the method returns a non-null value
	 * (the comment id, which indicates the request was successful). It finally
	 * asserts the created issue was deleted.
	 *
	 * @throws JsonProcessingException
	 * @throws UnirestException
	 */
	@Test
	void testAddCommentMethod() throws JsonProcessingException, UnirestException {
		// create issue/defect on Jira
		int n = new Random().nextInt(Integer.MAX_VALUE);
		String issueMetadata = String.format("test-close-defect-method-%s", n);
		String issueTitle = "Test Close Defect Method";

		// check the issue/defect was created on Jira and create a comment
		String createdIssueId = apiService.createDefect(issueMetadata, issueTitle);
		String createdCommentId = apiService.addCommentToIssue("testing add comment method", createdIssueId);

		assertNotNull(createdCommentId);

		// delete issue
		boolean wasDeleted = apiService.deleteDefect(createdIssueId);

		assertTrue(wasDeleted);
	}

}
