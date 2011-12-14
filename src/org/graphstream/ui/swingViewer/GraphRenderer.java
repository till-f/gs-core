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

import java.awt.Container;
import java.awt.Graphics2D;

import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;

/**
 * Interface for classes that draw a GraphicGraph in an AWT container.
 * 
 * <p>
 * There are two rendering mechanisms in this UI package: the viewer and
 * the renderers. The viewer is a complete architecture to create a Swing container (the View)
 * and manage several of them in a "viewer" that handles the animation (calls regularly
 * the views if the graph changed). It handles all the details of opening a window if needed,
 * setting up a graphic graph, and an eventual layout thread. The renderer is in charge of the
 * drawing proper. It renders in a "surface" provided by the view. Separating the viewer and views
 * from the renderers allow to have separate parts of the API handling very different tasks : to
 * the viewer and views the role of handling the swing components, the graphic graph and all the
 * pipelining with GraphStream. To the renderer the role of drawing a graph using whichever
 * rendering technology that can draw in an AWT container.
 * </p>
 * 
 * <p>Therefore, the renderer architecture is a way to only render a graphic graph in any surface,
 * handled directly by the developer. One could also use a renderer without using the {@link Viewer}
 * or the {@link View}s. When using the renderer you must handle the graphic graph by yourself, but
 * you have a lot more flexibility.
 * </p>
 * 
 * <p>
 * A renderer becomes active only after its {@link #open(GraphicGraph, Container)} method has been
 * called. This method specifies the drawing surface it should use (an AWT container) and the
 * graphic graph is draws on this surface. Similarly, when you are done with the renderer, you call
 * the {@link #close()} method to free the surface.
 * </p>
 * 
 * <p>
 * The renderer provides a {@link Camera} object that tells how the graph is viewed. The camera
 * allows to zoom on the graph, pan the view on the graph, rotate the view. It also allows the
 * renderer to know which graph elements are visible, and which are not, allowing to fasten the
 * drawing process when zooming on graphs with a very large number of elements.
 * </p>
 */
public interface GraphRenderer {
	/**
	 * Called before any rendering operation, the renderer becomes usable only after a call to this
	 * method.
	 * @param graph The graphic graph to draw.
	 * @param drawingSurface The container that will receive the painting.
	 */
	void open(GraphicGraph graph, Container drawingSurface);

	/**
	 * Called when the renderer is about to be released, no rendering can occurs after that.
	 */
	void close();

	/**
	 * Get a camera object to provide control commands on which part of the graph is
	 * shown by the renderer and allows to know which element is visible, allows to
	 * retrieve the elements at a given position, know some metrics on the graph, etc.
	 * 
	 * @return a Camera instance
	 */
	public abstract Camera getCamera();

	// Command

	/**
	 * Redisplay or update the graph.
	 * 
	 * <p>
	 * This method is called by the views in the swing viewer or by any other
	 * mechanism. The caller must inform the viewer where in the rendering
	 * surface the renderer should draw the graph by giving an area in pixels.
	 * </p>
	 * 
	 * @param g The Java2D graphics associated with the rendering surface.
	 * @param x The abscissa in pixels of the drawing area in the rendering surface.
	 * @param y The ordinate in pixels of the drawing area in the rendering surface.
	 * @param width The width in pixels of the drawing area in the rendering surface.
	 * @param height The height in pixels of the drawing area in the rendering surface.
	 */
	public abstract void render(Graphics2D g, int x, int y, int width, int height);

	/**
	 * Called by the mouse manager to specify where a node and sprite selection
	 * started, in pixels.
	 * 
	 * @param x1 The selection start abscissa in pixels.
	 * @param y1 The selection start ordinate in pixels.
	 */
	public abstract void beginSelectionAt(double x1, double y1);

	/**
	 * The selection already started grows toward position (x, y) in pixels. You must have called
	 * {@link #beginSelectionAt(double, double)} for this method to work.
	 * 
	 * @param x The new end selection abscissa in pixels.
	 * @param y The new end selection ordinate in pixels.
	 */
	public abstract void selectionGrowsAt(double x, double y);

	/**
	 * Called by the mouse manager to specify where a node and spite selection
	 * stopped in pixels. You must have called {@link #beginSelectionAt(double, double)}
	 * for this method to work.
	 * 
	 * @param x2 The selection stop abscissa.
	 * @param y2 The selection stop ordinate.
	 */
	public abstract void endSelectionAt(double x2, double y2);

	/**
	 * Force an element to move at the given location in pixels. This is
	 * mainly used by mouse managers that move an element. To move an element is graph units,
	 * use the "xy" or "xyz" attributes, as usual.
	 * 
	 * @param element The element.
	 * @param x The requested position abscissa in pixels.
	 * @param y The requested position ordinate in pixels.
	 */
	public abstract void moveElementAtPx(GraphicElement element, double x, double y);

	/**
	 * Save a bitmap or vector image file of the current rendering.
	 * 
	 * @param filename Name of the file to save, the extension is used to define the file format,
	 *  if the format is not supported, a default format is used, and the filename is changed
	 *  accordingly.
	 * @param width The width in pixels of the resulting image.
	 * @param height The height in pixels of the resulting image.
	 */
	public abstract void screenshot(String filename, int width, int height);

	/**
	 * Set a layer renderer that will be called each time the graph needs to be
	 * redrawn before the graph is rendered. Pass "null" to remove the layer
	 * renderer.
	 * 
	 * @param renderer The renderer (or null to remove it).
	 */
	public abstract void setBackLayerRenderer(LayerRenderer renderer);

	/**
	 * Set a layer renderer that will be called each time the graph needs to be
	 * redrawn after the graph is rendered. Pass "null" to remove the layer
	 * renderer.
	 * 
	 * @param renderer The renderer (or null to remove it).
	 */
	public abstract void setForeLayoutRenderer(LayerRenderer renderer);
}