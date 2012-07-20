package org.graphstream.ui.viewer.test;

import java.awt.Color;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;

public class TestColorAndSize {
	public static void main(String args[]) {
		(new TestColorAndSize()).test();
	}
	
	public void test() {
		System.out.println("You should see:");
		System.out.println("- A triangle graph.");
		System.out.println("- A node C whose size smoothly varies with time.");
		System.out.println("- A node C whose color smoothly varies from grey to red.");
		System.out.println("- A node B whose color changes between gray and blue.");
		
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
			C.addAttribute("ui.size", Units.PX, size);
			if(color==1) B.setAttribute("ui.color", Color.BLUE);
			else if(color ==0) B.setAttribute("ui.color", Color.GRAY);
			sleep(40);
		}
	}
	
	protected void sleep(long ms) {
		try { Thread.sleep(ms); } catch(InterruptedException e) {}
	}
	
	protected static String styleSheet = 
		"node { fill-color: #AAA; stroke-mode: plain; stroke-width: 1px; stroke-color: #777; }" +
		"node#C { fill-color: #AAA, red; fill-mode: dyn-plain; size-mode: dyn-size; }" +
		"node#B { fill-mode: dyn-plain; }" +
		"node:clicked { stroke-color: yellow; }" +
		"node:selected { stroke-color: red; }";
}