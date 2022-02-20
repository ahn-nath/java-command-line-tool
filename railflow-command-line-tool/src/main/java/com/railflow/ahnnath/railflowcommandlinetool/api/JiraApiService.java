package com.railflow.ahnnath.railflowcommandlinetool.api;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

/**
 * Contains methods and attributes that help make API with the Jira Cloud
 * Platform REST API.
 * 
 * @author ahn-nath
 * @version 2.0
 * @since 1.0
 * 
 **/
@Service
public class JiraApiService {

	@Value("${jira.api.instance}")
	private String apiInstanceUrl;

	@Value("${jira.api.token}")
	private String apiToken;

	@Value("${jira.api.email}")
	private String apiEmail;

	@Value("${jira.api.target.project}")
	private String targetProject;

	@Value("${jira.api.custom-field-name}")
	private String apiCustomField;

	private static final String open = "To Do";
	private static final String closed = "Done";
	private static final String resolutionName = "Done";

	private static Logger logger = LoggerFactory.getLogger(JiraApiService.class);
	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Receives the a custom issue field to check for and looks via JQL search on a
	 * given Jira instance. If any result was found, it means the custom field is
	 * present/exist.If custom field exist, the method returns true, otherwise it
	 * returns false.
	 *
	 * @apiNote https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-search/#api-rest-api-3-search-get
	 * @param apiCustomField to look for.
	 * 
	 * @return boolean with the final outcome.
	 * @throws UnirestException
	 * 
	 *
	 **/
	public boolean doesRequiredCustomFieldExist(String apiCustomField) throws UnirestException {
		int status;
		boolean anyValues = false;

		// make GET request to check if custom field exist
		HttpResponse<JsonNode> response = Unirest.get(String.format("%s/rest/api/3/search", apiInstanceUrl))
				.basicAuth(apiEmail, apiToken).header("Accept", "application/json")
				.queryString("jql", String.format("'%s' is not EMPTY OR '%s' is EMPTY", apiCustomField, apiCustomField))
				.asJson();

		// parse response
		status = response.getStatus();
		if (status == 200) {
			JsonNode jsonNode = response.getBody();
			anyValues = jsonNode.getObject().getInt("total") > 0 ? true : false;
		}

		return anyValues;

	}

	/**
	 * Receives the issue metadata and issue status to look for via JQL search on a
	 * given Jira instance. If any result was found, it is expected to be one issue
	 * object. each case or input. If not issue was found the String object returned
	 * will be null.
	 *
	 * @apiNote https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-search/#api-rest-api-3-search-get
	 * @param issueMetadata metadata to look for.
	 * @param issueStatus   parameter used to filter by 'open' and 'closed'
	 *                      issues/defects.
	 * @return String object with issue id.
	 * @throws UnirestException
	 * @throws JsonProcessingException
	 * 
	 *
	 **/
	public String findDefectByStatus(String issueMetadata, int issueStatus)
			throws UnirestException, JsonProcessingException {
		int status = 500;
		String issueId = null;
		String issueType = issueStatus == 1 ? open : closed;

		// make GET request to retrieve specified defect/bug by status and metadata
		HttpResponse<JsonNode> response = Unirest.get(String.format("%s/rest/api/3/search", apiInstanceUrl))
				.basicAuth(apiEmail, apiToken).header("Accept", "application/json")
				.queryString("jql",
						String.format("'%s'~'\"%s\"~0'AND status='%s'", apiCustomField, issueMetadata, issueType))
				.asJson();

		// parse response
		status = response.getStatus();
		if (status == 200) {
			JsonNode jsonNode = response.getBody();
			issueId = jsonNode.getObject().getInt("total") == 0 ? null
					: jsonNode.getObject().getJSONArray("issues").getJSONObject(0).getString("id");
		}

		return issueId;
	}

	/**
	 * Receives the issue metadata and issue title to create a new defect or issue
	 * of type 'bug'.
	 *
	 * @apiNote https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-post
	 * @param issueMetadata metadata to add to the created issue as a custom input.
	 * @param issueTitle    summary or issue title.
	 * @throws UnirestException
	 * 
	 *
	 **/
	public String createDefect(String issueMetadata, String issueTitle) throws UnirestException {
		int status = 500;

		// main JSON objects
		JSONObject payload = new JSONObject();
		JSONObject fields = new JSONObject();
		JSONArray properties = new JSONArray();

		// corresponding to fields object
		JSONObject issueType = new JSONObject();
		JSONObject project = new JSONObject();
		issueType.put("name", "Bug");
		project.put("key", targetProject.trim());

		// NOTE: replace property with custom field: readonlyfield
		// corresponding to properties object
		JSONObject property = new JSONObject();
		JSONObject value = new JSONObject();
		value.put(apiCustomField, issueMetadata);
		property.put("key", apiCustomField);
		property.put("value", value);

		// populate fields object
		fields.put("issuetype", issueType);
		fields.put("project", project);
		fields.put("summary", issueTitle);
		fields.put("customfield_10034", issueMetadata);

		// populate properties object
		properties.put(property);

		payload.put("fields", fields);
		payload.put("properties", properties);

		// make POST request to create a new defect wth specific summary, metadata and
		// properties
		HttpResponse<JsonNode> response = Unirest.post(String.format("%s/rest/api/3/issue", apiInstanceUrl))
				.basicAuth(apiEmail, apiToken).header("Accept", "application/json")
				.header("Content-Type", "application/json").body(payload).asJson();

		// parse response
		status = response.getStatus();
		if (status == 200 || status == 201) {

			// issue created
			JsonNode jsonNode = response.getBody();
			String issueId = jsonNode.getObject().getString("id");
			String issueKey = jsonNode.getObject().getString("key");

			logger.info(String.format("The defect was created on Jira with the id %s and key %s", issueId, issueKey));
			return issueId;
		} else {
			logger.info(String.format("Something happened and the the defect was not created"));
		}

		return null;

	}


	/**
	 * Deletes an issue or defect by id.
	 *
	 * @apiNote https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-delete
	 * @param issueId        id corresponding to the issue to be deleted.
	 * @throws UnirestException
	 * @throws JsonProcessingException
	 * @return boolean final outcome regarding how the operation went
	 *
	 **/
	public boolean deleteDefect(String issueKey) throws UnirestException, JsonProcessingException {
		int status = 500;
		boolean wasDeleted = false;

		HttpResponse<JsonNode> response = Unirest
				.delete(String.format("%s/rest/api/2/issue/%s", apiInstanceUrl, issueKey)).basicAuth(apiEmail, apiToken)
				.header("Accept", "application/json").asJson();

		status = response.getStatus();
		if (status == 200 || status == 204) {
			wasDeleted = true;
		}

		return wasDeleted;
	}

	/**
	 * Adds comment to issue.
	 *
	 * @apiNote https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issue-comments/#api-rest-api-3-issue-issueidorkey-comment-post
	 * @param commentContent comment content to add.
	 * @param issueId        id corresponding to the issue to be updated.
	 * @throws UnirestException
	 * @throws JsonProcessingException
	 * 
	 *
	 **/
	public String addCommentToIssue(String commentContent, String issueId)
			throws UnirestException, JsonProcessingException {
		int status = 500;
		JSONObject payload = new JSONObject();
		payload.put("body", commentContent);

		// make POST request to add comment to defect
		HttpResponse<JsonNode> response = Unirest
				.post(String.format("%s/rest/api/2/issue/%s/comment", apiInstanceUrl, issueId))
				.basicAuth(apiEmail, apiToken).header("Accept", "application/json")
				.header("Content-Type", "application/json").body(payload).asJson();

		// parse response
		status = response.getStatus();
		if (status == 200 || status == 201) {

			// issue comment created
			JsonNode jsonNode = response.getBody();
			String commentId = jsonNode.getObject().getString("id");
			String commentCreatedDate = jsonNode.getObject().getString("created");

			logger.info(String.format("The comment was created on Jira with the id %s on %s", commentId,
					commentCreatedDate));
			return commentId;
		} else {
			logger.info(String.format("Something happened and the comment was not created"));
		}

		return null;
	}

	/**
	 * Get transition identifiers by issue.
	 *
	 * @apiNote https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-transitions-get
	 * @param issueId id corresponding to the issue to be updated.
	 * @return String object with issue id.
	 * @throws UnirestException
	 * @throws JsonProcessingException
	 * 
	 *
	 **/
	public Map<String, String> getTransitionsByIssue(String issueId) throws UnirestException, JsonProcessingException {
		int status = 500;
		Map<String, String> transitionsMap = new HashMap<String, String>();

		// make GET request to get transitions identifiers by issue and map them to name
		HttpResponse<JsonNode> response = Unirest
				.get(String.format("%s/rest/api/3/issue/%s/transitions", apiInstanceUrl, issueId))
				.basicAuth(apiEmail, apiToken).header("Accept", "application/json").asJson();

		// parse response
		status = response.getStatus();
		if (status == 200) {

			// transitions retrieved
			JsonNode jsonNode = response.getBody();
			JSONArray array = jsonNode.getObject().getJSONArray("transitions");

			// get transition name and transition id to map
			for (int i = 0; i < array.length(); i++) {
				JSONObject object = array.getJSONObject(i);
				String transitionName = object.getString("name");
				String transitionId = object.getString("id");
				transitionsMap.put(transitionName, transitionId);
			}
			logger.info(String.format("The map of transitions was retrieved %s",
					mapper.writeValueAsString(transitionsMap)));
		} else {
			logger.info(String.format("Something happened and the transitions were not retrieved"));
		}

		return transitionsMap;
	}

	/**
	 * Transition issue to 'closed' status.
	 *
	 * @apiNote https://developer.atlassian.com/cloud/jira/platform/rest/v3/api-group-issues/#api-rest-api-3-issue-issueidorkey-transitions-post
	 * @param issueId id corresponding to the issue to be updated.
	 * @throws UnirestException
	 * @throws JsonProcessingException
	 * 
	 *
	 **/

	public void closeIssue(String issueId) throws UnirestException, JsonProcessingException {
		int status = 500;
		Map<String, String> transitionIds = getTransitionsByIssue(issueId);
		String transitionId = transitionIds != null ? transitionIds.get(closed) : null;

		// if we found a transition to specify, make API request
		if (transitionId != null) {
			JSONObject payload = new JSONObject();
			JSONObject transition = new JSONObject();
			JSONObject resolution = new JSONObject();

			// corresponding to payload object
			transition.put("id", transitionId);
			resolution.put("name", resolutionName);

			payload.put("transition", transition);
			payload.put("resolution", resolution);


			// make POST request to transition defect to a "closed" status
			HttpResponse<JsonNode> response = Unirest
					.post(String.format("%s/rest/api/2/issue/%s/transitions", apiInstanceUrl, issueId))
					.basicAuth(apiEmail, apiToken).header("Accept", "application/json")
					.header("Content-Type", "application/json").body(payload).asJson();
			
			status = response.getStatus();
			if (status == 200 || status == 204) {
				logger.info(String.format("The issue with the id [%s] was closed", issueId));
			}
		}

		else {
			logger.info(String.format("Something happened and the issue was not closed"));
		}

	}

}
