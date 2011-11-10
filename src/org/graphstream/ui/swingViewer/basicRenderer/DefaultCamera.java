/*
 * Copyright 2006 - 2011 
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
package org.graphstream.ui.swingViewer.basicRenderer;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
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
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.NodeSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.SpriteSkeleton;
import org.graphstream.ui.swingViewer.util.Camera;
import org.graphstream.ui.swingViewer.util.GraphMetrics;

/**
 * Define how the graph is viewed.
 * 
 * <p>
 * The camera is in charge of projecting the graph spaces in graph units (GU)
 * into user spaces (often in pixels). It defines the transformation (an affine
 * matrix) to passe from the first to the second (an the inverse to do the
 * reverse transformation, often useful). It also contains the graph
 * metrics, a set of values that give the overall dimensions of the graph in
 * graph units, as well as the view port, the area on the screen (or any
 * rendering surface) that will receive the results in pixels (or rendering
 * units).
 * </p>
 * 
 * <p>
 * The camera defines a center at which it always points. It can zoom on the
 * graph, pan in any direction and rotate along two axes.
 * </p>
 * 
 * <p>
 * The camera is also in charge of doing the visibility test. This test is made
 * automatically, when not in auto-fit mode (else all is by definition visible).
 * </p>
 */
public class DefaultCamera implements Camera {
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
	 * The graph-space -> pixel-space transformation.
	 */
	protected AffineTransform Tx = new AffineTransform();

	/**
	 * The inverse transform of Tx.
	 */
	protected AffineTransform xT;

	/**
	 * The previous affine transform.
	 */
	protected AffineTransform oldTx;

	/**
	 * The rotation angle.
	 */
	protected double rotation;

	/**
	 * Padding around the graph.
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
	 * of the graph space instead of the graph dimensions.
	 */
	protected double gviewport[] = null;
	
	/**
	 * New camera with default settings.
	 */
	public DefaultCamera() {
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
	
	public void setBounds(double minx, double miny, double minz, double maxx,
			double maxy, double maxz) {
		metrics.setBounds(minx, miny, minz, maxx, maxy, maxz);
	}

	public double getGraphDimension() {
		return metrics.diagonal;
	}
	
	/**
	 * True if the element should be visible on screen. The method used is to
	 * transform the center of the element (which is always in graph units)
	 * using the camera actual transformation to put it in pixel units. Then to
	 * look in the style sheet the size of the element and to test if its
	 * enclosing rectangle intersects the view port. For edges, its two nodes
	 * are used. As a speed-up by default if the camera is in automatic fitting
	 * mode, all element should be visible, and the test always returns true.
	 * 
	 * @param element
	 *            The element to test.
	 * @return True if the element is visible and therefore must be rendered.
	 */
	public boolean isVisible(GraphicElement element) {
		if(autoFit) {
			return true;
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

	public Point3 transformPxToGu(double x, double y) {
		Point2D.Double p = new Point2D.Double(x, y);
		xT.transform(p, p);
		return new Point3(p.x, p.y, 0);
	}

	public Point3 transformGuToPx(double x, double y, double z) {
		Point2D.Double p = new Point2D.Double(x, y);
		Tx.transform(p, p);
		return new Point3(p.x, p.y, 0);
	}

	public Point3 transformPxToGu(Point3 p) {
		Point2D.Double pp = new Point2D.Double(p.x, p.y);
		xT.transform(pp, pp);
		p.x = pp.x;
		p.y = pp.y;
		return p;
	}

	public Point3 transformGuToPx(Point3 p) {
		Point2D.Double pp = new Point2D.Double(p.x, p.y);
		Tx.transform(pp, pp);
		p.x = pp.x;
		p.y = pp.y;
		return p;
	}

	/**
	 * Process each node to check if it is in the actual view port, and mark
	 * invisible nodes. This method allows for fast node, sprite and edge
	 * visibility checking when drawing. This must be called before each
	 * rendering (if the view port changed).
	 */
	public void checkVisibility(GraphicGraph graph) {
		if(! autoFit) {
			double W = metrics.viewport.data[0];
			double H = metrics.viewport.data[1];
	
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
	 * Set the camera view in the given graphics and backup the previous
	 * transform of the graphics. Call {@link #popView(Graphics2D)} to restore
	 * the saved transform. You can only push one time the view.
	 * 
	 * @param g2
	 *            The Swing graphics to change.
	 */
	public void pushView(GraphicGraph graph, Graphics2D g2) {
		if (oldTx == null) {
			oldTx = g2.getTransform();

			if (autoFit)
				Tx = autoFitView(g2, Tx);
			else
				Tx = userView(g2, Tx);

			g2.setTransform(Tx);
		}
		
		checkVisibility(graph);
	}

	/**
	 * Restore the transform that was used before {@link #pushView(GraphicGraph, Graphics2D)}
	 * is used.
	 * 
	 * @param g2
	 *            The Swing graphics to restore.
	 */
	public void popView(Graphics2D g2) {
		if (oldTx != null) {
			g2.setTransform(oldTx);
			oldTx = null;
		}
	}

	/**
	 * Compute a transformation matrix that pass from graph units (user space)
	 * to pixel units (device space) so that the whole graph is visible.
	 * 
	 * @param g2
	 *            The Swing graphics.
	 * @param Tx
	 *            The transformation to modify.
	 * @return The transformation modified.
	 */
	protected AffineTransform autoFitView(Graphics2D g2, AffineTransform Tx) {
		double sx, sy;
		double tx, ty;
		double padXgu = getPaddingXgu() * 2;
		double padYgu = getPaddingYgu() * 2;
		double padXpx = getPaddingXpx() * 2;
		double padYpx = getPaddingYpx() * 2;

		sx = (metrics.viewport.data[0] - padXpx) / (metrics.size.data[0] + padXgu); // Ratio along X
		sy = (metrics.viewport.data[1] - padYpx) / (metrics.size.data[1] + padYgu); // Ratio along Y
		tx = metrics.lo.x + (metrics.size.data[0] / 2); // Center of graph in X
		ty = metrics.lo.y + (metrics.size.data[1] / 2); // Center of graph in Y

		if (sx > sy) // The least ratio.
			sx = sy;
		else
			sy = sx;

		Tx.setToIdentity();
		Tx.translate(metrics.viewport.data[0] / 2, metrics.viewport.data[1] / 2);
		if (rotation != 0)
			Tx.rotate(rotation / (180 / Math.PI));
		Tx.scale(sx, -sy);
		Tx.translate(-tx, -ty);

		xT = new AffineTransform(Tx);
		try {
			xT.invert();
		} catch (NoninvertibleTransformException e) {
			System.err.printf("cannot inverse gu2px matrix...%n");
		}

		zoom = 1;

		center.set(tx, ty, 0);
		metrics.setRatioPx2Gu(sx);
		metrics.loVisible.copy(metrics.lo);
		metrics.hiVisible.copy(metrics.hi);

		return Tx;
	}

	/**
	 * Compute a transformation that pass from graph units (user space) to a
	 * pixel units (device space) so that the view (zoom and centre) requested
	 * by the user is produced.
	 * 
	 * @param g2
	 *            The Swing graphics.
	 * @param Tx
	 *            The transformation to modify.
	 * @return The transformation modified.
	 */
	protected AffineTransform userView(Graphics2D g2, AffineTransform Tx) {
		double sx, sy;
		double tx, ty;
		double padXgu = getPaddingXgu() * 2;
		double padYgu = getPaddingYgu() * 2;
		double padXpx = getPaddingXpx() * 2;
		double padYpx = getPaddingYpx() * 2;
		double gw = gviewport != null ? gviewport[2] - gviewport[0]
				: metrics.size.data[0];
		double gh = gviewport != null ? gviewport[3] - gviewport[1]
				: metrics.size.data[1];

		sx = (metrics.viewport.data[0] - padXpx) / ((gw + padXgu) * zoom);
		sy = (metrics.viewport.data[1] - padYpx) / ((gh + padYgu) * zoom);
		tx = center.x;
		ty = center.y;

		if (sx > sy) // The least ratio.
			sx = sy;
		else
			sy = sx;

		Tx.setToIdentity();
		Tx.translate(metrics.viewport.data[0] / 2, metrics.viewport.data[1] / 2); 
		if (rotation != 0)
			Tx.rotate(rotation / (180 / Math.PI));
		Tx.scale(sx, -sy);
		Tx.translate(-tx, -ty);

		xT = new AffineTransform(Tx);
		try {
			xT.invert();
		} catch (NoninvertibleTransformException e) {
			System.err.printf("cannot inverse gu2px matrix...%n");
		}

		metrics.setRatioPx2Gu(sx);

		double w2 = (metrics.viewport.data[0] / sx) / 2;
		double h2 = (metrics.viewport.data[1] / sx) / 2;

		metrics.loVisible.set(center.x - w2, center.y - h2);
		metrics.hiVisible.set(center.x + w2, center.y + h2);

		return Tx;
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
	}

	/**
	 * Set the zoom (or percent of the graph visible), 1 means the graph is
	 * fully visible.
	 * 
	 * @param z
	 *            The zoom.
	 */
	public void setZoom(double z) {
		zoom = z;
	}

	/**
	 * Set the rotation angle around the centre.
	 * 
	 * @param theta
	 *            The rotation angle in degrees.
	 */
	public void setViewRotation(double theta) {
		rotation = theta;
	}

	/**
	 * Set the output view port size in pixels.
	 * 
	 * @param viewportWidth
	 *            The width in pixels of the view port.
	 * @param viewportHeight
	 *            The width in pixels of the view port.
	 */
	public void setViewport(double viewportWidth, double viewportHeight) {
		metrics.setViewport(viewportWidth, viewportHeight);
	}

	/**
	 * Set the graph padding.
	 * 
	 * @param graph
	 *            The graphic graph.
	 */
	public void setPadding(GraphicGraph graph) {
		padding.copy(graph.getStyle().getPadding());
	}

	protected double getPaddingXgu() {
		if (padding.units == Style.Units.GU && padding.size() > 0)
			return padding.get(0);

		return 0;
	}

	protected double getPaddingYgu() {
		if (padding.units == Style.Units.GU && padding.size() > 1)
			return padding.get(1);

		return getPaddingXgu();
	}

	protected double getPaddingXpx() {
		if (padding.units == Style.Units.PX && padding.size() > 0)
			return padding.get(0);

		return 0;
	}

	protected double getPaddingYpx() {
		if (padding.units == Style.Units.PX && padding.size() > 1)
			return padding.get(1);

		return getPaddingXpx();
	}

	/**
	 * Check if a sprite is visible in the current view port.
	 * 
	 * @param sprite
	 *            The sprite to check.
	 * @return True if visible.
	 */
	protected boolean isSpriteVisible(GraphicSprite sprite) {
		return isSpriteVisibleIn(sprite, 0, 0, metrics.viewport.data[0],
				metrics.viewport.data[1]);
	}

	/**
	 * Check if an edge is visible in the current view port.
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
	protected boolean isNodeVisibleIn(GraphicNode node, double X1, double Y1, double X2, double Y2) {
		NodeSkeleton nskel = (NodeSkeleton) node.getSkeleton();
		
		if(nskel == null) {
			nskel = new NodeSkeleton();
			node.setSkeleton(nskel);
		}
		
		return nskel.visibleIn(this, X1, Y1, X2, Y2, Units.PX);
	}

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
	protected boolean isSpriteVisibleIn(GraphicSprite sprite, double X1, double Y1,
			double X2, double Y2) {
		if (sprite.isAttachedToNode() && nodeInvisible.contains(sprite.getNodeAttachment().getId())) {
			return false;	// To speed up things.
		} else if (sprite.isAttachedToEdge() && !isEdgeVisible(sprite.getEdgeAttachment())) {
			return false;	// To speed up things.
		} else {
			SpriteSkeleton sskel = (SpriteSkeleton) sprite.getSkeleton();
			
			if(sskel != null) {
				return sskel.visibleIn(this, X1, Y1, X2, Y2, Units.PX);
			} else {
				throw new RuntimeException("cannot compute visibility of node without skeleton");
			}
		}
	}

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
	protected boolean nodeContains(GraphicElement elt, double x, double y) {
		NodeSkeleton skel = (NodeSkeleton) elt.getSkeleton();
		
		if(skel != null) {
			return skel.contains(this, x, y, Units.PX);
		}
		
		return false;
	}
	
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
	protected boolean spriteContains(GraphicElement elt, double x, double y) {
		SpriteSkeleton skel = (SpriteSkeleton) elt.getSkeleton();
		
		if(skel != null) {
			return skel.contains(this, x, y, Units.PX);
		}
		
		return false;
	}
}