package org.aksw.agdistis.algorithm;

import com.alibaba.fastjson.JSONObject;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.aksw.agdistis.experiments.TTW_test;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.*;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.util.*;

public class TwitterToWiki_EL {

    private ArrayList<String> twitterUserNames;

    private String targetUserName;

    private WikidataOpeator wikidataOpeator;

    private String algorithm;

    private int maxDepth;

    private RelatedEntitiesBuffer reb;

    private String targetEntityId;

    private JdbcCypherExecutor executor;

    long startTime, endTime;

    private static Logger logger = Logger.getLogger(TwitterToWiki_EL.class);

    public TwitterToWiki_EL() throws Exception {
        Properties prop = new Properties();
        InputStream input = NEDAlgo_HITS.class.getResourceAsStream("/config/agdistis.properties");
        prop.load(input);

        this.algorithm = prop.getProperty("algorithm");
        this.maxDepth = Integer.parseInt(prop.getProperty("maxDepth"));

        this.wikidataOpeator = new WikidataOpeator();
        this.wikidataOpeator.setResultsCountLimit(10);
        this.twitterUserNames = new ArrayList<>();

        this.reb = new RelatedEntitiesBuffer(System.getProperty("user.dir") + "\\src\\main\\resources\\config\\relatedEntities.json");

        this.executor = new JdbcCypherExecutor("neo4j", "123456");

    }

    public ArrayList<Node> run(JSONObject twitterInfo) throws Exception {
        this.targetEntityId = null;

        //1. get context entities from given id
        this.twitterUserNames = new ArrayList<>();
        this.targetUserName = twitterInfo.getString("userName");
        this.twitterUserNames.add(this.targetUserName);
        this.twitterUserNames.addAll(TwitterCandidate.getContextUserNames(twitterInfo));
        logger.info("r:userName:" + this.targetUserName);
        logger.info("r:contextUserCount:" + this.twitterUserNames.size());

        //*************************************************************

//        List<String> subList = this.twitterUserNames.subList(0, 3);
//        this.twitterUserNames = new ArrayList<>(subList);

        //*************************************************************


        //2. generate candidates for target user name
        WikidataSearch ws = new WikidataSearch();
        ws.setSearchStrings(this.targetUserName);
        ws.setMaxResultCount(10);
        ws.setMaxThreadPoolSize(10);
        ws.run();

        if (ws.getResults().size() == 0) {
            logger.info("r:getTargetCandidates:failed");
            return null;
        }

        logger.info("r:getTargetCandidates:success");
        logger.info("r:targetCandidateCount:" + ws.getResults().get(this.targetUserName).size());

        //只有一个，直接给结果
        if (ws.getResults().get(this.targetUserName).size() == 1) {
            this.targetEntityId = ws.getResults().get(this.targetUserName).get(0);
            return null;
        }

        startTime = System.currentTimeMillis();

        //3. generate candidates for each context entities
        ws = new WikidataSearch();
        ws.setSearchStrings(this.twitterUserNames);
        ws.setMaxResultCount(10);
        ws.setMaxThreadPoolSize(10);
        ws.run();

        HashMap<String, ArrayList<String>> results = ws.getResults();
        logger.info("r:contextCandidateSetCount:" + results.size());
        logger.info("r:searchCostTime:" + this.costTime(startTime));

        //3. insert into graph
        DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

        int groupId = 0;
        for (String userName: this.twitterUserNames) {
            if (!results.containsKey(userName))
                continue;

            this.addNodesToGraph(graph, results.get(userName), groupId);

            groupId += 1;
        }

        logger.info("r:bfsCandidateCount:" + graph.getVertices().size());

        startTime = System.currentTimeMillis();

        //4. breadth first search
        //this.breadthFirstSearch(graph);
        this.breadthFirstSearchByNeo4j(graph);
        logger.info("r:bfsCostTime:" + this.costTime(startTime));

        //5. HITS
        logger.info("start to hits...");
        HITS h = new HITS();
        h.runHits(graph, 20);

        //6. select the one
        ArrayList<Node> orderedList = new ArrayList<Node>();
        orderedList.addAll(graph.getVertices());
        Collections.sort(orderedList);

        ArrayList<Node> candidates = new ArrayList<>();

        for (Node node: orderedList) {
            if (node.getLevel() != 0)
                continue;

            if (node.containsId(0)) {
                candidates.add(node);
            }
        }

        this.targetEntityId = candidates.get(0).getCandidateURI();

        for (int i = 0; i < candidates.size(); i++) {
            logger.info(String.format("%s H: %f A: %f", candidates.get(i).getCandidateURI(), candidates.get(i).getHubWeight(), candidates.get(i).getAuthorityWeight()));
        }

        return candidates;
    }

    private float costTime(long startTime) {
        return (float)(System.currentTimeMillis() - startTime) / (float)1000;
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



    private void breadthFirstSearchByNeo4j(DirectedSparseGraph<Node, String> graph) throws Exception {


        NodeQueue q = new NodeQueue();
        for (Node node : graph.getVertices()) {
            q.add(node);
        }

        int count = 1;

        while (!q.isEmpty()) {
            Node currentNode = q.poll();
            StringBuilder sqlString = new StringBuilder();

            sqlString.append("MATCH ");
            for (int i = 0; i < this.maxDepth + 1; i++) {
                sqlString.append(String.format("(n%d:Item)", i));
                if (i != this.maxDepth)
                    sqlString.append(String.format("-[r%d:Related]->", i));
            }
            sqlString.append(String.format(" WHERE n0.name=\"%s\" RETURN ", currentNode.getCandidateURI()));

            for (int i = 0; i < this.maxDepth; i++) sqlString.append(String.format("n%d,", i));

            sqlString.append(String.format("n%d", this.maxDepth));

            HashMap edges = (HashMap) Neo4jQueryResultParser.getEdges(executor.query(sqlString.toString(), null));

            Iterator it = edges.values().iterator();

            while (it.hasNext()) {
                HashMap edgeMap = (HashMap) it.next();
                Iterator itEdge = edgeMap.keySet().iterator();
                String startId = (String) itEdge.next();
                String endId = (String) edgeMap.get(startId);

                Node startNode = new Node(startId, 0, 1, this.algorithm);
                Node endNode = new Node(endId, 0, 1, this.algorithm);
                graph.addEdge(String.valueOf(graph.getEdgeCount()), startNode, endNode);
            }

            //System.out.println(String.format("%d %s: %d", q.size(), currentNode.getCandidateURI(), edges.size()));
        }
    }

    private void addNodesToGraph(
            DirectedSparseGraph<Node, String> graph,
            ArrayList<String> entitiesId,
            int groupId
    ) throws Exception {
        for(String entityId: entitiesId) {
            Node currentNode = new Node(entityId, 0, 0, algorithm);
            currentNode.addId(groupId);
            graph.addVertex(currentNode);
        }
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void close() throws Exception {
        this.executor.close();
    }

    public static void main(String[] args) throws Exception {

        TwitterCandidate tc = new TwitterCandidate();
        JSONObject jb = tc.getJsonInfoByScreenName("RepChrisGibson");

        if (jb == null){
            System.out.println("get info json failed");
            return;
        }

        TwitterToWiki_EL tt = new TwitterToWiki_EL();
        tt.run(jb);
    }
}

