package org.aksw.agdistis.algorithm;

import com.alibaba.fastjson.JSONObject;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import org.aksw.agdistis.graph.HITS;
import org.aksw.agdistis.graph.Node;
import org.aksw.agdistis.util.*;

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

    public TwitterToWiki_EL() throws Exception {
        Properties prop = new Properties();
        InputStream input = NEDAlgo_HITS.class.getResourceAsStream("/config/agdistis.properties");
        prop.load(input);

        this.algorithm = prop.getProperty("algorithm");
        this.maxDepth = Integer.parseInt(prop.getProperty("maxDepth"));

        this.wikidataOpeator = new WikidataOpeator();
        this.wikidataOpeator.setResultsCountLimit(10);
        this.twitterUserNames = new ArrayList<>();
        //this.entities = new ArrayList<>();

        this.reb = new RelatedEntitiesBuffer(System.getProperty("user.dir") + "\\src\\main\\resources\\config\\relatedEntities.json");

        this.executor = new JdbcCypherExecutor("neo4j", "123456");

    }



    public ArrayList<Node> run(JSONObject twitterInfo) throws Exception {
        //1. get context entities from given id
        this.twitterUserNames = new ArrayList<>();
        this.targetUserName = twitterInfo.getString("userName");
        this.twitterUserNames.add(this.targetUserName);
        this.twitterUserNames.addAll(TwitterCandidate.getContextUserNames(twitterInfo));
        System.out.println("start linking target user: " + this.targetUserName);
        System.out.println("get context user: " + this.twitterUserNames.size());

        //*************************************************************

//        List<String> subList = this.twitterUserNames.subList(0, 3);
//        this.twitterUserNames = new ArrayList<>(subList);

        //*************************************************************


        //2. generate candidates for target user name
        WikidataSearch ws = new WikidataSearch();
        ws.setSearchStrings(this.targetUserName);
        ws.setMaxResultCount(10);
        ws.setMaxThreadPoolSize(20);
        ws.run();

        if (ws.getResults().size() == 0)
            return null;

        if (ws.getResults().get(this.targetUserName).size() == 1) {
            this.targetEntityId = ws.getResults().get(this.targetUserName).get(0);
            return null;
        }

        //3. generate candidates for each context entities
        ws = new WikidataSearch();
        ws.setSearchStrings(this.twitterUserNames);
        ws.setMaxResultCount(10);
        ws.setMaxThreadPoolSize(20);
        ws.run();

        HashMap<String, ArrayList<String>> results = ws.getResults();
        System.out.println("get total candidates: " + results.size());

        //3. insert into graph
        DirectedSparseGraph<Node, String> graph = new DirectedSparseGraph<Node, String>();

        int groupId = 0;
        for (String userName: this.twitterUserNames) {
            if (!results.containsKey(userName))
                continue;

            this.addNodesToGraph(graph, results.get(userName), groupId);

            groupId += 1;
        }

        System.out.println("insert entities into graph complete");

        //4. breadth first search
        //this.breadthFirstSearch(graph);
        this.breadthFirstSearchByNeo4j(graph);
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
                candidates.add(node);
            }
        }

        this.targetEntityId = candidates.get(0).getCandidateURI();

        for (int i = 0; i < candidates.size(); i++) {
            System.out.println("candidate id is: " + candidates.get(i).toString());
        }

        return candidates;
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
        JSONObject jb = tc.getJsonInfoByScreenName("VoteBarbara");

        if (jb == null){
            System.out.println("get info json failed");
            return;
        }

        TwitterToWiki_EL tt = new TwitterToWiki_EL();
        tt.run(jb);
    }
}

