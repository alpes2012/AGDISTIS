package org.aksw.agdistis.experiments;

import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvReader;
import com.csvreader.CsvWriter;
import org.aksw.agdistis.algorithm.TwitterCandidate;
import org.aksw.agdistis.util.SearchStringFilter;
import org.aksw.agdistis.util.WikidataSearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SearchTest {

    public static void main(String[] args) throws Exception {
        TwitterCandidate tc = new TwitterCandidate();
        CsvReader csvReader = new CsvReader("D:\\MyNutCloud\\研究生进程\\data\\美国政治人物-500.csv");
        HashMap<String, HashMap> allInfo = new HashMap<>();
        ArrayList<String> searchStrings = new ArrayList<>();
        while (csvReader.readRecord()) {
            String[] info = csvReader.getValues();
            String wikiId = info[0].substring(info[0].lastIndexOf("/") + 1);
            String accountName = info[2];
            HashMap<String, String> subInfo = new HashMap<>();

            JSONObject jb = tc.getJsonInfoByScreenName(accountName);

            if (jb == null)
                continue;

            subInfo.put("wikiId", wikiId);
            subInfo.put("accountName", accountName);
            subInfo.put("candidatesCount", Integer.toString(0));

            allInfo.put(jb.getString("userName"), subInfo);

            searchStrings.add(jb.getString("userName"));

            //System.out.println(jb.getString("userName"));
        }
        csvReader.close();

        WikidataSearch ws = new WikidataSearch();
        ws.setSearchStrings(searchStrings);
        ws.setMaxResultCount(5);
        ws.setMaxThreadPoolSize(20);
        ws.run();

        HashMap<String, ArrayList<String>> results = ws.getResults();

        Iterator it = results.keySet().iterator();
        int includeCount = 0;
        while (it.hasNext()) {
            String key = (String) it.next();
            System.out.println(key);
            allInfo.get(key).put("candidatesCount", Integer.toString(results.get(key).size()));
            if (results.get(key).contains(allInfo.get(key).get("wikiId"))) {
                includeCount += 1;
                allInfo.get(key).put("include", "true");
            }
            else
                allInfo.get(key).put("include", "false");
        }

        CsvWriter csvWriter = new CsvWriter("D:\\MyNutCloud\\研究生进程\\data\\candidates_radio_500.csv");

        it = allInfo.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            String[] record = new String[5];

            record[0] = key;
            record[1] = (String) allInfo.get(key).get("wikiId");
            record[2] = (String) allInfo.get(key).get("accountName");
            record[3] = (String) allInfo.get(key).get("candidatesCount");
            record[4] = (String) allInfo.get(key).get("include");

            csvWriter.writeRecord(record);
        }
        csvWriter.close();

        System.out.println(String.format("include radio: %f", (float)includeCount / (float)allInfo.size()));
    }
}
