package org.aksw.agdistis.algorithm;

import org.wikidata.wdtk.wikibaseapi.WbGetEntitiesSearchData;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;

public class WikidataOpeator {

    private WikibaseDataFetcher wbdf = WikibaseDataFetcher.getWikidataDataFetcher();;
    private WbGetEntitiesSearchData properties =  new WbGetEntitiesSearchData();


    public WikidataOpeator() throws  Exception {
        this.properties.language = "en";
        this.properties.limit = (long)50;
        this.properties.type = "item";
    }

    public

    public void setSearchLanguage(String lag) {
        this.properties.language = lag;
    }

    public void setResultsCountLimit(long limit) {
        this.properties.limit = limit < 50 ? limit : 50;
    }

    public void setSearchTypt(String type) {
        this.properties.type = type;
    }
}
