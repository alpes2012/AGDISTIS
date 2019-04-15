package org.aksw.agdistis.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class WikiSearchSingalThread implements Runnable {
    private String url = "https://www.wikidata.org/w/index.php?search=SearchString&title=Special%3ASearch&fulltext=1&ns0=1&ns120=1";

    private int maxResultCount = 20;

    private boolean excludPropertyItem = true;

    private String searchStr;

    private ArrayList<String> results = new ArrayList<>();

    public WikiSearchSingalThread() {

    }

    public void setMaxResultCount(int maxResultCount) {
        this.maxResultCount = maxResultCount;
    }

    public void setExcludPropertyItem(boolean excludPropertyItem) {
        this.excludPropertyItem = excludPropertyItem;
    }

    public void setSearchStr(String searchStr) {
        this.searchStr = searchStr;
    }

    public ArrayList<String> getResults() {
        return results;
    }

    @Override
    public void run() {
        try {
            this.results = this.search();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getHtmlCode(String url) throws Exception {
        URL reqURL = new URL(url); //创建URL对象
        HttpsURLConnection httpsConn = (HttpsURLConnection)reqURL.openConnection();

        if (httpsConn.getResponseCode() != 200) return "";

        InputStreamReader insr = new InputStreamReader(httpsConn.getInputStream(), "UTF-8");
        BufferedReader br = new BufferedReader(insr);

        StringBuffer stringBuffer = new StringBuffer("");

        String line = br.readLine();
        while (line != null) {
            stringBuffer.append(line);
            //stringBuffer.append("\n");
            line = br.readLine();
        }

        //System.out.println(stringBuffer.toString());

        br.close();
        insr.close();
        httpsConn.disconnect();

        return stringBuffer.toString();
    }

    public ArrayList<String> search() throws Exception {
        searchStr = this.preProcessSearchString(searchStr);

        String searchUrl = url.replace("SearchString", searchStr);

        String htmlCode = this.getHtmlCode(searchUrl);
        if (htmlCode == null || htmlCode.length() == 0)
            return null;

        Document doc = Jsoup.parse(htmlCode);
        Elements elements = doc.select("span.wb-itemlink-id");
        ArrayList<String> textes = (ArrayList<String>) elements.eachText();
        ArrayList<String> ret = new ArrayList<>();
        for (int i = 0; i < (this.maxResultCount <= textes.size() ? this.maxResultCount : textes.size()); i++) {
            if (this.excludPropertyItem == true && textes.get(i).contains("P"))
                continue;
            ret.add(textes.get(i).substring(1, textes.get(i).length() - 1));
        }

        return ret;
    }

    private String preProcessSearchString (String searchStr) {
        String ret;

        //SearchStringFilter ssf = new SearchStringFilter();
        //searchStr = ssf.filter(searchStr);

        //替换空格
        ret = searchStr.replace(" ", "%20");

        return ret;
    }

    public static void main(String[] args) throws Exception {
        WikiSearchSingalThread ws = new WikiSearchSingalThread();
        ws.setMaxResultCount(50);
        ws.setSearchStr("china");
        ws.run();
        for (String re: ws.getResults()) {
            System.out.println(re);
        }
    }
}
