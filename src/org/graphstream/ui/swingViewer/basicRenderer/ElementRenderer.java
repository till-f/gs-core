/*
 * Copyright 2006 - 2011 
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
package org.graphstream.ui.swingViewer.basicRenderer;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

import org.graphstream.graph.Element;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.StyleGroup.ElementEvents;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.BaseSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.EdgeSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.NodeSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.SpriteSkeleton;
import org.graphstream.ui.swingViewer.util.Camera;
import org.graphstream.ui.swingViewer.util.FontCache;

/**
 * Base renderer for element groups. 
 * 
 * <p>
 * This bass renderer handles the {@link #render(StyleGroup, Graphics2D, Camera)} method by
 * providing an execution base for specific renderers. The {@link #render(StyleGroup, Graphics2D, Camera)}
 * method is called at each frame for a group of element having the same style. This method will
 * take care of which elements are bulk (all with the same style), and which elements are dynamic
 * (with a dynamic color or size), as well as elements that may be modified due to an event (clicked,
 * selected, etc.). It call corresponding rendering methods for each.
 * </p>
 * 
 * <p>
 * First the {@link #render(StyleGroup, Graphics2D, Camera)} method will call
 * {@link #setupRenderingPass(StyleGroup, Graphics2D, Camera)}. This method may do all administrative
 * tasks needed before rendering. Then it calls {@link #pushStyle(StyleGroup, Graphics2D, Camera)}.
 * This method must setup all the settings that will remain unchanged during the rendering of all
 * elements. Then for each bulk element it first check if the element has a skeleton, and if not
 * add it, before calling {@link #renderElement(StyleGroup, Graphics2D, Camera, GraphicElement)} if
 * the element is visible or {@link #elementInvisible(StyleGroup, Graphics2D, Camera, GraphicElement)}
 * in the other case. {@link #renderElement(StyleGroup, Graphics2D, Camera, GraphicElement)} must
 * do the rendering proper. 
 * </p>
 * 
 * <p>
 * In a second phase the {@link #render(StyleGroup, Graphics2D, Camera)} method will process each
 * element that has a dynamic style. The graphic graph is organized so that if the sylesheet tells
 * that the element has a dynamic color or size, but the element has no "ui.color" or "ui.size"
 * attribute, the element is bulk and not dynamic. This way, only elements that are really dynamic
 * will be drawn in this phase. In this phase, for each element, the {@link #pushDynStyle(StyleGroup, Graphics2D, Camera, GraphicElement)}
 * method is first called to setup all dynamic parts of the style (mostly color and size) and then
 * {@link #renderElement(StyleGroup, Graphics2D, Camera, GraphicElement)}.
 * </p>
 * 
 * <p>
 * In a third phase, all the elements actually modified by an event are drawn. In this case, for
 * each element, the event is first pushed so that the style is modified. Then the {@link #pushStyle(StyleGroup, Graphics2D, Camera)}
 * method is called and then the {@link #renderElement(StyleGroup, Graphics2D, Camera, GraphicElement)}
 * method.
 * </p>
 * 
 * <p>
 * In a last phase, all the texts are rendered. Elements that have text may register to render
 * a text in the {@link #textRenderer}. Then the text renderer will push the text style once,
 * and render all the texts in place.
 * </p>
 * 
 * <p>
 * This class is completely generic and not dependent of any real drawing operations.
 * </p>
 */
public abstract class ElementRenderer {
	/**
	 * Allow to know if an event began or ended.
	 */
	protected boolean hadEvents = false;

	/**
	 * The set of elements that need a text label. The rendering of text is done in a
	 * separate pass, to avoid continually changing the graphics context.
	 */
	protected TextRenderer textRenderer = new TextRenderer();
	
	/**
	 * Render all the (visible) elements of the group.
	 */
	public void render(StyleGroup group, Graphics2D g, Camera camera) {
		setupRenderingPass(group, g, camera);
		pushStyle(group, g, camera);

		for (Element e : group.bulkElements()) {
			GraphicElement ge = (GraphicElement) e;

			maybeAddSkeleton(ge);
			
			if (camera.isVisible(ge))
				renderElement(group, g, camera, ge);
			else
				elementInvisible(group, g, camera, ge);
		}

		if (group.hasDynamicElements()) {
			for (Element e : group.dynamicElements()) {
				GraphicElement ge = (GraphicElement) e;

				maybeAddSkeleton(ge);

				if (camera.isVisible(ge)) {
					if (!group.elementHasEvents(ge)) {
						pushDynStyle(group, g, camera, ge);
						renderElement(group, g, camera, ge);
					}
				} else {
					elementInvisible(group, g, camera, ge);
				}
			}
		}

		if (group.hasEventElements()) {
			for (ElementEvents event : group.elementsEvents()) {
				GraphicElement ge = (GraphicElement) event.getElement();
	
				maybeAddSkeleton(ge);

				if (camera.isVisible(ge)) {
					event.activate();
					pushStyle(group, g, camera);
					renderElement(group, g, camera, ge);
					event.deactivate();
				} else {
					elementInvisible(group, g, camera, ge);
				}
			}

			hadEvents = true;
		} else {
			hadEvents = false;
		}
		
		textRenderer.renderTexts(group, camera, g);
	}

	/**
	 * Called before the whole rendering pass for all elements.
	 * 
	 * @param g
	 *            The Swing graphics.
	 * @param camera
	 *            The camera.
	 */
	protected abstract void setupRenderingPass(StyleGroup group, Graphics2D g,
			Camera camera);

	/**
	 * Called before the rendering of bulk and event elements.
	 * 
	 * @param g
	 *            The Swing graphics.
	 * @param camera
	 *            The camera.
	 */
	protected abstract void pushStyle(StyleGroup group, Graphics2D g,
			Camera camera);

	/**
	 * Called before the rendering of elements on dynamic styles. This must only
	 * change the style properties that can change dynamically.
	 * 
	 * @param g
	 *            The Swing graphics.
	 * @param camera
	 *            The camera.
	 * @param element
	 *            The graphic element concerned by the dynamic style change.
	 */
	protected abstract void pushDynStyle(StyleGroup group, Graphics2D g,
			Camera camera, GraphicElement element);

	/**
	 * Render a single element knowing the style is already prepared. Elements
	 * that are not visible are not drawn.
	 * 
	 * @param g
	 *            The Swing graphics.
	 * @param camera
	 *            The camera.
	 * @param element
	 *            The element to render.
	 */
	protected abstract void renderElement(StyleGroup group, Graphics2D g,
			Camera camera, GraphicElement element);

	/**
	 * Called during rendering in place of
	 * {@link #renderElement(StyleGroup, Graphics2D, Camera, GraphicElement)}
	 * to signal that the given element is not inside the view. The
	 * renderElement() method will be called as soon as the element becomes
	 * visible anew.
	 * 
	 * @param g
	 *            The Swing graphics.
	 * @param camera
	 *            The camera.
	 * @param element
	 *            The element to render.
	 */
	protected abstract void elementInvisible(StyleGroup group, Graphics2D g,
			Camera camera, GraphicElement element);

	/**
	 * Specific renderer for text labels.
	 * 
	 * <p>
	 * The text renderer is a component that allows to register which elements need a text label
	 * during the rendering pass (using the {@link #queueElement(GraphicElement)} method), and to
	 * then draw these labels in a single pass (since drawing text often require lots of common
	 * operations that can be done once before rendering all the labels). The rendering is then
	 * done using the {@link #renderTexts(StyleGroup, Camera, Graphics2D)} method. 
	 * </p>
	 */
	public static class TextRenderer {
		/**
		 * The set of elements that need a text label.
		 */
		protected ArrayList<GraphicElement> needTextRenderPass = new ArrayList<GraphicElement>();

		/**
		 * The actual text font.
		 */
		protected Font textFont;

		/**
		 * The actual text color.
		 */
		protected Color textColor;

		/**
		 * the Actual text size in points.
		 */
		protected int textSize;

		/**
		 * Register an element as needing to render a text when the text rendering pass will
		 * come.
		 * @param element The element.
		 */
		public void queueElement(GraphicElement element) {
			needTextRenderPass.add(element);
		}
		
		/**
		 * Render the queued elements texts.
		 * @param group The style group.
		 * @param camera The camera.
		 * @param g2 The graphics.
		 */
		public void renderTexts(StyleGroup group, Camera camera, Graphics2D g2) {
			AffineTransform Tx = g2.getTransform();
			configureText(group, camera, g2);
			
			for(GraphicElement element: needTextRenderPass) {
				renderText(group, g2, camera, element);
			}
			
			needTextRenderPass.clear();
			g2.setTransform(Tx);
		}
		
		protected void configureText(StyleGroup group, Camera camera, Graphics2D g2) {
			String fontName = group.getTextFont();
			StyleConstants.TextStyle textStyle = group.getTextStyle();

			textSize  = (int) group.getTextSize().value;
			textColor = group.getTextColor(0);
			textFont  = FontCache.defaultFontCache().getFont(fontName, textStyle, textSize);
			
			g2.setColor(textColor);
			g2.setFont(textFont);
			g2.setTransform(new AffineTransform());	// Go back in pixels.
		}

		protected void renderText(StyleGroup group, Graphics2D g, Camera camera, GraphicElement element) {
			if (element.label != null
			 && group.getTextMode() != StyleConstants.TextMode.HIDDEN
			 && group.getTextVisibilityMode() != StyleConstants.TextVisibilityMode.HIDDEN) {
				BaseSkeleton skel = (BaseSkeleton) element.getSkeleton();
				Point3 c = skel == null ? element.getCenter() : skel.getPosition(camera, null, Units.PX);
				Point3 s = skel == null ? new Point3(0, 0, 0) : skel.getSize(camera, Units.PX);

				// We draw the text always at the right of the element with 4 pixels of offset for readability.
				g.drawString(element.label, (float) (c.x + s.x/2) + 4, (float) (c.y + textSize / 3)); // approximation to gain time.
			}
		}
	}
	
	/**
	 * Check if an element has a skeleton, and if not add it.
	 */
	protected void maybeAddSkeleton(GraphicElement element) {
		if(element.getSkeleton() == null) {
			switch(element.getSelectorType()) {
				case NODE:   element.setSkeleton(new NodeSkeleton());   break;
				case EDGE:   element.setSkeleton(new EdgeSkeleton());   break;
				case SPRITE: element.setSkeleton(new SpriteSkeleton()); break;
			}
		}
	}
}