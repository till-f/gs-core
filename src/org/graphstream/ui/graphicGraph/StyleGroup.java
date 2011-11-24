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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.graphstream.graph.Element;
import org.graphstream.ui.graphicGraph.GraphicElement.SwingElementRenderer;
import org.graphstream.ui.graphicGraph.stylesheet.Rule;
import org.graphstream.ui.graphicGraph.stylesheet.Selector;
import org.graphstream.ui.graphicGraph.stylesheet.Style;

/**
 * A group of graph elements that share the same style.
 * 
 * <p>
 * The purpose of a style group is to allow retrieving all elements with the
 * same style easily. Most of the time, with graphic engines, pushing the
 * graphic state (the style, colors, line width, textures, gradients) is a
 * costly operation. Doing it once for several elements can speed up things a
 * lot. This is the purpose of the style group.
 * </p>
 * 
 * <p>
 * The action of drawing elements in group (first push style, then draw all
 * elements) are called bulk drawing. All elements that can be drawn at once
 * this way are called bulk elements.
 * </p>
 * 
 * <p>
 * In a style group it is not always possible do draw elements in a such a
 * "bulk" operation. If the style contains "dynamic values" for example, that is
 * value that depend on the value of an attribute stored on the element, or if
 * the element is modified by an event (clicked, selected), the element will not
 * be drawn the same as others.
 * </p>
 * 
 * <p>
 * The style group provides iterators on each of these categories of elements :
 * <ul>
 * <li>{@link #elements()} allows to browse all elements contained in the group
 * without exception. As this operation is less needed than others it is slower
 * (iterator on a hash map).</li>
 * <li>{@link #bulkElements()} allows to browse all remaining elements that have
 * no dynamic attribute or event. This operation is fast (iteration on an array).</li>
 * <li>{@link #dynamicElements()} allows to browse the subset of elements having
 * an attribute that modify their style. This operation is fast (iteration on
 * an array).</li>
 * <li>{@link #elementsEvents()} allows to browse the subset of elements
 * modified by an event. This operation is fast (iteration on an array).</li>
 * </ul>
 * Calling the two bulk and dynamic iterators would yield the same elements at
 * calling the elements() iterator. Elements are either stored in one or the other
 * (they are dynamic or bulk but not both). However an element can be in the
 * event set and still be in the dynamic or bulk set.
 * </p>
 * 
 * <p>
 * Therefore, when drawing, you can first push the style once and draw all the
 * bulk elements. Then for each dynamic element push the dynamic style and draw
 * the element, only if the element has no event.
 * </p>
 */
public class StyleGroup extends Style implements Iterable<AbstractGraphicElement> {
	/**
	 * Identifier for an element, its dynamic status, its events, etc.
	 */
	class ElementId {
		protected ArrayList<AbstractGraphicElement> where = null;
		protected int index = -1;
		protected int eventIndex = -1;
		protected ElementEvents events = null;
		
		/**
		 * Create the identifier an insert the element in "where".
		 * @param element The element to identify.
		 * @param where Where to store the element (either bulk or dyn).
		 */
		public ElementId(AbstractGraphicElement element, ArrayList<AbstractGraphicElement> where) {
			addToWhere(element, where);
		}
		
		/**
		 * The element associated with the identifier.
		 * @return The element.
		 */
		public AbstractGraphicElement get() {
			return where.get(index);
		}
		
		/**
		 * Remove the element.
		 */
		public void remove() {
			if(events != null) {
				removeFromEvents();
			}
			removeFromWhere();
		}
		
		/**
		 * Add tot he buld or dyn array.
		 * @param element The element.
		 * @param where bulk or dyn.
		 */
		protected void addToWhere(AbstractGraphicElement element, ArrayList<AbstractGraphicElement> where) {
			this.where = where;
			this.index = where.size();
			((AbstractGraphicElement)element).reindex(index);
			where.add(element);
		}
		
		/**
		 * Remove from the bulk or dyn array.
		 * @return The element.
		 */
		protected AbstractGraphicElement removeFromWhere() {
			AbstractGraphicElement me = where.get(index);
			int last = where.size() - 1;
			
			// If we did not removed at the end, we swap the end with the free index,
			// else we merely delete the position.
			if(index < last) {
				AbstractGraphicElement lastElt = where.get(last);
				where.set(index, lastElt);
				ElementId lastId = elements.get(lastElt.getId());
				lastId.where = where;
				lastId.index = index;
				lastElt.reindex(index);
			}

			where.remove(last);
			
			index = -1;
			where = null;
			me.reindex(-1);
			
			return me;
		}
		
		/**
		 * Remove from the ev array.
		 */
		protected void removeFromEvents() {
			int last = ev.size()-1;
			
			if(eventIndex < last) {
				ElementId lastId = ev.get(last);
				ev.set(eventIndex, lastId);
				lastId.eventIndex = eventIndex;
			}
			ev.remove(last);
			
			events = null;
			eventIndex = -1;
		}
		
		/**
		 * Add an event on the element.
		 * @param group The element style group.
		 * @param event The event.
		 */
		public void addEvent(StyleGroup group, String event) {
			if(events == null) {
				events     = new ElementEvents(get(), group, event);
				eventIndex = ev.size();
				
				ev.add(this);
			} else {
				events.pushEvent(event);
			}
		}
		
		/**
		 * Remove an event on the element, if this is the last event, the whole set of event is
		 * cleaned.
		 * @param event The event to remove.
		 */
		public void removeEvent(String event) {
			if (events != null) {
				events.popEvent(event);

				if (events.eventCount() == 0)
					removeFromEvents();
			}
		}
		
		public void setDynamic() {
			if(where != dyn) {
				AbstractGraphicElement me = removeFromWhere();
				addToWhere(me, dyn);
			}
		}
		
		public void unsetDynamic() {
			if(where == dyn) {
				AbstractGraphicElement me = removeFromWhere();
				addToWhere(me, bulk);
			}
		}
	}
	
	/**
	 * The group unique identifier.
	 */
	protected String id;

	/**
	 * The set of style rules.
	 */
	protected ArrayList<Rule> rules = new ArrayList<Rule>();

	/**
	 * Graph elements of this group by id.
	 */
	protected HashMap<String, ElementId> elements = new HashMap<String, ElementId>();

	/**
	 * Graph elements of this group by index, excepted the dynamic ones in {@link #dyn}.
	 * This field is entirely handled by the {@link ElementId} class.
	 */
	protected ArrayList<AbstractGraphicElement> bulk = new ArrayList<AbstractGraphicElement>();
	
	/**
	 * Dynamic graph elements of this group by index (not the ones in {@link #bulk}).
	 * This field is entirely handled by the {@link ElementId} class.
	 */
	protected ArrayList<AbstractGraphicElement> dyn = new ArrayList<AbstractGraphicElement>();
	
	/**
	 * Elements having an event on them, they may also be in {@link #bulk} or {@link #dyn}.
	 * This field is entirely handled by the {@link ElementId} class.
b	 */
	protected ArrayList<ElementId> ev = new ArrayList<ElementId>();
	
	/**
	 * The global events actually occurring.
	 */
	protected StyleGroupSet.EventSet eventSet;

	/**
	 * A set of events actually pushed only for this group.
	 */
	protected String[] curEvents;

	/**
	 * Associated renderers.
	 */
	public HashMap<String, SwingElementRenderer> renderers;

	/**
	 * New style group for a first graph element and the set of style rules that
	 * matches it. More graph elements can be added later.
	 * 
	 * @param identifier
	 *            The unique group identifier (see
	 *            {@link org.graphstream.ui.graphicGraph.stylesheet.StyleSheet#getStyleGroupIdFor(Element, ArrayList)}
	 *            ).
	 * @param rules
	 *            The set of style rules for the style group (see
	 *            {@link org.graphstream.ui.graphicGraph.stylesheet.StyleSheet#getRulesFor(Element)}
	 *            ).
	 * @param firstElement
	 *            The first element to construct the group.
	 */
	public StyleGroup(String identifier, Collection<Rule> rules,
			AbstractGraphicElement firstElement, StyleGroupSet.EventSet eventSet) {
		this.id       = identifier;
		this.values   = null; // To avoid consume memory since this style will not store anything.
		this.eventSet = eventSet;
		this.elements = new HashMap<String, ElementId>();
		this.bulk     = new ArrayList<AbstractGraphicElement>();
		this.dyn      = new ArrayList<AbstractGraphicElement>();
		this.ev       = new ArrayList<ElementId>();

		this.rules.addAll(rules);
		addElement(firstElement);

		for (Rule rule : rules)
			rule.addGroup(identifier);
	}

	/**
	 * The group unique identifier.
	 * 
	 * @return A style group identifier.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Type of graph element concerned by this style (node, edge, sprite,
	 * graph).
	 * 
	 * @return The type of the style group elements.
	 */
	public Selector.Type getType() {
		return rules.get(0).selector.type;
	}

	/**
	 * True if at least one of the style properties is dynamic (set according to
	 * an attribute of the element to draw). Such elements cannot therefore be
	 * drawn in a group operation, but one by one.
	 * 
	 * @return True if one property is dynamic.
	 */
	public boolean hasDynamicElements() {
		return(dyn.size() > 0);
	}

	/**
	 * If true this group contains some elements that are actually changed by an
	 * event. Such elements cannot therefore be drawn in a group operation, but
	 * one by one.
	 * 
	 * @return True if the group contains some elements changed by an event.
	 */
	public boolean hasEventElements() {
		return(ev.size()>0);
	}

	/**
	 * True if the given element actually has active events.
	 * 
	 * @param element
	 *            The element to test.
	 * @return True if the element has actually active events.
	 */
	public boolean elementHasEvents(AbstractGraphicElement element) {
		return (elements.get(element.getId()).events != null);
	}

	/**
	 * True if the given element has dynamic style values provided by specific
	 * attributes.
	 * 
	 * @param element
	 *            The element to test.
	 * @return True if the element has actually specific style attributes.
	 */
	public boolean elementIsDynamic(AbstractGraphicElement element) {
		return ((elements.get(element.getId())).where == dyn);
	}

	/**
	 * Get the value of a given property.
	 * 
	 * This is a redefinition of the method in {@link Style} to consider the
	 * fact a style group aggregates several style rules.
	 * 
	 * @param property
	 *            The style property the value is searched for.
	 */
	@Override
	public Object getValue(String property, String... events) {
		int n = rules.size();

		if (events == null || events.length == 0) {
			if (curEvents != null && curEvents.length > 0) {
				events = curEvents;
			} else if (eventSet.events != null && eventSet.events.length > 0) {
				events = eventSet.events;
			}
		}

		for (int i = 1; i < n; i++) {
			Style style = rules.get(i).getStyle();

			if (style.hasValue(property, events))
				return style.getValue(property, events);
		}

		return rules.get(0).getStyle().getValue(property, events);
	}

	/**
	 * True if there are no elements in the group.
	 * 
	 * @return True if the group is empty of elements.
	 */
	public boolean isEmpty() {
		return elements.isEmpty();
	}

	/**
	 * True if the group contains the element whose identifier is given.
	 * 
	 * @param elementId
	 *            The element to search.
	 * @return true if the element is in the group.
	 */
	public boolean contains(String elementId) {
		return elements.containsKey(elementId);
	}

	/**
	 * True if the group contains the element given.
	 * 
	 * @param element
	 *            The element to search.
	 * @return true if the element is in the group.
	 */
	public boolean contains(AbstractGraphicElement element) {
		return elements.containsKey(element.getId());
	}

	/**
	 * Return an element of the group, knowing its identifier.
	 * 
	 * @param id
	 *            The searched element identifier.
	 * @return The element corresponding to the identifier or null if not found.
	 */
	public AbstractGraphicElement getElement(String id) {
		ElementId eid = elements.get(id);
		
		if(eid != null)
			return eid.get();
		
		return null;
	}

	/**
	 * The number of elements of the group.
	 * 
	 * @return The element count.
	 */
	public int getElementCount() {
		return elements.size();
	}
	
	public Iterable<? extends AbstractGraphicElement> elements() {
		return new Iterable<AbstractGraphicElement>() {
			public Iterator<AbstractGraphicElement> iterator() {
				return new ElementIterator(elements.values().iterator());
			}
		};
	}
	
	@SuppressWarnings("all")
	public Iterator<AbstractGraphicElement> iterator() {
		return (Iterator<AbstractGraphicElement>)elements().iterator();
	}

	/**
	 * Iterable set of elements that can be drawn in a bulk operation, that is
	 * the subset of all elements that are not dynamic or modified by an event.
	 * 
	 * @return The iterable set of bulk elements.
	 */
	public Iterable<? extends Element> bulkElements() {
		return bulk;
	}
	
	/**
	 * Subset of elements that are actually modified by one or more events. The
	 * {@link ElementEvents} class contains the element and an array of events
	 * that can be pushed on the style group set.
	 * 
	 * @return The subset of elements modified by one or more events.
	 */
	public Iterable<ElementEvents> elementsEvents() {
		return new Iterable<ElementEvents>() {
			public Iterator<ElementEvents> iterator() {
				return new ElementEventsIterator(ev.iterator());
			}
		};
	}

	/**
	 * Subset of elements that have dynamic style values and therefore must be
	 * rendered one by one, not in groups like others. Even though elements
	 * style can specify some dynamics, the elements must individually have
	 * attributes that specify the dynamic value. If the elements do not have
	 * these attributes they can be rendered in bulk operations.
	 * 
	 * @return The subset of dynamic elements of the group.
	 */
	public Iterable<AbstractGraphicElement> dynamicElements() {
		return dyn;
	}

	/**
	 * The associated renderers.
	 * 
	 * @return A renderer or null if not found.
	 */
	public SwingElementRenderer getRenderer(String id) {
		if (renderers != null)
			return renderers.get(id);

		return null;
	}

	/**
	 * Set of events for a given element or null if the element has not
	 * currently occurring events.
	 * 
	 * @return A set of events or null if none occurring at that time.
	 */
	public ElementEvents getEventsFor(AbstractGraphicElement element) {
		return elements.get(element.getId()).events;
	}

	/**
	 * Test if an element is pushed as dynamic.
	 */
	public boolean isElementDynamic(AbstractGraphicElement element) {
		return (elements.get(element.getId()).where == dyn);
	}

	/**
	 * Add a new graph element to the group.
	 * 
	 * @param element
	 *            The new graph element to add.
	 */
	public void addElement(AbstractGraphicElement element) {
		if(element.getIndex() < 0) {
			elements.put(element.getId(), new ElementId(element, bulk));
		} else {
			throw new RuntimeException(String.format("Cannot add element %s to style group, element already as an index %s in another group.", element.getId(), element.getIndex()));
		}
	}

	/**
	 * Remove a graph element from the group.
	 * 
	 * @param element
	 *            The element to remove.
	 * @return The removed element, or null if the element was not found.
	 */
	public AbstractGraphicElement removeElement(AbstractGraphicElement element) {
		ElementId id = elements.remove(element.getId());
		
		if(id != null) {
			id.remove();
		}
		
		return element;
	}

	/**
	 * Push an event specifically for the given element. Events are stacked in
	 * order. Called by the GraphicElement.
	 * 
	 * @param element
	 *            The element to modify with an event.
	 * @param event
	 *            The event to push.
	 */
	protected void pushEventFor(AbstractGraphicElement element, String event) {
		ElementId id = elements.get(element.getId());
		
		if(id != null) {
			id.addEvent(this, event);
		} else {
			throw new RuntimeException(String.format("cannot push event %s for unknown element %s", event, element.getId()));
		}
	}

	/**
	 * Pop an event for the given element. Called by the GraphicElement.
	 * 
	 * @param element
	 *            The element.
	 * @param event
	 *            The event.
	 */
	protected void popEventFor(AbstractGraphicElement element, String event) {
		ElementId id = elements.get(element.getId());
		
		if(id != null) {
			id.removeEvent(event);
		}
	}

	/**
	 * Before drawing an element that has events, use this method to activate
	 * the events, the style values will be modified accordingly. Events for
	 * this element must have been registered via
	 * {@link #pushEventFor(AbstractGraphicElement, String)}. After rendering the
	 * {@link #deactivateEvents()} MUST be called.
	 * 
	 * @param element
	 *            The element to push events for.
	 */
	public void activateEventsFor(AbstractGraphicElement element) {
		ElementId id = elements.get(element.getId());
		
		if(id.events != null && curEvents == null) {
			curEvents = id.events.events();
		}
	}

	/**
	 * De-activate any events activated for an element. This method MUST be
	 * called if {@link #activateEventsFor(AbstractGraphicElement)} has been called.
	 */
	public void deactivateEvents() {
		curEvents = null;
	}

	/**
	 * Indicate the element has dynamic values and thus cannot be drawn in bulk
	 * operations. Called by the GraphicElement.
	 * 
	 * @param element
	 *            The element.
	 */
	protected void pushElementAsDynamic(AbstractGraphicElement element) {
		ElementId id = elements.get(element.getId());
		
		if(id != null) {
			id.setDynamic();
		} else {
			throw new RuntimeException(String.format("cannot push unknown element %s as dynamic", element.getId()));
		}
	}

	/**
	 * Indicate the element has no more dynamic values and can be drawn in bulk
	 * operations. Called by the GraphicElement.
	 * 
	 * @param element
	 *            The element.
	 */
	protected void popElementAsDynamic(AbstractGraphicElement element) {
		ElementId id = elements.get(element.getId());
		
		if(id != null) {
			id.unsetDynamic();
		}
	}

	/**
	 * Remove all graph elements of this group, and remove this group from the
	 * group list of each style rule.
	 */
	public void release() {
		for (Rule rule : rules)
			rule.removeGroup(id);

		for(ElementId id: elements.values()) {
			id.remove();
		}
		
		elements.clear();
		bulk.clear();
		dyn.clear();
		ev.clear();
	}

	/**
	 * Redefinition of the {@link Style} to forbid changing the values.
	 */
	@Override
	public void setValue(String property, Object value) {
		throw new RuntimeException(
				"you cannot change the values of a style group.");
	}

	/**
	 * Add a renderer to this group.
	 * 
	 * @param id
	 *            The renderer identifier.
	 * @param renderer
	 *            The renderer.
	 */
	public void addRenderer(String id, SwingElementRenderer renderer) {
		if (renderers == null)
			renderers = new HashMap<String, SwingElementRenderer>();

		renderers.put(id, renderer);
	}

	/**
	 * Remove a renderer.
	 * 
	 * @param id
	 *            The renderer identifier.
	 * @return The removed renderer or null if not found.
	 */
	public SwingElementRenderer removeRenderer(String id) {
		return renderers.remove(id);
	}

	@Override
	public String toString() {
		return toString(-1);
	}

	@Override
	public String toString(int level) {
		StringBuilder builder = new StringBuilder();
		String prefix = "";
		String sprefix = "    ";

		for (int i = 0; i < level; i++)
			prefix += sprefix;

		builder.append(String.format("%s%s%n", prefix, id));
		builder.append(String.format("%s%sContains : ", prefix, sprefix));

		for (ElementId id : elements.values()) {
			builder.append(String.format("%s ", id.get().getId()));
		}

		builder.append(String.format("%n%s%sStyle : ", prefix, sprefix));

		for (Rule rule : rules) {
			builder.append(String.format("%s ", rule.selector.toString()));
		}

		builder.append(String.format("%n"));

		return builder.toString();
	}

	// Nested classes

	/**
	 * Description of an element that is actually modified by one or more events
	 * occurring on it.
	 */
	public static class ElementEvents {
		/**
		 * Set of events on the element.
		 */
		protected String events[];

		/**
		 * The element.
		 */
		protected AbstractGraphicElement element;

		/**
		 * The group the element pertains to.
		 */
		protected StyleGroup group;

		// Construction

		protected ElementEvents(AbstractGraphicElement element, StyleGroup group, String event) {
			this.element = element;
			this.group = group;
			this.events = new String[1];

			events[0] = event;
		}

		// Access

		/**
		 * The element on which the events are occurring.
		 * 
		 * @return an element.
		 */
		public Element getElement() {
			return element;
		}

		/**
		 * Number of events actually affecting the element.
		 * 
		 * @return The number of events affecting the element.
		 */
		public int eventCount() {
			if (events == null)
				return 0;

			return events.length;
		}

		/**
		 * The set of events actually occurring on the element.
		 * 
		 * @return A set of strings.
		 */
		public String[] events() {
			return events;
		}

		// Command

		public void activate() {
			group.activateEventsFor(element);
		}

		public void deactivate() {
			group.deactivateEvents();
		}

		protected void pushEvent(String event) {
			int n = events.length + 1;
			String e[] = new String[n];
			boolean found = false;

			for (int i = 0; i < events.length; i++) {
				if (!events[i].equals(event))
					e[i] = events[i];
				else
					found = true;
			}

			e[events.length] = event;

			if (!found)
				events = e;
		}

		protected void popEvent(String event) {
			if (events.length > 1) {
				String e[] = new String[events.length - 1];
				boolean found = false;

				for (int i = 0, j = 0; i < events.length; i++) {
					if (!events[i].equals(event)) {
						if (j < e.length) {
							e[j++] = events[i];
						}
					} else {
						found = true;
					}
				}

				if (found)
					events = e;
			} else {
				if (events[0].equals(event)) {
					events = null;
				}
			}
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();

			builder.append(String.format("%s events {", element.getId()));
			for (String event : events)
				builder.append(String.format(" %s", event));
			builder.append(" }");

			return builder.toString();
		}
	}

	class ElementIterator implements Iterator<AbstractGraphicElement> {
		Iterator<ElementId> it;
		ElementIterator(Iterator<ElementId> it) {
			this.it = it;
		}
		public boolean hasNext() {
			return it.hasNext();
		}
		public AbstractGraphicElement next() {
			return it.next().get();
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	class ElementEventsIterator implements Iterator<ElementEvents> {
		Iterator<ElementId> it;
		ElementEventsIterator(Iterator<ElementId> it) {
			this.it = it;
		}
		public boolean hasNext() {
			return it.hasNext();
		}
		public ElementEvents next() {
			return it.next().events;
		}
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}