package org.aksw.agdistis.util;

import java.sql.*;
import java.util.*;
import org.neo4j.jdbc.Driver;

/**
 * @author Michael Hunger @since 22.10.13
 */
public class JdbcCypherExecutor {

    private final Connection conn;

    private String url = "jdbc:neo4j:bolt://localhost:7687/";

    public JdbcCypherExecutor(String url) throws Exception {
        this(url,null,null);
    }

    public JdbcCypherExecutor(String username, String password) throws Exception {
        Class.forName("org.neo4j.jdbc.Driver").newInstance();
        try {
            conn = DriverManager.getConnection(this.url,username,password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public JdbcCypherExecutor(String url,String username, String password) throws Exception {
        Class.forName("org.neo4j.jdbc.Driver").newInstance();
        this.url = url;
        try {
            conn = DriverManager.getConnection(this.url,username,password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterator<Map<String, Object>> query(String query, Map<String, Object> params) {
        try {
            final PreparedStatement statement = conn.prepareStatement(query);
            if (params != null)
                setParameters(statement, params);
            final ResultSet result = statement.executeQuery();
            return new Iterator<Map<String, Object>>() {

                boolean hasNext = result.next();
                public List<String> columns;

                public boolean hasNext() {
                    return hasNext;
                }

                private List<String> getColumns() throws SQLException {
                    if (columns != null) return columns;
                    ResultSetMetaData metaData = result.getMetaData();
                    int count = metaData.getColumnCount();
                    List<String> cols = new ArrayList<>(count);
                    for (int i = 1; i <= count; i++) cols.add(metaData.getColumnName(i));
                    return columns = cols;
                }

                public Map<String, Object> next() {
                    try {
                        if (hasNext) {
                            Map<String, Object> map = new LinkedHashMap<>();
                            for (String col : getColumns()) map.put(col, result.getObject(col));
                            hasNext = result.next();
                            if (!hasNext) {
                                result.close();
                                statement.close();
                            }
                            return map;
                        } else throw new NoSuchElementException();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }

                public void remove() {
                }
            };
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void close() throws Exception {
        conn.close();
    }

    private void setParameters(PreparedStatement statement, Map<String, Object> params) throws SQLException {
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            int index = Integer.parseInt(entry.getKey());
            statement.setObject(index, entry.getValue());
        }
    }

    public static void main(String[] args) throws Exception {
        JdbcCypherExecutor ex = new JdbcCypherExecutor("jdbc:neo4j:bolt://localhost:7687/", "neo4j", "123456");
        Iterator it = ex.query("match (n1:Item)-[r1:Related]->(n2:Item)-[r2:Related]->(n3:Item) where n1.name=\"Q4808526\" return n1,n2,n3", null);

        HashMap edges = (HashMap) Neo4jQueryResultParser.getEdges(it);

        System.out.println(edges.size());

        ex.close();
    }
}

