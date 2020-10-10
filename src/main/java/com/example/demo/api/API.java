/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.demo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Ruchit Gandhi <gandhi.ruc at Northeastern.edu>
 */
public abstract class API {
    
    protected final String createdMessage = "object Saved!";
    protected final String objectNotFoundMessage = "No such Object found for ObjectType/ObjectId.";
    
    @RequestMapping("/test")
    @ResponseBody
    public ResponseEntity testMessage() {
        return ok("{ message : 'Welcolme to INFO 7255'}");
    }
    
    /**
     * Returns a 200 response with no body.
     */
    protected ResponseEntity ok() {
        return ResponseEntity.ok().build();
    }

    /**
     * Returns a 200 response with a JSON body.
     */
    protected ResponseEntity ok(String jsonBody) {
        return ResponseEntity.ok(jsonBody);
    }
    
    /**
     * Returns a 200 response with a JSON body and ETag.
     */
    protected ResponseEntity ok(String jsonBody, String ETag) {
        return ResponseEntity.status(HttpStatus.OK).eTag(ETag).body(jsonBody);
    }
    
    /**
     * Returns a 201 response with specified body.
     */
    protected ResponseEntity created(String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body("{ message : '" + message + "'}");
    }
    
    /**
     * Returns a 201 response with specified body and ETag.
     */
    protected ResponseEntity created(String message, String ETag) {
        return ResponseEntity.status(HttpStatus.CREATED).eTag(ETag).body("{ message : '" + message + "'}");
    }
    
    /**
     * Returns a 304 response with a JSON body and ETag.
     */
    protected ResponseEntity notModified(String jsonBody, String ETag) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(ETag).body(jsonBody);
    }
    

    /**
     * Returns an error response with a message in the JSON body.
     */
    protected ResponseEntity error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body("{ message : '" + message + "' }");
    }

    /**
     * Returns a 400 response with a message in the JSON body.
     */
    protected ResponseEntity badRequest(String message) {
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Returns a 401 response with a message in the JSON body.
     */
    protected ResponseEntity unauthorized(String message) {
        return error(HttpStatus.UNAUTHORIZED, message);
    }

    /**
     * Returns a 404 response with message in the JSON body.
     */
    protected ResponseEntity notFound(String message) {
        return error(HttpStatus.NOT_FOUND, message);
    }

    /**
     * Returns a 500 response with message in the JSON body.
     */
    protected ResponseEntity internalServerError(String message) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

}
