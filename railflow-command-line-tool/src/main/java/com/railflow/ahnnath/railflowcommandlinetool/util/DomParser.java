package com.railflow.ahnnath.railflowcommandlinetool.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.railflow.ahnnath.railflowcommandlinetool.api.JiraApiService;

/**
 * Contains methods and attributes that help parse XML documents and make API
 * requests with the processed data.
 * 
 * @author ahn-nath
 * @version 2.0
 * @since 1.0
 * 
 **/

@Component
final public class DomParser {

	private static Logger logger = LoggerFactory.getLogger(DomParser.class);

	@Autowired
	JiraApiService jiraService;

	private static final int open = 1;
	private static final int closed = 2;
	final ObjectMapper mapper = new ObjectMapper();

	/**
	 * Receives a String object representing the path to a file and retrieves the
	 * XML document from local storage. Then it parses each relevant node found in
	 * file (testsuite and testcase data) and proceeds to make API requests based on
	 * each case or input.
	 *
	 * @param String pathToFile path to file in local storage.
	 *
	 **/
	public void parseXMLDoc(File file) throws UnirestException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;

		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);

			NodeList testsuitesList = doc.getElementsByTagName("testsuite");

			// iterate over each test suite
			for (int i = 0; i < testsuitesList.getLength(); i++) {
				Node n = testsuitesList.item(i);

				// for each test suite in list, get test cases as a list and properties
				if (n.getNodeType() == Node.ELEMENT_NODE) {
					Element testSuite = (Element) n;

					int failures = Integer.valueOf(testSuite.getAttribute("failures"));
					String suiteName = testSuite.getAttribute("name");
					String suiteTime = testSuite.getAttribute("time");
					String suiteTestsNumber = testSuite.getAttribute("tests");

					NodeList testcasesList = testSuite.getElementsByTagName("testcase");

					logger.info(String.format("TEST SUITE #%s", i));
					logger.info(String.format("FAILURES: %s", failures)); 
					System.out.println();

					// iterate over each test case
					for (int j = 0; j < testcasesList.getLength(); j++) {
						Node node = testcasesList.item(j);

						// for each testcase, get name property
						if (node.getNodeType() == Node.ELEMENT_NODE) {
							Element testCase = (Element) node;

							// attributes
							String testName = testCase.getAttribute("name");
							String testClassName = testCase.getAttribute("classname");
							boolean hasFailures = (testCase.getElementsByTagName("failure").getLength() > 0) ? true
									: false;

							// [suiteName-suiteTestsNumber-suiteTime-testName-testClassName]
							String metadata = String.format("%s-%s-%s-%s-%s", suiteName, suiteTestsNumber, suiteTime,
									testName, testClassName);

							// handle cases
							if (hasFailures) {
								// log metadata
								logger.info(String.format("TEST CASE #%s", j));
								logger.info(String.format("testcase name: %s", testName));
								logger.info(String.format("testcase class: %s", testClassName));
								System.out.println();

								// check if any open defects on Jira match current testcase
								String foundOpenIssueId = jiraService.findDefectByStatus(metadata, open);

								// if yes, add message to console and add comment on Jira
								if (foundOpenIssueId != null) {
									logger.info(
											String.format("Skipping defect creation since defect <%s> already exists",
													foundOpenIssueId));
									jiraService.addCommentToIssue("<testrail: defect still open>", foundOpenIssueId);
								}

								// if no, check if any closed defects on Jira match current testcase
								else {
									String foundClosedIssueId = jiraService.findDefectByStatus(metadata, closed);

									// if yes, create a new defect on Jira with [regression]
									if (foundClosedIssueId != null) {
										jiraService.createDefect(metadata,
												String.format("[regression] %s %s", testName, testClassName));
									}

									// if no, create a new ticket and add metadata
									else {
										jiraService.createDefect(metadata,
												String.format("%s %s", testName, testClassName));
									}
								}
							}
							// check if any open defects match current passing testcase
							else {

								String foundOpenIssueId = jiraService.findDefectByStatus(metadata, open);

								// if yes, close defect on Jira
								if (foundOpenIssueId != null) {
									jiraService.closeIssue(foundOpenIssueId);
								}
							}
						}
					}
				}
			}
		}

		catch (SAXException | FileNotFoundException e) {
			logger.info("There was an error with the file specified. The file should exist and have a XML format");

			logger.debug(e.getMessage());
		}

		catch (ParserConfigurationException e) {
			logger.debug(e.getMessage());
			// e.printStackTrace();

		} catch (JsonProcessingException e) {
			logger.info(
					"We encountered problems when processing (parsing, generating) JSON content after making an API request");
			logger.debug(e.getMessage());

		} catch (IOException e) {
			logger.info("We encountered problems when processing (parsing, generating) the file specified.");
			e.printStackTrace();
		}
	}

}
