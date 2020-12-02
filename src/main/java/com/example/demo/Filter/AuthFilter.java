package com.example.demo.Filter;

import java.io.BufferedReader;
import javax.servlet.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class AuthFilter implements Filter {

    /**
     *
     * @param servletRequest
     * @param servletResponse
     * @param filterChain
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("FILTERRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRRR");
        
 

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        
        final String header = request.getHeader("Authorization");
        
        if(header == null || header.isEmpty()){
            errorResponse(response);
            return;
        }

        String token = null;
        token = header.substring(7);

        if (token.isEmpty() || token.trim().equals("")) {
            errorResponse(response);
            return;
        }
        
        InputStream is = getClass().getClassLoader().getResourceAsStream("google-client-secret.json");
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            org.json.simple.JSONObject jsonObj = null;
        try {
            jsonObj = (org.json.simple.JSONObject) new JSONParser().parse(streamReader);
        } catch (ParseException ex) {
            Logger.getLogger(AuthFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        org.json.simple.JSONObject jsonData = (org.json.simple.JSONObject) jsonObj.get("web");
        HttpGet contactGetRequest = new HttpGet(jsonData.get("token_info")+token);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpResponse contactResponse = httpclient.execute(contactGetRequest);
        String responseJSON = EntityUtils.toString(contactResponse.getEntity(), Charset.forName("UTF-8"));
        
        if(responseJSON == null || responseJSON.isEmpty() || responseJSON.trim().equals("")){
            errorResponse(response);
            return;            
        }
        
        JSONObject tokenResponseJson = null;
        try {
           tokenResponseJson = (JSONObject) new JSONParser().parse(responseJSON);
        } catch (ParseException ex) {
            Logger.getLogger(AuthFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        if(tokenResponseJson==null){
            errorResponse(response);
            return;
        }
        
        if(tokenResponseJson.get("error") != null){
            errorResponse(response);
            return;
        }
        
        if(!(tokenResponseJson.get("issued_to") != null && tokenResponseJson.get("issued_to").equals(jsonData.get("client_id")))){
            errorResponse(response);
            return;
        }        
 
        filterChain.doFilter(request, response);
    }
    
    public void errorResponse(HttpServletResponse response) throws IOException{

            JSONObject newJsonObj = new JSONObject();
            newJsonObj.put("message", "Invalid Token or Token Expired!");
            String Json = newJsonObj.toString();
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getOutputStream().println(Json);
            response.setContentType("application/json");
            return;
   
    }
}
