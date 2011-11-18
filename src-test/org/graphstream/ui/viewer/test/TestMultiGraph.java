package org.graphstream.ui.viewer.test;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

public class TestMultiGraph {
	public static void main(String args[]) {
		(new TestMultiGraph()).test();
	}
	
	public void test() {
		Graph graph = new MultiGraph("multi");
		
		Node A = graph.addNode("A");
		Node B = graph.addNode("B");
		Node C = graph.addNode("C");

		A.addAttribute("xyz", 0, 1, 0); A.addAttribute("ui.label", "A");
		B.addAttribute("xyz", 1, 0, 0); B.addAttribute("ui.label", "B");
		C.addAttribute("xyz",-1, 0, 0); C.addAttribute("ui.label", "C");
		
		graph.addEdge("AB1", "A", "B");
		graph.addEdge("AB2", "A", "B");
		graph.addEdge("BC1", "B", "C");
		graph.addEdge("BC2", "B", "C");
		graph.addEdge("BC3", "B", "C");
		graph.addEdge("CA", "C", "A");
		graph.addEdge("CC", "C", "C");
		graph.addEdge("BB1", "B", "B");
		graph.addEdge("BB2", "B", "B");
		graph.addEdge("BB3", "B", "B");
		
		graph.display(false);
	}
}