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

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;

import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;

public class DefaultMouseManager implements MouseManager {
	/**
	 * The view this manager operates upon.
	 */
	protected View view;

	/**
	 * The graph to modify according to the view actions.
	 */
	protected GraphicGraph graph;

	/**
	 * Size of a zoom step.
	 */
	protected double zoomStep = 0.01;
	
	/**
	 * New mouse manager on the given view.
	 * 
	 * @param graph
	 *            The graph to control.
	 * @param view
	 *            The view to control.
	 */
	public DefaultMouseManager(GraphicGraph graph, View view) {
		this.view = view;
		this.graph = graph;
	}

	// Command

	protected void mouseButtonPress(MouseEvent event) {
		if(event.getClickCount() == 2) {
			zoom(-10);
		}
	}
	
	protected void mouseButtonPressInSelection(MouseEvent event) {
		view.requestFocus();

		if (!event.isShiftDown()) {
			unselectAll();
		}
	}

	protected void mouseButtonReleaseInSelection(MouseEvent event,
			ArrayList<GraphicElement> elementsInArea) {
		for (GraphicElement element : elementsInArea) {
			if (!element.hasAttribute("ui.selected")) {
				element.addAttribute("ui.selected");
				for(ViewerListener listener: view.getViewer().listeners) {
					listener.elementSelected(view, element);
				}
			}
		}
	}

	protected void mouseButtonPressOnElement(GraphicElement element,
			MouseEvent event) {
		if (event.isShiftDown()) {
			if(element.hasAttribute("ui.selected")) {
				element.removeAttribute("ui.selected");
				for(ViewerListener listener: view.getViewer().listeners) {
					listener.elementUnselected(view, element);
				}
			} else {
				element.addAttribute("ui.selected");
				for(ViewerListener listener: view.getViewer().listeners) {
					listener.elementSelected(view, element);
				}
			}
		} else {
			element.addAttribute("ui.clicked");
			for(ViewerListener listener: view.getViewer().listeners) {
				listener.elementClicked(view, element, event.getButton());
			}
		}
	}

	protected void elementMoving(GraphicElement element, MouseEvent event) {
		view.moveElementAtPx(element, event.getX(), event.getY());
	}

	protected void mouseButtonReleaseOffElement(GraphicElement element,
			MouseEvent event) {
		if (! event.isShiftDown()) {
			element.removeAttribute("ui.clicked");
			for(ViewerListener listener: view.getViewer().listeners) {
				listener.elementReleased(view, element, event.getButton());
			}
		}
	}

	protected void wheelRotated(int r) {
		zoom(r);
	}
	
	protected void mouseDragBegin(MouseEvent event, double x1, double y1) {
		unselectAll();
	}
	
	protected void mouseDrag(MouseEvent event, double x1, double y1, double x2, double y2) {
		Camera camera = view.getCamera();
		Point3 p = camera.getViewCenter();
		double dx = camera.getMetrics().lengthToGu(x1-x2, Units.PX);
		double dy = camera.getMetrics().lengthToGu(y2-y1, Units.PX);
		
		camera.setViewCenter(p.x + dx, p.y + dy, p.z);
	}
	
	protected void mouseDragEnd(MouseEvent event, double x1, double y1, double x2, double y2) {
		Camera camera = view.getCamera();
		Point3 p = camera.getViewCenter();
		double dx = camera.getMetrics().lengthToGu(x1-x2, Units.PX);
		double dy = camera.getMetrics().lengthToGu(y2-y1, Units.PX);
	
		camera.setViewCenter(p.x + dx, p.y + dy, p.z);		
	}
	
	// Mouse Listener

	protected GraphicElement curElement;

	protected double x1, y1;

	public void mouseClicked(MouseEvent event) {
		GraphicElement e = view.findNodeOrSpriteAt(event.getX(), event.getY());
		
		if(e == null) {
			mouseButtonPress(event);
		}
	}

	public void mousePressed(MouseEvent event) {
		curElement = view.findNodeOrSpriteAt(event.getX(), event.getY());

		if (curElement != null) {
			mouseButtonPressOnElement(curElement, event);
		} else {
			x1 = event.getX();
			y1 = event.getY();
			if(event.isShiftDown()) {
				mouseButtonPressInSelection(event);
				view.beginSelectionAt(x1, y1);
			} else {
				mouseDragBegin(event, x1, y1);
			}
		}
	}

	public void mouseDragged(MouseEvent event) {
		if (curElement != null) {
			elementMoving(curElement, event);
		} else {
			if(event.isShiftDown()) {
				view.selectionGrowsAt(event.getX(), event.getY());
			} else {
				double x2 = event.getX();
				double y2 = event.getY();
				mouseDrag(event, x1, y1, x2, y2);
				x1 = x2;
				y1 = y2;
			}
		}
	}

	public void mouseReleased(MouseEvent event) {
		if (curElement != null) {
			mouseButtonReleaseOffElement(curElement, event);
			curElement = null;
		} else {
			double x2 = event.getX();
			double y2 = event.getY();
			if(event.isShiftDown()) {
				double t;

				if (x1 > x2) {
					t = x1;
					x1 = x2;
					x2 = t;
				}
				if (y1 > y2) {
					t = y1;
					y1 = y2;
					y2 = t;
				}
				
				mouseButtonReleaseInSelection(event, view.allNodesOrSpritesIn(x1, y1, x2, y2));
				view.endSelectionAt(x2, y2);
			} else {
				mouseDragEnd(event, x1, y1, x2, y2);
			}
		}
	}

	public void mouseEntered(MouseEvent event) {
		// NOP
	}

	public void mouseExited(MouseEvent event) {
		// NOP
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		wheelRotated(e.getWheelRotation());
	}
	
	protected void unselectAll() {
		for (Node node : graph) {
			if (node.hasAttribute("ui.selected")) {
				node.removeAttribute("ui.selected");

				for(ViewerListener listener: view.getViewer().listeners) {
					listener.elementUnselected(view, node);
				}
			}
		}

		for (GraphicSprite sprite : graph.spriteSet()) {
			if (sprite.hasAttribute("ui.selected")) {
				sprite.removeAttribute("ui.selected");

				for(ViewerListener listener: view.getViewer().listeners) {
					listener.elementUnselected(view, sprite);
				}
			}
		}
	}
	
	protected void zoom(int of) {
		Camera camera = view.getCamera();
		double p = camera.getViewPercent();
		p += of * zoomStep;
	
		if(p > 0) {
			camera.setViewPercent(p+(of*zoomStep));
		}
	}
}