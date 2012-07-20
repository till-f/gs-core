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
package org.graphstream.ui.swingViewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;

import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.StyleGroupListener;

/**
 * A base to build graph renderers.
 *
 * <p>
 * This defines:
 * <ul>
 * 		<li>A reference to the graphic graph.</li>
 * 		<li>A "selection" object that represents the current selection, and is
 *      	updated according to the various methods changing the selection.</li>
 * 		<li>A reference to a "rendering surface" that can be any kind of container.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * In addition, this graph renderer is a listener for the style groups.
 * </p>
 */
public abstract class GraphRendererBase implements GraphRenderer,
		StyleGroupListener {
	/**
	 * The graph to draw.
	 */
	protected GraphicGraph graph;

	/**
	 * Current selection or null.
	 */
	protected Selection selection = null;

	/**
	 * The surface we are rendering on (used only
	 */
	protected Component renderingSurface;

	/**
	 * Open the renderer, keep a reference on the graphic graph and
	 * rendering surface, and registers as a listener on the style groups.
	 */
	public void open(GraphicGraph graph, Component renderingSurface) {
		if (this.graph != null)
			throw new RuntimeException(
					"renderer already open, use close() first");

		this.graph = graph;
		this.renderingSurface = renderingSurface;

		this.graph.getStyleGroups().addListener(this);
		selection = new Selection();
	}

	/**
	 * Release any reference to the graph, the rendering surface and
	 * removes this a as a listener for the style groups. 
	 */
	public void close() {
		if (graph != null) {
			graph.getStyleGroups().removeListener(this);
			renderingSurface = null;
			graph = null;
		}
	}

	public Component getRenderingSurface() {
		return renderingSurface;
	}

	// Selection

	public void beginSelectionAt(double x1, double y1) {
		selection.active = true;
		selection.lo.x = x1;
		selection.lo.y = y1;
		selection.hi.x = x1;
		selection.hi.y = y1;
	}

	public void selectionGrowsAt(double x, double y) {
		selection.hi.x = x;
		selection.hi.y = y;
	}

	public void endSelectionAt(double x2, double y2) {
		selection.active = false;
	}
	
	public boolean hasSelection() {
		return selection.active;
	}

	/**
	 * Utility method that draws a "nothing to display" message.
	 */
	protected void displayNothingToDo(Graphics2D g, int w, int h) {
		String msg1 = "Graph width/height/depth is zero !!";
		String msg2 = "Place components using the 'xyz' attribute.";

		g.setColor(Color.RED);
		g.drawLine(0, 0, w, h);
		g.drawLine(0, h, w, 0);

		double msg1length = g.getFontMetrics().stringWidth(msg1);
		double msg2length = g.getFontMetrics().stringWidth(msg2);

		double x = w / 2;
		double y = h / 2;

		g.setColor(Color.BLACK);
		g.drawString(msg1, (float)(x - msg1length / 2), (float)(y - 20));
		g.drawString(msg2, (float)(x - msg2length / 2), (float)(y + 20));
	}
}