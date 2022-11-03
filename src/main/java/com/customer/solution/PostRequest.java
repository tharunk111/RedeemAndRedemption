package com.customer.solution;

import java.io.IOException;



import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.ParseException;

import org.springframework.stereotype.Component;


@Component
public class PostRequest {
	
	public String post(String postBody) {
		String body = postBody;
		//String token = SaveToken.getToken();
		
		LoginRequest lr = new LoginRequest();
		String token = null;
		try {
			token = lr.getTokenFromLogin();
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		HttpRequest postRequest = null;
		try {
			postRequest = HttpRequest.newBuilder()
			.uri(new URI("https://api.gravtee.com/v1/bits/"))
			.headers("Authorization", token, "x-api-key", "JgxrFqZvkD4fcTX7vgMOX1L1i58TkM65qkLfijj0")
			.POST(BodyPublishers.ofString(body))
			.build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpResponse<String> postResponse = null;
		try {
			postResponse = httpClient.send(postRequest, BodyHandlers.ofString());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		return postResponse.body();
	}
	
	public static void main(String args[]) throws ParseException {	
	}
}
