package com.customer.solution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/points/")
public class RedemptionAndRefund {
	
	@Autowired
	PostRequest pr;
	
	@PostMapping("redeem/")
	public ResponseEntity<String> redeemPoints(@RequestBody String data){
		
		JSONParser parser = new JSONParser();
		JSONObject dataObj = null;
		String errMsg = "";
		try {
			dataObj = (JSONObject) parser.parse(data);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
		String h_bit_date = (String) dataObj.get("h_bit_date");
		
		String memberId = (String) dataObj.get("h_member_id");
		
		Long h_sponsor_id = (Long) dataObj.get("h_sponsor_id");
		
		String invoice = (String) dataObj.get("h_bit_source_generated_id");
		
		Long hpay_points = (Long) dataObj.get("h_pay_in_points");
		
		String postData = "{\r\n"
				+ "  \"h_program_id\": 260,\r\n"
				+ "  \"h_sponsor_id\":"+h_sponsor_id+",\r\n"
				+ "  \"h_bit_date\":\""+h_bit_date+"\",\r\n"
				+ "  \"h_member_id\":\""+memberId+"\",\r\n"
				+ "  \"h_bit_category\": \"ACCRUAL\",\r\n"
				+ "  \"h_bit_type\": \"PAYMENT_WITH_POINTS\",\r\n"
				+ "  \"h_bit_currency\": \"INR\",\r\n"
				+ "  \"h_pay_in_points\":"+hpay_points+",\r\n"
				+ "  \"h_bit_source\": \"POS\",\r\n"
				+ "  \"h_location\": null,\r\n"
				+ "  \"h_cashier_id\": \"\",\r\n"
				+ "  \"h_bit_source_generated_id\":\""+invoice+"\",\r\n"
				+ "  \"h_pos_id\": \"T-1\"\r\n"
				+ "}";
		
		String jdbcurl = "jdbc:sqlite:C:\\SQLite\\sqlite-tools-win32-x86-3390400\\usersdb.db";
		
		Connection con = null;
		Statement stm = null;
		try {
			con = DriverManager.getConnection(jdbcurl);
			stm = con.createStatement();
			String checkQuery = "SELECT * from transaction_tbl WHERE invoice_id=\""+invoice+"\";";
			ResultSet rs = stm.executeQuery(checkQuery);
			if(rs.next()){
				errMsg = "{\r\n"
						+ "    \"error_code\":\"Duplicate_Invoice_Id\",\r\n"
						+ "    \"erro_message\":\"InvoiceId must be Unique\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_ACCEPTABLE);
			}
		} catch (SQLException ex) {
			ex.getMessage();
		}
		
		
		String responseBody = pr.post(postData);
		
		
		JSONObject jsonObj = null;
		try {
			jsonObj = (JSONObject) parser.parse(responseBody);
		} catch (ParseException e) {
			e.getMessage();
		}
		
		String status = (String) jsonObj.get("status");
		
		if(status.equals("FAILED")) {
			JSONObject error = (JSONObject) jsonObj.get("error");
			String code = (String) error.get("code");
			if(code.equals("INSUFFICIENT_ACCOUNT_BALANCE")) {
				errMsg = "{\r\n"
						+ "    \"error_code\":\"Insufficient_Account_Balance\",\r\n"
						+ "    \"error_message\":\"Insufficient Account Balance\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_ACCEPTABLE);
			}
			else if(code.equals("MEMBER_NOT_FOUND")) {
				errMsg = "{\r\n"
						+ "    \"error_code\":\"MEMBER_NOT_FOUND\",\r\n"
						+ "    \"error_message\":\"Member is not Enrolled\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_FOUND);
			}
			else if(code.equals("INVALID_DATE_FORMAT")) {
				errMsg = "{\r\n"
						+ "    \"error_code\":\"INVALID_DATE_FORMAT\",\r\n"
						+ "    \"error_message\":\"Date format is not valid\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_ACCEPTABLE);
			}
			else if(code.equals("SPONSOR_NOT_FOUND")) {
				errMsg = "{\r\n"
						+ "    \"error_code\":\"SPONSOR_NOT_FOUND\",\r\n"
						+ "    \"error_message\":\"Sponsor ID is not valid\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_ACCEPTABLE);
			}
		}
		
		JSONArray jsonArr = (JSONArray) jsonObj.get("loyalty_balances");
		
		JSONObject jsontag = (JSONObject) jsonArr.get(0);
		
		double latest_balance = (double) jsontag.get("new_balance");
		
		String bitId = (String) jsonObj.get("bit_id");
		
		String ans = "{\r\n"
				+ "    \"member_id\":"+memberId+",\r\n"
				+ "    \"member_balance\":"+latest_balance+",\r\n"
				+ "    \"bit_id\":"+bitId+"\r\n"
				+ "}";
		
		String orderStatus = "REDEEMED";
		
		
		try {
			String query = "INSERT INTO transaction_tbl VALUES('"+invoice+"','"+bitId+"','"+orderStatus+"');";
			stm.execute(query);
			return new ResponseEntity<String>(ans, HttpStatus.CREATED);
		} catch (SQLException e) {
			e.printStackTrace();
			return new ResponseEntity<String>(e.getMessage(),HttpStatus.BAD_GATEWAY);
		}
		finally {
			  if (con != null) {
				  try {
					  con.close(); 
				  }
				  catch (SQLException e) {
					  System.out.println(e.getMessage());
				  }
			  }
			  if (stm != null) {
				  try {
					  stm.close(); 
				  }
				  catch (SQLException e) {
					  System.out.println(e.getMessage());
				  }
			  }
		}
		
	}
	
	
	
	
	
	@PostMapping("reversal/")
	public ResponseEntity<String> refund(@RequestBody String body){
		JSONParser parser = new JSONParser();
		JSONObject jsonObj = null;
		try {
			jsonObj = (JSONObject) parser.parse(body);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		String memberId = (String) jsonObj.get("h_member_id");
		String h_bit_date = (String) jsonObj.get("h_bit_date");
		String invoiceId = (String) jsonObj.get("h_bit_source_generated_id");
		Long sponsor_id = (Long) jsonObj.get("h_sponsor_id");
		
		String jdbcurl = "jdbc:sqlite:C:\\SQLite\\sqlite-tools-win32-x86-3390400\\usersdb.db";
		String postBody = null;
		String bitId = null;
		String status = null;
		String errMsg = null;
		Connection con = null;
		Statement stm = null;
		try {
			con = DriverManager.getConnection(jdbcurl);
			stm = con.createStatement();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		

		
		try {
			String getId = "SELECT * FROM transaction_tbl WHERE invoice_id=\""+invoiceId+"\";";
			ResultSet rs = stm.executeQuery(getId);
			bitId = rs.getString("bit_id");
			status = rs.getString("status");
			if(!rs.next()){
				errMsg = "{\r\n"
						+ "  “error_code”:”Invalid_Invoice_Id”,\r\n"
						+ "  “error_message”: “Invoice_Id not found”\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg,HttpStatus.NOT_FOUND);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if(status.equals("RETURNED")) {
			errMsg = "{\r\n"
					+ "  “error_code”:”Already_Cancelled_Transaction”,\r\n"
					+ "  “error_message”: “The Product has already returned”\r\n"
					+ "}";
			return new ResponseEntity<String>(errMsg, HttpStatus.NOT_ACCEPTABLE);
		}
		
		
		
		postBody = "{\r\n"
				+ "  \"h_member_id\":\""+memberId+"\",\r\n"
				+ "  \"h_bit_date\":\""+h_bit_date+"\",\r\n"
				+ "  \"h_sponsor_id\":"+sponsor_id+",\r\n"
				+ "  \"h_bit_category\": \"CANCELLATION\",\r\n"
				+ "  \"h_bit_type\": \"REVERSAL\",\r\n"
				+ "  \"h_bit_source\": \"POS\",\r\n"
				+ "  \"h_program_id\": 260,\r\n"
				+ "  \"cancel_bit_id\":\""+bitId+"\"\r\n"
				+ "}";
		String responseBody = pr.post(postBody);
		
		JSONObject refRes = null;
		try {
			refRes = (JSONObject) parser.parse(responseBody);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		String resStatus = (String) refRes.get("status");
		if(resStatus.equals("FAILED")) {
			JSONObject error = (JSONObject) refRes.get("error");
			String code = (String) error.get("code");
			if(code.equals("MEMBER_NOT_FOUND")) {
				errMsg = "{\r\n"
						+ "    \"error_code\":\"MEMBER_NOT_FOUND\",\r\n"
						+ "    \"error_message\":\"Member is not Enrolled\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_FOUND);
			}
			else if(code.equals("INVALID_DATE_FORMAT")) {
				errMsg = "{\r\n"
						+ "    \"error_code\":\"INVALID_DATE_FORMAT\",\r\n"
						+ "    \"error_message\":\"Date format is not valid\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_ACCEPTABLE);
			}
			else if(code.equals("SPONSOR_NOT_FOUND")) {
				errMsg = "{\r\n"
						+ "    \"error_code\":\"SPONSOR_NOT_FOUND\",\r\n"
						+ "    \"error_message\":\"Sponsor ID is not valid\"\r\n"
						+ "}";
				return new ResponseEntity<String>(errMsg, HttpStatus.NOT_ACCEPTABLE);
			}
		}
		
		
		String cancel_bit_id = (String) refRes.get("bit_id"); 
		
		String msg = "{\r\n"
				+ " \"h_member_id\": \""+memberId+"\",\r\n"
				+ " \"h_bit_source_generated_id\": \""+invoiceId+"\",\r\n"
				+ "  \"bit_id\": \""+cancel_bit_id+"\",\r\n"
				+ " \"redeem_bit_id\": \""+bitId+"\"\r\n"
				+ "}";
		
		String updateStatusQuery = "UPDATE transaction_tbl SET status=\"RETURNED\" WHERE invoice_id=\""+invoiceId+"\"";
		try {
			stm.execute(updateStatusQuery);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		return new ResponseEntity<String>(msg, HttpStatus.OK); 
		
	}
	
}
