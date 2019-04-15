package org.aksw.agdistis.util;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WikidataSearch {

    private ArrayList<String> searchStrings;

    private HashMap<String, ArrayList<String>> results = new HashMap<>();

    private int maxThreadPoolSize = 8;

    private int maxResultCount = 20;

    private int completeCount = 0;

    private int timeoutCount = 0;

    public WikidataSearch() {}

    public void run() throws Exception {
        ArrayList<WikiSearchSingalThread> wstArray = new ArrayList<>();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(this.maxThreadPoolSize, this.maxThreadPoolSize, 600, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(this.searchStrings.size()));

        for (String searchString : searchStrings) {
            WikiSearchSingalThread wst = new WikiSearchSingalThread();
            wst.setMaxResultCount(maxResultCount);
            wst.setSearchStr(searchString);
            executor.execute(wst);
            wstArray.add(wst);
        }

        while (true) {
            System.out.println("search progress：" + executor.getCompletedTaskCount() + "/" + wstArray.size());
            if (executor.getCompletedTaskCount() == wstArray.size() || timeOut((int)executor.getCompletedTaskCount())) {
                executor.shutdownNow();
                break;
            }
            else
                Thread.sleep(3000);
        }

        for (int i = 0; i < this.searchStrings.size(); i++) {
            if (wstArray.get(i).getResults().size() == 0)
                continue;
            this.results.put(searchStrings.get(i), wstArray.get(i).getResults());
        }
    }

    private boolean timeOut(int completeCount) {

        if (this.completeCount == completeCount)
            this.timeoutCount += 1;
        else {
            this.completeCount = completeCount;
            this.timeoutCount = 0;
        }

        if (this.timeoutCount > 10)
            return true;
        else
            return false;

    }

    public HashMap<String, ArrayList<String>> getResults() {
        return results;
    }

    public void setSearchStrings(ArrayList<String> searchStrings) {
        this.searchStrings = searchStrings;
//        for (String str: searchStrings) {
//            this.results.put(str, null);
//        }
    }

    public void setSearchStrings(String searchString) {
        this.searchStrings = new ArrayList<String>(Arrays.asList(searchString));
    }

    public void setMaxThreadPoolSize(int maxThreadPoolSize) {
        this.maxThreadPoolSize = maxThreadPoolSize;
    }

    public void setMaxResultCount(int maxResultCount) {
        this.maxResultCount = maxResultCount;
    }

    public static void main(String[] args) throws Exception {
        WikidataSearch ws = new WikidataSearch();
        ArrayList<String> search = new ArrayList<>();
        search.add("Buenos Aires");
        search.add("Córdoba");
        search.add("La Plata");
        ws.setSearchStrings(search);
        ws.run();
        HashMap<String, ArrayList<String>> results = ws.getResults();

        for (String str: results.keySet()) {
            System.out.print(str + ": ");
            for (String re: results.get(str)) {
                System.out.print(re + " ");
            }
            System.out.println("\n");
        }
    }
}
