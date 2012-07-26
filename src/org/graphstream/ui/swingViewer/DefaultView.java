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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

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
 * You are guaranteed that {@link #display(GraphicGraph, boolean)} is called only
 * by the Viewer. This allows to separate the cases where the redraw is triggered
 * by the Swing repaint mechanism, from repaints coming from the animation loop
 * of the Viewer.
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
 * (each view is a {@link JPanel} instance). The frame can be removed at any time.
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
 * 
 * <h3>Threads</h3>
 * 
 * <p>
 * As some methods are protected from concurrent accesses, this view will use the
 * {@link Viewer} instance as the lock. The {@link Viewer} uses itself as a lock
 * for its own synchronized methods.
 * </p>
 */
public class DefaultView extends BaseView implements WindowListener {

	/**
	 * The rendering surface.
	 */
	protected class Surface extends JPanel {
		private static final long serialVersionUID = 589113660791686872L;

		@Override
		public void paintComponent(Graphics g) {
			// super.paintComponent(g);	// We do not want any background. We handle it.
			Graphics2D g2 = (Graphics2D) g;
			render(g2);
			((BaseCamera)renderer.getCamera()).resetCameraChangedFlag();
		}		
	}
	
	/**
	 * The rendering surface.
	 */
	protected Surface surface;
	
	/**
	 * The (optional) frame.
	 */
	protected JFrame frame;

	/**
	 * True if the window is iconified, we can stop rendering.
	 */
	protected boolean isIconified = false;
	
	@Override
	public void open(String identifier, Viewer viewer, GraphRenderer renderer) {
		surface = new Surface();

		super.open(identifier, viewer, renderer);
		surface.setOpaque(true);
		surface.setDoubleBuffered(true);
	}

	@Override
	public void display(GraphicGraph graph, boolean graphChanged) {
		surface.repaint();
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
				frame.add(surface, BorderLayout.CENTER);
				frame.setSize(800, 600);
				frame.setVisible(true);
				frame.addWindowListener(this);
				if(shortcuts != null)
					shortcuts.installedInAWTComponent(frame);
				checkInitialAttributes();
			} else {
				frame.setVisible(true);
			}
		} else {
			if (frame != null) {
				frame.removeWindowListener(this);
				if(shortcuts != null)
					shortcuts.removedFromAWTComponent(frame);
				frame.remove(surface);
				frame.setVisible(false);
				frame.dispose();
				frame = null;
			}
		}
	}

	protected void render(Graphics2D g) {
		if(! isIconified) {
			surface.setBackground(graph.getStyle().getFillColor(0));
			renderer.render(g, surface.getX(), surface.getY(),
								surface.getWidth(), surface.getHeight());
		}

		String screenshot = (String) graph.getLabel("ui.screenshot");

		if (screenshot != null) {
			graph.removeAttribute("ui.screenshot");
			renderer.screenshot(screenshot, surface.getWidth(),
					surface.getHeight());
		}
	}

	// Selection

	@Override
	public void beginSelectionAt(double x1, double y1) {
		super.beginSelectionAt(x1, y1);
		surface.repaint();
	}

	@Override
	public void selectionGrowsAt(double x, double y) {
		super.selectionGrowsAt(x, y);
		surface.repaint();
	}

	@Override
	public void endSelectionAt(double x2, double y2) {
		super.endSelectionAt(x2, y2);
		surface.repaint();
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
					String
							.format(
									"The %s view is not up to date, do not know %s CloseFramePolicy.",
									getClass().getName(), viewer
											.getCloseFramePolicy()));
		}
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
		isIconified = false;
	}

	public void windowIconified(WindowEvent e) {
		isIconified = true;
	}

	public void windowOpened(WindowEvent e) {
		graph.removeAttribute("ui.viewClosed");
	}

	// Methods deferred to the renderer

	@Override
	public void setBackLayerRenderer(LayerRenderer renderer) {
		super.setBackLayerRenderer(renderer);
		surface.repaint();
	}

	@Override
	public void setForeLayoutRenderer(LayerRenderer renderer) {
		super.setForeLayoutRenderer(renderer);
		surface.repaint();
	}
	
	@Override
	public boolean isAWT() {
		return true;
	}

	@Override
	public Component getAWTComponent() {
		return surface;
	}
	
	@Override
	public Object getGUIComponent() {
		return surface;
	}
	
	@Override
	public void setFrameTitle(String title) {
		if(frame != null)
			frame.setTitle(title);
	}
}