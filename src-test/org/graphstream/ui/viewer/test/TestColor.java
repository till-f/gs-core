package org.graphstream.ui.viewer.test;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;

public class TestColor {
	public static void main(String args[]) {
		(new TestColor()).test();
	}
	
	public void test() {
		Graph graph = new SingleGraph("single");
		
		Node A = graph.addNode("A");
		Node B = graph.addNode("B");
		Node C = graph.addNode("C");
		
		graph.addEdge("AB", A.getIndex(), B.getIndex());
		graph.addEdge("BC", B.getIndex(), C.getIndex());
		graph.addEdge("CA", C.getIndex(), A.getIndex());
		
		A.addAttribute("xyz", 0, 1, 0); A.addAttribute("ui.label", "A");
		B.addAttribute("xyz", 1, 0, 0); B.addAttribute("ui.label", "B");
		C.addAttribute("xyz",-1, 0, 0); C.addAttribute("ui.label", "C");
		
		graph.display(false);
		graph.addAttribute("ui.quality");
		graph.addAttribute("ui.antialias");
		graph.addAttribute("ui.stylesheet", styleSheet);
		
		double size = 10;
		double sdir = 1;
		double color = 0;
		double cdir = 0.1f;
		
		while(true) {
			color += cdir;
			if(color > 1) { color = 1; cdir = -cdir; }
			else if(color < 0) { color = 0; cdir = -cdir; }
			size += sdir;
			if(size > 20) { size = 20; sdir = -sdir; }
			else if(size < 10) { size = 10; sdir = -sdir; }
			C.addAttribute("ui.color", color);
			C.addAttribute("ui.size", size, Units.PX);
			sleep(100);
		}
	}
	
	protected void sleep(long ms) {
		try { Thread.sleep(ms); } catch(InterruptedException e) {}
	}
	
	protected static String styleSheet = 
		"node { fill-color: #AAA; stroke-mode: plain; stroke-width: 1px; stroke-color: #777; }" +
		"node#C { fill-color: #AAA, red; fill-mode: dyn-plain; size-mode: dyn-size; }" +
		"node:clicked { stroke-color: yellow; }" +
		"node:selected { stroke-color: red; }";
}