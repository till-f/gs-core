package org.graphstream.ui.viewer.test;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;

/**
 * The most basic test, should draw a triangle graph.
 */
public class TestBasic {
	public static void main(String args[]) {
		(new TestBasic()).test();
	}
	
	public void test() {
		Graph graph = new SingleGraph("simple");
		
		graph.display(false);

		Node A = graph.addNode("A");
		Node B = graph.addNode("B");
		Node C = graph.addNode("C");

		graph.addEdge("AB", "A", "B");
		graph.addEdge("BC", "B", "C");
		graph.addEdge("CA", "C", "A");
		
		A.addAttribute("xyz", 0, 1, 0);
		B.addAttribute("xyz", 1, 0, 0);
		C.addAttribute("xyz",-1, 0, 0);
	}
}
