/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.demo.api;

import com.example.demo.Exceptions.AppException;
import com.example.demo.Exceptions.ObjectNotFoundException;
import com.example.demo.service.JsonService;
import com.example.demo.utils.MD5Utils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonMergePatch;
import javax.json.JsonPatch;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.ws.rs.Consumes;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

/**
 *
 * @author Ruchit Gandhi <gandhi.ruc at Northeastern.edu>
 */
@Controller
@RequestMapping(produces = "application/json")
public class RestController extends API {

    /**
     *
     */
    private Jedis cache = new Jedis();

    @Autowired(required = false)
    JsonService jsonService;
    
    private static String INDEX_NAME = "plan";

    /**
     *
     *
     * @param body
     * @param headers
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity save(@RequestBody String body,
            @RequestHeader Map<String, String> headers) {
        String ETag = null;
        try {
            JSONObject jsonObject = validateSchema(body);
            JSONObject cloneJsonObject = new JSONObject(
                    new JSONTokener(body)
            );
            String objType = jsonObject.getString("objectType");
            String objID = jsonObject.getString("objectId");

            if (cache.get(getKey(objType, objID)) != null) {
                System.out.println("Already Exist!");
                throw new AppException(alreadyExistsMessage);
            }

            String key = this.jsonService.savePlan(jsonObject, objType);
            
            Set<String> nameSet = new HashSet<>();
            jsonService.sendEachObject(cloneJsonObject, objType, objID, objType ,objType+"_join", nameSet, null, null);
            
            JSONObject plan = this.jsonService.getPlan(key);
            if (plan == null) {
                throw new ObjectNotFoundException("Unable to get plan");
            }

            ETag = MD5Utils.hashString(plan.toString());
            String ETagKey = getETagKey(objType, objID);
            cache.set(ETagKey, ETag);

        } catch (ValidationException e) {
            return badRequest(e.getMessage());
        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (NoSuchAlgorithmException e) {
            return internalServerError(e.getMessage());
        } catch (AppException e) {
            return conflict(e.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }

        return created(createdMessage, ETag);

    }

    /**
     *
     * @param objectType
     * @param objectId
     * @param ifMatch
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.GET, headers = "If-Match")
    @ResponseBody
    public ResponseEntity getJsonIfMatch(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId,
            @RequestHeader(name = HttpHeaders.IF_MATCH) String ifMatch) {
        JSONObject jsonObject = null;
        try {

            String hashString = getEtagString(objectType, objectId);

            if (hashString.equals(ifMatch)) {
                jsonObject = this.jsonService.getPlan(getKey(objectType, objectId));
                if (jsonObject == null || jsonObject.isEmpty()) {
                    throw new ObjectNotFoundException(objectNotFoundMessage);
                }
                return ok(jsonObject.toString(), hashString);
            } else {
                return notModified(null, hashString);
            }

        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
    }

    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public ResponseEntity getJson(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId) {
        String ETag = null;
        JSONObject jsonObject = null;
        try {
            jsonObject = this.jsonService.getPlan(getKey(objectType, objectId));
            if (jsonObject == null || jsonObject.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
            ETag = getEtagString(objectType, objectId);

        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok(jsonObject.toString(), ETag);

    }

    /**
     *
     * @param objectType
     * @param objectId
     * @param ifNoneMatch
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.GET, headers = "If-None-Match")
    @ResponseBody
    public ResponseEntity getJsonIfNoneMatch(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH) String ifNoneMatch) {
        JSONObject jsonObject = null;
        try {

            String hashString = getEtagString(objectType, objectId);

            if (!hashString.equals(ifNoneMatch)) {
                jsonObject = this.jsonService.getPlan(getKey(objectType, objectId));
                if (jsonObject == null || jsonObject.isEmpty()) {
                    throw new ObjectNotFoundException(objectNotFoundMessage);
                }
                return ok(jsonObject.toString(), hashString);
            } else {
                return notModified(null, hashString);
            }

        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
    }

    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.PATCH, produces = "application/json")
    @ResponseBody
    public ResponseEntity patchJson(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId, @RequestBody String body) {
        String ETag = null;
        JSONObject jsonObject = null;
        String resultJsonString = null;
        try {

            JSONObject bodyJson = validateSchema(body);

            jsonObject = this.jsonService.mergeJson(bodyJson, getKey(objectType, objectId));
            if (jsonObject == null || jsonObject.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
            
            resultJsonString = jsonObject.toString();
            String keyString = this.jsonService.updatePlan(jsonObject, (String)jsonObject.get("objectType"));
            
            if (keyString == null || keyString.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
             
             
            ETag = MD5Utils.hashString(resultJsonString);
            String ETagKey = getETagKey(objectType, objectId);
            cache.set(ETagKey, ETag);

        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok(resultJsonString, ETag);

    }
    
    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.PATCH, produces = "application/json", headers = "If-None-Match")
    @ResponseBody
    public ResponseEntity patchJsonIfNoneMatch(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId, @RequestBody String body,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH) String ifNoneMatch) {
        String ETag = null;
        JSONObject jsonObject = null;
        String resultJsonString = null;
        try {
            
            String hashString = getEtagString(objectType, objectId);
            if(hashString.equals(ifNoneMatch)){
                return notModified(null, hashString);
            }
            JSONObject bodyJson = validateSchema(body);

            jsonObject = this.jsonService.mergeJson(bodyJson, getKey(objectType, objectId));
            if (jsonObject == null || jsonObject.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
            
             resultJsonString = jsonObject.toString();
            String keyString = this.jsonService.updatePlan(jsonObject, (String)jsonObject.get("objectType"));
            
            if (keyString == null || keyString.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
            
            ETag = MD5Utils.hashString(resultJsonString);
            String ETagKey = getETagKey(objectType, objectId);
            cache.set(ETagKey, ETag);

        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok(resultJsonString, ETag);

    }
    
    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.PATCH, produces = "application/json", headers = "If-Match")
    @ResponseBody
    public ResponseEntity patchJsonIfMatch(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId, @RequestBody String body,
            @RequestHeader(name = HttpHeaders.IF_MATCH) String ifMatch) {
        String ETag = null;
        JSONObject jsonObject = null;
        String resultJsonString = null;
        try {
            
            String hashString = getEtagString(objectType, objectId);
            if(!hashString.equals(ifMatch)){
                return notModified(null, hashString);
            }
            JSONObject bodyJson = validateSchema(body);

            jsonObject = this.jsonService.mergeJson(bodyJson, getKey(objectType, objectId));
            if (jsonObject == null || jsonObject.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
            resultJsonString = jsonObject.toString();
            String keyString = this.jsonService.updatePlan(jsonObject, (String)jsonObject.get("objectType"));
            
            if (keyString == null || keyString.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
            
            ETag = MD5Utils.hashString(resultJsonString);
            String ETagKey = getETagKey(objectType, objectId);
            cache.set(ETagKey, ETag);

        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok(resultJsonString, ETag);

    }

    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.PUT, produces = "application/json")
    @ResponseBody
    public ResponseEntity putJson(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId, @RequestBody String body) {
        String ETag = null;
        String key = null;
        JSONObject plan = null;
        try {

            //validate schema
            JSONObject bodyJson = validateSchema(body);

            //update object and get key
            key = this.jsonService.updatePlan(bodyJson, (String) bodyJson.get("objectType"));
            if (key == null || key.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage + "-");
            }

            //Get saved obj on based of key
            plan = this.jsonService.getPlan(key);
            if (plan == null || plan.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }
            

            ETag = MD5Utils.hashString(plan.toString());
            String ETagKey = getETagKey(objectType, objectId);
            cache.set(ETagKey, ETag);

        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok(plan.toString(), ETag);

    }
    
    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.PUT, produces = "application/json", headers = "If-None-Match")
    @ResponseBody
    public ResponseEntity putJsonIfNoneMatch(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId, @RequestBody String body,
            @RequestHeader(name = HttpHeaders.IF_NONE_MATCH) String ifNoneMatch) {
        
        String ETag = null;
        String key = null;
        JSONObject plan = null;

        try {
            
            String hashString = getEtagString(objectType, objectId);
            if(hashString.equals(ifNoneMatch)){
                return notModified(null, hashString);
            }

            //validate schema
            JSONObject bodyJson = validateSchema(body);

            //update object and get key
            key = this.jsonService.updatePlan(bodyJson, (String) bodyJson.get("objectType"));
            if (key == null || key.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }

            //Get saved obj on based of key
            plan = this.jsonService.getPlan(key);
            if (plan == null || plan.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            } 
    

            ETag = MD5Utils.hashString(plan.toString());
            String ETagKey = getETagKey(objectType, objectId);
            cache.set(ETagKey, ETag);

        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok(plan.toString(), ETag);

    }
    
    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.PUT, produces = "application/json", headers = "If-Match")
    @ResponseBody
    public ResponseEntity putJsonIfMatch(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId, @RequestBody String body,
            @RequestHeader(name = HttpHeaders.IF_MATCH) String ifMatch) {
        
        String ETag = null;
        String key = null;
        JSONObject plan = null;
        
        try {
            
            String hashString = getEtagString(objectType, objectId);
            if(!hashString.equals(ifMatch)){
                return notModified(null, hashString);
            }

            //validate schema
            JSONObject bodyJson = validateSchema(body);

            //update object and get key
            key = this.jsonService.updatePlan(bodyJson, (String) bodyJson.get("objectType"));
            if (key == null || key.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            }

            //Get saved obj on based of key
            plan = this.jsonService.getPlan(key);
            if (plan == null || plan.isEmpty()) {
                throw new ObjectNotFoundException(objectNotFoundMessage);
            } 
            
            
           

            ETag = MD5Utils.hashString(plan.toString());
            String ETagKey = getETagKey(objectType, objectId);
            cache.set(ETagKey, ETag);
            
            

        } catch (JSONException ex) {
            return badRequest(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok(plan.toString(), ETag);

    }

    /**
     *
     * @param objectType
     * @param objectId
     * @return
     */
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.DELETE, produces = "application/json")
    @ResponseBody
    public ResponseEntity deleteJson(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId) {
        try {
            if (!this.jsonService.deletePlan(getKey(objectType, objectId))) {
                
                throw new ObjectNotFoundException("Done");
            }
            cache.del(getETagKey(objectType, objectId));
        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok("{ 'Message' : 'Object Deleted!'}");
    }
    
    

    private JSONObject validateSchema(String json) throws ValidationException, JSONException {

        InputStream schemaStream = RestController.class.getResourceAsStream("/schema.json");

        JSONObject jsonSchema = new JSONObject(
                new JSONTokener(schemaStream)
        );

        JSONObject jsonSubject = new JSONObject(
                new JSONTokener(json)
        );

        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(jsonSubject);

        return jsonSubject;

    }

    private JSONObject findInCache(String objectType, String objectId) throws ObjectNotFoundException, JSONException {

        JSONObject res = null;
        String key = getKey(objectType, objectId);
        String value = cache.get(key);
        if (value == null) {
            throw new ObjectNotFoundException(objectNotFoundMessage);
        }
        res = new JSONObject(
                new JSONTokener(value)
        );
        return res;
    }

    private String getKey(String objectType, String objectId) {

        return objectType + "_" + objectId;

    }

    private String getETagKey(String objectType, String objectId) {

        return getKey(objectType, objectId) + "|" + "ETag";

    }

    private String getEtagString(String objectType, String objectId) {
        return cache.get(getETagKey(objectType, objectId));
    }

    @RequestMapping(value = "/clean-redis", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity cleanRedis() {

        try {
            Set<String> keys = cache.keys("*");
            cache.del(keys.toArray(new String[keys.size()]));
        } catch (Exception e) {
            return internalServerError(e.getLocalizedMessage());
        }

        return ok("{ message : '" + "All objects deleted!" + "'}");

    }

    public static String format(JsonValue json) {
        StringWriter stringWriter = new StringWriter();
        prettyPrint(json, stringWriter);
        return stringWriter.toString();
    }

    public static void prettyPrint(JsonValue json, Writer writer) {
        Map<String, Object> config
                = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory writerFactory = Json.createWriterFactory(config);
        try (JsonWriter jsonWriter = writerFactory.createWriter(writer)) {
            jsonWriter.write(json);
        }
    }

}
