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
package org.graphstream.ui.swingViewer.basicRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;

import javax.imageio.ImageIO;

import org.graphstream.graph.Element;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicElement.Skeleton;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.StyleGroupSet;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.Value;
import org.graphstream.ui.swingViewer.Camera;
import org.graphstream.ui.swingViewer.GraphRendererBase;
import org.graphstream.ui.swingViewer.LayerRenderer;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.EdgeSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.NodeSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.SpriteSkeleton;
import org.graphstream.ui.swingViewer.util.FPSLogger;
import org.graphstream.ui.swingViewer.util.GraphMetrics;
import org.graphstream.ui.swingViewer.util.Graphics2DOutput;

/**
 * A very simple renderer for the graph that respect only a thin subset of CSS.
 * 
 * <p>
 * This is a minimal implementation of a renderer that only supports a subset of
 * the CSS :
 * <ul>
 * 		<li>Fill</li>
 * 		<li>Size</li>
 * 		<li>Stroke</li>
 * 		<li>Text</li>
 * </ul>
 * </p>
 */
public class SwingBasicGraphRenderer extends GraphRendererBase {
	/**
	 * Set the view on the view port defined by the metrics.
	 */
	protected BasicCamera camera = null;

	/**
	 * Specific renderer for nodes.
	 */
	protected NodeRenderer nodeRenderer = new NodeRenderer();

	/**
	 * Specific renderer for edges.
	 */
	protected EdgeRenderer edgeRenderer = new EdgeRenderer();

	/**
	 * Specific renderer for sprites.
	 */
	protected SpriteRenderer spriteRenderer = new SpriteRenderer();

	/**
	 * Render the background of the graph, before anything is drawn.
	 */
	protected LayerRenderer backRenderer = null;

	/**
	 * Render the foreground of the graph, after anything is drawn.
	 */
	protected LayerRenderer foreRenderer = null;

	/**
	 * Optional output log of the frame-per-second.
	 */
	protected FPSLogger fpsLog = null;
	
	@Override
	public void open(GraphicGraph graph, Component renderingSurface) {
		camera = new BasicCamera(graph);
		super.open(graph, renderingSurface);
		graph.setSkeletonFactory(new SwingBasicSkeletonFactory());
	}

	@Override
	public void close() {
		if(fpsLog != null) {
			fpsLog.close();
			fpsLog = null;
		}
		
		graph.setSkeletonFactory(null);
		super.close();
		camera = null;
	}

	public Camera getCamera() {
		return camera;
	}

	public void render(Graphics2D g, int x, int y, int width, int height) {
		if (graph != null) {
			beginFrame();
			
			if (camera.getGraphViewport() == null
			 && camera.getMetrics().diagonal == 0
			 && (graph.getNodeCount() == 0 && graph.getSpriteCount() == 0)) {
				displayNothingToDo(g, width, height);
			} else {
				renderGraph(g, x, y, width, height);
				renderSelection(g);
			}
			
			endFrame();
		}
	}

	/**
	 * Create or remove the FPS logger and start measuring time if activated.
	 */
	protected void beginFrame() {
		if(graph.hasLabel("ui.log")) {
			if(fpsLog == null) {
				fpsLog = new FPSLogger(graph.getLabel("ui.log").toString());
			}
		} else {
			if(fpsLog != null) {
				fpsLog.close();
				fpsLog = null;
			}
		}
		
		if(fpsLog != null) {
			fpsLog.beginFrame(graph);
		}
	}
	
	/**
	 * End measuring frame time.
	 */
	protected void endFrame() {
		if(fpsLog != null) {
			fpsLog.endFrame(graph);
		}
	}

	public void moveElementAtPx(GraphicElement element, double x, double y) {
		Point3 p = camera.transformPxToGu(x, y);
		element.move(p.x, p.y, element.getCenter().z);
	}

	/**
	 * Render the whole graph.
	 */
	protected void renderGraph(Graphics2D g, double x, double y, double width, double height) {
		setupGraphics(g);
		renderGraphBackground(g);
		renderBackLayer(g, true);
		camera.pushView(g, x, y, width, height);
		renderBackLayer(g, false);
		renderGraphElements(g);
		renderGraphForeground(g);
		renderForeLayer(g, false);
		camera.popView(g);
		renderForeLayer(g, true);
	}

	protected void setupGraphics(Graphics2D g) {
		// XXX we do this at each frame !!! Why not doing this only when it changes !!! XXX
		if (graph.hasAttribute("ui.antialias")) {
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		} else {
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		if (graph.hasAttribute("ui.quality")) {
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		} else {
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
			g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		}
	}

	/**
	 * Render the background of the graph. This merely colors the background with the fill color.
	 */
	protected void renderGraphBackground(Graphics2D g) {
		StyleGroup group = graph.getStyle();

		g.setColor(group.getFillColor(0));
		g.fillRect(0, 0,
				(int) camera.getMetrics().surfaceViewport[2],
				(int) camera.getMetrics().surfaceViewport[3]);
	}

	/**
	 * Render the foreground of the graph. This draws a border if there is a stroke around the
	 * graph.
	 */
	protected void renderGraphForeground(Graphics2D g) {
		StyleGroup style = graph.getStyle();
		Rectangle2D rect = new Rectangle2D.Double();
		GraphMetrics metrics = camera.getMetrics();
		double px1 = metrics.px1;
		Value stroke = style.getShadowWidth();
		
		if (style.getStrokeMode() != StyleConstants.StrokeMode.NONE
		 && style.getStrokeWidth().value != 0) {
			rect.setFrame(metrics.lo.x, metrics.lo.y + px1, metrics.size.data[0] - px1, metrics.size.data[1] - px1);
			g.setStroke(new BasicStroke((float)metrics.lengthToGu(stroke)));
			g.setColor(graph.getStyle().getStrokeColor(0));
			g.draw(rect);
		}
	}
	
	/**
	 * Render each element of the graph.
	 */
	protected void renderGraphElements(Graphics2D g) {
		StyleGroupSet sgs = graph.getStyleGroups();

		if (sgs != null) {
			for (HashSet<StyleGroup> groups : sgs.zIndex()) {
				for (StyleGroup group : groups) {
					renderGroup(g, group);
				}
			}
		}
	}

	/**
	 * Render a style group.
	 */
	protected void renderGroup(Graphics2D g, StyleGroup group) {
		switch (group.getType()) {
			case NODE:   nodeRenderer.render(group, g, camera);   break;
			case EDGE:   edgeRenderer.render(group, g, camera);   break;
			case SPRITE: spriteRenderer.render(group, g, camera); break;
			case GRAPH: break;
			default: throw new RuntimeException("WTF? "+group.getType());
		}
	}

	protected void renderSelection(Graphics2D g) {
		if (selection.active && selection.lo.x != selection.hi.x && selection.lo.y != selection.hi.y) {
			double t;
			double x1 = selection.lo.x;
			double y1 = selection.lo.y;
			double x2 = selection.hi.x;
			double y2 = selection.hi.y;
			double w  = camera.getMetrics().surfaceViewport[2];
			double h  = camera.getMetrics().surfaceViewport[3];

			Point3 lo = camera.transformPxToGu(new Point3(selection.lo));
			Point3 hi = camera.transformPxToGu(new Point3(selection.hi));

			if (x1 > x2) {
				t  = x1;
				x1 = x2;
				x2 = t;
				t = lo.x;
				lo.x = hi.x;
				hi.x = t;
			}
			if (y1 > y2) {
				t  = y1;
				y1 = y2;
				y2 = t;
				t = lo.y;
				lo.y = hi.y;
				hi.y = t;
			}

			Stroke s = g.getStroke();

			g.setStroke(new BasicStroke(1));
			g.setColor(new Color(222, 222, 222));
			g.drawLine(0, (int) y1, (int) w, (int) y1);
			g.drawLine(0, (int) y2, (int) w, (int) y2);
			g.drawLine((int) x1, 0, (int) x1, (int) h);
			g.drawLine((int) x2, 0, (int) x2, (int) h);
			g.setColor(new Color(250, 200, 0));
			g.drawRect((int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
			g.setStroke(s);
			
			g.setColor(graph.getStyle().getStrokeColor().get(0));
			g.drawString(String.format("(%.3f, %.3f)", lo.x, lo.y), (int)x1, (int)y1);
			g.drawString(String.format("(%.3f, %.3f)", hi.x, hi.y), (int)x2, (int)y2);
		}
	}

	protected void renderBackLayer(Graphics2D g, boolean inPixels) {
		if (backRenderer != null) {
			renderLayer(g, backRenderer, inPixels);
		}
	}

	protected void renderForeLayer(Graphics2D g, boolean inPixels) {
		if (foreRenderer != null) {
			renderLayer(g, foreRenderer, inPixels);
		}
	}

	protected void renderLayer(Graphics2D g, LayerRenderer renderer, boolean inPixels) {
		if(inPixels == renderer.rendersInPX()) {
			renderer.render(g, graph, camera);
		}
	}

	/**
	 * Show the center, the low and high points of the graph, and the visible
	 * area (that should always map to the window borders).
	 */
	protected void debugVisibleArea(Graphics2D g) {
		Rectangle2D rect = new Rectangle2D.Double();
		GraphMetrics metrics = camera.getMetrics();

		double x = metrics.loVisible.x;
		double y = metrics.loVisible.y;
		double w =  Math.abs(metrics.hiVisible.x - x);
		double h =  Math.abs(metrics.hiVisible.y - y);

		rect.setFrame(x, y, w, h);
		g.setStroke(new BasicStroke((float)(metrics.px1 * 4)));
		g.setColor(Color.RED);
		g.draw(rect);

		g.setColor(Color.BLUE);
		Ellipse2D ellipse = new Ellipse2D.Double();
		double px1 = metrics.px1;
		ellipse.setFrame(camera.getViewCenter().x - 3 * px1, camera.getViewCenter().y - 3 * px1, px1 * 6, px1 * 6);
		g.fill(ellipse);
		ellipse.setFrame(metrics.lo.x - 3 * px1, metrics.lo.y - 3 * px1, px1 * 6, px1 * 6);
		g.fill(ellipse);
		ellipse.setFrame(metrics.hi.x - 3 * px1, metrics.hi.y - 3 * px1, px1 * 6, px1 * 6);
		g.fill(ellipse);
	}

	public void screenshot(String filename, int width, int height) {
		if (graph != null) {
			try {
				if (filename.toLowerCase().endsWith("png")) {
					BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					renderGraph(img.createGraphics(), 0, 0, width, height);
	
					File file = new File(filename);
					ImageIO.write(img, "png", file);
				} else if (filename.toLowerCase().endsWith("bmp")) {
					BufferedImage img = new BufferedImage(width, height,
							BufferedImage.TYPE_INT_RGB);
					renderGraph(img.createGraphics(), 0, 0, width, height);
	
					File file = new File(filename);
					ImageIO.write(img, "bmp", file);
				} else if(filename.toLowerCase().endsWith("svg")) {
					String plugin = "org.graphstream.ui.batik.BatikGraphics2D";
					Class<?> c = Class.forName(plugin);
					Object o = c.newInstance();
					if(o instanceof Graphics2DOutput) {
						Graphics2DOutput out = (Graphics2DOutput) o;
						Graphics2D g2 = out.getGraphics();
						render(g2, 0, 0, width, height);
						out.outputTo(filename);
					} else {
						System.err.printf("plugin %s is not an instance of Graphics2DOutput (%s)%n", plugin, o.getClass().getName());
					}
				} else if(filename.toLowerCase().endsWith("svg")) {
					try {
						String plugin = "org.graphstream.ui.batik.BatikGraphics2D";
						Class<?> c = Class.forName(plugin);
						Object o = c.newInstance();
						if(o instanceof Graphics2DOutput) {
							Graphics2DOutput out = (Graphics2DOutput) o;
							Graphics2D g2 = out.getGraphics();
							render(g2, 0, 0, width, height);
							out.outputTo(filename);
						} else {
							System.err.printf("plugin %s is not an instance of Graphics2DOutput (%s)%n", plugin, o.getClass().getName());
						}
					} catch(Exception e) {
						e.printStackTrace();
					}
				} else {// if (filename.toLowerCase().endsWith("jpg") || filename.toLowerCase().endsWith("jpeg")) {
					BufferedImage img = new BufferedImage(width, height,
							BufferedImage.TYPE_INT_RGB);
					renderGraph(img.createGraphics(), 0, 0, width, height);
	
					File file = new File(filename);
					ImageIO.write(img, "jpg", file);
				}
			} catch(Exception e) {
				e.printStackTrace();
				// XXX a better error handling ?
			}
		}
	}

	public void setBackLayerRenderer(LayerRenderer renderer) {
		backRenderer = renderer;
	}

	public void setForeLayerRenderer(LayerRenderer renderer) {
		foreRenderer = renderer;
	}

	// Style Group Listener

	public void elementStyleChanged(Element element, StyleGroup oldStyle,
			StyleGroup style) {
	}
	
	// Nested classes
	
	/**
	 * Factory for the skeletons adapted to this renderer.
	 */
	class SwingBasicSkeletonFactory implements GraphicGraph.SkeletonFactory {
		public Skeleton newNodeSkeleton() {
			return new NodeSkeleton();
		}

		public Skeleton newEdgeSkeleton() {
			return new EdgeSkeleton();
		}

		public Skeleton newSpriteSkeleton() {
			return new SpriteSkeleton();
		}
	}
}