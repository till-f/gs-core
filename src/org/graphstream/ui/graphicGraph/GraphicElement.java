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
package org.graphstream.ui.graphicGraph;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.stylesheet.Selector;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;

/**
 * Super class of all graphic node, edge, and sprite elements.
 * 
 * <p>
 * Base class for nodes, sprites and edges graphic representation, used in the {@link GraphicGraph}.
 * </p>
 * 
 * <h2>Graphics ?</h2>
 * 
 * <p>
 * Each graphic element has specific fields and methods dedicated to graphics. Each element at least
 * has:
 * <ul>
 * 	<li>a style (group), defining the CSS properties applying on the element (and all the
 *     elements of the same group),</li>
 * 	<li>a label, that may be null (by default),</li>
 *  <li>a hidden flag, indicating if the element should be visible.</li>
 * </p>
 * 
 * <p>
 * The element also defines the basic behavior to reload the style when needed,
 * defines abstract methods to set and get its position ({@link #getCenter()}), and to do
 * appropriate actions when specific predefined attributes change (most of them starting
 * with the prefix "ui.").
 * </p>
 * 
 * <h2>Attachments</h2>
 *
 * <p>
 * An element can register a set of other elements that are "attached" to it and want to be
 * notified if it moves. This can be the case of some edges that want for example to modify their
 * skeletons when the two nodes they are attached to move. This can also be the case of sprites
 * that are either attached to a node or edge. By default the attachment list is not created to
 * avoid consuming memory. It is created as soon as there is at least one attachment.
 * You can attach another element using {@link #addAttachment(GraphicElement)}. You correspondingly
 * remove it using {@link #removeAttachment(GraphicElement)}. Each attached element
 * {@link #attachMoved(GraphicElement)} method is called when this element will move.
 * </p>
 * 
 * <p>
 * By default, the {@link #attachMoved(GraphicElement)} method calls the skeleton
 * {@link Skeleton#positionChanged()} position changed method. See skeletons under.
 * </p>
 * 
 * <h2>Skeletons</h2>
 * 
 * <p>
 * Each element may have a {@link GraphicElement.Skeleton}. The basic idea of skeleton is to let
 * graph renderers register an object that is notified for each change in position, and changes
 * of the generic "ui." attributes like dynamic size, "ui.points", dynamic color, etc. The skeleton
 * most often  is a way to store a complex geometry that needs to be recomputed when the element
 * moved or informations about it (label, icon for example) changed. By default, elements have no
 * skeleton. There can be only one skeleton per element.
 * </p>
 * 
 * <p>
 * The skeletons must be registered specifically using {@link #setSkeleton(Skeleton)}. When the
 * skeleton is registered, its {@link Skeleton#installed(GraphicElement)} method is called. Correspondingly,
 * skeletons must be unregistered using {@link #setSkeleton(Skeleton)} with a null value. The
 * skeleton {@link Skeleton#uninstalled()} method will be called. If you set a new skeleton with
 * {@link #setSkeleton(Skeleton)}, the old one will have its {@link Skeleton#uninstalled()} method
 * called. If this element is removed, and if there is a skeleton, this one will also see its
 * {@link Skeleton#uninstalled()} method called.
 * </p>
 * 
 * <p>
 * Once installed the skeleton receives events for each change in the graphic element, allowing
 * it to update its internal representation.
 * </p>
 * 
 * <p>
 * For example, if an edge is drawn with a complex curve, the skeleton will be notified each time
 * the edge position changed, if the edge requested position notification in its nodes, and it will
 * be able to recompute the curve only when the edge really changed, not each time it must draw it. 
 * </p>
 * 
 * <h2>Attributes</h2>
 *
 * <p>
 * The graphic element has the ability to store attributes like any other graph
 * element, however the attributes stored by the graphic element are by default restricted.
 * There is a filter on the attribute adding methods that let pass only:
 * <ul>
 * <li>All attributes starting with "ui.".</li>
 * <li>The "x", "y", "z", "xy" and "xyz" attributes.</li>
 * <li>The "stylesheet" attribute.</li>
 * <li>The "label" attribute.</li>
 * </ul>
 * All other attributes are filtered and not stored. The result is that if the
 * graphic graph is used as an input (a source of graph events) some attributes
 * will not pass through the filter.
 * </p>
 * 
 * <p>
 * The graphic element will process some attributes specifically. It can handle the "ui.style" and
 * modify the style sheet accordingly. It can call the skeleton each time some "ui." attribute
 * changed. It will handle automatically the storage of the label from the "ui.label" or "label"
 * attributes. It can handle the x, y, z, xy and xyz attributes to change its position, etc.
 * See the {@link #attributeChanged(String, long, String, AttributeChangeEvent, Object, Object)}
 * method and the corresponding methods in {@link GraphicNode}, {@link GraphicEdge} and
 * {@link GraphicSprite}.
 * </p>
 */
public abstract class GraphicElement extends AbstractGraphicElement {
	/**
	 * Interface for renderers registered in each style group.
	 */
	public interface GraphicElementRenderer {}

	/**
	 * Skeleton of a graphic element.
	 * 
	 * <p>
	 * A special object that renderers can register inside the graphic element to define more
	 * graphic fields, and to monitor changes in dedicated attributes. There is actually only one
	 * such skeleton per element. The renderer can then use the skeletons to help render the
	 * graphic elements.
	 * </p>
	 * 
	 * <p>
	 * The advantage of using skeletons is that the geometry of elements can be stored in them, and
	 * that they are listeners of the changes on the element. The skeleton is therefore updated
	 * only when needed.
	 * </p> 
	 * 
	 * <p>
	 * Graphic elements are not required to have skeletons. Inside a graphic graph, some elements
	 * may have a skeleton and other not. Once added to an element, the skeleton is listener of
	 * this element. For edges, the skeleton is both listener of the edge, but can also receive
	 * notifications from the nodes of the element for the positionChanged() event (but the edge
	 * must have been registered as an attachment of the nodes). This is very
	 * useful if the geometry of the edge needs to be recomputed.
	 * </p>
	 */
	public interface Skeleton {
		/**
		 * Called when the skeleton is first inserted in the element.
		 */
		void installed(GraphicElement element);
		/**
		 * Called when the skeleton is removed from the element.
		 */
		void uninstalled();
		/**
		 * Called each time the position of the element changed. The position is stored inside the
		 * graphic element. For sprites, this is also called if the sprite was attached or detached,
		 * as the position interpretation changes in this case.
		 */
		void positionChanged();
		/**
		 * Called each time the "ui.point" attribute changed. If there are no more points, the
		 * new value is null.
		 * @param newValue the new value for the points.
		 */
		void pointsChanged(Object newValue);
		/**
		 * Called each time the "ui.size" attribute changed. If there is no more dynamic size,
		 * the new value is null.
		 * @param newValue the new value for the size.
		 */
		void sizeChanged(Object newValue);
		/**
		 * Called each time the "label" or "ui.label" attribute changed. The
		 * label is stored inside the graphic element.
		 */
		void labelChanged();
		/**
		 * Called each time the "ui.icon" attribute changed. If there is no more dynamic icon,
		 * the new value is null.
		 * @param newValue the new value for the icon. 
		 */
		void iconChanged(Object newValue);
		/**
		 * Called each time the "ui.color" attribute changed. If there is no more dynamic color,
		 * the new value is null.
		 * @param newValue The new value for the color. 
		 */
		void colorChanged(Object newValue);
		/**
		 * Called each time an "ui.something" attribute change.
		 * @param attribute The attribute name.
		 * @param newValue The new attribute value.
		 */
		void unknownUIAttributeChanged(String attribute, Object newValue);
		/**
		 * Called each time the style field of the graphic element changes
		 * or if the contents of the style change.
		 */
		void styleChanged();
	}

	/**
	 * Graph containing this element.
	 */
	protected GraphicGraph mygraph;

	/**
	 * The label or null if not specified.
	 */
	public String label;

	/**
	 * Do not show.
	 */
	public boolean hidden = false;
	
	/**
	 * Associated GUI component.
	 */
	public Object component;
	
	/**
	 * The element skeleton. There is actually only one such skeleton.
	 */
	protected Skeleton skeleton;
	
	/**
	 * Registers the elements that are attached to this element and requested to be notified when it
	 * moves. This is the case of some edges that attach to their nodes to be notified when it
	 * moves, or of sprites that are attached to nodes or edges. However, as often sprites and edges
	 * will not need to be notified, this field is created lazily.
	 * 
	 * XXX Would a HashSet be better suited for this? In case of removal
	 * XXX this is slow.
	 */
	public ArrayList<GraphicElement> attached = null;

	/**
	 * New element.
	 * @param id The unique identifier.
	 * @param graph back reference to the graphic graph containing the element.
	 */
	public GraphicElement(String id, GraphicGraph graph) {
		super(id);
		this.mygraph = graph;
		setIndex(-1);
	}

	/**
	 * The graphic graph containing this element.
	 */
	public GraphicGraph myGraph() {
		return mygraph;
	}

	@Override
	protected String myGraphId()
	{
		return mygraph.getId();
	}

	@Override
	protected long newEvent()
	{
		return mygraph.newEvent();
	}
	
	@Override
	protected boolean nullAttributesAreErrors() {
		return mygraph.nullAttributesAreErrors();
	}

	/**
	 * Type of selector for the graphic element (Node, Edge, Sprite ?).
	 */
	public abstract Selector.Type getSelectorType();

	/**
	 * Label or null if not set.
	 */
	public String getLabel() {
		return label;
	}
	
	/**
	 * The skeleton or null if none.
	 */
	abstract public Skeleton getSkeleton();

	/**
	 * The element center. For nodes and sprites this is the position of the element. For edges
	 * this is the center point between the two nodes (should not be that useful for edges).
	 */
	public abstract Point3 getCenter();

	/**
	 * The associated GUI component.
	 */
	public Object getComponent() {
		return component;
	}

	// Commands
	
	/**
	 * Register an attachment so that it is notified each time the node moves.
	 */
	public void addAttachment(GraphicElement element) {
		if(attached == null)
			attached = new ArrayList<GraphicElement>();
		
		attached.add(element);
	}

	/**
	 * Unregister an attachment.
	 */
	public void removeAttachment(GraphicElement element) {
		// XXX would a hash-set be better ?
		if(attached != null) {
			int pos = attached.indexOf(element);
			if(pos >=0)
				attached.remove(pos);
			// XXX Should we keep the edgeSkeletons array is empty ?
		}
	}

	/**
	 * The graphic element was removed from the graphic graph, clean up.
	 */
	protected void removed() {
		if(skeleton != null)
			skeleton.uninstalled();
	}

	/**
	 * Try to force the element to move at the give location in graph units
	 * (GU). For edges, this may move the two attached nodes.
	 * 
	 * @param x
	 *            The new X.
	 * @param y
	 *            The new Y.
	 * @param z
	 *            the new Z.
	 */
	public abstract void move(double x, double y, double z);

	/**
	 * The point of attachment moved.
	 * 
	 * Some elements may be attached to others (sprites, edges), and may register on the
	 * notification list of their attach point to be notified when it moves. When the attach moves,
	 * the attached element is called with this method.
	 * 
	 * @param attachPoint
	 *            The point of attach that moved, a graphic element.
	 */
	public abstract void attachMoved(GraphicElement attachPoint);
	
	/**
	 * Set the GUI component of this element.
	 * 
	 * @param component
	 *            The component.
	 */
	public void setComponent(Object component) {
		this.component = component;
	}
	
	/**
	 * Set the graphic element skeleton. The installed() and uninstalled() methods are called for
	 * the new and old skeletons if not null.
	 * @param skeleton The new skeleton or null to remove it.
	 */
	public void setSkeleton(Skeleton skeleton) {
		if(this.skeleton != skeleton) {
			if(this.skeleton != null)
				this.skeleton.uninstalled();

			if(skeleton != null)
				skeleton.installed(this);
				
			this.skeleton = skeleton;
		}
	}
	
	/**
	 * Change the style of this graphic element.
	 * @param style The new style.
	 */
	@Override
	public void changeStyle(StyleGroup style) {
		super.changeStyle(style);
		
		if(skeleton != null)
			skeleton.styleChanged();
	}
	
	/**
	 * Handle the "ui.class", "label", "ui.style", etc. attributes and call the graphic element
	 * listener if one is present.
	 */
	@Override
	protected void attributeChanged(String sourceId, long timeId,
									String attribute, AttributeChangeEvent event,
									Object oldValue,Object newValue) {
		if (event == AttributeChangeEvent.ADD || event == AttributeChangeEvent.CHANGE) {
			// ADD or CHANGE
			if (attribute.equals("ui.class")) {
				mygraph.styleGroups.checkElementStyleGroup(this);
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.styleChanged();	// XXX Check the styleChanged is not yet called.
			} else if (attribute.equals("label") || attribute.equals("ui.label")) {
				label = StyleConstants.convertLabel(newValue);
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.labelChanged();
			} else if (attribute.equals("ui.style")) {
				// Cascade the new style in the style sheet.

				if (newValue instanceof String) {
					try {
						mygraph.styleSheet.parseStyleFromString(new Selector(
								getSelectorType(), getId(), null),
								(String) newValue);
					} catch (java.io.IOException e) {
						System.err.printf(
								"Error while parsing style for %S '%s' :",
								getSelectorType(), getId());
						System.err.printf("    %s%n", e.getMessage());
						System.err.printf("    The style was ignored");
					}

					mygraph.graphChanged = true;
					if(skeleton != null) skeleton.styleChanged();	// XXX Check the styleChanged is not yet called.
				} else {
					System.err.printf("Error invalid style specification for the 'ui.style' attribute, needs a string.");
				}
			} else if (attribute.equals("ui.hide")) {
				hidden = true;
				mygraph.graphChanged = true;
			} else if (attribute.equals("ui.clicked")) {
				style.pushEventFor(this, "clicked");
				mygraph.graphChanged = true;
			} else if (attribute.equals("ui.selected")) {
				style.pushEventFor(this, "selected");
				mygraph.graphChanged = true;
			} else if (attribute.equals("ui.color")) {
				style.pushElementAsDynamic(this);
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.colorChanged(newValue);
			} else if (attribute.equals("ui.size")) {
				style.pushElementAsDynamic(this);
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.sizeChanged(newValue);
			} else if (attribute.equals("ui.icon")) {
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.iconChanged(newValue);
			} else if (attribute.equals("ui.points")) {
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.pointsChanged(newValue);
			} else {
				if(skeleton != null) skeleton.unknownUIAttributeChanged(attribute, newValue);
			}
		} else {
			// REMOVE
			if (attribute.equals("ui.class")) {
				Object o = attributes.remove("ui.class");	// Horror !
				mygraph.styleGroups.checkElementStyleGroup(this);
				attributes.put("ui.class", o);
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.styleChanged();
			} else if (attribute.equals("label")
					|| attribute.equals("ui.label")) {
				label = null;
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.labelChanged();
			} else if (attribute.equals("ui.hide")) {
				hidden = false;
				mygraph.graphChanged = true;
			} else if (attribute.equals("ui.clicked")) {
				style.popEventFor(this, "clicked");
				mygraph.graphChanged = true;
			} else if (attribute.equals("ui.selected")) {
				style.popEventFor(this, "selected");
				mygraph.graphChanged = true;
			} else if (attribute.equals("ui.color")) {
				style.popElementAsDynamic(this);
				if(skeleton != null) skeleton.colorChanged(null);
				mygraph.graphChanged = true;
			} else if (attribute.equals("ui.size")) {
				style.popElementAsDynamic(this);
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.sizeChanged(null);
			} else if (attribute.equals("ui.icon")) {
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.iconChanged(null);
			} else if (attribute.equals("ui.points")) {
				mygraph.graphChanged = true;
				if(skeleton != null) skeleton.pointsChanged(null);
			} else {
				if(skeleton != null) skeleton.unknownUIAttributeChanged(attribute, null);
			}
		}
	}

	// Overriding of standard attribute changing to filter them.

	protected static Pattern acceptedAttribute;

	static {
		acceptedAttribute = Pattern
				.compile("(ui\\..*)|x|y|z|xy|xyz|label|stylesheet");
	}

	@Override
	public void addAttribute(String attribute, Object... values) {
		Matcher matcher = acceptedAttribute.matcher(attribute);

		if (matcher.matches())
			super.addAttribute(attribute, values);
	}

	// Make change _ methods visible

	@Override
	protected void addAttribute_(String sourceId, long timeId,
			String attribute, Object... values) {
		super.addAttribute_(sourceId, timeId, attribute, values);
	}

	@Override
	protected void changeAttribute_(String sourceId, long timeId,
			String attribute, Object... values) {
		super.changeAttribute_(sourceId, timeId, attribute, values);
	}

	@Override
	protected void setAttribute_(String sourceId, long timeId,
			String attribute, Object... values) {
		super.setAttribute_(sourceId, timeId, attribute, values);
	}

	@Override
	protected void addAttributes_(String sourceId, long timeId,
			Map<String, Object> attributes) {
		super.addAttributes_(sourceId, timeId, attributes);
	}

	@Override
	protected void removeAttribute_(String sourceId, long timeId,
			String attribute) {
		super.removeAttribute_(sourceId, timeId, attribute);
	}
}