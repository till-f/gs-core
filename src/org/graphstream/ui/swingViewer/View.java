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

import javax.swing.JPanel;

import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;

/**
 * A view on a graphic graph.
 * 
 * <p>
 * A view is renderering surface where a {@link GraphRenderer} draws the graphic
 * graph of the {@link Viewer}. Basically it is a an AWT container (indeed a
 * {@link JPanel}). 
 * </p>
 * 
 * <h2>Threads</h2>
 * 
 * <p>
 * As for the {@link Viewer}, the views ALWAYS run in the Swing thread. Some parts
 * of the view interface are made to be used from this thread only, other parts are protected
 * from concurrent accesses.
 * </p>
 * 
 */
public abstract class View extends JPanel {
	private static final long serialVersionUID = 4372240131578395549L;
	/**
	 * Parent viewer.
	 */
	protected Viewer viewer;

	/**
	 * The view identifier.
	 */
	private String id;

	/**
	 * New view.
	 * 
	 * @param identifier
	 *            The view unique identifier.
	 */
	public View(String identifier, Viewer viewer) {
		this.id = identifier;
		this.viewer = viewer;
	}

	/**
	 * The view unique identifier.
	 */
	public String getId() {
		return id;
	}
	
	/**
	 * The parent viewer.
	 */
	public Viewer getViewer() {
		return viewer;
	}

	/**
	 * Get a camera object to provide control on which part of the graph appears in the view. Be
	 * careful, this camera object can be used only in the Swing thread.
	 */
	public abstract Camera getCamera();

	/**
	 * Search for the first node or sprite (in that order) that contains the
	 * point at coordinates (x, y). This method works only in the Swing thread.
	 * 
	 * @param x
	 *            The point abscissa.
	 * @param y
	 *            The point ordinate.
	 * @return The first node or sprite at the given coordinates or null if
	 *         nothing found.
	 */
	public abstract GraphicElement findNodeOrSpriteAt(double x, double y);

	/**
	 * Search for all the nodes and sprites contained inside the rectangle
	 * (x1,y1)-(x2,y2). This method works only in the Swing thread.
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
	 * Redisplay or update the view contents. You should not need to call this
	 * method yourself, it is called automatically by the viewer.
	 * 
	 * @param graph
	 *            The graphic graph to represent.
	 * @param graphChanged
	 *            True if the graph changed since the last call to this method.
	 */
	protected abstract void display(GraphicGraph graph, boolean graphChanged);

	/**
	 * Close definitively this view. You should not need to call this method yourself,
	 * it is called by the viewer when the view is removed from it.
	 * 
	 * @param graph
	 *            The graphic graph.
	 */
	protected abstract void close(GraphicGraph graph);

	/**
	 * Flag to tell to open this view JPanel in a frame. The argument allows to put the panel in
	 * a new frame or to remove it from the frame (if it already exists). This method is called
	 * by the Viewer.
	 * 
	 * @param on
	 *            Add the panel in its own frame or remove it if it already was
	 *            in its own frame.
	 */
	protected abstract void openInAFrame(boolean on);

	/**
	 * Set the size of the view frame, if any. If this view has been open in a frame, this changes
	 * the size of the frame containing it. 
	 * 
	 * @param width The new width.
	 * @param height The new height.
	 */
	protected abstract void resizeFrame(int width, int height);

	/**
	 * Called by the mouse manager to specify where a node and sprite selection
	 * started. This method works only in the Swing thread.
	 * 
	 * @param x1
	 *            The selection start abscissa.
	 * @param y1
	 *            The selection start ordinate.
	 */
	public abstract void beginSelectionAt(double x1, double y1);

	/**
	 * The selection already started grows toward position (x, y). This method works only in
	 * the Swing thread.
	 * 
	 * @param x
	 *            The new end selection abscissa.
	 * @param y
	 *            The new end selection ordinate.
	 */
	public abstract void selectionGrowsAt(double x, double y);

	/**
	 * Called by the mouse manager to specify where a node and spite selection
	 * stopped. This method works only in the Swing thread.
	 * 
	 * @param x2
	 *            The selection stop abscissa.
	 * @param y2
	 *            The selection stop ordinate.
	 */
	public abstract void endSelectionAt(double x2, double y2);

	/**
	 * Force an element to move at the given location in pixels. This method works only in the
	 * Swing thread.
	 * 
	 * @param element
	 *            The element.
	 * @param x
	 *            The requested position abscissa in pixels.
	 * @param y
	 *            The requested position ordinate in pixels.
	 */
	public abstract void moveElementAtPx(GraphicElement element, double x,
			double y);

	/**
	 * Set a layer renderer that will be called each time the graph needs to be
	 * redrawn before the graph is rendered. Pass "null" to remove the layer
	 * renderer. This method works from any thread.
	 * 
	 * @param renderer
	 *            The renderer (or null to remove it).
	 */
	public abstract void setBackLayerRenderer(LayerRenderer renderer);

	/**
	 * Set a layer renderer that will be called each time the graph needs to be
	 * redrawn after the graph is rendered. Pass "null" to remove the layer
	 * renderer. This method works from any thread.
	 * 
	 * @param renderer
	 *            The renderer (or null to remove it).
	 */
	public abstract void setForeLayoutRenderer(LayerRenderer renderer);
	
	/**
	 * The actual manager for mouse event. The mouse manager calls the view
	 * method and tells what to do when the user interacts with the view using
	 * the mouse. The returned object can only be used in the Swing thread.
	 * 
	 * @return The actual mouse manager.
	 */
	public abstract MouseManager getMouseManager();
	
	/**
	 * Change the mouse manager. This method works from any thread.
	 * 
	 * @param mouseManager The new mouse manager.
	 */
	public abstract void setMouseManager(MouseManager mouseManager);
	
	/**
	 * The actual shortcut and keyboard manager. The shortcut manager calls the
	 * view methods and tells what to do when the user interacts with the view
	 * using the keyboard. The returned object can only be used in the Swing thread.
	 * 
	 * @return The actual shortcut manager.
	 */
	public abstract ShortcutManager getShortcutManager();
	
	/**
	 * Change the shortcut and keyboard manager. This method works from any thread.
	 * @param shortcutManager The new shortcut manager.
	 */
	public abstract void setShortcutManager(ShortcutManager shortcutManager);
}