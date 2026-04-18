import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * GraphVisualizer.java
 * Swing-based GUI for the Delivery Route Planner.
 *
 * Layout:
 *   LEFT  — Graph canvas (draws nodes, edges, shortest path)
 *   RIGHT — Control panel (density buttons, source/dest dropdowns,
 *            result display, comparison table)
 *
 * Visual highlights:
 *   • Dark background (#1a1a2e)
 *   • Gray lines = all roads
 *   • Thick GREEN animated line = shortest path
 *   • YELLOW circle = source node
 *   • RED circle    = destination node
 *   • Edge weights shown as small labels
 */
public class GraphVisualizer extends JFrame {

    // ── Colors ────────────────────────────────────────────
    private static final Color BG_DARK      = new Color(15,  17,  35);
    private static final Color BG_PANEL     = new Color(20,  24,  48);
    private static final Color BG_CARD      = new Color(26,  30,  60);
    private static final Color COL_EDGE     = new Color(60,  72, 110);
    private static final Color COL_NODE     = new Color(50,  90, 180);
    private static final Color COL_NODE_SRC = new Color(230, 190,  30);
    private static final Color COL_NODE_DST = new Color(220,  60,  60);
    private static final Color COL_NODE_WP  = new Color(180,  80, 220);
    private static final Color COL_PATH     = new Color( 40, 210,  80);
    private static final Color COL_TEXT     = new Color(220, 220, 240);
    private static final Color COL_MUTED    = new Color(130, 140, 170);
    private static final Color COL_ACCENT   = new Color( 80, 160, 255);
    private static final Color COL_BTN_ACT  = new Color( 40, 100, 210);
    private static final Color COL_BTN_IDLE = new Color( 35,  42,  80);

    private static final int NODE_R = 22; // node circle radius

    // ── State ─────────────────────────────────────────────
    private Graph           currentGraph;
    private String          currentDensity = "Sparse";
    private Dijkstra.Result currentResult  = null;
    private int             animStep       = 0;   // for path animation
    private javax.swing.Timer animTimer;

    // Results cache for comparison table
    private Map<String, Dijkstra.Result> resultCache = new LinkedHashMap<>();

    // ── UI components ─────────────────────────────────────
    private GraphCanvas   canvas;
    private JComboBox<String> srcBox, dstBox;
    private List<JToggleButton> wpButtons;
    private JPanel        wpPanel;
    private JLabel        pathLabel, costLabel, timeLabel, infoLabel;
    private JPanel        tablePanel;
    private JButton       btnSparse, btnMedium, btnDense;

    // ─────────────────────────────────────────────────────
    public GraphVisualizer() {
        super("Delivery Route Planner — Dijkstra's Shortest Path");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1050, 600);
        setMinimumSize(new Dimension(900, 560));
        setBackground(BG_DARK);

        buildUI();
        loadGraph("Sparse");
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ─────────────────────────────────────────────────────
    // BUILD UI
    // ─────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());

        // Title bar
        JPanel titleBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        titleBar.setBackground(new Color(10, 12, 28));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 50, 90)));
        JLabel titleLbl = new JLabel("  DELIVERY ROUTE PLANNER");
        titleLbl.setFont(new Font("Monospaced", Font.BOLD, 15));
        titleLbl.setForeground(new Color(80, 160, 255));
        JLabel subLbl = new JLabel("— Dijkstra's Algorithm");
        subLbl.setFont(new Font("Monospaced", Font.PLAIN, 13));
        subLbl.setForeground(COL_MUTED);
        titleBar.add(titleLbl);
        titleBar.add(subLbl);
        add(titleBar, BorderLayout.NORTH);

        // Main split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(630);
        split.setDividerSize(3);
        split.setBorder(null);

        // Canvas
        canvas = new GraphCanvas();
        canvas.setBackground(BG_DARK);
        canvas.setPreferredSize(new Dimension(630, 520));
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Click to add a new node dynamically!
                if (e.getButton() == MouseEvent.BUTTON1) {
                    addDynamicNode(e.getX(), e.getY());
                }
            }
        });
        split.setLeftComponent(canvas);

        // Right control panel
        split.setRightComponent(buildRightPanel());

        add(split, BorderLayout.CENTER);
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(14, 14, 14, 14));

        // ── Density buttons ──────────────────────────────
        panel.add(sectionLabel("MAP DENSITY"));
        JPanel densityRow = new JPanel(new GridLayout(1, 3, 6, 0));
        densityRow.setBackground(BG_PANEL);
        btnSparse = densityBtn("Sparse",  12, "e");
        btnMedium = densityBtn("Medium",  20, "s");
        btnDense  = densityBtn("Dense",   35, "y");
        densityRow.add(btnSparse);
        densityRow.add(btnMedium);
        densityRow.add(btnDense);
        densityRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        panel.add(densityRow);
        panel.add(Box.createVerticalStrut(12));

        // ── Source / Destination ─────────────────────────
        panel.add(sectionLabel("SOURCE & DESTINATION"));
        String[] nodeNames = {"D0","D1","D2","D3","D4","D5","D6","D7","D8","D9"};
        JPanel sdPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        sdPanel.setBackground(BG_PANEL);
        sdPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        sdPanel.add(styledLabel("Source:"));
        srcBox = styledCombo(nodeNames);
        srcBox.setSelectedIndex(0);
        sdPanel.add(srcBox);

        sdPanel.add(styledLabel("Destination:"));
        dstBox = styledCombo(nodeNames);
        dstBox.setSelectedIndex(9);
        sdPanel.add(dstBox);

        panel.add(sdPanel);
        panel.add(Box.createVerticalStrut(10));

        // ── Waypoints ────────────────────────────────────
        panel.add(sectionLabel("MIDDLE STOPS (OPTIONAL)"));
        wpPanel = new JPanel(new GridLayout(0, 5, 4, 4));
        wpPanel.setBackground(BG_PANEL);
        wpButtons = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            JToggleButton btn = createWaypointButton("D" + i);
            wpButtons.add(btn);
            wpPanel.add(btn);
        }
        wpPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.add(wpPanel);
        panel.add(Box.createVerticalStrut(10));

        // ── Run button ───────────────────────────────────
        JButton runBtn = new JButton("  ▶  RUN DIJKSTRA  ");
        runBtn.setFont(new Font("Monospaced", Font.BOLD, 13));
        runBtn.setBackground(COL_BTN_ACT);
        runBtn.setForeground(Color.WHITE);
        runBtn.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        runBtn.setFocusPainted(false);
        runBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        runBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        runBtn.addActionListener(e -> runDijkstra());
        runBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { runBtn.setBackground(new Color(60,120,240)); }
            public void mouseExited(MouseEvent e)  { runBtn.setBackground(COL_BTN_ACT); }
        });
        panel.add(runBtn);
        panel.add(Box.createVerticalStrut(14));

        // ── Result ───────────────────────────────────────
        panel.add(sectionLabel("RESULT"));
        JPanel resultCard = card();
        resultCard.setLayout(new GridLayout(4, 1, 0, 4));

        pathLabel = resultLine("Path:", "—");
        costLabel = resultLine("Cost:", "—");
        timeLabel = resultLine("Time:", "—");
        infoLabel = resultLine("Info:", "—");
        resultCard.add(pathLabel);
        resultCard.add(costLabel);
        resultCard.add(timeLabel);
        resultCard.add(infoLabel);
        resultCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        panel.add(resultCard);
        panel.add(Box.createVerticalStrut(14));

        // ── Comparison table ─────────────────────────────
        panel.add(sectionLabel("COMPARISON TABLE"));
        tablePanel = new JPanel();
        tablePanel.setBackground(BG_CARD);
        tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.Y_AXIS));
        tablePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 52, 100), 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        tablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        // Header
        tablePanel.add(tableRow("Density", "Edges", "Cost", "Time (ms)", true));
        tablePanel.add(Box.createVerticalStrut(4));
        panel.add(tablePanel);

        // ── Complexity note ──────────────────────────────
        panel.add(Box.createVerticalStrut(12));
        JLabel cLabel = new JLabel("<html><font color='#5080a0'>Complexity: </font>" +
            "<font color='#60a0ff'>O((V+E) log V)</font></html>");
        cLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        cLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(cLabel);

        highlightDensityBtn("Sparse");
        return panel;
    }

    // ─────────────────────────────────────────────────────
    // GRAPH LOADING & DIJKSTRA
    // ─────────────────────────────────────────────────────
    private void loadGraph(String density) {
        currentDensity = density;
        switch (density) {
            case "Sparse" -> currentGraph = Graph.createSparse();
            case "Medium" -> currentGraph = Graph.createMedium();
            case "Dense"  -> currentGraph = Graph.createDense();
        }
        currentResult = null;
        animStep      = 0;
        if (animTimer != null) animTimer.stop();
        highlightDensityBtn(density);
        canvas.repaint();
    }

    private void runDijkstra() {
        int src = srcBox.getSelectedIndex();
        int dst = dstBox.getSelectedIndex();
        if (src == dst) {
            pathLabel.setText("<html><b style='color:#e06060'>Source = Destination!</b></html>");
            return;
        }

        List<Integer> wps = new ArrayList<>();
        for (int i = 0; i < wpButtons.size(); i++) {
            if (wpButtons.get(i).isSelected() && i != src && i != dst) {
                wps.add(i);
            }
        }

        currentResult = Dijkstra.runMultiStopTSP(currentGraph, src, dst, wps);
        resultCache.put(currentDensity, currentResult);

        if (!currentResult.reachable) {
            pathLabel.setText("<html><b style='color:#e06060'>No path found</b></html>");
            return;
        }

        // Update result labels
        String pathStr = Dijkstra.formatPath(currentResult.path);
        pathLabel.setText("<html><b style='color:#aab8d0'>Path: </b>" +
            "<b style='color:#40d060'>" + pathStr + "</b></html>");
        costLabel.setText("<html><b style='color:#aab8d0'>Cost: </b>" +
            "<b style='color:#ffffff'>" + currentResult.pathCost + " units</b></html>");
        timeLabel.setText("<html><b style='color:#aab8d0'>Time: </b>" +
            "<b style='color:#60aaff'>" + String.format("%.4f ms", currentResult.timeMs) +
            "</b>  <i style='color:#606080'>(" + currentResult.timeNano + " ns)</i></html>");
        
        long perms = 1;
        int numWp = currentResult.waypoints != null ? currentResult.waypoints.size() : 0;
        for (int i = 2; i <= numWp; i++) perms *= i;
        int edgesChecked = currentGraph.getTotalEdges(); // simple heuristic statement
        infoLabel.setText("<html><b style='color:#aab8d0'>Info: </b>" +
            "<b style='color:#c0a0e0'>Eval'd " + perms + " TSP path combos, expanded " + currentResult.path.size() + " segments.</b></html>");

        updateComparisonTable();
        startPathAnimation();
    }

    // ─────────────────────────────────────────────────────
    // ANIMATED PATH DRAWING
    // ─────────────────────────────────────────────────────
    private void startPathAnimation() {
        animStep = 0;
        if (animTimer != null) animTimer.stop();
        canvas.repaint();
        animTimer = new javax.swing.Timer(200, e -> {
            if (animStep < currentResult.path.size()) {
                animStep++;
                canvas.repaint();
            } else {
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        animTimer.start();
    }

    // ─────────────────────────────────────────────────────
    // COMPARISON TABLE UPDATE
    // ─────────────────────────────────────────────────────
    private void updateComparisonTable() {
        // Remove old rows (keep header at index 0)
        while (tablePanel.getComponentCount() > 2) tablePanel.remove(2);

        for (Map.Entry<String, Dijkstra.Result> entry : resultCache.entrySet()) {
            Dijkstra.Result r = entry.getValue();
            String timeStr = String.format("%.4f", r.timeMs);
            tablePanel.add(tableRow(entry.getKey(),
                String.valueOf(currentGraph.getTotalEdges()),
                String.valueOf(r.pathCost),
                timeStr,
                false));
        }
        tablePanel.revalidate();
        tablePanel.repaint();
    }

    // ─────────────────────────────────────────────────────
    // CANVAS (inner class)
    // ─────────────────────────────────────────────────────
    class GraphCanvas extends JPanel {

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            if (currentGraph == null) return;

            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int W = getWidth(), H = getHeight();
            // Subtle grid
            g.setColor(new Color(30, 36, 65));
            g.setStroke(new BasicStroke(0.5f));
            for (int x = 0; x < W; x += 40) g.drawLine(x, 0, x, H);
            for (int y = 0; y < H; y += 40) g.drawLine(0, y, 0 + W, y);

            List<Graph.Node> nodes = currentGraph.getNodes();

            // ── Draw all edges ───────────────────────────
            g.setStroke(new BasicStroke(1.5f));
            g.setFont(new Font("Monospaced", Font.PLAIN, 10));

            for (int u = 0; u < currentGraph.getNumNodes(); u++) {
                for (Graph.Edge edge : currentGraph.getEdges(u)) {
                    if (edge.to > u) { // draw each edge once
                        Graph.Node a = nodes.get(u);
                        Graph.Node b = nodes.get(edge.to);
                        g.setColor(COL_EDGE);
                        g.drawLine(a.x, a.y, b.x, b.y);
                        // Weight label at midpoint
                        int mx = (a.x + b.x) / 2;
                        int my = (a.y + b.y) / 2;
                        g.setColor(new Color(80, 90, 130));
                        g.drawString(String.valueOf(edge.weight), mx - 6, my - 3);
                    }
                }
            }

            // ── Draw shortest path edges (animated) ──────
            if (currentResult != null && currentResult.reachable) {
                // Glow effect — draw thick faint line first
                g.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(40, 200, 80, 40));
                drawPathEdges(g, nodes, Math.min(animStep, currentResult.path.size()));

                // Main path line
                g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(COL_PATH);
                drawPathEdges(g, nodes, Math.min(animStep, currentResult.path.size()));
            }

            // ── Draw nodes ───────────────────────────────
            for (int i = 0; i < nodes.size(); i++) {
                Graph.Node n = nodes.get(i);
                Color fill;
                if (currentResult != null && i == currentResult.source)      fill = COL_NODE_SRC;
                else if (currentResult != null && i == currentResult.destination) fill = COL_NODE_DST;
                else if (currentResult != null && currentResult.waypoints != null && currentResult.waypoints.contains(i)) fill = COL_NODE_WP;
                else fill = COL_NODE;

                // Outer glow
                Color glow = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 45);
                g.setColor(glow);
                g.fillOval(n.x - NODE_R - 6, n.y - NODE_R - 6, (NODE_R + 6) * 2, (NODE_R + 6) * 2);

                // Circle fill
                g.setColor(fill.darker());
                g.fillOval(n.x - NODE_R, n.y - NODE_R, NODE_R * 2, NODE_R * 2);
                g.setStroke(new BasicStroke(2f));
                g.setColor(fill);
                g.drawOval(n.x - NODE_R, n.y - NODE_R, NODE_R * 2, NODE_R * 2);

                // Node label
                g.setFont(new Font("Monospaced", Font.BOLD, 13));
                g.setColor(Color.WHITE);
                FontMetrics fm = g.getFontMetrics();
                String lbl = String.valueOf(i);
                g.drawString(lbl, n.x - fm.stringWidth(lbl) / 2, n.y + fm.getAscent() / 2 - 1);
            }

            // ── Legend ───────────────────────────────────
            drawLegend(g, W, H);
        }

        private void drawPathEdges(Graphics2D g, List<Graph.Node> nodes, int upTo) {
            if (currentResult.path.size() < 2) return;
            for (int i = 0; i < upTo - 1 && i < currentResult.path.size() - 1; i++) {
                Graph.Node a = nodes.get(currentResult.path.get(i));
                Graph.Node b = nodes.get(currentResult.path.get(i + 1));
                g.drawLine(a.x, a.y, b.x, b.y);

                // Arrow at midpoint
                double dx = b.x - a.x, dy = b.y - a.y;
                double len = Math.sqrt(dx * dx + dy * dy);
                double mx = a.x + dx * 0.52, my = a.y + dy * 0.52;
                double angle = Math.atan2(dy, dx);
                int as = 9;
                int[] ax = {
                    (int)(mx + as * Math.cos(angle)),
                    (int)(mx + as * 0.5 * Math.cos(angle + 2.4)),
                    (int)(mx + as * 0.5 * Math.cos(angle - 2.4))
                };
                int[] ay = {
                    (int)(my + as * Math.sin(angle)),
                    (int)(my + as * 0.5 * Math.sin(angle + 2.4)),
                    (int)(my + as * 0.5 * Math.sin(angle - 2.4))
                };
                g.fillPolygon(ax, ay, 3);
            }
        }

        private void drawLegend(Graphics2D g, int W, int H) {
            int lx = 14, ly = H - 80;
            g.setFont(new Font("Monospaced", Font.PLAIN, 11));
            // background
            g.setColor(new Color(10, 12, 30, 180));
            g.fillRoundRect(lx - 6, ly - 28, 185, 86, 8, 8);
            g.setColor(new Color(40, 52, 90));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(lx - 6, ly - 28, 185, 86, 8, 8);

            Color[] cols = {COL_NODE_SRC, COL_NODE_DST, COL_NODE_WP, COL_PATH, COL_EDGE};
            String[] lbls = {"Source node", "Destination node", "Waypoint", "Shortest path", "Road (edges/click to add node)"};
            for (int i = 0; i < 5; i++) {
                g.setColor(cols[i]);
                g.fillOval(lx + 6, ly - 20 + i * 14, 9, 9);
                g.setColor(new Color(190, 200, 220));
                g.drawString(lbls[i], lx + 20, ly - 20 + i * 14 + 9);
            }
        }
    }

    // ─────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────
    private void addDynamicNode(int x, int y) {
        if (currentGraph == null) return;
        int newId = currentGraph.addDynamicNode(x, y);
        String name = "D" + newId;
        
        // Update comboboxes
        srcBox.addItem(name);
        dstBox.addItem(name);
        
        // Add new waypoint button
        JToggleButton btn = createWaypointButton(name);
        wpButtons.add(btn);
        wpPanel.add(btn);
        
        // Re-layout and repaint
        wpPanel.revalidate();
        wpPanel.repaint();
        canvas.repaint();
    }

    private JToggleButton createWaypointButton(String text) {
        JToggleButton btn = new JToggleButton(text);
        btn.setFont(new Font("Monospaced", Font.PLAIN, 10));
        btn.setBackground(COL_BTN_IDLE);
        btn.setForeground(COL_MUTED);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addItemListener(e -> {
            if (btn.isSelected()) {
                btn.setBackground(new Color(120, 60, 160));
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(COL_BTN_IDLE);
                btn.setForeground(COL_MUTED);
            }
        });
        return btn;
    }

    private JButton densityBtn(String label, int edges, String shortcut) {
        JButton btn = new JButton("<html><center><b>" + label + "</b><br><small>" + edges + " edges</small></center></html>");
        btn.setFont(new Font("Monospaced", Font.PLAIN, 11));
        btn.setBackground(COL_BTN_IDLE);
        btn.setForeground(COL_TEXT);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 4));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> loadGraph(label));
        return btn;
    }

    private void highlightDensityBtn(String density) {
        for (JButton b : new JButton[]{btnSparse, btnMedium, btnDense}) {
            b.setBackground(COL_BTN_IDLE);
            b.setForeground(COL_MUTED);
        }
        JButton active = density.equals("Sparse") ? btnSparse : density.equals("Medium") ? btnMedium : btnDense;
        active.setBackground(COL_BTN_ACT);
        active.setForeground(Color.WHITE);
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.BOLD, 10));
        l.setForeground(new Color(80, 100, 150));
        l.setBorder(new EmptyBorder(0, 0, 5, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel styledLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        l.setForeground(COL_MUTED);
        return l;
    }

    private JComboBox<String> styledCombo(String[] items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setBackground(BG_CARD);
        cb.setForeground(COL_TEXT);
        cb.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return cb;
    }

    private JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(BG_CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(40, 52, 100), 1),
            new EmptyBorder(8, 10, 8, 10)
        ));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private JLabel resultLine(String key, String val) {
        JLabel l = new JLabel("<html><b style='color:#aab8d0'>" + key + " </b>" + val + "</html>");
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        l.setForeground(COL_TEXT);
        return l;
    }

    private JPanel tableRow(String d, String e, String cost, String time, boolean header) {
        JPanel row = new JPanel(new GridLayout(1, 4, 0, 0));
        row.setBackground(header ? new Color(30, 40, 75) : BG_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        String[] vals = {d, e, cost, time};
        for (String v : vals) {
            JLabel l = new JLabel(v, SwingConstants.CENTER);
            l.setFont(new Font("Monospaced", header ? Font.BOLD : Font.PLAIN, 10));
            l.setForeground(header ? new Color(150, 170, 220) :
                (v.equals(cost) ? new Color(80, 200, 100) :
                 v.equals(time) ? new Color(80, 160, 255) : COL_MUTED));
            row.add(l);
        }
        return row;
    }
}
