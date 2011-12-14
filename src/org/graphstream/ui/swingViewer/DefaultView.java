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

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;

import javax.swing.JFrame;

import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;

/**
 * Base for constructing views.
 * 
 * <p>
 * This base view is an abstract class that provides mechanisms that are
 * necessary in any view :
 * <ul>
 * 		<li>the painting and repainting mechanism.</li>
 * 		<li>the optional frame handling.</li>
 * 		<li>the frame closing protocol.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * This view also handle a current selection of nodes and sprites.
 * </p>
 * 
 * <h3>The painting mechanism</h3>
 * 
 * <p>
 * This mechanism pushes a repaint query each time the viewer asks us to
 * repaint. There are two repaint mechanisms. The first one is the repaints
 * triggered by the Swing event system (each time the panel is moved, resized,
 * etc.). It calls directly repaint() that will in turn call paintComponent().
 * The second is the one triggered by the viewer. The viewer will call
 * {@link #display(GraphicGraph, boolean)} (that only calls repaint()) on a
 * regular basis (by default 25 frames per second), excepted if the graph did not
 * changed, in which case no redrawing is needed, and the CPU cycles can be saved.
 * </p>
 * 
 * <p>
 * The main method to implement is {@link #render(Graphics2D)}. This method is
 * called each time the graph needs to be rendered anew in the canvas.
 * </p>
 * 
 * <p>
 * All the painting, by default, is deferred to a {@link GraphRenderer}
 * instance. This mechanism allows developers that do not want to mess with the
 * viewer/view mechanisms to render a graph in any Swing surface.
 * </p>
 * 
 * <h3>The optional frame handling</h3>
 * 
 * <p>
 * This abstract view is able to create a frame that is added around this panel
 * (each view is a JPanel instance). The frame can be removed at any time.
 * </p>
 * 
 * <h3>The frame closing protocol</h3>
 * 
 * <p>
 * This abstract view handles the closing protocol. This means that it will
 * close the view if needed, or only hide it to allow reopening it later.
 * Furthermore it adds the "ui.viewClosed" attribute to the graph when the view
 * is closed or hidden, and removes it when the view is shown. The value of this
 * graph attribute is the identifier of the view.
 * </p>
 */
public class DefaultView extends View implements WindowListener {

	private static final long serialVersionUID = - 4489484861592064398L;

	/**
	 * The graph to render, shortcut to the viewers reference.
	 */
	protected GraphicGraph graph;

	/**
	 * The (optional) frame.
	 */
	protected JFrame frame;

	/**
	 * The graph changed since the last repaint.
	 */
	protected boolean graphChanged;

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

	/**
	 * New view.
	 * @param viewer The parent viewer.
	 * @param identifier The view unique identifier.
	 * @param renderer The view renderer.
	 */
	public DefaultView(Viewer viewer, String identifier, GraphRenderer renderer) {
		super(identifier, viewer);

		this.graph = viewer.getGraphicGraph();
		this.renderer = renderer;

		setOpaque(true);
		setDoubleBuffered(true);
		setShortcutManager(new DefaultShortcutManager(this));
		setMouseManager(new DefaultMouseManager(this.graph, this));
		renderer.open(graph, this);
	}

	@Override
	public Camera getCamera() {
		return renderer.getCamera();
	}
	
	@Override
	public void display(GraphicGraph graph, boolean graphChanged) {
		repaint();
	}

	@Override
	public void paintComponent(Graphics g) {
		// super.paintComponent(g);	// We do not want any background. We handle it.
		checkTitle();
		Graphics2D g2 = (Graphics2D) g;
		render(g2);
		((BaseCamera)renderer.getCamera()).resetCameraChangedFlag();
	}

	protected void checkTitle() {
		if (frame != null) {
			String titleAttr = String.format("ui.%s.title", getId());
			String title = (String) graph.getLabel(titleAttr);

			if (title == null)
				title = (String) graph.getLabel("ui.default.title");

			if (title == null)
				title = (String) graph.getLabel("ui.title");
			
			if (title != null)
				frame.setTitle(title);
		}
	}

	@Override
	public void close(GraphicGraph graph) {
		renderer.close();
		graph.addAttribute("ui.viewClosed", getId());
		removeKeyListener(shortcuts);
		removeMouseListener(mouseClicks);
		removeMouseMotionListener(mouseClicks);
		openInAFrame(false);
	}
	
	@Override
	public void resizeFrame(int width, int height) {
		if(frame != null) {
			frame.setSize(width, height);
		}
	}

	@Override
	public void openInAFrame(boolean on) {
		if (on) {
			if (frame == null) {
				frame = new JFrame("GraphStream");
				frame.setLayout(new BorderLayout());
				frame.add(this, BorderLayout.CENTER);
				frame.setSize(800, 600);
				frame.setVisible(true);
				frame.addWindowListener(this);
				frame.addKeyListener(shortcuts);
			} else {
				frame.setVisible(true);
			}
		} else {
			if (frame != null) {
				frame.removeWindowListener(this);
				frame.removeKeyListener(shortcuts);
				frame.remove(this);
				frame.setVisible(false);
				frame.dispose();
			}
		}
	}

	public void render(Graphics2D g) {
		setBackground(graph.getStyle().getFillColor(0));
		renderer.render(g, getX(), getY(), getWidth(), getHeight());

		String screenshot = (String) graph.getLabel("ui.screenshot");

		if (screenshot != null) {
			graph.removeAttribute("ui.screenshot");
			renderer.screenshot(screenshot, getWidth(), getHeight());
		}
	}

	// Selection

	@Override
	public void beginSelectionAt(double x1, double y1) {
		renderer.beginSelectionAt(x1, y1);
		repaint();
	}

	@Override
	public void selectionGrowsAt(double x, double y) {
		renderer.selectionGrowsAt(x, y);
		repaint();
	}

	@Override
	public void endSelectionAt(double x2, double y2) {
		renderer.endSelectionAt(x2, y2);
		repaint();
	}

	// Window Listener

	public void windowActivated(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		graph.addAttribute("ui.viewClosed", getId());

		switch (viewer.getCloseFramePolicy()) {
		case CLOSE_VIEWER:
			viewer.removeView(getId());
			break;
		case HIDE_ONLY:
			if (frame != null)
				frame.setVisible(false);
			break;
		case EXIT:
			System.exit(0);
		default:
			throw new RuntimeException(
					String.format(
							"The %s view is not up to date, do not know %s CloseFramePolicy.",
							getClass().getName(), viewer.getCloseFramePolicy()));
		}
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
		graph.removeAttribute("ui.viewClosed");
	}

	// Methods deferred to the renderer

	@Override
	public ArrayList<GraphicElement> allNodesOrSpritesIn(double x1, double y1,
			double x2, double y2) {
		return renderer.getCamera().allNodesOrSpritesIn(x1, y1, x2, y2);
	}

	@Override
	public GraphicElement findNodeOrSpriteAt(double x, double y) {
		return renderer.getCamera().findNodeOrSpriteAt(x, y);
	}

	@Override
	public void moveElementAtPx(GraphicElement element, double x, double y) {
		renderer.moveElementAtPx(element, x, y);
	}

	@Override
	public void setBackLayerRenderer(LayerRenderer renderer) {
		this.renderer.setBackLayerRenderer(renderer);
		repaint();
	}

	@Override
	public void setForeLayoutRenderer(LayerRenderer renderer) {
		this.renderer.setForeLayoutRenderer(renderer);
		repaint();
	}
	
	@Override
	public MouseManager getMouseManager() {
		return mouseClicks;
	}
	
	@Override
	public void setMouseManager(MouseManager mouseManager) {
		if(mouseClicks != null) {
			removeMouseListener(mouseClicks);
			removeMouseMotionListener(mouseClicks);
			removeMouseWheelListener(mouseClicks);
		}
		
		mouseClicks = mouseManager;
		
		if(mouseClicks != null) {
			addMouseListener(mouseClicks);
			addMouseMotionListener(mouseClicks);
			addMouseWheelListener(mouseClicks);
		}
	}
	
	@Override
	public ShortcutManager getShortcutManager() {
		return shortcuts;
	}
	
	@Override
	public void setShortcutManager(ShortcutManager shortcutManager) {
		if(shortcuts != null) {
			removeKeyListener(shortcuts);
		}
		
		shortcuts = shortcutManager;
		
		if(shortcuts != null) {
			addKeyListener(shortcuts);
		}
	}
}