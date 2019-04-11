package org.aksw.agdistis.algorithm;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.aksw.agdistis.util.RelatedEntitiesBuffer;
import org.jsoup.nodes.Document;
import org.wikidata.wdtk.datamodel.helpers.JsonSerializer;
import org.wikidata.wdtk.datamodel.interfaces.*;
import org.wikidata.wdtk.wikibaseapi.WbGetEntitiesSearchData;
import org.wikidata.wdtk.wikibaseapi.WbSearchEntitiesResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.NoSuchEntityErrorException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class WikidataOpeator {

    private WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();
    private WbGetEntitiesSearchData properties =  new WbGetEntitiesSearchData();



    public WikidataOpeator() throws  Exception {
        WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();
        this.properties.language = "en";
        this.properties.limit = (long)50;
        this.properties.type = "item";


    }

    public ArrayList<String> search(String searchStr) throws Exception {

        ArrayList<WbSearchEntitiesResult> lResult;
        //WbGetEntitiesSearchData properties =  new WbGetEntitiesSearchData();
        this.properties.search = searchStr;

        lResult = (ArrayList<WbSearchEntitiesResult>) this.wbdf.searchEntities(this.properties);

        ArrayList<String> lRet = new ArrayList<>();
        for (WbSearchEntitiesResult wr : lResult) { lRet.add(wr.getEntityId()); }

        return lRet;
    }

    public Map<String, EntityDocument> getDocuments(ArrayList<String> idList) throws Exception {
        return this.wbdf.getEntityDocuments(idList);
    }

    public EntityDocument getDocument(String id) throws Exception {
        return this.wbdf.getEntityDocument(id);
    }

    public ArrayList<String> getRelatedItems(String id) throws Exception {

        //DocumentDataFilter filter = this.wbdf.getFilter();
        //filter.setLanguageFilter();
        ItemDocument itemDocument = (ItemDocument)this.wbdf.getEntityDocument(id);
        ArrayList<String> lRet = new ArrayList<>();
        if (itemDocument == null) return lRet;

        for (Iterator itStatment = itemDocument.getAllStatements(); itStatment.hasNext();) {
            try{
                JSONObject statment = JSON.parseObject(JsonSerializer.getJsonString((Statement) itStatment.next()));
                if (statment.isEmpty()) continue;
                if (statment.getJSONObject("mainsnak").getString("datatype").compareTo("wikibase-item") != 0)
                    continue;

                String subId = statment.getJSONObject("mainsnak").getJSONObject("datavalue").getJSONObject("value").getString("id");

                //System.out.println(subId);

                lRet.add(subId);
            }
            catch (NullPointerException e) {
                continue;
            }
        }



        return lRet;
    }


    public void setSearchLanguage(String lag) {
        this.properties.language = lag;
    }

    public void setResultsCountLimit(long limit) {
        this.properties.limit = limit < 50 ? limit : 50;
    }

    public void setSearchTypt(String type) {
        this.properties.type = type;

    }

    public static void main(String[] args) throws Exception {
        TwitterCandidate tc = new TwitterCandidate();
        JSONArray array = tc.getTwitterDataArray();
    }

//    public static void main(String[] args) throws Exception {
//        WikidataOpeator wo = new WikidataOpeator();
//        for (int i = 1000; i < 10000; i++) {
//
//            ItemDocument item;
//            String id = String.format("Q%d", i);
//            try {
//                item = (ItemDocument)wo.getDocument(id);
//            }
//            catch (NoSuchEntityErrorException e) {
//                continue;
//            }
//
//            if (item == null) continue;
//
//            String label = item.getLabels().get("en").getText();
//            String des = item.getDescriptions().get("en").getText();
//            int statmentCount = 0;
//            Iterator it = item.getAllStatements();
//            while (it.hasNext()) {
//                it.next();
//                statmentCount += 1;
//            }
//
//            System.out.println(String.format("%s : %d : %s : %s", id, statmentCount, label, des));
//
//        }
//    }
}
