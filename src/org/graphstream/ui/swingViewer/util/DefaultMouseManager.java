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

package org.graphstream.ui.swingViewer.util;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import javax.swing.event.MouseInputListener;

import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.Camera;
import org.graphstream.ui.swingViewer.View;
import org.graphstream.ui.swingViewer.ViewerListener;

public class DefaultMouseManager implements MouseManager, MouseInputListener, MouseWheelListener {
	// Attribute

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

	// Construction

	public void init(GraphicGraph graph, View view) {
		this.view = view;
		this.graph = graph;
		view.addMouseListener(this);
		view.addMouseMotionListener(this);
		view.addMouseWheelListener(this);
	}
	
	public void release() {
		view.removeMouseListener(this);
		view.removeMouseMotionListener(this);
		view.removeMouseWheelListener(this);
	}

	// Command

	protected void mouseButtonPress(MouseEvent event) {
		if(event.getClickCount() == 2) {
			if(event.getButton() == 1) {
				zoom(-10);
			} else if(event.getButton() == 3) {
				zoom(10);
			} else System.err.printf("BUtton %d%n", event.getButton());
		}
	}
	
	protected void mouseButtonPressInSelection(MouseEvent event) {
		if(!event.isShiftDown()) {
			unselectAll();
		}
	}
	
	protected void mouseButtonReleaseInSelection(MouseEvent event, ArrayList<GraphicElement> elementsInArea) {
		for(GraphicElement element: elementsInArea) {
			if(!element.hasAttribute("ui.selected")) {
				element.addAttribute("ui.selected");
				for(ViewerListener listener: view.getViewer().getEachListener()) {
					listener.elementSelected(view, element);
				}
			}
		}
	}

	protected void mouseButtonPressOnElement(GraphicElement element,
			MouseEvent event) {
		if(event.isShiftDown()) {
			if(element.hasAttribute("ui.selected")) {
				element.removeAttribute("ui.selected");
				for(ViewerListener listener: view.getViewer().getEachListener()) {
					listener.elementUnselected(view, element);
				}
			} else {
				element.addAttribute("ui.selected");
				for(ViewerListener listener: view.getViewer().getEachListener()) {
					listener.elementSelected(view, element);
				}
			}
		} else {
			view.freezeElement(element, true);
			
			element.addAttribute("ui.clicked");
			for(ViewerListener listener: view.getViewer().getEachListener()) {
				listener.elementClicked(view, element, event.getButton());
			}
		}
		
		if (event.getButton() == 3) {
			element.addAttribute("ui.selected");
		} else {
			element.addAttribute("ui.clicked");
		}
	}

	protected void mouseButtonReleaseOfElement(GraphicElement element, MouseEvent event) {
		if(event.isShiftDown()) {
			view.freezeElement(element, false);
			element.removeAttribute("ui.clicked");
			for(ViewerListener listener: view.getViewer().getEachListener()) {
				listener.elementReleased(view, element, event.getButton());
			}
		} 
	}

	protected void elementMoving(GraphicElement element, MouseEvent event) {
		view.moveElementAtPx(element, event.getX(), event.getY());
	}
	
	protected void mouseWheelRotated(int r) {
		zoom(r);
	}
	
	protected void mouseDragBegin(MouseEvent event, double x1, double y1) {
		unselectAll();
	}
	
	protected void mouseDrag(MouseEvent event, double x1, double y1, double x2, double y2) {
		Camera camera = view.getCamera();
		GraphMetrics metrics = camera.getMetrics();
		Point3 p = camera.getViewCenter();
		double dx = metrics.lengthToGu(x1-x2, Units.PX);
		double dy = metrics.lengthToGu(y2-y1, Units.PX);
		
		camera.setViewCenter(p.x+dx, p.y+dy, p.z);
	}
	
	protected void mouseDragEnd(MouseEvent event, double x1, double y1, double x2, double y2) {
		//mouseDrag(event, x1, y1, x2, y2);// Avoid to pass the view in non auto-fit mode
		// if no drag occurred (just a click).
	}

	// Mouse Listener

	protected GraphicElement curElement;

	protected double x1, y1;

	public void mouseClicked(MouseEvent event) {
		GraphicElement e = view.getCamera().findNodeOrSpriteAt(event.getX(), event.getY());
		
		if(e == null) {
			mouseButtonPress(event);
		}
	}

	public void mousePressed(MouseEvent event) {
		view.requestFocus();
		
		curElement = view.getCamera().findNodeOrSpriteAt(event.getX(), event.getY());

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
			if(event.isShiftDown() || view.hasSelection()) {
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
			mouseButtonReleaseOfElement(curElement, event);
			curElement = null;
		} else {
			double x2 = event.getX();
			double y2 = event.getY();
			if(event.isShiftDown()||view.hasSelection()) {
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

				mouseButtonReleaseInSelection(event, view.getCamera().allNodesOrSpritesIn(x1, y1, x2, y2));
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
		// NOP
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		mouseWheelRotated(e.getWheelRotation());
	}
	
	// Utility
	
	protected void unselectAll() {
		for (Node node : graph) {
			if (node.hasAttribute("ui.selected")) {
				node.removeAttribute("ui.selected");

				for(ViewerListener listener: view.getViewer().getEachListener()) {
					listener.elementUnselected(view, node);
				}
			}
		}

		for (GraphicSprite sprite : graph.spriteSet()) {
			if (sprite.hasAttribute("ui.selected")) {
				sprite.removeAttribute("ui.selected");

				for(ViewerListener listener: view.getViewer().getEachListener()) {
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