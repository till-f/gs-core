/*
 * Copyright 2006 - 2011 
 *     Stefan Balev 	<stefan.balev@graphstream-project.org>
 *     Julien Baudry	<julien.baudry@graphstream-project.org>
 *     Antoine Dutot	<antoine.dutot@graphstream-project.org>
 *     Yoann Pigné		<yoann.pigne@graphstream-project.org>
 *     Guilhelm Savin	<guilhelm.savin@graphstream-project.org>
 * 
 * This file is part of GraphStream <http://graphstream-project.org>.
 * 
 * GraphStream is a library whose purpose is to handle static or dynamic
 * graph, create them from scratch, file or any source and display them.
 * 
 * This program is free software distributed under the terms of two licenses, the
 * CeCILL-C license that fits European law, and the GNU Lesser General Public
 * License. You can  use, modify and/ or redistribute the software under the terms
 * of the CeCILL-C license as circulated by CEA, CNRS and INRIA at the following
 * URL <http://www.cecill.info> or under the terms of the GNU LGPL as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C and LGPL licenses and that you accept their terms.
 */

package org.graphstream.ui.viewer.test;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.Viewer;

public class TestVisibility extends JFrame {
	private static final long serialVersionUID = 1L;

	public static void main(String args[]) {
		(new TestVisibility()).test();
	}
	
	public void test() {
		GraphicGraph graph = new GraphicGraph("mygraph");
		Viewer viewer = new Viewer(graph);
		View view = viewer.addDefaultView(false);
		
		graph.addAttribute("ui.stylesheet", styleSheet);
		
		GraphicNode A = graph.addNode("A");
		GraphicNode B = graph.addNode("B");
		GraphicNode C = graph.addNode("C");
		GraphicNode D = graph.addNode("D");
		GraphicNode E = graph.addNode("E");		
		GraphicNode F = graph.addNode("F");		
		GraphicNode G = graph.addNode("G");		
		GraphicNode H = graph.addNode("H");

		graph.addEdge("AB", "A", "B");
		graph.addEdge("BC", "B", "C");
		graph.addEdge("CD", "C", "D");
		graph.addEdge("DA", "D", "A");
		
		graph.addEdge("CF", "C", "F");
		graph.addEdge("DE", "D", "E");

		graph.addEdge("EF", "E", "F");
		graph.addEdge("FG", "F", "G");
		graph.addEdge("GH", "G", "H");
		graph.addEdge("HE", "H", "E");

		A.addAttribute("xy", 0, 1); A.addAttribute("ui.label", "A");
		B.addAttribute("xy", 0, 0); B.addAttribute("ui.label", "B");
		C.addAttribute("xy", 1, 0); C.addAttribute("ui.label", "C");
		D.addAttribute("xy", 1, 1); D.addAttribute("ui.label", "D");
		E.addAttribute("xy", 2, 1); E.addAttribute("ui.label", "E");
		F.addAttribute("xy", 2, 0); F.addAttribute("ui.label", "F");
		G.addAttribute("xy", 3, 0); G.addAttribute("ui.label", "G");
		H.addAttribute("xy", 3, 1); H.addAttribute("ui.label", "H");
		
		D.addAttribute("ui.hide");
		
		add(view, BorderLayout.CENTER);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setVisible(true);

		view.getCamera().setViewPercent(0.75);
	}
	
	public static String styleSheet =
		"node#E { visibility-mode: hidden; }";
}
