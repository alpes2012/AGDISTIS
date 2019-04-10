package org.aksw.agdistis.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RelatedEntitiesBuffer {

    private JSONArray jsonArray;

    private HashMap<String, ArrayList<String>> buffer;

    private String bufferFileName;

    public RelatedEntitiesBuffer(String bufferFileName) throws Exception {
        this.buffer = new HashMap<>(10240);
        this.bufferFileName = bufferFileName;

        if (this.fileExists(this.bufferFileName)) {
            this.jsonArray = JSON.parseArray(this.readToString(this.bufferFileName));
            if (this.jsonArray != null)
                this.jsonArrayToHashMap();
        }


    }

    public void add(String id, ArrayList<String> relatedEntities) throws Exception {
        this.buffer.put(id, relatedEntities);
    }

    public ArrayList<String> get(String id) throws Exception {
        return this.buffer.get(id);
    }

    public boolean isContain(String id) {
        return this.buffer.containsKey(id);
    }

    public void writeToFile() throws Exception {
        JSONWriter jsonWriter = new JSONWriter(new FileWriter(this.bufferFileName));
        jsonWriter.startArray();
        Iterator it = this.buffer.entrySet().iterator();
        while (it.hasNext()) {
            jsonWriter.startObject();
            HashMap.Entry entry = (HashMap.Entry) it.next();
            String id = (String) entry.getKey();
            ArrayList<String> related = (ArrayList<String>) entry.getValue();
            jsonWriter.writeKey("id");
            jsonWriter.writeValue(id);
            jsonWriter.writeKey("related");
            jsonWriter.startArray();

            for (String subId: related) {
                jsonWriter.startObject();
                jsonWriter.writeKey("id");
                jsonWriter.writeValue(subId);
                jsonWriter.endObject();
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        }

        jsonWriter.endArray();
        jsonWriter.flush();
        jsonWriter.close();
    }

    private void jsonArrayToHashMap() throws Exception {
        Iterator it = this.jsonArray.iterator();
        while (it.hasNext()) {
            String id;
            ArrayList<String> relatedEntities = new ArrayList<>();

            JSONObject json = (JSONObject) it.next();
            id = json.getString("id");

            Iterator it2 = ((JSONArray) json.getObject("related", JSONArray.class)).iterator();
            while (it2.hasNext()) {
                JSONObject json2 = (JSONObject) it2.next();
                relatedEntities.add(json2.getString("id"));
            }

            this.buffer.put(id, relatedEntities);
        }
    }

    private void hashmapToJson() throws Exception {
        StringBuilder jsonString = new StringBuilder("[");
        Iterator it = this.buffer.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry entry = (HashMap.Entry) it.next();
            String id = (String) entry.getKey();
            ArrayList<String> related = (ArrayList<String>) entry.getValue();
            jsonString.append("{id: " + id + ",[");

            for (String subId: related) {
                jsonString.append("{id: " + subId + "},");
            }
            jsonString.deleteCharAt(jsonString.length() - 1);
            jsonString.append("]},");
        }

        jsonString.deleteCharAt(jsonString.length() - 1);
        jsonString.append("]");

        this.jsonArray = JSON.parseArray(jsonString.toString());
    }

    private boolean fileExists(String fileName) throws Exception {
        File file = new File(fileName);
        return file.exists();
    }

    private String readToString(String fileName) throws Exception {
        String encoding = "UTF-8";
        File file = new File(fileName);
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        FileInputStream in = new FileInputStream(file);
        in.read(filecontent);
        in.close();

        return new String(filecontent, encoding);
    }
}
