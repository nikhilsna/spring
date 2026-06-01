package com.open.spring.mvc.capstone;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CapstoneDemoTest {

    @Test
    public void testListOps() {
        List<String> res = CapstoneController.demoListOps();
        assertNotNull(res);
        assertEquals(2, res.size());
        assertTrue(res.contains("one"));
        assertTrue(res.contains("three"));
    }

    @Test
    public void testGraphBfsDfs() {
        CapstoneController.Graph g = new CapstoneController.Graph();
        g.addEdge("a","b");
        g.addEdge("a","c");
        List<String> bfs = g.bfs("a");
        assertTrue(bfs.size() >= 1);
        List<String> dfs = g.dfs("a");
        assertTrue(dfs.size() >= 1);
    }

    @Test
    public void testHashing() {
        String hash = CapstoneController.hashWithBCrypt("password123");
        assertNotNull(hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }
}
