/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.demo.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpHeaders;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 *
 * @author Ruchit Gandhi <gandhi.ruc at Northeastern.edu>
 */

@Component
public class QueueListenerService {
    
    public int tempCount = 0;
    
    
    public String global_uri = "";
    public String global_indexName = "";

    public void receiveMessage(Map<String, String> message) {
        System.out.println("Message received: " + message);
        String operation = message.get("operation");
        String uri = message.get("uri");
        String body = message.get("body");
        String indexName = message.getOrDefault("index","plan");
        String mainObjectId = message.getOrDefault("mainObjectId", "1");
        
        global_uri = uri;
        global_indexName = indexName;

        JSONObject jsonBody = new JSONObject(body);

        switch (operation) {
            case "SAVE": {
                System.out.println("SAVE OPERATION CALLED FOR "+jsonBody.getString("objectType") + " : "+ jsonBody.getString("objectId"));
                this.indexObject(global_uri, global_indexName, jsonBody, mainObjectId);
                break;
            }
            case "DELETE": {
                this.deleteIndex(uri, indexName, jsonBody);
                break;
            }
        }
    }

    private int executeRequest(HttpUriRequest request) {
        int result = 0;
        try (CloseableHttpClient httpClient = HttpClients.createDefault();
                CloseableHttpResponse response = httpClient.execute(request)) {
            System.out.println("ElasticSearch Response: " + response.toString());
            result = response.getStatusLine().getStatusCode();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

     private JSONObject printTest(JSONObject object, String mainObjectType, String mainObjectID, String thiskey, String joinName,Map<String, Set<String>> relationMap) {
         
         JSONObject thisObjectOnly = new JSONObject();
        
//        System.out.println(" printTest() CALLED!!!!!!!!!!! -  | mainObjectType : "+mainObjectType+" | mainObjectID : "+mainObjectID);
        System.out.println(" =================================================XXXXXXX | "+thiskey+" | START -  "+ tempCount++ +"  XXXXXXXXX=================================================  ");
        JSONObject obj = new JSONObject();

        for (String key : object.keySet()) {
            Object value = object.get(key);
            
            
            if (value instanceof JSONObject) {
                
                JSONObject nestedObj = new JSONObject();
                JSONObject nestedProperties = printTest((JSONObject) value, mainObjectType, mainObjectID, key,joinName,relationMap);
                if (nestedProperties.length() > 0) {
                    nestedObj.put("properties", nestedProperties);
                }
                
              nestedObj.put("type", "nested");
              obj.put(key, nestedObj);
                
            } else if (value instanceof JSONArray) {
                 JSONArray jsonArray = (JSONArray) value;
                 for (Object object1 : jsonArray) {
                     printTest((JSONObject) object1, mainObjectType, mainObjectID, key, joinName,relationMap);
                 }
             } else {
                 thisObjectOnly.put(key, value);
             }
         }

         System.out.println(" --------------------- ------------------- " + thiskey + " -----------------------");

         if (!mainObjectType.equals(thisObjectOnly.get("objectType"))) {
             JSONObject childJoin = new JSONObject();
             childJoin.put("name", thisObjectOnly.getString("objectType"));
             childJoin.put("parent", mainObjectID);
             thisObjectOnly.put(joinName, childJoin);
             
             Set<String> rSet = relationMap.getOrDefault(global_indexName, new HashSet<String>());
             rSet.add(thisObjectOnly.getString("objectType"));
             relationMap.put(global_indexName, rSet);
             
         }

         System.out.println(thisObjectOnly.toString(6));
         
         this.indexObject(global_uri, global_indexName, thisObjectOnly, mainObjectID);
         System.out.println(" --------------------- ------------------- -----------------------");


        System.out.println(" =================================================XXXXXXXX END XXXXXXXX=================================================  ");
        return obj;
     
     }
    

    private JSONObject getMappingJSON(JSONObject object, String mainObjectType, String mainObjectID) {
        

        JSONObject obj = new JSONObject();

        for (String key : object.keySet()) {
            Object value = object.get(key);
            if (value instanceof JSONObject) {
                JSONObject nestedObj = new JSONObject();
                JSONObject nestedProperties = getMappingJSON((JSONObject) value, mainObjectType, mainObjectID);
                if (nestedProperties.length() > 0) {
                    nestedObj.put("properties", nestedProperties);
                }

                
               nestedObj.put("type", "nested");
                
              
                
                obj.put(key, nestedObj);
                
          
                

            } else if (value instanceof JSONArray) {
                
          
                JSONArray jsonArray  = (JSONArray) value;
                
                
                JSONObject nestedObj = new JSONObject();
                JSONObject nestedProperties = getMappingJSON(((JSONArray) value).getJSONObject(0), mainObjectType, mainObjectID);
                if (nestedProperties.length() > 0) {
                    nestedObj.put("properties", nestedProperties);
                }
                nestedObj.put("type", "nested");
                obj.put(key, nestedObj);
            }
        }
      
        return obj;
    }

 
    public String getJoinName(String indexName, String subname){
        return indexName+"_"+subname;
    }

    private void indexObject(String uri, String indexName, JSONObject objectBody, String mainObjectId) {
        
        if(mainObjectId == null || mainObjectId.isEmpty() || mainObjectId.trim().equals("")){
            mainObjectId="1";
        }
        
        System.out.println("com.tejas.bdidemo.listener.IndexingListener.indexObject()");
        System.out.println(uri + "/" + indexName + "/_doc/" + objectBody.getString("objectId"));
        HttpPut request = new HttpPut(uri + "/" + indexName + "/_doc/" + objectBody.getString("objectId") + "?routing="+ mainObjectId);
        request.addHeader(HttpHeaders.CONTENT_TYPE, String.valueOf(ContentType.APPLICATION_JSON));
        try {
            request.setEntity(new StringEntity(objectBody.toString()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        this.executeRequest(request);
    }

    private void deleteIndex(String uri, String indexName, JSONObject objectBody) {
        HttpDelete request = new HttpDelete(uri + "/" + indexName + "/_doc/" + objectBody.getString("objectId"));

        this.executeRequest(request);
    }
}
