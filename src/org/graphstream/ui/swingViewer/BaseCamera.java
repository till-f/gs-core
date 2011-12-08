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
package org.graphstream.ui.swingViewer;

import java.util.ArrayList;
import java.util.HashSet;

import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.stylesheet.Style;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.swingViewer.util.GraphMetrics;

/**
 * Base implementation of a Camera.
 * 
 * <p>
 * This camera defines a {@link #setBounds(double, double, double, double, double, double)}
 * method allowing the viewer to set the graph bounds it computed (once per frame) so that the
 * metrics are up to date. This is generally done by the {@link View} class.
 * </p>
 * 
 * <p>
 * Similarly, it defines a {@link #setSurfaceSize(double, double)} 
 * to setup the rendering surface dimensions. This also must be setup at each frame. This
 * is generally done by the {@link View} class.
 * </p>
 * 
 * <p>
 * It contains a {@link #cameraChanged} flag telling if the settings of the camera
 * changed since this flag was last checked. You can use {@link #resetCameraChangedFlag()} to
 * reset it. This is normally used by the {@link View} class and you should not need to use it.
 * </p>
 * 
 * <p>
 * This default implementation implements a large set of the visibility test
 * ({@link #nodeInvisible}), stores the settings
 * of the camera ({@link #zoom}, {@link #center}, {@link #rotation}, {@link #padding}). The
 * actual mode ({@link #autoFit}) and if not the graph view port ({@link #gviewport}).
 * </p>
 */
public abstract class BaseCamera implements Camera {
	/**
	 * Information on the graph overall dimension and position.
	 */
	protected GraphMetrics metrics = new GraphMetrics();

	/**
	 * Automatic centering of the view.
	 */
	protected boolean autoFit = true;

	/**
	 * The camera center of view.
	 */
	protected Point3 center = new Point3();

	/**
	 * The camera zoom.
	 */
	protected double zoom;

	/**
	 * The rotation angle in degrees.
	 */
	protected double rotation;

	/**
	 * Padding around the graph, with its units.
	 */
	protected Values padding = new Values(Style.Units.GU, 0, 0, 0);

	/**
	 * Which node is visible. This allows to mark invisible nodes to fasten
	 * visibility tests for nodes, attached sprites and edges.
	 */
	protected HashSet<String> nodeInvisible = new HashSet<String>();

	/**
	 * The graph view port, if any. The graph view port is a view inside the
	 * graph space. It allows to compute the view according to a specified area
	 * of the graph space instead of the graph dimensions. It is expressed in GU.
	 */
	protected double gviewport[] = null;
	
	/**
	 * A dirty flag that tells if the camera was changed during the last frame.
	 */
	protected boolean cameraChanged = true;
	
	public Point3 getViewCenter() {
		return center;
	}

	public void setViewCenter(double x, double y, double z) {
		setAutoFitView(false);
		center.set(x, y, z);
	}
	
	public void setViewCenter(double x, double y) {
		setViewCenter(x, y, 0);
	}

	public double getViewPercent() {
		return zoom;
	}
	
	public void setViewPercent(double percent) {
		setAutoFitView(false);
		setZoom(percent);
	}

	public double getViewRotation() {
		return rotation;
	}

	public GraphMetrics getMetrics() {
		return metrics;
	}

	public double[] getGraphViewport() {
		return gviewport;
	}

	public void setGraphViewport(double minx, double miny, double maxx, double maxy) {
		setAutoFitView(false);
		setViewCenter(minx + (maxx - minx), miny + (maxy - miny));
		
		gviewport = new double[4];
		gviewport[0] = minx;
		gviewport[1] = miny;
		gviewport[2] = maxx;
		gviewport[3] = maxy;
		
		setZoom(1);
	}

	public void removeGraphViewport() {
		gviewport = null;
		resetView();
	}

	/**
	 * Enable or disable automatic adjustment of the view to see the entire
	 * graph.
	 * 
	 * @param on
	 *            If true, automatic adjustment is enabled.
	 */
	public void setAutoFitView(boolean on) {
		if (autoFit && (!on)) {
			// We go from autoFit to user view, ensure the current center is at
			// the middle of the graph, and the zoom is at one.

			zoom = 1;
			center.set(metrics.lo.x + (metrics.size.data[0] / 2), metrics.lo.y
					+ (metrics.size.data[1] / 2), 0);
		}

		autoFit = on;
		cameraChanged = true;
	}

	/**
	 * Set the zoom (or percent of the graph visible), 1 means the graph is
	 * fully visible.
	 * 
	 * @param z
	 *            The zoom.
	 */
	protected void setZoom(double z) {
		zoom = z;
		cameraChanged = true;
	}

	/**
	 * Set the rotation angle around the centre.
	 * 
	 * @param theta
	 *            The rotation angle in degrees.
	 */
	public void setViewRotation(double theta) {
		rotation = theta;
		cameraChanged = true;
	}

	/**
	 * Set the rendering surface size in pixels.
	 * 
	 * @param surfaceWidth
	 *            The width in pixels of the view port.
	 * @param surfaceHeight
	 *            The width in pixels of the view port.
	 */
	public void setSurfaceSize(double surfaceWidth, double surfaceHeight) {
		metrics.setSurfaceSize(surfaceWidth, surfaceHeight);
		cameraChanged = true;
	}

	/**
	 * Set the graph padding.
	 * 
	 * @param graph
	 *            The graphic graph.
	 */
	public void setPadding(GraphicGraph graph) {
		padding.copy(graph.getStyle().getPadding());
		cameraChanged = true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(String.format("Camera :%n"));

		builder.append(String.format("    autoFit  = %b%n", autoFit));
		builder.append(String.format("    center   = %s%n", center));
		builder.append(String.format("    rotation = %f%n", rotation));
		builder.append(String.format("    zoom     = %f%n", zoom));
		builder.append(String.format("    padding  = %s%n", padding));
		builder.append(String.format("    metrics  = %s%n", metrics));

		return builder.toString();
	}

	public void resetView() {
		setAutoFitView(true);
		setViewRotation(0);
	}
	
	/**
	 * Set the bounds of the graphic graph in GU. Called by the Viewer.
	 * 
	 * @param minx
	 *            Lowest abscissa.
	 * @param miny
	 *            Lowest ordinate.
	 * @param minz
	 *            Lowest depth.
	 * @param maxx
	 *            Highest abscissa.
	 * @param maxy
	 *            Highest ordinate.
	 * @param maxz
	 *            Highest depth.
	 */
	public void setBounds(double minx, double miny, double minz, double maxx,
			double maxy, double maxz) {
		metrics.setBounds(minx, miny, minz, maxx, maxy, maxz);
		cameraChanged = true;
	}

	public double getGraphDimension() {
		return metrics.diagonal;
	}
	
	/**
	 * True if the element should be visible on screen.
	 * 
	 * <p>
	 * The method used is to transform the center of the element (which is always in graph units)
	 * using the camera actual transformation to put it in pixel units. Then to look in the style
	 * sheet the size of the element and to test if its enclosing rectangle intersects the view
	 * port. For edges, its two nodes are used. As a speed-up by default if the camera is in
	 * automatic fitting mode, all element should be visible, and the test always returns true
	 * (excepted if the element has an explicit "hidden" flag).
	 * </p>
	 * 
	 * <p>
	 * This method does not look at the style visibility mode options as the visibility in styles
	 * is defined for the whole group and not for individual elements, it is far faster to check
	 * this once before the rendering pass of style groups. This means that if the element has
	 * a style that make it invisible, this method may still return true.
	 * </p>
	 * 
	 * @param element
	 *            The element to test.
	 * @return True if the element is visible and therefore must be rendered.
	 */
	public boolean isVisible(GraphicElement element) {
		if(autoFit) {
  	        return ((! element.hidden) );// We do the style visibility test in the renderers.
		} else {
			switch (element.getSelectorType()) {
				case NODE:
					return !nodeInvisible.contains(element.getId());
				case EDGE:
					return isEdgeVisible((GraphicEdge) element);
				case SPRITE:
					return isSpriteVisible((GraphicSprite) element);
				default:
					return false;
			}
		}
	}

	public abstract Point3 transformPxToGu(double x, double y);

	public abstract Point3 transformGuToPx(double x, double y, double z);

	public abstract Point3 transformPxToGu(Point3 p);

	public abstract Point3 transformGuToPx(Point3 p);

	/**
	 * Process each node to check if it is in the actual view port, and mark
	 * invisible nodes. This method allows for fast node, sprite and edge
	 * visibility checking when drawing. This must be called before each
	 * rendering (if the view port changed).
	 */
	public void checkVisibility(GraphicGraph graph) {
		if(! autoFit) {
			double W = metrics.surfaceSize.data[0];
			double H = metrics.surfaceSize.data[1];
	
			nodeInvisible.clear();
	
			for (Node node : graph) {
				GraphicNode gnode = (GraphicNode) node;
				boolean visible =  (!gnode.hidden) && gnode.positioned && isNodeVisibleIn((GraphicNode) node, 0, 0, W, H);
	
				if (!visible)
					nodeInvisible.add(node.getId());
			}
		}
	}

	/**
	 * Search for the first node or sprite (in that order) that contains the
	 * point at coordinates (x, y).
	 * 
	 * @param graph
	 *            The graph to search for.
	 * @param x
	 *            The point abscissa.
	 * @param y
	 *            The point ordinate.
	 * @return The first node or sprite at the given coordinates or null if
	 *         nothing found.
	 */
	public GraphicElement findNodeOrSpriteAt(GraphicGraph graph, double x,
			double y) {
		
		for (Node n : graph) {
			GraphicNode node = (GraphicNode) n;

			if (nodeContains(node, x, y))
				return node;
		}

		for (GraphicSprite sprite : graph.spriteSet()) {
			if (spriteContains(sprite, x, y))
				return sprite;
		}

		return null;
	}

	/**
	 * Search for all the nodes and sprites contained inside the rectangle
	 * (x1,y1)-(x2,y2).
	 * 
	 * @param graph
	 *            The graph to search for.
	 * @param x1
	 *            The rectangle lowest point abscissa.
	 * @param y1
	 *            The rectangle lowest point ordinate.
	 * @param x2
	 *            The rectangle highest point abscissa.
	 * @param y2
	 *            The rectangle highest point ordinate.
	 * @return The set of sprites and nodes in the given rectangle.
	 */
	public ArrayList<GraphicElement> allNodesOrSpritesIn(GraphicGraph graph,
			double x1, double y1, double x2, double y2) {
		ArrayList<GraphicElement> elts = new ArrayList<GraphicElement>();

		for (Node node : graph) {
			if (isNodeVisibleIn((GraphicNode) node, x1, y1, x2, y2))
				elts.add((GraphicNode) node);
		}

		for (GraphicSprite sprite : graph.spriteSet()) {
			if (isSpriteVisibleIn(sprite, x1, y1, x2, y2))
				elts.add(sprite);
		}

		return elts;
	}

	/**
	 * Check if a sprite is visible in the current view port.
	 * 
	 * @param sprite
	 *            The sprite to check.
	 * @return True if visible.
	 */
	protected boolean isSpriteVisible(GraphicSprite sprite) {
		return isSpriteVisibleIn(sprite, 0, 0, metrics.surfaceSize.data[0],
				metrics.surfaceSize.data[1]);
	}

	/**
	 * Check if an edge is visible in the current view port.
	 * 
	 * This method tests the visibility of nodes. If the two nodes are invisible, the edge is
	 * considered invisible. This is not completely exact, since an edge can still be visible if
	 * its two nodes are out of view. However it has the advantage of speed.
	 * 
	 * @param edge
	 *            The edge to check.
	 * @return True if visible.
	 */
	protected boolean isEdgeVisible(GraphicEdge edge) {
		GraphicNode node0 = edge.getNode0();
		GraphicNode node1 = edge.getNode1();
		
		if(edge.hidden)
			return false;
		
		if((!node1.positioned) || (!node0.positioned))
			return false;
		
		boolean node0Invis = nodeInvisible.contains(node0.getId());
		boolean node1Invis = nodeInvisible.contains(node1.getId());

		return !(node0Invis && node1Invis);
	}

	/**
	 * Is the given node visible in the given area in pixels.
	 * 
	 * @param node
	 *            The node to check.
	 * @param X1
	 *            The min abscissa of the area.
	 * @param Y1
	 *            The min ordinate of the area.
	 * @param X2
	 *            The max abscissa of the area.
	 * @param Y2
	 *            The max ordinate of the area.
	 * @return True if the node lies in the given area.
	 */
	protected abstract boolean isNodeVisibleIn(GraphicNode node, double X1, double Y1, double X2, double Y2);

	/**
	 * Is the given sprite visible in the given area in pixels.
	 * 
	 * @param sprite
	 *            The sprite to check.
	 * @param X1
	 *            The min abscissa of the area.
	 * @param Y1
	 *            The min ordinate of the area.
	 * @param X2
	 *            The max abscissa of the area.
	 * @param Y2
	 *            The max ordinate of the area.
	 * @return True if the node lies in the given area.
	 */
	protected abstract boolean isSpriteVisibleIn(GraphicSprite sprite, double X1, double Y1,
			double X2, double Y2);

	/**
	 * Check if a node contains the given point (x,y) in pixels.
	 * 
	 * @param elt
	 *            The node.
	 * @param x
	 *            The point abscissa.
	 * @param y
	 *            The point ordinate.
	 * @return True if (x,y) is in the given element.
	 */
	protected abstract boolean nodeContains(GraphicElement elt, double x, double y);
	
	protected boolean edgeContains(GraphicElement elt, double x, double y) {
		return false;
	}

	/**
	 * Check if a sprite contains the given point (x,y) in pixels.
	 * 
	 * @param elt
	 *            The sprite.
	 * @param x
	 *            The point abscissa.
	 * @param y
	 *            The point ordinate.
	 * @return True if (x,y) is in the given element.
	 */
	protected abstract boolean spriteContains(GraphicElement elt, double x, double y);
	
	/**
	 * A flag set to true if something changed in the camera settings
	 * {@link #resetCameraChangedFlag()} was last called.
	 */
	public boolean cameraChangedFlag() {
		return cameraChanged;
	}
	
	/**
	 * Reset the "camera changed" flag to false. This should only be used in the
	 * view or viewer that handle the camera.
	 */
	public void resetCameraChangedFlag() {
		cameraChanged = false;
	}
}