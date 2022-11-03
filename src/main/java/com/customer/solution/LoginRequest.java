package com.customer.solution;

import java.io.IOException;




import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.ParseException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;



public class LoginRequest {

	static HttpResponse<String> loginResponse = null;
	static HttpRequest loginRequest = null;
	public static String token;
	
	public String getTokenFromLogin() throws ParseException {
		String body = "{\r\n"
				+ "  \"username\": \"bhanu.prakash+1@lji.io\",\r\n"
				+ "  \"password\": \"Testing@123\"\r\n"
				+ "}\r\n"
				+ "";		
		try {
		loginRequest = HttpRequest.newBuilder()
			.uri(new URI("https://api.gravtee.com/v1/login/?basic_info=true"))
			.header("x-api-key", "JgxrFqZvkD4fcTX7vgMOX1L1i58TkM65qkLfijj0")
			.POST(BodyPublishers.ofString(body))
			.build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HttpClient httpclient = HttpClient.newHttpClient();
		try {
			loginResponse = httpclient.send(loginRequest, BodyHandlers.ofString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		
		JSONParser parser = new JSONParser();
		Object obj = null;
		try {
			obj = parser.parse(loginResponse.body());
		} catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
		
		JSONObject tokenObj = (JSONObject) obj;
		
		
		token = "JWT " + (String) tokenObj.get("token");
		
		
		
		
		return token;
		
	}
	
	
	
	public static void main(String args[]){
		
	}
}
