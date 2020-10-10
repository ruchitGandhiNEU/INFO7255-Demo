/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.example.demo.api;

import com.example.demo.Exceptions.ObjectNotFoundException;
import com.example.demo.utils.MD5Utils;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
public class RestController extends API {

    /**
     *
     */
    private Jedis cache = new Jedis();
    
    /**
     *
     *
     * @param body
     * @param headers
     * @return
     */
    @RequestMapping(value = "/", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<String> save(@RequestBody String body,
            @RequestHeader Map<String, String> headers) {
        String ETag = null;
        try {
            JSONObject jsonObject = validateSchema(body);
            String objType = jsonObject.getString("objectType");
            String objID = jsonObject.getString("objectId");
            String key = getKey(objType, objID);
            cache.set(key, body);

            ETag = MD5Utils.hashString(body);
            String ETagKey = getETagKey(objType, objID);
            cache.set(ETagKey, ETag);

        } catch (JSONException | ValidationException e) {
            return badRequest(e.getMessage());
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
        try {

            String hashString = getEtagString(objectType, objectId);

            if (hashString.equals(ifMatch)) {
                JSONObject jsonObject = findInCache(objectType, objectId);
                return ok(jsonObject.toString(), hashString);
            } else {
                return notModified(null, hashString);
            }

        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
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
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity getJson(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId) {
        String ETag = null;
        JSONObject jsonObject = null;
        try {
            jsonObject = findInCache(objectType, objectId);

            ETag = getEtagString(objectType, objectId);

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
        try {

            String hashString = getEtagString(objectType, objectId);

            if (!hashString.equals(ifNoneMatch)) {
                JSONObject jsonObject = findInCache(objectType, objectId);
                return ok(jsonObject.toString(), hashString);
            } else {
                return notModified(null, hashString);
            }

        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
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
    @RequestMapping(value = "/{object}/{id}", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity deleteJson(@PathVariable("object") String objectType,
            @PathVariable("id") String objectId) {
        try {
            JSONObject jsonObject = findInCache(objectType, objectId);
            cache.del(getKey(objectType, objectId));
            cache.del(getETagKey(objectType, objectId));
        } catch (ObjectNotFoundException ex) {
            return notFound(ex.getMessage());
        } catch (Exception e) {
            return internalServerError(e.getMessage());
        }
        return ok("{ message : 'Object Deleted!'}");
    }

    private JSONObject validateSchema(String json) throws JSONException, ValidationException {

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

    private JSONObject findInCache(String objectType, String objectId) throws ObjectNotFoundException {

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

        return objectType + "|" + objectId;

    }

    private String getETagKey(String objectType, String objectId) {

        return objectType + "|" + objectId + "|" + "ETag";

    }

    private String getEtagString(String objectType, String objectId) {
        return cache.get(getETagKey(objectType, objectId));
    }

    @RequestMapping(value = "/clean-redis", method = RequestMethod.PUT)
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

}
