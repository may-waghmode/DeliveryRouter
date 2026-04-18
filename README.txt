====================================================
  DELIVERY ROUTE PLANNER — Dijkstra's Algorithm
  DSAA Course Project
====================================================

FILES:
  Graph.java          — Graph data structure (adjacency list)
  Dijkstra.java       — Dijkstra's algorithm with timing
  GraphVisualizer.java— Swing GUI with dark theme
  Main.java           — Entry point (terminal + GUI)

HOW TO RUN:
  Step 1:  javac *.java
  Step 2:  java Main

WHAT YOU WILL SEE:
  Terminal — comparison table for Sparse/Medium/Dense graphs
  GUI      — interactive window to visualize shortest paths

GUI USAGE:
  1. Click Sparse / Medium / Dense to switch map
  2. Select Source and Destination from dropdowns
  3. Click RUN DIJKSTRA
  4. Watch the path animate in GREEN on the canvas
  5. See cost and time update in the result panel

ALGORITHMS USED:
  - Dijkstra's (Min-Heap Priority Queue)
  - Time Complexity: O((V+E) log V)
  - Space Complexity: O(V)

OUTCOME:
  Dense graph  → shorter path (more shortcut edges)
  Dense graph  → slightly more time (more edges to relax)
  Dijkstra     → always finds the OPTIMAL shortest path
====================================================
