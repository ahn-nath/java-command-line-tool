# Railflow command line utility
### _Decrypt your test file files, and store what matters to you_

![Railflow logo](https://docs.railflow.io/img/banner.png)

![Build Status](https://travis-ci.org/joemccann/dillinger.svg?branch=master)

The Railflow CLI takes a bunch of arguments and processes/parses JUnit schema files that contain unit test results from the CI system and sends those results to JIRA using its respective REST API.

- Command Line Interface.
- Parsing of JUnit, XML files.
- Works with the Jira API.

## Features
![Diagram](/resources/app-diagram.png)
- Process JUnit file from the specified path.
- Send test suites, test cases, duration, failure message, and exceptions to the JIRA REST API.
- Exposes JIRA specifics for making API connection: Auth details [1], JIRA project ID/Key [2], custom field key for storing the defect/issue metadata


The application relies on the Jira REST API to make HTTP requests.

> The Jira REST API enables you to interact with Jira programmatically. 
> Use this API to build apps, script interactions with Jira, or develop any other type of integration

### Jira workflow
 Send processed data via the Jira API
- For all failures in the JUnit file
  First, check if there is an OPEN defect (using Jira entity property match).
    - If yes
        - Print to console/log that ‘skipping defect creation since defect <jira id> already exists
        - Add a comment in JIRA ‘<testrail: defect still open>’
    - If no
        - Query the same metadata on ‘CLOSED’ defects. If found, then this defect may be a ‘regression’
        Create a new defect with the title [regression] …
        - If the CLOSED query returns nothing, then create a new ticket and add metadata using Jira-entity-properties API

- For passes in the JUnit file
    - Check if any open defects (using Jira-entity-properties API) match on test metadata and if yes, close/resolve defects. The logic here is that if a test is now passing, then the defect can be closed/resolved.
    - If no match is found on the OPEN defect, don’t do anything in Jira.

## Tech

The app uses a number of open source projects and tools to work properly:

- [Picocli CLI](https://picocli.info/) -  a one-file framework for creating Java command line applications with almost zero code.
- [Spring Boot](https://spring.io/projects/spring-boot) - a tool that makes developing web application and microservices with Spring Framework faster and easier.
- [Java](https://www.java.com/) - a high level object oriented programming language.
- [JUnit](https://junit.org/junit5/) - a unit testing framework for the Java programming language.
- [Unirest](http://kong.github.io/unirest-java/) - a lightweight HTTP client library from Mashape.


## Installation

The app requires [Java](https://www.java.com/) v11 to run.

- Clone this repository:
    ```sh
    gh repo clone railflow/nathaly
    ```

- Open the folder with your terminal and go to the root directory:
    ```sh
    cd railflow-command-line-tool
    ```
    
- Run the following command to create the executable jar file:
  ```sh
    mvnw package
    ```
    
- If the build was successful and the tests passed, go to the target folder:
   ```sh
    cd target
    ```
- Run the application by using the following command with the format: java -jar <jar generated name> -v 
    ```sh
    java -jar railflow-command-line-tool-0.0.1-SNAPSHOT.jar -v
    ```

If everything went as expected, you should see a "Running railflow-cli application..." message being displayed to the console.
 ![Example - successfully running](/resources/running-app-example-01.png)

## Usage
### Parse JUnit files
You will need:
- The path to a local file of XML format that corresponds to JUnit tests. 
- If you wish to use your own Jira instance, go to the application.properties file located in the 'resources' folder of the main directory and change the properties:
    - jira.api.instance: URL to your Jira instance.
    - jira.api.token: token required to make API calls and update your Jira instance.
    - jira.api.email: email account corresponding to the owner of the specified Jira instance and token.
    - jira.api.target.project: Jira project key/id to associate your defects/issues with.
**NOTE:** You also need to create an issue custom field with the value of the *jira.api.custom-field-name* property, which should be equal to railflow-metadata.

If these requirements are met, you can continue.

While the application is running, use the following command: 
 ```sh
 java -jar railflow-command-line-tool-0.0.1-SNAPSHOT.jar parse <path to your local file>
  ``` 
 Remember to replace the path to your local file. 

- The next step would be to make sure that the application is creating, commenting on, updating, and fetching defects (Jira issues of type 'Bug') when necessary.

Here is a list of some of the methods used to parse JUnit files and update Jira instance. 

| Method | Associated documentation on Jira |
| ------ | ------ |
| doesRequiredCustomFieldExist(String apiCustomField) | [Search for issues using JQL (GET)](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-search/#api-rest-api-3-search-get) |
| findDefectByStatus(String issueMetadata, int issueStatus)| [Search for issues using JQL (GET)](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-search/#api-rest-api-3-search-get) |
| createDefect(String issueMetadata, String issueTitle) | [Create issue](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-post) |
| deleteDefect(String issueKey) | [Delete issue](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-delete) |
| addCommentToIssue(String commentContent, String issueId) | [Add comment](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-comments/#api-rest-api-3-issue-issueidorkey-comment-post) |
| getTransitionsByIssue(String issueId)  | [Get transitions](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-transitions-get) |
| closeIssue(String issueId)  | [Transition issue](https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-transitions-post) |



## Development
In the future, the tool may be integrated with the TestRails API and follow the process described below:
   - Process JUnit file from specified URL path.
   - Process the Junit file and create sections, tests, plans, runs, milestones according to each case.
        - case: https://www.gurock.com/testrail/docs/api/reference/cases/#addcase
        - section: https://www.gurock.com/testrail/docs/api/reference/sections/
        - milestone: https://www.gurock.com/testrail/docs/api/reference/milestones/#addmilestone
        - plan: https://www.gurock.com/testrail/docs/api/reference/plans/#addplan
        - results: https://www.gurock.com/testrail/docs/api/reference/results/#addresults
    - Send processed data via the TestRail API.

## License
[To be defined]


