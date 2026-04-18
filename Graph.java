import java.util.*;

/**
 * Graph.java
 * Represents the delivery map as a weighted undirected graph.
 * Each node is a distribution point with (x, y) coordinates for visualization.
 * Edges represent roads with a weight (distance).
 */
public class Graph {

    // Inner class: one edge in the adjacency list
    public static class Edge {
        public int to;
        public int weight;

        public Edge(int to, int weight) {
            this.to     = to;
            this.weight = weight;
        }
    }

    // Inner class: a node with screen coordinates for drawing
    public static class Node {
        public int id;
        public String name;
        public int x, y;   // pixel position on canvas

        public Node(int id, String name, int x, int y) {
            this.id   = id;
            this.name = name;
            this.x    = x;
            this.y    = y;
        }
    }

    private int numNodes;
    private List<Node>       nodes;
    private List<List<Edge>> adjList;

    public Graph(int numNodes) {
        this.numNodes = numNodes;
        this.nodes    = new ArrayList<>();
        this.adjList  = new ArrayList<>();
        for (int i = 0; i < numNodes; i++)
            adjList.add(new ArrayList<>());
    }

    /** Add a distribution point with a screen position. */
    public void addNode(int id, String name, int x, int y) {
        nodes.add(new Node(id, name, x, y));
    }

    /** Add a bidirectional road between two points. */
    public void addEdge(int from, int to, int weight) {
        adjList.get(from).add(new Edge(to, weight));
        adjList.get(to).add(new Edge(from, weight));
    }

    /** Dynamically add a node at screen coords, auto-connecting to nearest 2 nodes. */
    public int addDynamicNode(int x, int y) {
        int id = numNodes;
        nodes.add(new Node(id, "D" + id, x, y));
        adjList.add(new ArrayList<>());
        numNodes++;
        
        if (numNodes > 1) {
            List<Node> sorted = new ArrayList<>(nodes);
            sorted.remove(sorted.size() - 1); // remove self
            sorted.sort((a,b) -> {
                int d1 = (a.x - x)*(a.x - x) + (a.y - y)*(a.y - y);
                int d2 = (b.x - x)*(b.x - x) + (b.y - y)*(b.y - y);
                return Integer.compare(d1, d2);
            });
            int connections = Math.min(2, sorted.size());
            for (int i=0; i<connections; i++) {
                Node closest = sorted.get(i);
                int dist = (int) Math.sqrt(Math.pow(closest.x - x, 2) + Math.pow(closest.y - y, 2)) / 5;
                addEdge(id, closest.id, Math.max(10, dist));
            }
        }
        return id;
    }

    public int           getNumNodes() { return numNodes; }
    public List<Node>    getNodes()    { return nodes; }
    public List<Edge>    getEdges(int node) { return adjList.get(node); }

    /** Count total edges (each undirected edge counted once). */
    public int getTotalEdges() {
        int total = 0;
        for (List<Edge> list : adjList) total += list.size();
        return total / 2;
    }

    // ─────────────────────────────────────────────────────
    // FACTORY METHODS — build Sparse / Medium / Dense graphs
    // ─────────────────────────────────────────────────────

    /**
     * SPARSE — 10 nodes, 12 edges.
     * Like a rural area: few connecting roads, long detours.
     */
    public static Graph createSparse() {
        Graph g = new Graph(10);
        // Positions spread across a 600×400 canvas
        g.addNode(0, "D0", 80,  200);
        g.addNode(1, "D1", 180, 80);
        g.addNode(2, "D2", 300, 60);
        g.addNode(3, "D3", 430, 100);
        g.addNode(4, "D4", 520, 200);
        g.addNode(5, "D5", 460, 320);
        g.addNode(6, "D6", 320, 370);
        g.addNode(7, "D7", 180, 340);
        g.addNode(8, "D8", 100, 330);
        g.addNode(9, "D9", 310, 210);

        // Only 12 edges — sparse connectivity
        g.addEdge(0, 1, 29);
        g.addEdge(1, 2, 22);
        g.addEdge(2, 3, 18);
        g.addEdge(3, 4, 24);
        g.addEdge(4, 5, 20);
        g.addEdge(5, 6, 22);
        g.addEdge(6, 7, 21);
        g.addEdge(7, 8, 14);
        g.addEdge(8, 0, 17);
        g.addEdge(1, 9, 25);
        g.addEdge(9, 5, 28);
        g.addEdge(2, 9, 32);

        return g;
    }

    /**
     * MEDIUM — 10 nodes, 20 edges.
     * Like a town: moderate roads, some shortcuts available.
     */
    public static Graph createMedium() {
        Graph g = new Graph(10);
        g.addNode(0, "D0", 80,  200);
        g.addNode(1, "D1", 180, 80);
        g.addNode(2, "D2", 300, 60);
        g.addNode(3, "D3", 430, 100);
        g.addNode(4, "D4", 520, 200);
        g.addNode(5, "D5", 460, 320);
        g.addNode(6, "D6", 320, 370);
        g.addNode(7, "D7", 180, 340);
        g.addNode(8, "D8", 100, 330);
        g.addNode(9, "D9", 310, 210);

        // 20 edges — medium connectivity
        g.addEdge(0, 1, 29);
        g.addEdge(0, 8, 17);
        g.addEdge(0, 7, 38);
        g.addEdge(1, 2, 22);
        g.addEdge(1, 9, 25);
        g.addEdge(2, 3, 18);
        g.addEdge(2, 9, 32);
        g.addEdge(3, 4, 24);
        g.addEdge(3, 9, 27);
        g.addEdge(4, 5, 20);
        g.addEdge(4, 9, 35);
        g.addEdge(5, 6, 22);
        g.addEdge(5, 9, 28);
        g.addEdge(6, 7, 21);
        g.addEdge(6, 9, 25);
        g.addEdge(7, 8, 14);
        g.addEdge(7, 9, 30);
        g.addEdge(8, 9, 36);
        g.addEdge(1, 7, 42);
        g.addEdge(2, 6, 45);

        return g;
    }

    /**
     * DENSE — 10 nodes, 35 edges.
     * Like a city: almost every node connected to every other,
     * many shortcut paths available.
     */
    public static Graph createDense() {
        Graph g = new Graph(10);
        g.addNode(0, "D0", 80,  200);
        g.addNode(1, "D1", 180, 80);
        g.addNode(2, "D2", 300, 60);
        g.addNode(3, "D3", 430, 100);
        g.addNode(4, "D4", 520, 200);
        g.addNode(5, "D5", 460, 320);
        g.addNode(6, "D6", 320, 370);
        g.addNode(7, "D7", 180, 340);
        g.addNode(8, "D8", 100, 330);
        g.addNode(9, "D9", 310, 210);

        // 35 edges — dense connectivity (near complete graph)
        g.addEdge(0, 1, 29); g.addEdge(0, 2, 48);
        g.addEdge(0, 7, 38); g.addEdge(0, 8, 17);
        g.addEdge(0, 9, 40); g.addEdge(0, 6, 55);
        g.addEdge(1, 2, 22); g.addEdge(1, 3, 38);
        g.addEdge(1, 9, 25); g.addEdge(1, 7, 42);
        g.addEdge(1, 8, 32); g.addEdge(1, 6, 50);
        g.addEdge(2, 3, 18); g.addEdge(2, 9, 32);
        g.addEdge(2, 4, 42); g.addEdge(2, 6, 45);
        g.addEdge(3, 4, 24); g.addEdge(3, 9, 27);
        g.addEdge(3, 5, 38); g.addEdge(3, 6, 44);
        g.addEdge(4, 5, 20); g.addEdge(4, 9, 35);
        g.addEdge(4, 6, 40); g.addEdge(4, 7, 52);
        g.addEdge(5, 6, 22); g.addEdge(5, 9, 28);
        g.addEdge(5, 7, 40); g.addEdge(5, 8, 48);
        g.addEdge(6, 7, 21); g.addEdge(6, 9, 25);
        g.addEdge(6, 8, 36); g.addEdge(7, 8, 14);
        g.addEdge(7, 9, 30); g.addEdge(8, 9, 36);
        g.addEdge(0, 3, 62);

        return g;
    }
}
