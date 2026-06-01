package com.open.spring.mvc.capstone;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class CapstoneController {


    public static List<String> demoListOps() {
        List<String> list = new ArrayList<>(); // ArrayList
        list.add("one"); // O(1)
        list.add("two");
        list.add("three");
        list.remove("two"); // O(n)
        // iterate
        for (String s : list) {
            // no-op
        }
        return list;
    }

    public static Map<String, Integer> demoMapSet() {
        Map<String, Integer> counts = new HashMap<>();
        counts.put("apple", 1);
        counts.put("banana", 2);
        // unique set
        Set<String> unique = new HashSet<>();
        unique.add("apple");
        unique.add("apple");
        counts.put("uniqueCount", unique.size());
        return counts;
    }

    /**
     * Demonstrates Stack and Queue usage via Deque.
     */
    public static Map<String, Object> demoStackQueue() {
        Deque<String> stack = new ArrayDeque<>();
        stack.push("a");
        stack.push("b");
        String popped = stack.pop(); // LIFO

        Queue<String> queue = new ArrayDeque<>();
        queue.add("x");
        queue.add("y");
        String polled = queue.poll(); // FIFO

        return Map.of("popped", popped, "polled", polled);
    }

    /**
     * Simple graph implementation for BFS/DFS demonstration. Uses adjacency map.
     */
    public static class Graph {
        private final Map<String, List<String>> adj = new HashMap<>();

        public void addEdge(String u, String v) {
            adj.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            adj.computeIfAbsent(v, k -> new ArrayList<>()); // ensure v exists
        }

        /**
         * Breadth-first search from a start node.
         * Big-O: O(V + E)
         */
        public List<String> bfs(String start) {
            List<String> order = new ArrayList<>();
            if (!adj.containsKey(start)) return order;
            Set<String> seen = new HashSet<>();
            Queue<String> q = new ArrayDeque<>();
            q.add(start); seen.add(start);
            while (!q.isEmpty()) {
                String cur = q.poll();
                order.add(cur);
                for (String nb : adj.getOrDefault(cur, Collections.emptyList())) {
                    if (seen.add(nb)) q.add(nb);
                }
            }
            return order;
        }

        /**
         * Depth-first search (recursive). Big-O: O(V + E)
         */
        public List<String> dfs(String start) {
            List<String> order = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            dfsHelper(start, seen, order);
            return order;
        }

        private void dfsHelper(String node, Set<String> seen, List<String> order) {
            if (node == null || !adj.containsKey(node) || !seen.add(node)) return;
            order.add(node);
            for (String nb : adj.get(node)) dfsHelper(nb, seen, order);
        }
    }

    /**
     * Simple tree node and a placeholder for a tree-based ML model (decision tree stub).
     */
    public static class TreeNode {
        public String value;
        public TreeNode left, right;

        public TreeNode(String value) { this.value = value; }
    }

    /**
     * Very small example of building a decision-like tree and traversing it.
     */
    public static List<String> demoTree() {
        TreeNode root = new TreeNode("root");
        root.left = new TreeNode("left");
        root.right = new TreeNode("right");
        List<String> out = new ArrayList<>();
        traverseTree(root, out);
        return out;
    }

    private static void traverseTree(TreeNode node, List<String> out) {
        if (node == null) return;
        out.add(node.value);
        traverseTree(node.left, out);
        traverseTree(node.right, out);
    }

    /**
     * Sorting example with Comparator.
     */
    public static List<Integer> demoSorting(List<Integer> input) {
        // Big-O: O(n log n)
        List<Integer> copy = new ArrayList<>(input);
        copy.sort(Comparator.naturalOrder());
        return copy;
    }

    /**
     * Demonstrates a simple linear search and a binary search (on sorted lists).
     */
    public static boolean linearSearch(List<String> list, String target) {
        // O(n)
        for (String s : list) if (Objects.equals(s, target)) return true;
        return false;
    }

    public static boolean binarySearch(List<String> sortedList, String target) {
        // O(log n) — requires sorted input
        int idx = Collections.binarySearch(sortedList, target);
        return idx >= 0;
    }

    /**
     * Hashing example using BCrypt for password-like data.
     */
    public static String hashWithBCrypt(String plain) {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        return enc.encode(plain);
    }

    /**
     * Interface and abstract class demonstration.
     */
    public interface Searchable {
        boolean contains(String target);
    }

    public static abstract class BaseCapstoneService {
        private final String name; // private field with getter

        protected BaseCapstoneService(String name) { this.name = name; }

        public String getName() { return name; }

        public abstract String info();
    }

    public static class DemoService extends BaseCapstoneService implements Searchable {
        private final List<String> store = new ArrayList<>();

        public DemoService() { super("DemoService"); }

        @Override
        public String info() { return "DemoService storing " + store.size() + " items"; }

        @Override
        public boolean contains(String target) { return store.contains(target); }

        public void add(String s) { store.add(s); }

        public boolean remove(String s) { return store.remove(s); }
    }

    /**
     * Small factory (Factory design pattern) producing services.
     */
    public static class ServiceFactory {
        public static BaseCapstoneService create(String type) {
            if ("demo".equalsIgnoreCase(type)) return new DemoService();
            return new DemoService();
        }
    }

}
