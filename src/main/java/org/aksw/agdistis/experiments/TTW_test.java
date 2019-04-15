package org.aksw.agdistis.experiments;

import com.alibaba.fastjson.JSONObject;
import org.aksw.agdistis.algorithm.TwitterCandidate;
import org.aksw.agdistis.algorithm.TwitterToWiki_EL;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TTW_test {

    public static HashMap<String, String> getTestDataFromCSV(String csvFileName) throws Exception {
        HashMap<String, String> ret = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(csvFileName));
        String line = br.readLine();
        while (line != null) {
            String[] info = line.split(",");
            String wikiId = info[0].substring(info[0].lastIndexOf("/") + 1);
            //String userName = info[1];
            String accountName = info[2];
            ret.put(accountName, wikiId);

            line = br.readLine();
        }

        br.close();

        System.out.println(String.format("get twitter-wiki data: %d", ret.size()));

        return ret;
    }

    public static void main(String[] args) throws Exception {

        int totalCount = 0;
        int correctCount = 0;

        TwitterToWiki_EL tt = new TwitterToWiki_EL();
        TwitterCandidate tc = new TwitterCandidate();

        //1 get twitter-wiki data from csv file
        HashMap twData = TTW_test.getTestDataFromCSV("D:\\MyNutCloud\\研究生进程\\data\\美国政治人物-500.csv");
        ArrayList<String> linkedId = new ArrayList<>();

        //2 linking
        Iterator it = twData.entrySet().iterator();
        long totalStartTime=System.currentTimeMillis();
        while (it.hasNext()) {
            System.out.println("###################################################################");

            Map.Entry entry = (Map.Entry) it.next();
            String accountName = (String) entry.getKey();
            String wikiId = (String) entry.getValue();

            //2.1 get statistic info for a twitter account from json file
            JSONObject jb = tc.getJsonInfoByScreenName(accountName);
            if (jb == null) {
                System.out.println(String.format("get info json failed: %s", accountName));
                continue;
            }

            //2.2 run TwitterToWiki_EL.java
            tt.run(jb);

            //2.3 save result
            String targetId = tt.getTargetEntityId();
            if (targetId == null) {
                System.out.println(String.format("linked failed: %s %s", accountName, wikiId));
                targetId += 1;
                continue;
            }
            System.out.println(String.format("linked result: %s %s %s", accountName, wikiId, targetId));
            linkedId.add(targetId);

            totalCount += 1;
            if (wikiId.equals(targetId))
                correctCount += 1;

            System.out.println(String.format("********* current accuracy: %d / %d *********", correctCount, totalCount));

        }
        //3 close
        tt.close();

        //4 calculate ratio
        float accuracy = (float)correctCount / (float)totalCount;
        System.out.println(String.format("accuracy: %d / %d = %f", correctCount, totalCount, accuracy));
        float totalCostTime = (float)(System.currentTimeMillis() - totalStartTime) / (float)1000;
        System.out.println(String.format("total cost time: %fs", totalCostTime));
    }
}
