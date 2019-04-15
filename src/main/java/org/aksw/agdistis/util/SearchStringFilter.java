package org.aksw.agdistis.util;

import java.util.ArrayList;
import java.util.Arrays;

public class SearchStringFilter {

    public SearchStringFilter() {
    }


    private String repalce(String string) {

        //去除所有非字母
        String reg = "[^a-zA-Z]";
        string = string.replaceAll(reg," ");

        //去除双空格
        string = string.replace("  ", " ");

        return string;
    }

    private String removeStopWords(String string) {
        ArrayList<String> stopWords = new ArrayList<>(Arrays.asList("Sen", "Governor", "Rep", "Congressman", "Auditor", "U S", "Senator", "Vice", "President", "Fmr", "Gov", "US", "NYS", "Dr", "MD", "M D"));
        for (String word: stopWords) {
            string = string.replace(word, "");
        }

        return string;
    }

    public void filter(ArrayList<String> strings) {
        for (int i = 0; i < strings.size(); i++) {
            String string = strings.get(i);
            string = this.repalce(string);
            string = this.removeStopWords(string);

            strings.set(i, string);
        }
     }

     public String filter(String string) {
         //string = this.repalce(string);
         string = this.removeStopWords(string);

         return string;
     }

    public static void main(String[] args) {
        SearchStringFilter ssf = new SearchStringFilter();
        ArrayList<String> strs = new ArrayList<>(Arrays.asList("Sen. Jos?Rodr韌uez", "Bill Cassidy, M.D."));
        ssf.filter(strs);

        System.out.println(strs.toArray()[0]);
        System.out.println(strs.toArray()[1]);
    }
}
