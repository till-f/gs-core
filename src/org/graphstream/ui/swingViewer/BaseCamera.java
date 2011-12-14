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

import java.awt.Graphics2D;
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
 * This implementation is made to be used by the rendering classes. In addition of the user
 * methods that allow only to control the view, it defines methods useful to the renderer.
 * </p>
 * 
 * <p>
 * It defines the two important methods {@link #pushView(Graphics2D, double, double, double, double)}
 * and {@link #popView(Graphics2D)} that allow to prepare the camera for rendering using the
 * given Java2D graphics and a view-port inside the rendering surface in pixels. Once finished
 * rendering, one calls {@link #popView(Graphics2D)} to restore the Java2D graphics in their initial
 * state.
 * </p>
 * 
 * <p>
 * It defines a {@link #setBounds()} method allowing implementations to setup the bounds of the
 * graph in {@link #pushView(Graphics2D, double, double, double, double)}.
 * a frame.
 * </p>
 * 
 * <p>
 * It contains a {@link #cameraChanged} flag telling if the settings of the camera
 * changed since this flag was last checked. You can use {@link #resetCameraChangedFlag()} to
 * reset it. The {@link #cameraChangedFlag()} method is a user method, used by the views to
 * know if a new rendering is needed. At the contrary the {@link #resetCameraChangedFlag()} is
 * a renderer method (not present in the {@link Camera} interface) used 
 * </p>
 * 
 * <p>
 * This default implementation implements a large set of the visibility test
 * ({@link #nodeInvisible}), so that implementations only have to define the four methods
 * {@link #isNodeVisibleIn(GraphicNode, double, double, double, double)},
 * {@link #isSpriteVisibleIn(GraphicSprite, double, double, double, double)},
 * {@link #nodeContains(GraphicElement, double, double)} and
 * {@link #spriteContains(GraphicElement, double, double)}.
 * </p>
 * 
 * <p>
 * This default implementation also stores the settings
 * of the camera ({@link #zoom}, {@link #center}, {@link #rotation}, {@link #padding}), as
 * well as the actual mode ({@link #autoFit}) and if not the graph view port ({@link #gviewport}).
 * The graph view-port tells which part of the graph is visible in graph units.
 * </p>
 */
public abstract class BaseCamera implements Camera {
	/**
	 * The graphic graph we are viewing through the camera.
	 */
	protected GraphicGraph graph;
	
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
	
	/**
	 * New camera on the given graphic graph.
	 * @param graph The graphic graph to render.
	 */
	public BaseCamera(GraphicGraph graph) {
		this.graph = graph;
	}
	
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
	 * Prepare for drawing a new frame, setup the camera for rendering on the drawing surface
	 * in the given area, using the the Java2D graphics of the rendering surface.
	 * 
	 * <p>
	 * This method must be called before any rendering to setup the camera. You MUST call
	 * {@link #popView(Graphics2D)} after rendering to cleanup the Java2D graphics.
	 * </p>
	 *
	 * @param g The Java2D graphics of the rendering surface.
	 * @param x The abscissa of the view-port in pixels.
	 * @param y The ordinate of the view-port in pixels.
	 * @param width The width in pixels of the view port.
	 * @param height The height in pixels of the view port.
	 */
	public abstract void pushView(Graphics2D g, double x, double y, double width, double height);

	/**
	 * Finish the rendering phase and cleanup the Java2D graphics.
	 * @param g The Java2D graphics.
	 */
	public abstract void popView(Graphics2D g);
	
	/**
	 * Retrieve the graph padding from the graph style.
	 * 
	 * It is an utility method to be used in {@link #pushView(Graphics2D, double, double, double, double)}
	 * to setup the camera and metrics before computing the view.
	 */
	protected void setPadding() {
		padding.copy(graph.getStyle().getPadding());
		cameraChanged = true;
	}
	
	/**
	 * Set the bounds of the graphic graph in GU. This is generally called just before
	 * setting up the view for a new frame. It can be computed from the {@link #graph}
	 * field, and therefore should be used internally, hence it is protected.
	 */
	protected void setBounds() {
		Point3 lo = graph.getMinPos();
		Point3 hi = graph.getMaxPos();
		
		metrics.setBounds(lo.x, lo.y, lo.z, hi.x, hi.y, hi.z);
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
	 * invisible nodes. 
	 * 
	 * <p>
	 * This method allows for fast node, attached sprite and edge
	 * visibility checking when drawing, by storing once and for all before
	 * rendering a frame, which node is in the visible area. This must therefore
	 * be called before each rendering, in the {@link #pushView(Graphics2D, double, double, double, double)}
	 * method.
	 * </p>
	 * 
	 * <p>
	 * This method updates the {@link #nodeInvisible} field. It uses the
	 * {@link #isNodeVisibleIn(GraphicNode, double, double, double, double)} test.
	 * </p>
	 */
	public void checkVisibility(GraphicGraph graph) {
		if(! autoFit) {
			double X = metrics.surfaceViewport[0];
			double Y = metrics.surfaceViewport[1];
			double W = metrics.surfaceViewport[2];
			double H = metrics.surfaceViewport[3];
	
			nodeInvisible.clear();
	
			for (Node node : graph) {
				GraphicNode gnode = (GraphicNode) node;
				boolean visible =  (!gnode.hidden) && gnode.positioned && isNodeVisibleIn((GraphicNode) node, X, Y, X+W, Y+H);
	
				if (!visible)
					nodeInvisible.add(node.getId());
			}
		}
	}

	public GraphicElement findNodeOrSpriteAt(double x, double y) {
		
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

	public ArrayList<GraphicElement> allNodesOrSpritesIn(double x1, double y1, double x2, double y2) {
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
		if(! autoFit) {
			double X = metrics.surfaceViewport[0];
			double Y = metrics.surfaceViewport[1];
			double W = metrics.surfaceViewport[2];
			double H = metrics.surfaceViewport[3];

			return isSpriteVisibleIn(sprite, X, Y, X+W, Y+H);
		} else {
			return true;
		}
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