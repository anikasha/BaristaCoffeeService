/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.service;

import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.HashMap;
import java.util.Map;
import java.util.*;
import java.io.*;
import java.net.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.parsers.*;
import org.xml.sax.InputSource;
import org.w3c.dom.*;


/**
 * This is the Microservice resource class.
 * See <a href="https://github.com/wso2/msf4j#getting-started">https://github.com/wso2/msf4j#getting-started</a>
 * for the usage of annotations.
 *
 * @since 0.1-SNAPSHOT
 */
@Path("/service")
public class BarristaService {


    //TODO: Replace Java Hashmaps by use of WSO2 Data Services for persistenc (need simple table structure support as below).   
    
    Map<String, String[]> allOrders = new HashMap<>(); //all orders
    //Structure
    //(OrderId, [CustomerName, Drink, Price])

    List<String> previousOrders = new ArrayList<>();
    
    Map<String, String> Coffee = new HashMap(); //all drink options
    //Structure
    //(Drink, Price)


//Utility classes

    //Generates a new order number 
    int genNewOrderNumber() { 
	Random rand = new Random();
	return rand.nextInt(1000);
    }


public static String getCharacterDataFromElement(Element e) {
    Node child = e.getFirstChild();
    if (child instanceof CharacterData) {
       CharacterData cd = (CharacterData) child;
       return cd.getData();
    }
    return "?";
  }


    @GET
    @Path("/{name}")
    public String hello(@PathParam("name") String name) {
	System.out.println("GET invoked");
        return "\nHello from WSO2 MSF4J v 2.1.0-" + name + "\n";
    }

//Service #0
    //Initializing Coffee Menu
    @GET
    @Path("coffeemenu")
    public Response initCoffee () {
		//Coffee.put("Latte", "2.00");
		//Coffee.put("Mocha", "3.00");
		//Coffee.put("BrewedCoffee", "1.00");
		//Coffee.put("Tea","2.50");
		//Coffee.put("MintDrink","4.00");
		String str = null;

		try {

	        URL url = new URL ("http://10.200.0.144:9763/services/MyFirstDS/getCoffee");
	       URLConnection urlc = url.openConnection();
		urlc.setDoOutput(true);
       		 urlc.setAllowUserInteraction(false);
	
       		 //get result
        	BufferedReader br = new BufferedReader(new InputStreamReader(urlc
            .getInputStream()));
        String l = null;
        while ((l=br.readLine())!=null) {
            System.out.println(l);
	    System.out.println('\n');
	str = str + l;
        }

        br.close();
	} catch (Exception e){
		e.printStackTrace();
}

try {
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    InputSource is = new InputSource();
	    is.setCharacterStream(new StringReader(str));

	    Document doc = db.parse(is);
	    NodeList nodes = doc.getElementsByTagName("Entry");

	    // iterate the menu
	    for (int i = 0; i < nodes.getLength(); i++) {
		Element element = (Element) nodes.item(i);

		NodeList name = element.getElementsByTagName("id");
		Element line = (Element) name.item(0);
		System.out.println("ID: " + getCharacterDataFromElement(line));

		NodeList title = element.getElementsByTagName("drink");
		line = (Element) title.item(0);
		String s1 = getCharacterDataFromElement(line);
		System.out.println("Drink: " + getCharacterDataFromElement(line));

		NodeList price = element.getElementsByTagName("price");
		line = (Element) price.item(0);
		String s2 = getCharacterDataFromElement(line);
		System.out.println("Price: " + getCharacterDataFromElement(line));
		
		Coffee.put(s1,s2);
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}


		System.out.println("Initialized Coffee Type Menu"); //debug
		String s = "Initialized Coffee Type Menu:\nLatte\nMocha\nBrewedCoffee\nTea\nMintDrink";
		return Response.status(200)
		    .entity(s)
		    .build();
	}
    
    //Service #1
    //Customer placing an order for a new drink.
    //TBD: Should be HTTP POST. Need to fix Reponse codes. 
    @GET
    @Path("order/{name}/{type}")
    public Response placeOrder(@PathParam ("name") String name, //Customer Name
			     @PathParam("type") String type) { //Coffee Type
		
    		//Check if the coffee type that customer asked for exists 
    		if (!Coffee.containsKey(type.trim())) { 
    			System.out.println("The coffee Type:" + type + " doesn't exist");
    			String s = "The drink type: " + type + " doesn't exist";
			return Response.status(200)
			    .entity(s)
			    .build();
    		}
		

			String[] s = new String[3];
			s[0] = name; s[1] = type; s[2] = Coffee.get(type);
			allOrders.put(Integer.toString(genNewOrderNumber()), s);
			
		        String msg =  "Order Placed\nYour Price is: "+ Coffee.get(type);
			
			return Response.status(200)
			    .entity(msg)
			    .build();
    }

   
    // Service #2
    //Customer making changes to an already placed order v2
    @GET
    @Path("changeorder/{orderid}/{name}/{neworder}")
    public Response changeOrder(@PathParam ("orderid") String orderid, //Order ID
			      @PathParam ("name") String name, //Customer name
			      @PathParam ("neworder") String neworder) {//New customer order

	if (!Coffee.containsKey(neworder.trim())) { 
    			System.out.println("The coffee Type:" + neworder + " doesn't exist");
    			String msg =  "The drink type: " + neworder + " doesn't exist";
			return Response.status(200).entity(msg).build();
    		}

       
	//Check if this is a valid orderid
	if (allOrders.containsKey(orderid.trim())) {
	    
	    String[] s = new String[3];

	    s = allOrders.get(orderid);
		    
	    s[0] = name; s[1] = neworder; s[2] = Coffee.get(neworder);

    	    allOrders.remove(orderid.trim());
	    
	    allOrders.put(orderid.trim(), s);
	    
	    String msg = "The customer " + name + " has successfully changed his/her order";
	    return Response.status(200).entity(msg).build();
	} else {
	    String msg = "The customer " + name + "  hasn't placed his/her order yet";
	    return Response.status(200).entity(msg).build();
	}
    }

    //Service #4
    //Barista retreiving a list of orders
    @GET
    @Path("retrieveorders")
    public String retreiveOrders () {

	Set<String> allOrderNumbers = allOrders.keySet();
	
	String x=""; String[] info = new String[3];
	for (String s : allOrderNumbers) {
	    info = allOrders.get(s);
	    x = x + s + ":" + info[0] + "." + info[1] + "." + info[2] + "\n";
	}
	return x;
    }

    void dummyCashRegister(String a, String b, String c, String d) {
	
	System.out.println ("Message from Cash Register: Successfully Paid");
	
    }

    //Service #3 and #6
    //Cutomer making payments to his/her order and receiving his/her drink
    //Barista will remove the order after payment
    @GET
    @Path("payorder/{orderid}/{cardNo}/{expires}")
    public Response payYourOrder(@PathParam ("orderid") String orderid,
			       @PathParam ("cardNo") String cardNo,
			       @PathParam ("expires") String expires) {

	String[] s = new String[3];

        s = allOrders.get(orderid);
	
	if (allOrders.containsKey(orderid.trim())) { //Validate order ID
	    
	    //Pay (Customer name and price)
	    dummyCashRegister (s[0], s[2], cardNo, expires);

	    previousOrders.add(orderid);

	    allOrders.remove(orderid.trim());
	 
	    //return "Customer: " + s[0] + " received drink and paid this amount =" + s[2];
	    String msg = "<payment>"+"<cardNo>"+cardNo+"</cardNo>"+"<expires>"+expires+"</expires>"+"<name>"+s[0]+"</name>"+"<amount>"+s[2]+"</amount>"+"</payment>";
	    System.out.println (msg);
	    return Response.status(201).entity (msg).build();
	}  
	 else {
	    //return "Order ID: " + orderid + " doesn't exist";
		 String msg = "Order ID" + orderid + "  doesn't exist";
		 return Response.status(403).entity(msg).build();
	 }
    }

    //Service #5
    //Barista checks if the payment has been received for an order
    @GET
    @Path("paystatus/{orderid}")
    public String payStatus(@PathParam ("orderid") String orderid) {

	if (previousOrders.contains(orderid)) {
	    return "Order " + orderid + " has been paid";
	} else {
	    if (!allOrders.containsKey(orderid.trim())) {
		return "Orderid " + orderid + " is not valid";
		} else { 
		     return "Payment on order number " + orderid + " is still pending";
	    }
	}
	
    }
}
