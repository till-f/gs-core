/*
 * Copyright 2006 - 2012
 *      Stefan Balev       <stefan.balev@graphstream-project.org>
 *      Julien Baudry	<julien.baudry@graphstream-project.org>
 *      Antoine Dutot	<antoine.dutot@graphstream-project.org>
 *      Yoann Pigné	<yoann.pigne@graphstream-project.org>
 *      Guilhelm Savin	<guilhelm.savin@graphstream-project.org>
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

import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.JFrame;

import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.file.FileSourceDGS;
import org.graphstream.stream.file.FileSourceGML;
import org.graphstream.stream.thread.*;
import org.graphstream.ui.swingViewer.*;

/**
 * Test and show the use of two distinct viewers in the same JFrame.
 * 
 *  The graphs are the same, but the layouts are different.
 */
public class DemoTwoGraphsInOneViewer extends JFrame {
	private static final long serialVersionUID = 1L;

	public static final String GRAPH = "data/dolphins.gml";
//	public static final String GRAPH = "data/dorogovtsev_mendes6000.dgs";
	
	public static void main(String args[]) {
		(new DemoTwoGraphsInOneViewer()).test();
	}

	public void test() {
		Graph graph1 = new MultiGraph("g1");
		Graph graph2 = new MultiGraph("g2");
		Viewer viewer1 = new Viewer(new ThreadProxyPipe(graph1));
		Viewer viewer2 = new Viewer(new ThreadProxyPipe(graph2));

		graph1.addAttribute("ui.stylesheet", styleSheet1);
		graph2.addAttribute("ui.stylesheet", styleSheet2);
		
//		graph1.addAttribute("layout.stabilization-limit", 1);
//		graph2.addAttribute("layout.stabilization-limit", 1);
		
		//View view1 =
		viewer1.addDefaultView(false);
		viewer2.addDefaultView(false);
		viewer1.enableAutoLayout();
		viewer2.enableAutoLayout();

		FileSource in = GRAPH.endsWith(".gml") ? new FileSourceGML() : new FileSourceDGS();
		readGraph(graph1, in);
		readGraph(graph2, in);

		setLayout(new GridLayout(1, 2));
		add(viewer1.getDefaultView().getAWTComponent());
		add(viewer2.getDefaultView().getAWTComponent());
		setSize(800, 600);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	protected void readGraph(Graph graph, FileSource source) {
		source.addSink(graph);
		try {
			source.begin(getClass().getResourceAsStream(GRAPH));
			while(source.nextEvents()) {
			}
			source.end();
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		source.removeSink(graph);

	}
	
	protected String styleSheet1 =
		"graph { padding: 40px; }" +
		"node { fill-color: red; }";
	
	protected String styleSheet2 =
		"graph { padding: 40px; }" +
		"node { fill-color: blue; }";
}
