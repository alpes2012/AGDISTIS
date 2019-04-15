package org.aksw.agdistis.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Neo4jQueryResultParser {

    public static Map<String, Map<String, String>> getEdges(Iterator it) throws Exception {

        Map<String, Map<String, String>> edgeMap = new HashMap<>();
        //int count = 0;
        while (it.hasNext()) {
            //System.out.println(it.next().toString());
            Map<String, Map<String, String>> ob = (Map<String, Map<String, String>>) it.next();
            Iterator entryIt = ob.entrySet().iterator();
            String preId = "";

            while (entryIt.hasNext()) {
                Map.Entry entry = (Map.Entry) entryIt.next();
                String key = (String) entry.getKey();
                Map value = (Map) entry.getValue();

                if (preId.equals(""))
                    preId = (String) value.get("name");
                else {
                    String edgeName = preId + value.get("name");
                    HashMap<String, String> subMap =  new HashMap<>();
                    subMap.put(preId, (String) value.get("name"));
                    edgeMap.put(edgeName, subMap);
                    preId = (String) value.get("name");
                    //System.out.println(count);
                    //count += 1;
                }
            }
        }

        return edgeMap;
    }
}
