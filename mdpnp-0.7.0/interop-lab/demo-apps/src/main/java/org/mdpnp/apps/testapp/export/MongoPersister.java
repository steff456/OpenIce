package org.mdpnp.apps.testapp.export;

import com.google.common.eventbus.Subscribe;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;
import ice.Patient;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import jdk.nashorn.api.scripting.ScriptObjectMirror;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Persister to route data to the mongo database. The actual saving of the value is delegated to the javascript
 * handler which is required to implement a function 'persist' that with the following signature:
 *
 * var persist = function(mongoDatabase, patient, value)
 * 1. com.mongodb.client.MongoDatabase mongoDatabase
 * 2. ice.Patient patient
 * 3. org.mdpnp.apps.testapp.export.Value value
 *
 * The function should return { "status" : "OK" } as an indication of success or a description of a failure otherwise.
 *
 */
public class MongoPersister extends DataCollectorAppFactory.PersisterUIController  {

    private static final Logger log = LoggerFactory.getLogger(MongoPersister.class);

    private Invocable invocable;
    private MongoClient mongoClient;
    private MongoDatabase mongoDatabase;
    private StringWriter sw = new StringWriter();

    @FXML
    TextField fHost, fPortNumber, fDbName, fScriptName;

    @Override
    public String getName() {
        return "mongo";
    }

    @Override
    public void setup() {

        // This is a hack to ease data entry if running locally in the lab
        //
        try {
            InetAddress address = InetAddress.getByName("arvi.jsn.mdpnp");
            fHost.setText(address.getHostAddress());
            fDbName.setText("warfighter");
            fScriptName.setText("MongoPersisterWF.js");

        } catch (UnknownHostException nothere) {
            // OK, not running in the lab
        }

    }

    @Override
    public void stop() throws Exception {
        if(mongoClient != null)
            mongoClient.close();
    }

    @Override
    public boolean start() throws Exception {

        String script = fScriptName.getText();
        System.out.println(fScriptName.getText());
        if(isEmpty(script))
        {
        	System.out.println("Empty!!!");
        	return false;        	
        }
        if(!initJSRuntime(script))
        {
        	System.out.println("initJSRuntime!!!");
        	return false;        	
        }
        if(!makeMongoClient())
        {
        	System.out.println("makeMongoClient!!!");
        	return false;        	
        }
        return true;
    }

    @Subscribe
    public void handleDataSampleEvent(final NumericsDataCollector.NumericSampleEvent evt) throws Exception {
        persist(evt);
    }

    void persist(final NumericsDataCollector.NumericSampleEvent evt) throws Exception {

        try {
            if(mongoDatabase == null || evt==null)
                throw new IllegalArgumentException("Mongo or value are null");
            
            System.out.println("................");
            System.out.println(evt.getValue());
            System.out.println(evt.getMetricId());
            HashMap hm = new HashMap();
            hm.put("value", evt.getValue());
            hm.put("time", evt.getDevTime());
            hm.put("metric", evt.getMetricId());
            Document doc = new Document(hm);
            mongoDatabase.getCollection("prueba").insertOne(doc);
//            Object result = invocable.invokeFunction("persistNumeric", mongoDatabase, evt.getValue(), evt.getMetricId(), evt.getDevTime());
            System.out.println("#################");
//            invocable.invokeMethod(result, "print");
//            System.out.println("res: " + result);
            System.out.println("................");
            String status = "OK";
//            ScriptObjectMirror result = (ScriptObjectMirror) invocable.invokeFunction("persistNumeric", mongoDatabase, "", evt);
//            String status = (String) result.get("status");
//            String m = (String) result.get("mongo");
//            String p = (String) result.get("p");
//            String v = (String) result.get("v");
//            System.out.println("mongo;patient;value \n" + m + ";" + p + "p" + v + "v");
            if(!"OK".equals(status)) {
            	System.out.println("Failed to save:" + status);
            }
        }
        catch(Exception ex) {
        	System.out.println("Failed to save" + ex);
        	ex.printStackTrace();
        }
    }

    boolean initJSRuntime(String jsFile) throws ScriptException {

        InputStream is = null;

        if(jsFile.contains(File.separator)) {

            if(!jsFile.contains(":")) {
                File f = new File(jsFile);
                if (!f.exists())
                    throw new ScriptException("File does not exist: " + jsFile);
                try {
                    URL url = f.toURI().toURL();
                    is = url.openStream();
                } catch (Exception ex) {
                    throw new ScriptException("Failed to locate script " + jsFile);
                }
            }
            else {
                try {
                    URL url = new URL(jsFile);
                    is = url.openStream();
                } catch (Exception ex) {
                    throw new ScriptException("Failed to locate script " + jsFile);
                }
            }
        }
        else {
            is = getClass().getResourceAsStream(jsFile);
        }

        if(is != null) {
            ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
            engine.getContext().setWriter(sw);
            engine.eval(new InputStreamReader(is));
            invocable = (Invocable) engine;

            try {
                is.close();
            } catch (IOException toobad) {}
        }
        return invocable != null;
    }

    private boolean makeMongoClient() throws Exception {
    	System.out.println("----------------");
        String host = fHost.getText();
        String port = fPortNumber.getText();
        String dbName = fDbName.getText();
        System.out.println("host;port;dbName" + host + ";" + port + ";" + dbName);
        if (isEmpty(host) || isEmpty(port) || isEmpty(dbName))
        {
        	System.out.println("hay algo mal con la entrada");
        	return false;
        }

        int p = Integer.parseInt(port);

        return makeMongoClient(host, p, dbName);
    }

    boolean makeMongoClient(String host, int port, String dbName) throws Exception {

        mongoClient = new MongoClient(new ServerAddress(host, port),
                                      new MongoClientOptions.Builder().build());
        System.out.println("*******");
        System.out.println("mongoClient: " + mongoClient);
        if(!confirmDatabase(dbName)) {
        	System.out.println("Database '" + dbName + "' not found on " + host + ":" + port);
            return false;
        }

        mongoDatabase = mongoClient.getDatabase(dbName);
        System.out.println("mongoDB: " + mongoDatabase);

        return true;
    }

    private boolean confirmDatabase(String dbName)
    {
        com.mongodb.client.MongoIterable<String> s = mongoClient.listDatabaseNames();
        com.mongodb.client.MongoCursor<String> c = s.iterator();

        while(c.hasNext()) {
            String n = c.next();
            if(dbName.equals(n))
                return true;
        }
        return false;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().length()==0;
    }
}
