package org.aksw.agdistis.algorithm;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.jena.atlas.iterator.Iter;
import org.openrdf.query.algebra.Str;
import org.aksw.agdistis.model.TwitterData;
import com.alibaba.fastjson.JSON;

import javax.json.Json;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class TwitterCandidate {

    private String twitterInfoFile;
    private JSONArray tdArray;

    public TwitterCandidate() throws Exception {
        Properties prop = new Properties();
        InputStream input = TwitterCandidate.class.getResourceAsStream("/config/agdistis.properties");
        prop.load(input);
        this.twitterInfoFile = prop.getProperty("twitterInfoFile");
        String infoStr = this.readToString(this.twitterInfoFile);
        tdArray = JSON.parseArray(infoStr);

    }

    public JSONArray getTwitterDataArray() {
        return tdArray;
    }

    public JSONObject getTwitterDataByScreenName(String screenName) {
        for (Iterator it = this.tdArray.iterator(); it.hasNext();) {
            JSONObject userObj = (JSONObject) it.next();
            if (userObj.getString("screenName").toLowerCase().compareTo(screenName.toLowerCase()) == 0)
                return userObj;
        }

        return null;
    }

    public ArrayList<String> getContextUserNamesByScreenName(String screenName) {
        ArrayList<String> ret = new ArrayList<>();
        for (Iterator it = this.tdArray.iterator(); it.hasNext();) {
            JSONObject userObj = (JSONObject) it.next();
            if (userObj.getString("screenName").toLowerCase().compareTo(screenName.toLowerCase()) == 0) {
                //mention
                for (Iterator itMention = userObj.getObject("mention", JSONArray.class).iterator(); itMention.hasNext();) {
                    JSONObject mention = (JSONObject)itMention.next();
                    ret.add(mention.getString("userName"));
                }
                //retweet
                for (Iterator itMention = userObj.getObject("retweet", JSONArray.class).iterator(); itMention.hasNext();) {
                    JSONObject mention = (JSONObject)itMention.next();
                    ret.add(mention.getString("userName"));
                }

                //following

                //quote

                break;
            }
        }

        return ret;
    }

    public String getText(ArrayList<String> twitterCandidates) {
        StringBuilder stringBuilder = new StringBuilder(2048);

        for (String candidate: twitterCandidates) {
            stringBuilder.append("<entity>");
            stringBuilder.append(candidate);
            stringBuilder.append("</entity>");
        }

        return stringBuilder.toString();
    }



    public static void main(String[] args) throws Exception {
        TwitterCandidate tc = new TwitterCandidate();
        ArrayList<String> info = tc.getContextUserNamesByScreenName("aaronpena");

        System.out.println(tc.getText(info));

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
