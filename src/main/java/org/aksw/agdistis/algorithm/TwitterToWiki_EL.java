package org.aksw.agdistis.algorithm;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONWriter;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.NodeQueue;
import org.aksw.agdistis.util.RelatedEntitiesBuffer;
import org.aksw.agdistis.util.WikidataSearch;
import org.apache.jena.base.Sys;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;

import java.io.FileWriter;
import java.io.InputStream;
import java.util.*;

public class TwitterToWiki_EL {

    //private Logger log = LoggerFactory.getLogger(TwitterToWiki_EL.class);

    private ArrayList<WikiEntity> entities;

    private ArrayList<String> twitterUserNames;

    private String targetUserName;

    private WikidataOpeator wikidataOpeator;

    private String algorithm;

    private int maxDepth;

    private RelatedEntitiesBuffer reb;

    private String targetEntityId;

    public TwitterToWiki_EL() throws Exception {
        Properties prop = new Properties();
        InputStream input = NEDAlgo_HITS.class.getResourceAsStream("/config/agdistis.properties");
        prop.load(input);

        this.algorithm = prop.getProperty("algorithm");
        this.maxDepth = Integer.parseInt(prop.getProperty("maxDepth"));

        this.wikidataOpeator = new WikidataOpeator();
        this.wikidataOpeator.setResultsCountLimit(10);
        this.twitterUserNames = new ArrayList<>();
        this.entities = new ArrayList<>();

        this.reb = new RelatedEntitiesBuffer(System.getProperty("user.dir") + "\\src\\main\\resources\\config\\relatedEntities.json");

    }



    public void run(JSONObject twitterInfo) throws Exception {
        //1. get context entities from given id
        this.targetUserName = twitterInfo.getString("userName");
        this.twitterUserNames.add(this.targetUserName);
        this.twitterUserNames.addAll(TwitterCandidate.getContextUserNames(twitterInfo));
        System.out.println("start linking target user: " + this.targetUserName);
        System.out.println("get context user: " + this.twitterUserNames.size());

        //*************************************************************

//        List<String> subList = this.twitterUserNames.subList(0, 3);
//        this.twitterUserNames = new ArrayList<>(subList);

        //*************************************************************


        //2. generate candidates for each context entities
        WikidataSearch ws = new WikidataSearch();
        ws.setSearchStrings(this.twitterUserNames);
        ws.setMaxResultCount(10);
        ws.setMaxThreadPoolSize(20);
        ws.run();

        HashMap<String, ArrayList<String>> results = ws.getResults();
        int groupId = 0;
        for (String str:  this.twitterUserNames) {
            this.entities.addAll(this.getCandidates(results.get(str), groupId));
            groupId += 1;
        }
        System.out.println("total candidates: " + this.entities.size());

        //3. insert into graph
        HashMap<String, Node> nodes = new HashMap<String, Node>();
        DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

        for (WikiEntity entity: this.entities) {
            this.addNodeToGraph(graph, nodes, entity);
        }
        System.out.println("insert entities into graph complete");

        //4. breadth first search
        this.breadthFirstSearch(graph);
        System.out.println("breadth first search complete");

        //5. HITS
        System.out.print("start to hits...");
        HITS h = new HITS();
        h.runHits(graph, 20);
        System.out.println("done!");

        //6. select the one
        ArrayList<Node> orderedList = new ArrayList<Node>();
        orderedList.addAll(graph.getVertices());
        Collections.sort(orderedList);

        ArrayList<Node> candidates = new ArrayList<>();

        for (Node node: orderedList) {
            if (node.getLevel() != 0)
                continue;

            if (node.containsId(0)) {
                this.targetEntityId = node.getCandidateURI();
                candidates.add(node);
            }
        }

        for (int i = 0; i < candidates.size(); i++) {
            System.out.println("target id is: " + candidates.get(i).toString());
        }

    }

    private void breadthFirstSearch(DirectedSparseGraph<Node, String> graph) throws Exception {
        NodeQueue q = new NodeQueue();
        for (Node node : graph.getVertices()) {
            q.add(node);
        }

        int writeCounter = 0;

        while (!q.isEmpty()) {
            Node currentNode = q.poll();
            int level = currentNode.getLevel();

            if (level >= maxDepth) continue;
            if (Integer.parseInt(currentNode.getCandidateURI().substring(1)) <= 500) continue;

            ArrayList<String> relatedId;
            if (this.reb.isContain(currentNode.getCandidateURI())) {
                relatedId = this.reb.get(currentNode.getCandidateURI());
            }
            else {
                relatedId = this.wikidataOpeator.getRelatedItems(currentNode.getCandidateURI());
                this.reb.add(currentNode.getCandidateURI(), relatedId);
            }

            if (relatedId.size() > 30) continue;

            System.out.println(String.format("%s : %d : %d : %d", currentNode.getCandidateURI(), level, relatedId.size(), q.size()));
            for (String id: relatedId) {
                int levelNow = level + 1;
                Node node = new Node(id, 0, levelNow, this.algorithm);
                q.add(node);
                graph.addEdge(String.valueOf(graph.getEdgeCount()), currentNode, node);
            }

            writeCounter += 1;

            if (writeCounter / 20 > 1 && writeCounter % 20 == 0)
                this.reb.writeToFile();
        }

        this.reb.writeToFile();
    }



    private ArrayList<WikiEntity> getCandidates(ArrayList<String> ids, int groupId) throws Exception {
        ArrayList<WikiEntity> ret = new ArrayList<>();
        Iterator iterator = ids.iterator();
        while (iterator.hasNext()) {
            String id = (String)iterator.next();

            WikiEntity we = new WikiEntity();
            we.setEntityId(id);
            we.setGroupId(groupId);

            ret.add(we);
        }

        return ret;
    }

    private void addNodeToGraph(
            DirectedSparseGraph<Node, String> graph,
            HashMap<String, Node> nodes,
            WikiEntity entity
    ) throws Exception {
        Node currentNode = new Node(entity.entityId, 0, 0, algorithm);
        currentNode.addId(entity.groupId);
        graph.addVertex(currentNode);
        nodes.put(entity.entityId, currentNode);
    }

    private class WikiEntity {
        private String entityId; //wiki id
        private int groupId; //candidate entity number
        private EntityDocument entityDocument;
        private ArrayList<WikiEntity> relatedEntities;

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public int getGroupId() {
            return groupId;
        }

        public void setGroupId(int groupId) {
            this.groupId = groupId;
        }

        public ArrayList<WikiEntity> getRelatedEntities() {
            return relatedEntities;
        }

        public void setRelatedEntities(ArrayList<WikiEntity> relatedEntities) {
            this.relatedEntities = relatedEntities;
        }

        public EntityDocument getEntityDocument() {
            return entityDocument;
        }

        public void setEntityDocument(EntityDocument entityDocument) {
            this.entityDocument = entityDocument;
        }
    }

    public static void main(String[] args) throws Exception {

        TwitterCandidate tc = new TwitterCandidate();
        JSONObject jb = tc.getJsonInfoByScreenName("JBPritzker");

        if (jb == null){
            System.out.println("get info json failed");
            return;
        }

        TwitterToWiki_EL tt = new TwitterToWiki_EL();
        tt.run(jb);
    }
}

