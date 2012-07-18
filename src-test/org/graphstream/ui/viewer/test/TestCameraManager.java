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

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.CameraManager;
import org.graphstream.ui.swingViewer.Viewer;

public class TestCameraManager {
	public static void main(String args[]) {
		(new TestCameraManager()).test();
	}
	
	public void test() {
		Graph graph = new SingleGraph("foo");
		Node A = graph.addNode("A");
		Node B = graph.addNode("B");
		Node C = graph.addNode("C");

		graph.addEdge("AB", "A", "B");
		graph.addEdge("BC", "B", "C");
		graph.addEdge("CA", "C", "A");
		
		A.addAttribute("xyz",  0,  1, 0);
		B.addAttribute("xyz", -1, -1, 0);
		C.addAttribute("xyz",  1, -1, 0);
		
		Viewer viewer = graph.display(false);
		
		CameraManager cam = new CameraManager(graph);
		
		String vid = viewer.getDefaultViewId();
		int mode = 0;
		
		while(true) {
			switch(mode) {
				case 0: cam.setViewAutoFit(vid); break;
				case 1: cam.setViewCenter(vid, 0, 1, 0); break;
				case 2: cam.setViewCenter(vid, 0, 0, 0); cam.setViewPercent(vid, 1.5); break;
				case 3: cam.setViewRotation(vid, 45); break;
			}
				
			mode = (mode + 1)%4;
			
			try { Thread.sleep(1000); } catch(Exception e) {}
		}
	}
}