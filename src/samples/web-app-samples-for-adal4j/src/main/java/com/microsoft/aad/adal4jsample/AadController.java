/*******************************************************************************
 * Copyright � Microsoft Open Technologies, Inc.
 * 
 * All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
 * ANY IMPLIED WARRANTIES OR CONDITIONS OF TITLE, FITNESS FOR A
 * PARTICULAR PURPOSE, MERCHANTABILITY OR NON-INFRINGEMENT.
 * 
 * See the Apache License, Version 2.0 for the specific language
 * governing permissions and limitations under the License.
 ******************************************************************************/
package com.microsoft.aad.adal4jsample;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.microsoft.aad.adal4j.AuthenticationResult;

@Controller
@RequestMapping("/secure/aad")
public class AadController {

	@RequestMapping(method = RequestMethod.GET)
	public String getDirectoryObjects(ModelMap model, HttpServletRequest httpRequest) {
		HttpSession session = httpRequest.getSession();
		AuthenticationResult result = (AuthenticationResult) session.getAttribute(AuthHelper.PRINCIPAL_SESSION_NAME);
		if (result == null) {
			model.addAttribute("error", new Exception("AuthenticationResult not found in session."));
			return "/error";
		} else {
			String data, group;
			try {
				// this is sample code and returns ALL users for this Tennant.
				String accessToken = result.getAccessToken();
				String tenant = session.getServletContext().getInitParameter("tenant");
				data = this.getUsernamesFromGraph(accessToken, tenant);
				model.addAttribute("users", data);
				group = this.getGroupsFromGraph(accessToken, tenant);
				model.addAttribute("group", group);
			} catch (Exception e) {
				e.printStackTrace();
				model.addAttribute("error", e);
				return "/error";
			}
		}
		return "/secure/aad";
	}

	private String getGroupsFromGraph(final String accessToken, final String tenant) throws Exception,
			MalformedURLException {
		URL url = new URL(String.format("https://graph.windows.net/%s/groups?api-version=2013-11-08", tenant));

		JSONObject response = getResponseFromRUL(accessToken, url);
		return createGroupResponse(response);
	}

	private String getGroupFromGraph(final String accessToken, final String tenant, final String userUPN) throws Exception,
			MalformedURLException {
		URL url = new URL(String.format("https://graph.windows.net/%s/users/%s/memberOf?api-version=2013-11-08", tenant,
				userUPN));

		JSONObject response = getResponseFromRUL(accessToken, url);
		return createGroupResponse(response);
	}

	private JSONObject getResponseFromRUL(final String accessToken, URL url) throws IOException, JSONException {
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

		conn.setRequestProperty("Authorization", accessToken);
		conn.setRequestProperty("Accept", "application/json;odata=minimalmetadata");

		int responseCode = conn.getResponseCode();
		String goodRespStr = HttpClientHelper.getResponseStringFromConn(conn, responseCode < 399);

		JSONObject response = HttpClientHelper.processGoodRespStr(responseCode, goodRespStr);
		return response;
	}

	private String createGroupResponse(JSONObject response) throws Exception {
		JSONArray groups = new JSONArray();
		Group group;
		StringBuilder builder = new StringBuilder();
		builder.append("GROUPS <br/>");
		groups = JSONHelper.fetchDirectoryObjectJSONArray(response);
		if (groups != null) {
			for (int i = 0; i < groups.length(); i++) {
				JSONObject thisUserJSONObject = groups.optJSONObject(i);
				group = new Group();
				JSONHelper.convertJSONObjectToDirectoryObject(thisUserJSONObject, group);
				builder.append(group.getDisplayName() + "<br/>");
			}
		}
		return builder.toString();
	}

	private String getUsernamesFromGraph(String accessToken, String tenant) throws Exception {
		URL url = new URL(String.format("https://graph.windows.net/%s/users?api-version=2013-11-08", tenant));
		JSONObject response = getResponseFromRUL(accessToken, url);
		return createUserResponse(accessToken, tenant,response);
	}

	private String getUserFromGraph(String accessToken, String tenant, String userUPN) throws Exception {
		URL url = new URL(String.format("https://graph.windows.net/%s/users/%s?api-version=2013-11-08", tenant, userUPN));
		JSONObject response = getResponseFromRUL(accessToken, url);
		return createUserResponse(accessToken, tenant, response);
	}

	private String createUserResponse(String accessToken, String tenant,JSONObject response) throws Exception {
		JSONArray users = new JSONArray();

		users = JSONHelper.fetchDirectoryObjectJSONArray(response);

		StringBuilder builder = new StringBuilder();
		User user = new User();
		builder.append("USERS <br/>");
		// user.setUserPrincipalName(userPrincipalName);
		// builder.append(user.getUserPrincipalName() + "<br/>");
		if (users != null) {
			for (int i = 0; i < users.length(); i++) {
				JSONObject thisUserJSONObject = users.optJSONObject(i);
				user = new User();
				JSONHelper.convertJSONObjectToDirectoryObject(thisUserJSONObject, user);
				builder.append(user.getUserPrincipalName());
				builder.append("(" + user.getObjectId() + ")");
				builder.append("<br/>");
				
				String userUPN = user.getObjectId();
				String group = this.getGroupFromGraph(accessToken, tenant, userUPN);
				builder.append(group);
			
			}
		}
		return builder.toString();
	}

}
