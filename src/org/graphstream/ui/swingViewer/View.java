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

import java.awt.Component;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.swingViewer.util.MouseManager;
import org.graphstream.ui.swingViewer.util.ShortcutManager;

/**
 * A view on a graphic graph.
 * 
 * <p>
 * A view is rendering surface where a {@link GraphRenderer} draws the graphic
 * graph of the {@link Viewer}. Basically it is a an AWT container (indeed a
 * {@link JPanel}). So you can put a view in your own interfaces. However note
 * that the rendering will not necessarily occur directly in this JPanel, but
 * can occur in a sub-component. To obtain the real rendering surface, use
 * {@link #getComponent()}.
 * </p>
 * 
 * <h2>Threads</h2>
 * 
 * <p>
 * As for the {@link Viewer}, the views ALWAYS run in the Swing thread. Some parts
 * of the view interface are made to be used from this thread only, other parts are protected
 * from concurrent accesses. This is documented individually in each method.
 * </p>
 * 
 * <p>
 * You will probably have few occasions to use views in another thread than the Swing
 * one, excepted when creating them. The view provides control on the way the graph is
 * seen using a {@link Camera} object. This object must be used ONLY in the Swing thread,
 * If you are in another thread, use the {@link CameraManager} class to control the view.
 * </p>
 * 
 * <h2>View control</h2>
 * 
 * <p>
 * You can setup a {@link MouseManager} and a {@link ShortcutManager} to handle user interaction
 * with the view. By default the {@link DefaultMouseManager} and the {@link DefaultShortcutManager}
 * classes are used. Theses classes are only a mouse listener and a keyboard listener, and they
 * modify the view or camera according to the user interactions.
 * </p>
 * 
 * <p>
 * You can control a "selection area" that is usually drawn with the mouse in the view. This
 * selection will put a "ui.selected" attribute on each node that lies within the selection
 * area bounds. It is rare to have to use these methods excepted when creating a
 * {@link MouseManager}.
 * </p>
 * 
 * <p>
 * In addition to this, you can put a {@link LayerRenderer} either under the graph rendering or
 * above it. A layer renderer is an object that is called either before or after rendering by the
 * view and is passed a {@link Graphics2D} instance to draw inside the view. You can with this
 * draw whatever you want in addition to the graph. One common use of the back layer renderer is
 * to put another viewer in it to render two graphs one above the other.
 * </p>
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
	protected String id;

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
	 * careful, this camera object can be used only in the Swing thread. If you want to control
	 * the camera from another thread use the {@link CameraManager} class.
	 */
	public abstract Camera getCamera();

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
	 * Open this view. This must be the first operation on the view.
	 * @param identifier The identifier of the view.
	 * @param viewer The parent viewer, containing the view.
	 * @param renderer The graph renderer to use in the view.
	 */
	public void open(String identifier, Viewer viewer, GraphRenderer renderer) {
		this.id = identifier;
		this.viewer = viewer;
	}

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
	 * the size of the frame containing it.  This must be called in the Swing thread.
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
	 * True if a selection was begun and not yet ended.
	 *
	 * @return True if a selection is actually drawn.
	 */
	public abstract boolean hasSelection();
	
	/**
	 * Freeze an element so that the optional layout cannot move it.
	 * 
	 * @param element
	 * 			The element.
	 * @param frozen
	 * 			If true the element cannot be moved automatically.
	 */
	public abstract void freezeElement(GraphicElement element, boolean frozen);
	
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
	 * Change the mouse manager. This method works from any thread.
	 * 
	 * @param mouseManager The new mouse manager.
	 */
	public abstract void setMouseManager(MouseManager mouseManager);
	
	/**
	 * Change the shortcut and keyboard manager. This method works from any thread.
	 * @param shortcutManager The new shortcut manager.
	 */
	public abstract void setShortcutManager(ShortcutManager shortcutManager);
	
	/**
	 * The component where the rendering really occurs. This must be used instead
	 * of the view itself when you need the surface where the rendering really occurs,
	 * since the view may contain sub-components.
	 * @return The rendering surface component.
	 */
	public abstract Component getComponent();
}