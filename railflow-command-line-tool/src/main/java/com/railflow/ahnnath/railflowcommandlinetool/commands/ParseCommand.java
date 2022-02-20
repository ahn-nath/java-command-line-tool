package com.railflow.ahnnath.railflowcommandlinetool.commands;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mashape.unirest.http.exceptions.UnirestException;
import com.railflow.ahnnath.railflowcommandlinetool.api.JiraApiService;
import com.railflow.ahnnath.railflowcommandlinetool.util.DomParser;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Contains methods and attributes necessary to use the sub command 'parse' that
 * is used by the application tool to parse XML documents via path to local
 * files.
 * 
 * @author ahn-nath
 * @version 2.0
 * @since 1.0
 * 
 **/

@Component
@Command(name = "parse", description = "Receives a given file and parses it.", mixinStandardHelpOptions = true)
final public class ParseCommand implements Runnable {

	// The file whose data to parse
	@Parameters(index = "0", description = "The file whose data to parse.")
	File file;

	@Autowired
	DomParser parser;

	@Autowired
	JiraApiService jiraService;

	@Value("${jira.api.instance}")
	private String apiInstanceUrl;

	@Value("${jira.api.custom-field-name}")
	private String apiCustomField;

	private static Logger logger = LoggerFactory.getLogger(ParseCommand.class);

	// The custom issue field is required to add the 'railflow-metadata'
	// corresponding to each 'testcase' to be created as a bug
	private static boolean hasCustomField = false;

	@Override
	public void run() {
		try {
			if (!hasCustomField) {
				logger.info(String.format("Verifying if required custom field <%s> exist in %s", apiCustomField,
						apiInstanceUrl));
				boolean doesRequiredCustomFieldExist = jiraService.doesRequiredCustomFieldExist(apiCustomField);

				if (doesRequiredCustomFieldExist) {
					hasCustomField = true;
					logger.info(String.format("Successfully verified. The required custom field <%s> exist in %s",
							apiCustomField, apiInstanceUrl));
					parser.parseXMLDoc(file);
				} else {
					logger.info(String.format(
							"Unsuccessfully verified. The required custom field does not seem to exist in %s. Please create a custom field with name '%s' and try again",
							apiInstanceUrl, apiCustomField));
				}
			} else {
				parser.parseXMLDoc(file);
			}

		} catch (UnirestException e) {
			logger.info("There was an error HTTP request made to the the Jira API ");
			logger.debug(e.getMessage());
		}

		finally {
			logger.info("Parsing process finished...");
		}

	}

}
