package com.railflow.ahnnath.railflowcommandlinetool.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import static picocli.CommandLine.RunLast;


/**
 * Contains methods and attributes that correspond to the configuration of the
 * command line utility application.
 * 
 * @author ahn-nath
 * @version 2.0
 * @since 1.0
 * 
 **/
@Component 
@Command(name = "railflow-cli", description = "The CLI will take a bunch of arguments and process/parse JUnit schema file and send those results to JIRA and TestRail using their respective REST APIs", mixinStandardHelpOptions = true)
public class RailflowCliCommand implements Runnable {
	

	@Option(names = { "-v", "--verbose" }, description = "...")
	boolean verbose;
	
	private static Logger logger = LoggerFactory.getLogger(RailflowCliCommand.class);
	
	@Value("${jira.api.instance}")
	private String apiInstanceUrl;

	@Value("${jira.api.email}")
	private String apiEmail;

	@Value("${jira.api.target.project}")
	private String targetProject;
	
    public static void main(String[] args) {
    	 CommandLine commandLine = new CommandLine(new  RailflowCliCommand());
         commandLine.addSubcommand("parse", new ParseCommand());

         commandLine.parseWithHandler(new RunLast(), args);
    }

    @Override
    public void run() {
		if (verbose) {
			logger.info(String.format("Running railflow-cli application..."));
			logger.info(String.format("- Jira instance: %s.", apiInstanceUrl));
			logger.info(String.format("- Project: %s.", targetProject));
			logger.info(String.format("- Jira user: %s.", apiEmail));
		}
    }
}