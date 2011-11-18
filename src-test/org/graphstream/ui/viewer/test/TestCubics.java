package org.graphstream.ui.viewer.test;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;

public class TestCubics {
	public static void main(String args[]) {
		(new TestCubics()).test();
	}
	
	public void test() {
		Graph graph = new MultiGraph("multi");
		
		Node A = graph.addNode("A");
		Node B = graph.addNode("B");
		Node C = graph.addNode("C");

		A.addAttribute("xyz", 0, 1, 0); A.addAttribute("ui.label", "A");
		B.addAttribute("xyz", 1, 0, 0); B.addAttribute("ui.label", "B");
		C.addAttribute("xyz",-1, -0.5, 0); C.addAttribute("ui.label", "C");
		
		graph.addEdge("AB1", "A", "B");
		graph.addEdge("AB2", "A", "B");
		graph.addEdge("BC", "B", "C");
		graph.addEdge("CA", "C", "A");
		graph.addEdge("BB", "B", "B");
		
		graph.addAttribute("ui.stylesheet", styleSheet);
		graph.display(false);
	}
	
	protected String styleSheet = 
		"edge { shape: cubic-curve; }";
}