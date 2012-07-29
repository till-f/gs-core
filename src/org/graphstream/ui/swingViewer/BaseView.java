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

import org.graphstream.stream.AttributeSink;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.swingViewer.util.DefaultMouseManager;
import org.graphstream.ui.swingViewer.util.DefaultShortcutManager;
import org.graphstream.ui.swingViewer.util.MouseManager;
import org.graphstream.ui.swingViewer.util.ShortcutManager;

/**
 * A basic implementation of a view that is independent of the GUI toolkit.
 */
public abstract class BaseView implements View, AttributeSink {
	/**
	 * Parent viewer.
	 */
	protected Viewer viewer;

	/**
	 * The view identifier.
	 */
	protected String id;

	/**
	 * The graph to render, shortcut to the viewers reference.
	 */
	protected GraphicGraph graph;

	/**
	 * Manager for events with the keyboard.
	 */
	protected ShortcutManager shortcuts;

	/**
	 * Manager for events with the mouse.
	 */
	protected MouseManager mouseClicks;

	/**
	 * The graph renderer.
	 */
	protected GraphRenderer renderer;
	
	public void open(String identifier, Viewer viewer, GraphRenderer renderer) {
		this.id = identifier;
		this.viewer = viewer;
		this.graph = viewer.getGraphicGraph();
		this.renderer = renderer;

		setMouseManager(null);
		setShortcutManager(null);
		renderer.open(graph, getAWTComponent());
		checkInitialAttributes();
		graph.addAttributeSink(this);
	}

	public String getId() {
		return id;
	}
	
	public Viewer getViewer() {
		return viewer;
	}

	public Camera getCamera() {
		return renderer.getCamera();
	}

	public abstract void display(GraphicGraph graph, boolean graphChanged);
	
	public void close(GraphicGraph graph) {
		renderer.close();
		graph.removeAttributeSink(this);
		graph.addAttribute("ui.viewClosed", getId());
		
		if(shortcuts != null) {
			if(isAWT())
				shortcuts.removedFromAWTComponent(getAWTComponent());
			shortcuts.release();
		}
		
		if(mouseClicks != null) {
			mouseClicks.release();
		}
		
		openInAFrame(false);
	}

	public abstract void resizeFrame(int width, int height);

	public abstract void openInAFrame(boolean on);

	// Selection

	public void beginSelectionAt(double x1, double y1) {
		renderer.beginSelectionAt(x1, y1);
	}

	public void selectionGrowsAt(double x, double y) {
		renderer.selectionGrowsAt(x, y);
	}

	public void endSelectionAt(double x2, double y2) {
		renderer.endSelectionAt(x2, y2);
	}
	
	public boolean hasSelection() {
		return renderer.hasSelection();
	}

	// Methods deferred to the renderer

	public void moveElementAtPx(GraphicElement element, double x, double y) {
		// The feedback on the node positions is often off since not needed
		// and generating lots of events. We activate it here since the
		// movement of the node is decided by the viewer. This is one of the
		// only moment when the viewer really moves a node.
		boolean on = graph.feedbackXYZ();
		graph.feedbackXYZ(true);
		renderer.moveElementAtPx(element, x, y);
		graph.feedbackXYZ(on);
	}
	
	public void freezeElement(GraphicElement element, boolean frozen) {
		if(frozen) {
			element.addAttribute("layout.frozen");
		} else {
			element.removeAttribute("layout.frozen");
		}
	}

	public void setBackLayerRenderer(LayerRenderer renderer) {
		synchronized(viewer) {
			this.renderer.setBackLayerRenderer(renderer);
		}
	}

	public void setForeLayoutRenderer(LayerRenderer renderer) {
		synchronized(viewer) {
			this.renderer.setForeLayerRenderer(renderer);
		}
	}
	
	public void setMouseManager(MouseManager manager) {
		if(mouseClicks != null)
			mouseClicks.release();
		
		if(manager == null)
			manager = new DefaultMouseManager();

		manager.init(graph, this);
		
		mouseClicks = manager;
	}

	public void setShortcutManager(ShortcutManager manager) {
		if(shortcuts != null)
			shortcuts.release();
		
		if(manager == null)
			manager = new DefaultShortcutManager();
		
		manager.init(graph, this);
		
		shortcuts = manager;
	}
	
	public abstract boolean isAWT();

	public abstract Component getAWTComponent();
	
	public abstract Object getGUIComponent();
	
	public abstract void setFrameTitle(String title);
	
// AttributeSink

	public void graphAttributeAdded(String sourceId, long timeId, String attribute, Object value) {
		handleAttributes(attribute, value);
	}

	public void graphAttributeChanged(String sourceId, long timeId, String attribute, Object oldValue, Object newValue) {
		handleAttributes(attribute, newValue);
	}

	public void graphAttributeRemoved(String sourceId, long timeId, String attribute) {
		handleAttributes(attribute, null);
	}

	public void nodeAttributeAdded(String sourceId, long timeId, String nodeId, String attribute, Object value) { } 
	public void nodeAttributeChanged(String sourceId, long timeId, String nodeId, String attribute, Object oldValue, Object newValue) { } 
	public void nodeAttributeRemoved(String sourceId, long timeId, String nodeId, String attribute) { } 
	public void edgeAttributeAdded(String sourceId, long timeId, String edgeId, String attribute, Object value) { } 
	public void edgeAttributeChanged(String sourceId, long timeId, String edgeId, String attribute, Object oldValue, Object newValue) { } 
	public void edgeAttributeRemoved(String sourceId, long timeId, String edgeId, String attribute) { }
	
	/**
	 * Decode the "ui.camera." and "ui.title." attributes and modify the view and camera 
	 * settings accordingly.
	 * 
	 * <p>
	 * This method understands the attributes:
	 * <ul>
	 * 		<li>"ui.VIEWID.camera.center", with VIEWID the identifier of this view,
	 *          whose value must be either a Point3 or an array of three {@link Number}s.</li>
	 *      <li>The "ui.VIEWID.camera.zoom" attribute which tells the view percent,
	 *          and whose value must be a {@link Number}.</li>
	 *      <li>The "ui.VIEWID.camera.angle" attribute which must be an angle in degreen
	 *          with a {@link Number} value.</li>
	 *      <li>The "ui.VIEWID.title", or "ui.title" attributes whose value must be a
	 *          string and that change the eventual view frame title.</li>
	 * </p>
	 * 
	 * <p>
	 * It also understands the attributes
	 * </p>
	 *
	 * @param attribute The attribute to decode.
	 * @param value The eventual value of the attribute, pass null to mean "attribute removed".
	 */
	protected void handleAttributes(String attribute, Object value) {
		if(attribute.startsWith("ui.")) {
			attribute = attribute.substring(3);
			int pos = attribute.indexOf('.');
			if(pos >= 0) {
				String viewId = attribute.substring(0, pos);
				if(viewId.equals(getId())) {
					attribute = attribute.substring(pos+1);
					handleAttributeValue(attribute, value);
				} else {
					System.err.printf("cannot handle attribute ui.%s, view %s is unknown%n", attribute, viewId);
				}
			} else if(attribute.equals("ui.title")) {
				// The general ui.title the same for all frames.
				handleAttributeValue("title", value);
			}
		}
	}
	
	/**
	 * Apply the attribute action on the view or camera. The actions can be "camera.center",
	 * "camera.zoom" and "camera.angle" and "title".
	 * @param attribute The attribute action.
	 * @param value The value associated with the action.
	 * @see #handleAttributes(String, Object)
	 */
	protected void handleAttributeValue(String attribute, Object value) {
		if(value != null) {
			if(attribute.equals("camera.center")) {
				Point3 center = new Point3();
				if(value instanceof Point3) {
					center.copy((Point3)value);
				} else if(value instanceof Object[]) {
					Object[] tab = (Object[]) value;
					if(tab.length > 2 && tab[0] instanceof Number && tab[1] instanceof Number && tab[2] instanceof Number)
						center.set(((Number)tab[0]).doubleValue(), ((Number)tab[1]).doubleValue(), ((Number)tab[2]).doubleValue());
					else if(tab.length > 2 && tab[0] instanceof Number && tab[1] instanceof Number)
						center.set(((Number)tab[0]).doubleValue(), ((Number)tab[1]).doubleValue(), 0);
					else center.copy(getCamera().getViewCenter());
				} else {
					center.copy(getCamera().getViewCenter());
				}
				getCamera().setViewCenter(center.x, center.y, center.z);
			} else if(attribute.equals("camera.zoom")) {
				if(value instanceof Number) {
					double zoom = ((Number)value).doubleValue();
					getCamera().setViewPercent(zoom);
				}
			} else if(attribute.equals("camera.angle")) {
				if(value instanceof Number) {
					double angle = ((Number)value).doubleValue();
					getCamera().setViewRotation(angle);
				}				
			} else if(attribute.equals("title")) {
				setFrameTitle((String)value);
			}
		} else {
			if(attribute.equals("camera.center")) {
				getCamera().setAutoFitView(true);
			} else if(attribute.equals("camera.zoom")) {
				getCamera().setViewPercent(1);
			} else if(attribute.equals("camera.angle")) {
				getCamera().setViewRotation(0);
			}			
		}
	}
	
	/**
	 * Check some known graph attributes to configure the view and camera. 
	 */
	protected void checkInitialAttributes() {
		String basis  = String.format("ui.%s.", getId());
		String title  = String.format("%s.title", basis);
		String zoom   = String.format("%s.camera.zoom", basis);
		String angle  = String.format("%s.camera.angle", basis);
		String center = String.format("%s.camera.center", basis);
		
		if(graph.hasLabel("ui.title")) {
			handleAttributeValue("title", graph.getLabel("ui.title"));
		} else if(graph.hasLabel(title)) {
			handleAttributeValue("title", graph.getLabel("title"));
		} else if(graph.hasAttribute(zoom)) {
			handleAttributeValue("camera.zoom", graph.getAttribute(zoom));
		} else if(graph.hasAttribute(angle)) {
			handleAttributeValue("camera.angle", graph.getAttribute(angle));
		} else if(graph.hasAttribute(center)) {
			handleAttributeValue("camera.center", graph.getAttribute(center));
		}
	}
}