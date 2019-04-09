package org.aksw.agdistis.algorithm;

import com.alibaba.fastjson.JSONObject;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.graph.NodeQueue;
import org.aksw.agdistis.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocument;

import java.io.File;
import java.io.FileInputStream;
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

    }



    public void run(JSONObject twitterInfo) throws Exception {
        //1. get context entities from given id
        this.targetUserName = twitterInfo.getString("userName");
        this.twitterUserNames.add(this.targetUserName);
        this.twitterUserNames.addAll(TwitterCandidate.getContextUserNames(twitterInfo));
        System.out.println("start linking target user: " + this.targetUserName);
        System.out.println("get context user: " + this.twitterUserNames.size());

        //*************************************************************

        List<String> subList = this.twitterUserNames.subList(0, 10);
        this.twitterUserNames = new ArrayList<>(subList);

        //*************************************************************


        //2. generate candidates for each context entities
        for (int iCount = 0; iCount < this.twitterUserNames.size(); iCount++) {
            this.entities.addAll(this.getCandidates(this.twitterUserNames.get(iCount), iCount));
            System.out.println("get candidates for: " + this.twitterUserNames.get(iCount));
        }
        System.out.println("total candidates: " + this.twitterUserNames.size());

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
    }

    private void breadthFirstSearch(DirectedSparseGraph<Node, String> graph) throws Exception {
        NodeQueue q = new NodeQueue();
        for (Node node : graph.getVertices()) {
            q.add(node);
        }
        while (!q.isEmpty()) {
            Node currentNode = q.poll();
            int level = currentNode.getLevel();

            if (level >= maxDepth) continue;
            if (Integer.parseInt(currentNode.getCandidateURI().substring(1)) <= 1000) continue;

            ArrayList<String> relatedId = this.wikidataOpeator.getRelatedItems(currentNode.getCandidateURI());

            if (relatedId.size() > 30) continue;

            System.out.println(String.format("%s : %d : %d : %d", currentNode.getCandidateURI(), level, relatedId.size(), q.size()));
            for (String id: relatedId) {
                int levelNow = level + 1;
                Node node = new Node(id, 0, levelNow, this.algorithm);
                q.add(node);
                graph.addEdge(String.valueOf(graph.getEdgeCount()), currentNode, node);
            }
        }
    }



    private ArrayList<WikiEntity> getCandidates(String userName, int groupId) throws Exception {
        ArrayList<WikiEntity> ret = new ArrayList<>();
        ArrayList<String> ids = this.wikidataOpeator.search(userName);
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

//    private ArrayList<WikiEntity> getCandidates(String userName, int groupId) throws Exception {
//        ArrayList<WikiEntity> ret = new ArrayList<>();
//        Map<String, EntityDocument> candidatesMap = this.wikidataOpeator.getDocuments(this.wikidataOpeator.search(userName));
//        Iterator iterator = candidatesMap.entrySet().iterator();
//        while (iterator.hasNext()) {
//            Map.Entry entry = (Map.Entry)iterator.next();
//            String id = (String)entry.getKey();
//            EntityDocument document = (EntityDocument)entry.getValue();
//
//            WikiEntity we = new WikiEntity();
//            we.setEntityId(id);
//            we.setGroupId(groupId);
//            we.setEntityDocument(document);
//
//            ret.add(we);
//        }
//
//        return ret;
//    }

    private void addNodeToGraph(
            DirectedSparseGraph<Node, String> graph,
            HashMap<String, Node> nodes,
            WikiEntity entity
    ) throws Exception {
        Node currentNode = new Node(entity.entityId, 0, 0, algorithm);
        graph.addVertex(currentNode);
        currentNode.addId(entity.groupId);
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
        JSONObject jb = tc.getJsonInfoByScreenName("aaronpena");

        if (jb == null){
            System.out.println("get info json failed");
            return;
        }

        TwitterToWiki_EL tt = new TwitterToWiki_EL();
        tt.run(jb);
    }
}

