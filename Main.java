import javax.swing.SwingUtilities;
import java.util.*;

/**
 * Main.java
 * Entry point for the Delivery Route Planner.
 *
 * 1. Runs Dijkstra on all three graph densities and prints a
 *    comparison table to the terminal.
 * 2. Launches the Swing GUI for interactive visualization.
 *
 * Compile:  javac *.java
 * Run:     \ java Main
 */
public class Main {

    // Fixed source and destination for terminal comparison
    private static final int SRC = 0;
    private static final int DST = 9;

    public static void main(String[] args) {

        printHeader();

        // ── Run on all three densities ────────────────────
        Graph sparse = Graph.createSparse();
        Graph medium = Graph.createMedium();
        Graph dense  = Graph.createDense();

        Dijkstra.Result rSparse = Dijkstra.run(sparse, SRC, DST);
        Dijkstra.Result rMedium = Dijkstra.run(medium, SRC, DST);
        Dijkstra.Result rDense  = Dijkstra.run(dense,  SRC, DST);

        // ── Individual results ────────────────────────────
        printResult("SPARSE GRAPH", sparse,  rSparse);
        printResult("MEDIUM GRAPH", medium,  rMedium);
        printResult("DENSE  GRAPH", dense,   rDense);

        // ── Comparison table ──────────────────────────────
        printComparisonTable(sparse, medium, dense, rSparse, rMedium, rDense);

        // ── Launch GUI ────────────────────────────────────
        System.out.println();
        System.out.println("  Launching GUI...");
        System.out.println("═".repeat(62));

        SwingUtilities.invokeLater(GraphVisualizer::new);
    }

    // ─────────────────────────────────────────────────────
    private static void printHeader() {
        System.out.println();
        System.out.println("  ╔══════════════════════════════════════════════════════════╗");
        System.out.println("  ║       DELIVERY ROUTE PLANNER — Dijkstra's Algorithm      ║");
        System.out.println("  ║       Shortest Path between Distribution Points          ║");
        System.out.println("  ╚══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.printf("  Source Node      : D%d%n", SRC);
        System.out.printf("  Destination Node : D%d%n", DST);
        System.out.println("  Algorithm        : Dijkstra's (Min-Heap Priority Queue)");
        System.out.println("  Complexity       : O((V + E) log V)");
        System.out.println();
    }

    private static void printResult(String title, Graph g, Dijkstra.Result r) {
        System.out.println("  ┌─────────────────────────────────────────────────────────┐");
        System.out.printf ("  │  %-55s│%n", title);
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        System.out.printf ("  │  Nodes : %-5d  Edges : %-5d                            │%n",
            g.getNumNodes(), g.getTotalEdges());

        if (r.reachable) {
            String path = Dijkstra.formatPath(r.path);
            System.out.printf("  │  Path  : %-47s│%n", path);
            System.out.printf("  │  Cost  : %-5d units                                   │%n", r.pathCost);
            System.out.printf("  │  Time  : %.6f ms  (%d ns)%n", r.timeMs, r.timeNano);
        } else {
            System.out.println("  │  No path found between source and destination           │");
        }
        System.out.println("  └─────────────────────────────────────────────────────────┘");
        System.out.println();
    }

    private static void printComparisonTable(
            Graph gs, Graph gm, Graph gd,
            Dijkstra.Result rs, Dijkstra.Result rm, Dijkstra.Result rd) {

        System.out.println("  ╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("  ║              PATH COST vs EXECUTION TIME COMPARISON           ║");
        System.out.println("  ╠═══════════╦═══════╦═══════╦════════════╦════════════╦════════╣");
        System.out.println("  ║  Density  ║ Nodes ║ Edges ║ Path Cost  ║ Time (ms)  ║  ns    ║");
        System.out.println("  ╠═══════════╬═══════╬═══════╬════════════╬════════════╬════════╣");

        printRow("Sparse", gs, rs);
        printRow("Medium", gm, rm);
        printRow("Dense",  gd, rd);

        System.out.println("  ╚═══════════╩═══════╩═══════╩════════════╩════════════╩════════╝");
        System.out.println();
        printObservations(rs, rm, rd);
    }

    private static void printRow(String label, Graph g, Dijkstra.Result r) {
        if (r.reachable) {
            System.out.printf(
                "  ║  %-9s║  %-5d║  %-5d║  %-10d║  %-10.6f║  %-6d║%n",
                label, g.getNumNodes(), g.getTotalEdges(),
                r.pathCost, r.timeMs, r.timeNano
            );
        } else {
            System.out.printf(
                "  ║  %-9s║  %-5d║  %-5d║  %-10s║  %-10s║  %-6s║%n",
                label, g.getNumNodes(), g.getTotalEdges(), "N/A", "N/A", "N/A"
            );
        }
        System.out.println("  ╠═══════════╬═══════╬═══════╬════════════╬════════════╬════════╣");
    }

    private static void printObservations(Dijkstra.Result rs, Dijkstra.Result rm, Dijkstra.Result rd) {
        System.out.println("  OBSERVATIONS:");
        System.out.println("  ─────────────────────────────────────────────────────────────");

        if (rs.reachable && rd.reachable) {
            int costDiff = rs.pathCost - rd.pathCost;
            System.out.printf("  • Dense graph found a %s path (cost diff: %d units)%n",
                costDiff > 0 ? "SHORTER" : "longer", Math.abs(costDiff));
        }

        if (rs.reachable && rm.reachable && rd.reachable) {
            long maxTime = Math.max(rs.timeNano, Math.max(rm.timeNano, rd.timeNano));
            long minTime = Math.min(rs.timeNano, Math.min(rm.timeNano, rd.timeNano));
            System.out.printf("  • Execution time range : %d ns  to  %d ns%n", minTime, maxTime);
            System.out.println("  • More edges = more relaxation steps = slightly higher time");
            System.out.println("  • Dijkstra guarantees the OPTIMAL shortest path in all cases");
        }

        System.out.println("  • Priority Queue (Min-Heap) keeps algorithm efficient at O((V+E)logV)");
        System.out.println();
    }
}
