package com.railflow.ahnnath.railflowcommandlinetool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.railflow.ahnnath.railflowcommandlinetool.commands.ParseCommand;
import com.railflow.ahnnath.railflowcommandlinetool.commands.RailflowCliCommand;

import picocli.CommandLine;

/**
 * This class, annotated with the @SpringBootApplication annotation, can be used
 * to enable Spring Boot’s auto-configuration and @Component scan mechanisms. It
 * initializes the main configuration the command line utility by
 * programmatically adding sub-commands and setting up important dependencies.
 * 
 * @author ahn-nath
 * @version 2.0
 * @since 2.0
 * 
 **/
@SpringBootApplication
public class RailflowCommandLineToolApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(RailflowCommandLineToolApplication.class, args);
	}

	private RailflowCliCommand mainCommand;
	private ParseCommand parseCommand;

	@Autowired
	public RailflowCommandLineToolApplication(RailflowCliCommand mainCommand, ParseCommand parseCommand) {
		this.mainCommand = mainCommand;
		this.parseCommand = parseCommand;
	}

	@Override
	public void run(String... args) throws Exception {
		CommandLine commandLine = new CommandLine(mainCommand);
		commandLine.addSubcommand("parse", parseCommand);

		commandLine.parseWithHandler(new CommandLine.RunLast(), args);

	}

}
