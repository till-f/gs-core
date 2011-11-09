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
	 */
	class TextRenderer {
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

				g.drawString(element.label, (float) c.x, (float) (c.y + textSize / 3)); // approximation to gain time.
			}
		}
	}
	
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