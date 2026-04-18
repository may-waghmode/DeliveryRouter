import java.util.*;

/**
 * Dijkstra.java
 * Implements Dijkstra's shortest path algorithm using a Min-Heap (PriorityQueue).
 * Records execution time in nanoseconds for performance comparison.
 *
 * Time Complexity  : O((V + E) log V)
 * Space Complexity : O(V)
 */
public class Dijkstra {

    // ── Result object returned after each run ──────────────
    public static class Result {
        public int[]   dist;          // dist[i] = shortest distance from source to node i
        public int[]   prev;          // prev[i] = previous node on shortest path to i
        public List<Integer> path;    // ordered list of nodes: source → ... → destination
        public int     pathCost;      // total cost of shortest path
        public long    timeNano;      // execution time in nanoseconds
        public double  timeMs;        // execution time in milliseconds
        public int     source;
        public int     destination;
        public boolean reachable;     // false if no path exists
        public List<Integer> waypoints; // ordered list of waypoints visited

        public Result(int n) {
            dist = new int[n];
            prev = new int[n];
            path = new ArrayList<>();
        }
    }

    // ── Internal node used inside the priority queue ────────
    private static class PQNode implements Comparable<PQNode> {
        int node, cost;
        PQNode(int node, int cost) { this.node = node; this.cost = cost; }

        @Override
        public int compareTo(PQNode other) {
            return Integer.compare(this.cost, other.cost); // min-heap by cost
        }
    }

    /**
     * Run Dijkstra from source to destination on the given graph.
     * @param graph       The delivery map graph
     * @param source      Starting distribution point
     * @param destination Target distribution point
     * @return            Result object with path, cost, and execution time
     */
    public static Result run(Graph graph, int source, int destination) {
        int n = graph.getNumNodes();
        Result result = new Result(n);
        result.source      = source;
        result.destination = destination;

        // ── Initialize ──────────────────────────────────────
        Arrays.fill(result.dist, Integer.MAX_VALUE);
        Arrays.fill(result.prev, -1);
        result.dist[source] = 0;

        PriorityQueue<PQNode> pq = new PriorityQueue<>();
        pq.offer(new PQNode(source, 0));

        boolean[] visited = new boolean[n];

        // ── START TIMER ─────────────────────────────────────
        long startTime = System.nanoTime();

        // ── Main loop ────────────────────────────────────────
        while (!pq.isEmpty()) {
            PQNode current = pq.poll();
            int u = current.node;

            // Skip if already finalized
            if (visited[u]) continue;
            visited[u] = true;

            // Early exit if we reached destination
            if (u == destination) break;

            // Relax all neighbors
            for (Graph.Edge edge : graph.getEdges(u)) {
                int v = edge.to;
                int newDist = result.dist[u] + edge.weight;

                if (!visited[v] && newDist < result.dist[v]) {
                    result.dist[v] = newDist;
                    result.prev[v] = u;
                    pq.offer(new PQNode(v, newDist));
                }
            }
        }

        // ── STOP TIMER ──────────────────────────────────────
        long endTime = System.nanoTime();
        result.timeNano = endTime - startTime;
        result.timeMs   = result.timeNano / 1_000_000.0;

        // ── Reconstruct path ─────────────────────────────────
        if (destination != -1) {
            result.reachable = (result.dist[destination] != Integer.MAX_VALUE);
            if (result.reachable) {
                result.pathCost = result.dist[destination];
                // Walk backwards from destination using prev[]
                LinkedList<Integer> path = new LinkedList<>();
                int curr = destination;
                while (curr != -1) {
                    path.addFirst(curr);
                    curr = result.prev[curr];
                }
                result.path = new ArrayList<>(path);
            }
        } else {
            result.reachable = true;
        }

        return result;
    }

    /**
     * Run Dijkstra to find the shortest path from source to destination,
     * visiting all specified waypoints in the optimal (shortest) order natively using TSP approach.
     */
    public static Result runMultiStopTSP(Graph graph, int source, int destination, List<Integer> waypoints) {
        if (waypoints == null || waypoints.isEmpty()) {
            return run(graph, source, destination);
        }

        int n = graph.getNumNodes();
        long startTime = System.nanoTime();

        // 1. Compute single-source shortest paths for source and all waypoints
        Map<Integer, Result> allPaths = new HashMap<>();
        allPaths.put(source, run(graph, source, -1));
        for (int wp : waypoints) {
            allPaths.put(wp, run(graph, wp, -1));
        }

        // 2. Generate permutations of waypoints to find the optimal visitation order
        List<List<Integer>> perms = new ArrayList<>();
        generatePermutations(waypoints, 0, perms);

        int minCost = Integer.MAX_VALUE;
        List<Integer> bestPerm = null;

        for (List<Integer> perm : perms) {
            int cost = 0;
            int curr = source;
            boolean possible = true;
            for (int next : perm) {
                int d = allPaths.get(curr).dist[next];
                if (d == Integer.MAX_VALUE) { possible = false; break; }
                cost += d;
                curr = next;
            }
            if (possible) {
                int dDest = allPaths.get(curr).dist[destination];
                if (dDest == Integer.MAX_VALUE) possible = false;
                else cost += dDest;
            }

            if (possible && cost < minCost) {
                minCost = cost;
                bestPerm = new ArrayList<>(perm);
            }
        }

        long endTime = System.nanoTime();

        Result res = new Result(n);
        res.source = source;
        res.destination = destination;
        res.timeNano = endTime - startTime;
        res.timeMs = res.timeNano / 1_000_000.0;

        if (bestPerm == null) {
            res.reachable = false;
            return res;
        }

        res.reachable = true;
        res.pathCost = minCost;

        // 3. Reconstruct full sequence path
        List<Integer> fullPath = new ArrayList<>();
        int curr = source;
        List<Integer> sequence = new ArrayList<>(bestPerm);
        sequence.add(destination);

        for (int next : sequence) {
            Result part = allPaths.get(curr);

            LinkedList<Integer> seg = new LinkedList<>();
            int c = next;
            while (c != curr && c != -1) {
                seg.addFirst(c);
                c = part.prev[c];
            }
            if (curr == source && fullPath.isEmpty()) {
                fullPath.add(curr);
            }
            fullPath.addAll(seg);
            curr = next;
        }

        res.path = fullPath;
        res.waypoints = bestPerm;
        return res;
    }

    private static void generatePermutations(List<Integer> arr, int k, List<List<Integer>> res) {
        if (k == arr.size()) {
            res.add(new ArrayList<>(arr));
            return;
        }
        for (int i = k; i < arr.size(); i++) {
            Collections.swap(arr, i, k);
            generatePermutations(arr, k + 1, res);
            Collections.swap(arr, i, k);
        }
    }

    /** Pretty-print the path as "0 → 3 → 7 → 9" */
    public static String formatPath(List<Integer> path) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            sb.append(path.get(i));
            if (i < path.size() - 1) sb.append(" → ");
        }
        return sb.toString();
    }
}
