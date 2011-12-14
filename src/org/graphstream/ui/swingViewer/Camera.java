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

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.swingViewer.util.GraphMetrics;

/**
 * Define how the graph is viewed, what part is visible, etc.
 * 
 * <p>
 * The camera is in charge of projecting coordinates in graph units (GU)
 * into user spaces (often in pixels, PX). It defines the transformation
 *  to pass from the first to the second. It also contains the graph
 * metrics, a set of values that give the overall dimensions of the graph in
 * graph units, as well as the view port in pixels (the area on the screen (or any
 * rendering surface) that will receive the results in pixels), and various
 * conversion method to pass from GU to PX and the reverse. However the main
 * convertion methods are in this class: {@link #transformGuToPx(Point3)} and
 * {@link #transformPxToGu(Point3)}. 
 * </p>
 * 
 * <p>
 * The camera defines a center at which it always points ({@link #getViewCenter()}). This center is
 * at the center of the view (indeed). The camera can then zoom on the graph by specifying the
 * percent of the view visible ({@link #setViewPercent(double)}, this is very comparable to the
 * aperture, the camera is not becoming closer to the center point with the zoom, you can
 * also use the {@link #setGraphViewport(double, double, double, double)} and to remove it us
 * {@link #removeGraphViewport()}). It can also pan
 * in any direction by moving the center point ({@link #setViewCenter(double, double, double)}).
 * Finally, it can also rotate along two axes ({@link #setViewRotation(double)}) in 3D or one axe
 * in 2D.
 * </p>
 * 
 * <p>
 * The camera has two modes: The first is called "auto-fit". In this mode the camera
 * uses the graph bounds to adapt the view so that the whole graph is always visible.
 * The settings of the camera are changed automatically to do this.
 * The other mode is the user mode. It is activated as soon as the user change the view
 * zoom, moves the center, or rotate the view ({@link #setViewCenter(double, double, double)},
 * {@link #setViewPercent(double)}, {@link #setViewRotation(double)}). In this mode the settings
 * of the camera are never automatically changed. Once in user mode, you can switch back to auto-fit
 * mode with {@link #setAutoFitView(boolean)}. 
 * </p>
 * 
 * <p>
 * Knowing the transformation also allows to provide services like "what element
 * is visible ?" (in the camera view). The camera plays a great role in determining
 * which element of the view is effectively in view. This allows renderers to process
 * the element to draw faster. If the camera is in auto-fit mode, all elements are always
 * visible. Else, the camera can check witch element are visible and maintains a cache
 * of these elements until the next change in the view or in the graph. See the
 * {@link #isVisible(GraphicElement)} method.
 * </p>
 * 
 * <p>
 * The {@link #resetView()} method allows to switch back the camera in auto-fit mode and
 * additionally reset all settings to their defaults.
 * </p>
 */
public interface Camera {
	/**
	 * The view center (a point in graph units).
	 */
	Point3 getViewCenter();

	/**
	 * Change the view center.
	 * 
	 * @param x
	 *            The new abscissa.
	 * @param y
	 *            The new ordinate.
	 * @param z
	 *            The new depth.
	 */
	void setViewCenter(double x, double y, double z);

	/**
	 * The portion of the graph visible.
	 * 
	 * @return A real for which value 1 means the graph is fully visible and
	 *         uses the whole view port.
	 */
	double getViewPercent();

	/**
	 * Zoom the view.
	 * 
	 * @param percent
	 *            Percent of the graph visible.
	 */
	void setViewPercent(double percent);

	/**
	 * The current rotation angle.
	 * 
	 * @return The rotation angle in degrees.
	 */
	double getViewRotation();

	/**
	 * Rotate the view around its center point by a given theta angles (in
	 * degrees).
	 * 
	 * @param theta
	 *            The rotation angle in degrees.
	 */
	void setViewRotation(double theta);

	/**
	 * A number in GU that gives the approximate graph size (often the diagonal
	 * of the graph). This allows to compute displacements in the graph as
	 * percent of its overall size. For example this can be used to move the
	 * view center.
	 * 
	 * @return The graph estimated size in graph units.
	 */
	double getGraphDimension();

	/**
	 * Remove the specified graph view port.
	 * 
	 * @see #setGraphViewport(double, double, double, double)
	 */
	void removeGraphViewport();

	/**
	 * Specify exactly the minimum and maximum points in GU that are visible
	 * (more points may be visible due to aspect-ratio constraints).
	 * 
	 * @param minx
	 *            The minimum abscissa visible.
	 * @param miny
	 *            The minimum ordinate visible.
	 * @param maxx
	 *            The maximum abscissa visible.
	 * @param maxy
	 *            The maximum abscissa visible.
	 * @see #removeGraphViewport()
	 */
	void setGraphViewport(double minx, double miny, double maxx, double maxy);

	/**
	 * Reset the view to the automatic mode.
	 */
	void resetView();

	/**
	 * Get the {@link GraphMetrics} object linked to this Camera. It can be used
	 * to convert pixels to graphic units and vice versa, as well as knowing the
	 * size of the rendering surface in pixels, the overall size of the graph in
	 * graph units, the lowest and highest points in graph units, the center,
	 * etc. 
	 * 
	 * @return a GraphMetrics instance
	 */
	GraphMetrics getMetrics();
	
	/**
	 * Enable or disable automatic adjustment of the view to see the entire
	 * graph.
	 * 
	 * @param on
	 *            If true, automatic adjustment is enabled.
	 */
	void setAutoFitView(boolean on);

	/**
	 * Transform a point in graph units into pixels.
	 * 
	 * @param x
	 *            The source point abscissa in pixels.
	 * @param y
	 *            The source point ordinate in pixels.
	 * @param z
	 *            The source point depth in pixels.
	 * @return The transformed point.
	 */
	Point3 transformGuToPx(double x, double y, double z);
	
	/**
	 * Return the given point in pixels converted in graph units (GU) using the
	 * inverse transformation of the current projection matrix. The inverse
	 * matrix is computed only once each time a new projection matrix is
	 * created.
	 * 
	 * @param x
	 *            The source point abscissa in pixels.
	 * @param y
	 *            The source point ordinate in pixels.
	 * @return The resulting points in graph units.
	 */
	Point3 transformPxToGu(double x, double y);
	
	/**
	 * Transform a point in graph units into pixels.
	 *
	 * @param p The point to transform.
	 * @return The transformed point.
	 */
	Point3 transformGuToPx(Point3 p);
	
	/**
	 * Return the given point in pixels converted in graph units (GU) using the
	 * inverse transformation of the current projection matrix. The inverse
	 * matrix is computed only once each time a new projection matrix is
	 * created.
	 * 
	 * @param p The point to transform.
	 * @return The resulting points in graph units.
	 */
	Point3 transformPxToGu(Point3 p);
	
	/**
	 * True if the element would be visible on screen. The method used is to
	 * transform the center of the element (which is always in graph units)
	 * using the camera actual transformation to put it in pixel units. Then to
	 * look in the style sheet the size of the element and to test if its
	 * enclosing rectangle intersects the view port. For edges, its two nodes
	 * are used.
	 * 
	 * @param element
	 *            The element to test.
	 * @return True if the element is visible and therefore must be rendered.
	 */
	boolean isVisible(GraphicElement element);

	/**
	 * Search for the first node or sprite (in that order) that contains the
	 * point at coordinates (x, y).
	 * 
	 * <p>
	 * If several elements contains the point, an arbitrary one is returned.
	 * </p>
	 * 
	 * @param x
	 *            The point abscissa.
	 * @param y
	 *            The point ordinate.
	 * @return The first node or sprite containing the given coordinates or null if
	 *         nothing found.
	 */
	public abstract GraphicElement findNodeOrSpriteAt(double x, double y);

	/**
	 * Search for all the nodes and sprites contained inside the rectangle
	 * (x1,y1)-(x2,y2).
	 * 
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
	public abstract ArrayList<GraphicElement> allNodesOrSpritesIn(double x1,
			double y1, double x2, double y2);
	
	/**
	 * Tells if the camera changed since the last rendering of the graph. The resetting
	 * of this flag is up to individual camera implementations that know when a frame
	 * has been rendered.
	 * @return True if the camera settings changed since the last rendering.
	 */
	public boolean cameraChangedFlag();
}