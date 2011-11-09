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
package org.graphstream.ui.graphicGraph;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;

import org.graphstream.graph.Edge;
import org.graphstream.graph.EdgeFactory;
import org.graphstream.graph.Element;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.NodeFactory;
import org.graphstream.graph.ElementNotFoundException;
import org.graphstream.graph.IdAlreadyInUseException;
import org.graphstream.graph.implementations.AbstractElement;
import org.graphstream.stream.AttributeSink;
import org.graphstream.stream.ElementSink;
import org.graphstream.stream.Sink;
import org.graphstream.stream.SourceBase;
import org.graphstream.stream.SourceBase.ElementType;
import org.graphstream.stream.file.FileSink;
import org.graphstream.stream.file.FileSource;
import org.graphstream.stream.sync.SinkTime;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.stylesheet.Style;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.StyleSheet;
import org.graphstream.ui.graphicGraph.stylesheet.Value;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;

/**
 * Graph representation used in display classes.
 * 
 * <p>
 * <em>Warning</em>: This class is NOT a general graph class, and it should NOT be used as it.
 * This class is particularly dedicated to fast drawing of the graph and is internally
 * arranged to be fast for this task only. It implements graph solely to be easily susceptible
 * to be used as a sink and source for graph events. Some of the common methods
 * of the Graph interface are not functional and will throw an exception if
 * used (as documented in their respective JavaDoc).
 * </p>
 * 
 * <p>
 * The main goal of the graphic graph is to allows fast drawing of a graph.
 * </p>
 * 
 * <p>
 * Its purpose is therefore to represent a graph with some often used
 * graphic attributes (like position, label, etc.) stored as fields in the nodes
 * and edges and a new kind of element: sprites. Each of these element also owns
 * a styles pertaining to a style sheet that tries to imitate the way CSS works.
 * </p>
 *
 * <p>
 * The graphic graph does not completely duplicate a graph, it only
 * store things that are useful for drawing it. Although it implements "Graph",
 * some methods are not implemented and will throw a runtime exception. These
 * methods are mostly utility methods like write(), read(), and naturally
 * display().
 * </p>
 * 
 * <h2>Style</h2>
 * 
 * <p>
 * The style sheet is uploaded on the graph using an attribute correspondingly
 * named "stylesheet" or "ui.stylesheet" (the second one is better). It can be
 * a string that contains the whole style sheet, or an URL of the form :
 * </p>
 * 
 * <pre>
 * url(name)
 * </pre>
 * 
 * <h2>Attributes</h2>
 * 
 * <p>
 * The graphic graph has the ability to store attributes like any other graph
 * element, however the attributes stored by the graphic graph are restricted.
 * There is a filter on the attribute adding methods that let pass only:
 * <ul>
 * 		<li>All attributes starting with "ui.".</li>
 * 		<li>The "x", "y", "z", "xy" and "xyz" attributes.</li>
 * 		<li>The "stylesheet" attribute (although "ui.stylesheet" is preferred).</li>
 * 		<li>The "label" attribute.</li>
 * </ul>
 * All other attributes are filtered and not stored. The result is that if the
 * graphic graph is used as an input (a source of graph events) some attributes
 * will not pass through the filter.
 * </p>
 * 
 * <h2>Internal representation</h2>
 * 
 * <p>
 * The implementation of this graph relies on the {@link StyleGroupSet} class and this
 * is indeed its way to store its elements (grouped by style and Z level).
 * </p>
 * 
 * <p>
 * In addition to this, it provides, as all graphs do, the relational information
 * for edges.
 * </p>
 * 
 * <p>
 * This graph defines specific factories for nodes and edges that create GraphicNode,
 * and GraphicEdge instances. These factories cannot be changed. In addition, the
 * graph handles the specific "ui.sprite." attributes to generate GraphicSprite
 * instances that allow to easily handle sprites. In this graph, sprites are elements
 * like nodes and edges.
 * </p>
 * 
 * <p>
 * To ease redrawing, the graph handles a "graphChanged" flag that is set to true
 * at each change in the structure that would require a redraw. You an query this
 * flag using {@link #graphChangedFlag()}, and reset it to false using
 * {@link #resetGraphChangedFlag()}.
 * </p>
 * 
 * <p>
 * The graph is also able to compute its lower and upper bounds in graph units, that
 * is, the coordinates of the top-left-front (lower bound) and upper-right-back (upper
 * bound) node or sprite. You can use the {@link #computeBounds()} method to refresh
 * this information, and you can access the lower bound using {@link #getMinPos()} and
 * the upper bound with {@link #getMaxPos()}.
 * </p>
 */
public class GraphicGraph extends AbstractElement implements Graph,
		StyleGroupListener {
	/**
	 * Set of styles.
	 */
	protected StyleSheet styleSheet;

	/**
	 * Associate graphic elements with styles.
	 */
	protected StyleGroupSet styleGroups;

	/**
	 * Connectivity.  The way nodes are connected one with another via edges. The map is sorted
	 * by node. For each node an array of edges lists the connectivity.
	 */
	protected HashMap<GraphicNode, ArrayList<GraphicEdge>> connectivity;

	/**
	 * The style of this graph. This is a shortcut to avoid searching it in the style sheet.
	 */
	protected StyleGroup style;

	/**
	 * Memorize the step events.
	 */
	protected double step = 0;

	/**
	 * Dirty bit, set to true each time the graph was modified internally and a redraw is needed,
	 * this flag is controlled by this class as well as {@link GraphicNode}, {@link GraphicEdge}
	 * and {@link GraphicSprite}.
	 */
	protected boolean graphChanged;

	/**
	 * Dirty bit, set to true each time a sprite or node moved, this avoids recomputing bounds
	 * endlessly. This flag is controlled by the {@link GraphicSprite} and the {@link GraphicNode}
	 * classes.
	 */
	protected boolean boundsChanged = true;

	/**
	 * Maximum position of a node or sprite in the graphic graph. Computed by
	 * {@link #computeBounds()}.
	 */
	protected Point3 hi = new Point3();

	/**
	 * Minimum position of a node or sprite in the graphic graph. Computed by
	 * {@link #computeBounds()}.
	 */
	protected Point3 lo = new Point3();

	/**
	 * Set of listeners of this graph.
	 */
	protected GraphListeners listeners;

	/**
	 * Time of other known sources.
	 */
	protected SinkTime sinkTime = new SinkTime();
	
	/**
	 * Are null attributes access an error ?
	 */
	protected boolean nullAttrError = false;

	/*
	 * XXX Probably remove this XXX 
	 * 
	 * Report back the XYZ events on nodes and sprites? If enabled, each change in the position
	 * of nodes and sprites will be sent to potential listeners of the graph. By default this is
	 * disabled. Be careful ! Do NOT enable this field if the graph receives the "xyz" attributes
	 * from something else than a layout directly connected to it (if graph.display(false) was
	 * called for example). This field is here to ask the
	 * graph to change a "xyz" attribute if its nodes are moved directly (if graph.display(true)
	 * was called for example).
	 */
	protected boolean feedbackXYZ = true;

	/**
	 * The set of listeners for this graph. 
	 */
	protected class GraphListeners extends SourceBase {
		public GraphListeners(String id, SinkTime sinkTime) {
			super(id);
			sourceTime.setSinkTime(sinkTime);
		}

		public long newEvent() {
			return sourceTime.newEvent();
		}
	};

	/**
	 * New empty graphic graph.
	 * 
	 * A default style sheet is created, it then can be "cascaded" with other
	 * style sheets.
	 */
	public GraphicGraph(String id) {
		super(id);

		listeners = new GraphListeners(id, sinkTime);
		styleSheet = new StyleSheet();
		styleGroups = new StyleGroupSet(styleSheet);
		connectivity = new HashMap<GraphicNode, ArrayList<GraphicEdge>>();

		styleGroups.addListener(this);
		styleGroups.addElement(this); // Add style to this graph.

		style = styleGroups.getStyleFor(this);
	}

	// Access

	@Override
	protected String myGraphId() // XXX
	{
		return getId();
	}

	@Override
	protected long newEvent() // XXX
	{
		return listeners.newEvent();
	}

	/**
	 * Dirty bit, set to true if the graph was edited or changed in any way since the last reset
	 * of the "changed" flag and needs a repaint.
	 * 
	 * @return true if the graph was changed and needs a repaint.
	 */
	public boolean graphChangedFlag() {
		return graphChanged;
	}

	/**
	 * Reset the dirty bit flag. Renderers call this method after having fully rendered the graphic
	 * graph. If the flag is not reset, no need to repaint the graph.
	 * 
	 * @see #graphChangedFlag()
	 */
	public void resetGraphChangedFlag() {
		graphChanged = false;
	}

	/**
	 * The style sheet. This style sheet is the result of the "cascade" or
	 * accumulation of styles added via attributes of the graph.
	 * 
	 * @return A style sheet.
	 */
	public StyleSheet getStyleSheet() {
		return styleSheet;
	}

	/**
	 * The graph style group.
	 * 
	 * @return A style group.
	 */
	public StyleGroup getStyle() {
		return style;
	}

	/**
	 * The complete set of style groups.
	 * 
	 * @return The style groups.
	 */
	public StyleGroupSet getStyleGroups() {
		return styleGroups;
	}

	@Override
	public String toString() {
		return String.format("[%s %d nodes %d edges]", getId(), getNodeCount(),
				getEdgeCount());
	}

	public double getStep() {
		return step;
	}

	/**
	 * The maximum position of a node or sprite. Notice that this is updated
	 * only each time the {@link #computeBounds()} method is called.
	 * 
	 * @return The maximum node or sprite position.
	 */
	public Point3 getMaxPos() {
		return hi;
	}

	/**
	 * The minimum position of a node or sprite. Notice that this is updated
	 * only each time the {@link #computeBounds()} method is called.
	 * 
	 * @return The minimum node or sprite position.
	 */
	public Point3 getMinPos() {
		return lo;
	}

	/*
	 * Does the graphic graph publish via attribute changes the XYZ changes on nodes and sprites
	 * when changed ?. This is disabled by default, and enabled as soon as there is at least one
	 * listener.
	public boolean feedbackXYZ() {
		return feedbackXYZ;
	}
	 */

	// Command

	/*
	 * Should the graphic graph publish via attribute changes the XYZ changes on
	 * nodes and sprites when changed ?.
	 * 
	 * Be very careful with this setting. If some part of the pipeline is already
	 * in charge of publishing "xyz" attributes (in the sources this graph is sink of), this
	 * graph will propagate them as usual to its sink, no need to enable feedback). However
	 * sometimes the nodes are moved directly by a special process (a layout for example) that
	 * use the "move" methods of the GraphicNode and GraphicSprite classes. In this case, no
	 * "xyz" attribute never passes in the event stream, so you can enable this flag to allow
	 * the graph to produce them.
	public void feedbackXYZ(boolean on) {
		feedbackXYZ = on;
	}
	 */

	/**
	 * Compute the overall bounds of the graphic graph according to the nodes
	 * and sprites positions.
	 * 
	 * <p>
	 * We can only compute the graph bounds from the
	 * nodes and sprites centers to avoid an endless recursive call to this method,
	 * since the node and sprite sizes may in certain circumstances be computed
	 * according to the graph bounds. The bounds are stored in the graph metrics.
	 * </p>
	 * 
	 * <p>
	 * This operation will process each node and sprite and is therefore costly.
	 * However it does this computation again only when a node or sprite moved,
	 * according to an internal flag set by each {@link GraphicNode} or
	 * {@link GraphicSprite} when then move. Therefore it can be called several times,
	 * if nothing moved in the graph, the computation will not be redone.
	 * </p>
	 * 
	 * @see #getMaxPos()
	 * @see #getMinPos()
	 */
	public void computeBounds() {
		if (boundsChanged) {
			lo.x = lo.y = lo.z = 10000000; // A bug with Float.MAX_VALUE during
											// comparisons ?
			hi.x = hi.y = hi.z = -10000000; // A bug with Float.MIN_VALUE during
											// comparisons ?

			for (Node n : getEachNode()) {
				GraphicNode node = (GraphicNode) n;
				Point3 pos = node.center;

				if(!node.hidden && node.positioned) {
					if (pos.x < lo.x)
						lo.x = pos.x;
					else if (pos.x > hi.x)
						hi.x = pos.x;
					if (pos.y < lo.y)
						lo.y = pos.y;
					else if (pos.y > hi.y)
						hi.y = pos.y;
					if (pos.z < lo.z)
						lo.z = pos.z;
					else if (pos.z > hi.z)
						hi.z = pos.z;
				}
			}

			for (GraphicSprite sprite : spriteSet()) {
				if (!sprite.isAttached()
						&& sprite.getUnits() == StyleConstants.Units.GU) {
					Point3 pos = sprite.center;

					if(!sprite.hidden) {
						if (pos.x < lo.x)
							lo.x = pos.x;
						else if (pos.x > hi.x)
							hi.x = pos.x;
						if (pos.y < lo.y)
							lo.y = pos.y;
						else if (pos.y > hi.y)
							hi.y = pos.y;
						if (pos.z < lo.z)
							lo.z = pos.z;
						else if (pos.z > hi.z)
							hi.z = pos.z;
					}
				}
			}

			boundsChanged = false;
		}
	}

	protected GraphicEdge addEdge(String sourceId, long timeId, String id,
			String from, String to, boolean directed,
			HashMap<String, Object> attributes) {
		GraphicEdge edge = (GraphicEdge) styleGroups.getEdge(id);

		if (edge == null) {
			GraphicNode n1 = (GraphicNode) styleGroups.getNode(from);
			GraphicNode n2 = (GraphicNode) styleGroups.getNode(to);

			if (n1 == null || n2 == null)
				throw new RuntimeException(
						"org.miv.graphstream.ui.graphicGraph.GraphicGraph.addEdge() : ERROR : one of the nodes does not exist");

			edge = new GraphicEdge(id, n1, n2, directed, attributes);

			styleGroups.addElement(edge);

			ArrayList<GraphicEdge> l1 = connectivity.get(n1);
			ArrayList<GraphicEdge> l2 = connectivity.get(n2);

			if (l1 == null) {
				l1 = new ArrayList<GraphicEdge>();
				connectivity.put(n1, l1);
			}

			if (l2 == null) {
				l2 = new ArrayList<GraphicEdge>();
				connectivity.put(n2, l2);
			}

			l1.add(edge);
			l2.add(edge);
			edge.countSameEdges(l1);

			graphChanged = true;

			listeners.sendEdgeAdded(sourceId, timeId, id, from, to, directed);
		}

		return edge;
	}

	protected GraphicNode addNode(String sourceId, long timeId, String id,
			HashMap<String, Object> attributes) {
		GraphicNode node = (GraphicNode) styleGroups.getNode(id);

		if (node == null) {
			node = new GraphicNode(this, id, attributes);

			styleGroups.addElement(node);

			graphChanged = true;

			listeners.sendNodeAdded(sourceId, timeId, id);
		}

		return node;
	}

	/**
	 * Force a node to move at a new position.
	 * @param id The node identifier.
	 * @param x The new abscissa.
	 * @param y The new ordinate.
	 * @param z The new depth.
	 */
	protected void moveNode(String id, double x, double y, double z) {
		GraphicNode node = (GraphicNode) styleGroups.getNode(id);

		if (node != null) {
			node.move(x, y, z);
//			node.x = x;
//			node.y = y;
//			node.z = z;
//			node.addAttribute("x", x);
//			node.addAttribute("y", y);
//			node.addAttribute("z", z);

			graphChanged = true;
		}
	}

	public Edge removeEdge(String sourceId, long timeId, String id)
			throws ElementNotFoundException {
		GraphicEdge edge = (GraphicEdge) styleGroups.getEdge(id);

		if (edge != null) {
			listeners.sendEdgeRemoved(sourceId, timeId, id);

			if (connectivity.get(edge.from) != null)
				connectivity.get(edge.from).remove(edge);
			if (connectivity.get(edge.to) != null)
				connectivity.get(edge.to).remove(edge);

			styleGroups.removeElement(edge);
			edge.removed();

			graphChanged = true;
		}

		return edge;
	}

	public Edge removeEdge(String sourceId, long timeId, String from, String to)
			throws ElementNotFoundException {
		GraphicNode node0 = (GraphicNode) styleGroups.getNode(from);
		GraphicNode node1 = (GraphicNode) styleGroups.getNode(to);

		if (node0 != null && node1 != null) {
			ArrayList<GraphicEdge> edges0 = connectivity.get(node0);
			ArrayList<GraphicEdge> edges1 = connectivity.get(node1);

			for (GraphicEdge edge0 : edges0) {
				for (GraphicEdge edge1 : edges1) {
					if (edge0 == edge1) {
						removeEdge(sourceId, timeId, edge0.getId());
						return edge0;
					}
				}
			}
		}

		return null;
	}

	public Node removeNode(String sourceId, long timeId, String id) {
		GraphicNode node = (GraphicNode) styleGroups.getNode(id);

		if (node != null) {
			listeners.sendNodeRemoved(sourceId, timeId, id);

			if (connectivity.get(node) != null) {
				// We must do a copy of the connectivity set for the node
				// since we will be modifying the connectivity as we process
				// edges.
				ArrayList<GraphicEdge> l = new ArrayList<GraphicEdge>(
						connectivity.get(node));

				for (GraphicEdge edge : l)
					removeEdge(sourceId, newEvent(), edge.getId());

				connectivity.remove(node);
			}

			styleGroups.removeElement(node);
			node.removed();

			graphChanged = true;
		}

		return node;
	}

	@SuppressWarnings("unchecked")
	public <T extends Node> T getNode(String id) {
		return (T) styleGroups.getNode(id);
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> T getEdge(String id) {
		return (T) styleGroups.getEdge(id);
	}

	public GraphicSprite getSprite(String id) {
		return styleGroups.getSprite(id);
	}

	@Override
	protected void attributeChanged(String sourceId, long timeId,
			String attribute, AttributeChangeEvent event, Object oldValue,
			Object newValue) {

		// One of the most important method. Most of the communication comes
		// from attributes.

		if (attribute.equals("ui.stylesheet") || attribute.equals("stylesheet")) {
			if (event == AttributeChangeEvent.ADD
					|| event == AttributeChangeEvent.CHANGE) {
				if (newValue instanceof String) {
					try {
						styleSheet.load((String) newValue);
						graphChanged = true;
					} catch (IOException e) {
						System.err
								.printf("Error while parsing style sheet for graph '%s' : %n",
										getId());
						if (((String) newValue).startsWith("url"))
							System.err.printf("    %s%n", ((String) newValue));
						System.err.printf("    %s%n", e.getMessage());
					}
				} else {
					System.err
							.printf("Error with stylesheet specification what to do with '%s' ?%n",
									newValue);
				}
			} else // Remove the style.
			{
				styleSheet.clear();
				graphChanged = true;
			}
		} else if (attribute.startsWith("ui.sprite.")) {
			// Defers the sprite handling to the sprite API.
			spriteAttribute(event, null, attribute, newValue);
			graphChanged = true;
		}

		listeners.sendAttributeChangedEvent(sourceId, timeId, getId(),
				ElementType.GRAPH, attribute, event, oldValue, newValue);
	}

	public void clear(String sourceId, long timeId) {
		listeners.sendGraphCleared(sourceId, timeId);
		connectivity.clear();
		styleGroups.clear();

		step = 0;
		graphChanged = true;
	}

	/**
	 * Display the node/edge relations, for debugging purposes.
	 */
	public void printConnectivity() {
		Iterator<GraphicNode> keys = connectivity.keySet().iterator();

		System.err.printf("Graphic graph connectivity:%n");

		while (keys.hasNext()) {
			GraphicNode node = keys.next();
			System.err.printf("    [%s] -> ", node.getId());
			ArrayList<GraphicEdge> edges = connectivity.get(node);
			for (GraphicEdge edge : edges)
				System.err.printf(" (%s %d)", edge.getId(),
						edge.getMultiIndex());
			System.err.printf("%n");
		}
	}

	// Style group listener interface

	public void elementStyleChanged(Element element, StyleGroup oldStyle,
			StyleGroup style) {
		if (element instanceof GraphicElement) {
			GraphicElement ge = (GraphicElement) element;
			ge.changeStyle(style);
			graphChanged = true;
		} else if (element instanceof GraphicGraph) {
			GraphicGraph gg = (GraphicGraph) element;
			gg.style = style;
			graphChanged = true;
		} else {
			throw new RuntimeException("WTF ?");
		}
	}

	public void styleChanged(StyleGroup style) {
		
	}

	// Graph interface

	public Iterable<? extends Edge> getEachEdge() {
		return styleGroups.edges();
	}

	public Iterable<? extends Node> getEachNode() {
		return styleGroups.nodes();
	}

	@SuppressWarnings("all")
	public <T extends Node> Collection<T> getNodeSet() {
		return new AbstractCollection<T>() {
			public Iterator<T> iterator() {
				return getNodeIterator();
			}

			public int size() {
				return getNodeCount();
			}
		};
	}

	@SuppressWarnings("all")
	public <T extends Edge> Collection<T> getEdgeSet() {
		return new AbstractCollection<T>() {
			public Iterator<T> iterator() {
				return getEdgeIterator();
			}

			public int size() {
				return getNodeCount();
			}
		};
	}

	@SuppressWarnings("unchecked")
	public Iterator<Node> iterator() {
		return (Iterator<Node>) styleGroups.getNodeIterator();
	}

	public void addSink(Sink listener) {
		listeners.addSink(listener);
	}

	public void removeSink(Sink listener) {
		listeners.removeSink(listener);
	}

	public void addAttributeSink(AttributeSink listener) {
		listeners.addAttributeSink(listener);
	}

	public void removeAttributeSink(AttributeSink listener) {
		listeners.removeAttributeSink(listener);
	}

	public void addElementSink(ElementSink listener) {
		listeners.addElementSink(listener);
	}

	public void removeElementSink(ElementSink listener) {
		listeners.removeElementSink(listener);
	}

	public Iterable<AttributeSink> attributeSinks() {
		return listeners.attributeSinks();
	}

	public Iterable<ElementSink> elementSinks() {
		return listeners.elementSinks();
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> T addEdge(String id, String from, String to)
			throws IdAlreadyInUseException, ElementNotFoundException {
		return (T)addEdge(getId(), newEvent(), id, from, to, false, null);
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> T addEdge(String id, String from, String to, boolean directed)
			throws IdAlreadyInUseException, ElementNotFoundException {
		return (T)addEdge(getId(), newEvent(), id, from, to, directed, null);
	}

	@SuppressWarnings("unchecked")
	public <T extends Node> T addNode(String id) throws IdAlreadyInUseException {
		return (T)addNode(getId(), newEvent(), id, null);
	}

	public void clear() {
		clear(getId(), newEvent());
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> T removeEdge(String id) throws ElementNotFoundException {
		return (T)removeEdge(getId(), newEvent(), id);
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> T removeEdge(String from, String to)
			throws ElementNotFoundException {
		return (T)removeEdge(getId(), newEvent(), from, to);
	}

	@SuppressWarnings("unchecked")
	public <T extends Node> T removeNode(String id) throws ElementNotFoundException {
		return (T)removeNode(getId(), newEvent(), id);
	}

	public org.graphstream.ui.swingViewer.Viewer display() {
		throw new RuntimeException("GraphicGraph is used by display() and cannot recursively define display()");
	}

	public org.graphstream.ui.swingViewer.Viewer display(boolean autoLayout) {
		throw new RuntimeException("GraphicGraph is used by display() and cannot recursively define display()");
	}

	public void stepBegins(double step) {
		stepBegins(getId(), newEvent(), step);
	}

	public EdgeFactory<? extends Edge> edgeFactory() {
		throw new RuntimeException("GraphicGraph does not support EdgeFactory");
	}

	public int getEdgeCount() {
		return styleGroups.getEdgeCount();
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> Iterator<T> getEdgeIterator() {
		return (Iterator<T>)styleGroups.getEdgeIterator();
	}

	public int getNodeCount() {
		return styleGroups.getNodeCount();
	}

	public int getSpriteCount() {
		return styleGroups.getSpriteCount();
	}

	@SuppressWarnings("unchecked")
	public <T extends Node> Iterator<T> getNodeIterator() {
		return (Iterator<T>)styleGroups.getNodeIterator();
	}

	public Iterator<? extends GraphicSprite> getSpriteIterator() {
		return styleGroups.getSpriteIterator();
	}

	public Iterable<? extends GraphicSprite> spriteSet() {
		return styleGroups.sprites();
	}

	public boolean isAutoCreationEnabled() {
		return false;
	}

	public NodeFactory<? extends Node> nodeFactory() {
		throw new RuntimeException("GraphicGraph does not support NodeFactory");
	}

	public void setAutoCreate(boolean on) {
		throw new RuntimeException("GraphicGraph does not support auto-creation");
	}

	public boolean isStrict() {
		return false;
	}

	public void setStrict(boolean on) {
		throw new RuntimeException("GraphicGraph does not support strict checking");
	}

	@Override
	public boolean nullAttributesAreErrors() {
		return nullAttrError;
	}
	
	public void setNullAttributesAreErrors(boolean on) {
		nullAttrError = on;
	}
	
	public void setEdgeFactory(EdgeFactory<? extends Edge> ef) {
		throw new RuntimeException(
				"you cannot change the edge factory for graphic graphs !");
	}

	public void setNodeFactory(NodeFactory<? extends Node> nf) {
		throw new RuntimeException(
				"you cannot change the node factory for graphic graphs !");
	}

	public void read(String filename) throws IOException {
		throw new RuntimeException("GraphicGraph does not support I/O");
	}

	public void read(FileSource input, String filename) throws IOException {
		throw new RuntimeException("GraphicGraph does not support I/O");
	}

	public void write(FileSink output, String filename) throws IOException {
		throw new RuntimeException("GraphicGraph does not support I/O");
	}

	public void write(String filename) throws IOException {
		throw new RuntimeException("GraphicGraph does not support I/O");
	}

	// Output interface

	public void edgeAttributeAdded(String sourceId, long timeId, String edgeId,
			String attribute, Object value) {
		if (sinkTime.isNewEvent(sourceId, timeId)) {
			Edge edge = getEdge(edgeId);

			if (edge != null)
				((GraphicEdge) edge).addAttribute_(sourceId, timeId, attribute,
						value);
		}
	}

	public void edgeAttributeChanged(String sourceId, long timeId,
			String edgeId, String attribute, Object oldValue, Object newValue) {
		if (sinkTime.isNewEvent(sourceId, timeId)) {
			Edge edge = getEdge(edgeId);

			if (edge != null)
				((GraphicEdge) edge).changeAttribute_(sourceId, timeId,
						attribute, newValue);
		}
	}

	public void edgeAttributeRemoved(String sourceId, long timeId,
			String edgeId, String attribute) {
		if (sinkTime.isNewEvent(sourceId, timeId)) {
			Edge edge = getEdge(edgeId);

			if (edge != null)
				((GraphicEdge) edge).removeAttribute_(sourceId, timeId,
						attribute);
		}
	}

	public void graphAttributeAdded(String sourceId, long timeId,
			String attribute, Object value) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			addAttribute_(sourceId, timeId, attribute, value);
	}

	public void graphAttributeChanged(String sourceId, long timeId,
			String attribute, Object oldValue, Object newValue) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			changeAttribute_(sourceId, timeId, attribute, newValue);
	}

	public void graphAttributeRemoved(String sourceId, long timeId,
			String attribute) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			removeAttribute_(sourceId, timeId, attribute);
	}

	public void nodeAttributeAdded(String sourceId, long timeId, String nodeId,
			String attribute, Object value) {
		if (sinkTime.isNewEvent(sourceId, timeId)) {
			Node node = getNode(nodeId);

			if (node != null)
				((GraphicNode) node).addAttribute_(sourceId, timeId, attribute,
						value);
		}
	}

	public void nodeAttributeChanged(String sourceId, long timeId,
			String nodeId, String attribute, Object oldValue, Object newValue) {
		if (sinkTime.isNewEvent(sourceId, timeId)) {
			Node node = getNode(nodeId);

			if (node != null)
				((GraphicNode) node).changeAttribute_(sourceId, timeId,
						attribute, newValue);
		}
	}

	public void nodeAttributeRemoved(String sourceId, long timeId,
			String nodeId, String attribute) {
		if (sinkTime.isNewEvent(sourceId, timeId)) {
			Node node = getNode(nodeId);

			if (node != null)
				((GraphicNode) node).removeAttribute_(sourceId, timeId,
						attribute);
		}
	}

	public void edgeAdded(String sourceId, long timeId, String edgeId,
			String fromNodeId, String toNodeId, boolean directed) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			addEdge(sourceId, timeId, edgeId, fromNodeId, toNodeId, directed,
					null);
	}

	public void edgeRemoved(String sourceId, long timeId, String edgeId) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			removeEdge(sourceId, timeId, edgeId);
	}

	public void graphCleared(String sourceId, long timeId) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			clear(sourceId, timeId);
	}

	public void nodeAdded(String sourceId, long timeId, String nodeId) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			addNode(sourceId, timeId, nodeId, null);
	}

	public void nodeRemoved(String sourceId, long timeId, String nodeId) {
		if (sinkTime.isNewEvent(sourceId, timeId))
			removeNode(sourceId, timeId, nodeId);
	}

	public void stepBegins(String sourceId, long timeId, double time) {
		step = time;

		listeners.sendStepBegins(sourceId, timeId, time);
	}

	// Sprite interface

	/**
	 * This method is called when a sprite attribute changed on the graph (or
	 * on a node or edge). It handle the attributes, creating or deleting {@link GraphicSprite}
	 * objects as needed, or updating them to reflect their current state. This
	 * is the main method in charge of handling the set of sprites.
	 */
	protected void spriteAttribute(AttributeChangeEvent event, Element element,
			String attribute, Object value) {

		String spriteId = attribute.substring(10); // Remove the "ui.sprite." prefix.
		int pos = spriteId.indexOf('.'); // Look if there is something after the sprite id.
		String attr = null;

		if (pos > 0) {
			attr = spriteId.substring(pos + 1); // Cut the sprite id.
			spriteId = spriteId.substring(0, pos); // Cut the sprite attribute name.
		}

		if (attr == null) {
			addOrChangeSprite(event, element, spriteId, value);
		} else {
			if (event == AttributeChangeEvent.ADD) {
				GraphicSprite sprite = styleGroups.getSprite(spriteId);

				// We add the sprite, in case of a replay, some attributes of the sprite can be
				// changed before the sprite is declared.
				if (sprite == null) {
					addOrChangeSprite(AttributeChangeEvent.ADD, element, spriteId, null);
					sprite = styleGroups.getSprite(spriteId);
				}
				
				sprite.addAttribute(attr, value);
			} else if (event == AttributeChangeEvent.CHANGE) {
				GraphicSprite sprite = styleGroups.getSprite(spriteId);

				if (sprite == null) {
					addOrChangeSprite(AttributeChangeEvent.ADD, element, spriteId, null);
					sprite = styleGroups.getSprite(spriteId);
				}
				
				sprite.changeAttribute(attr, value);
			} else if (event == AttributeChangeEvent.REMOVE) {
				GraphicSprite sprite = styleGroups.getSprite(spriteId);

				if (sprite != null)
					sprite.removeAttribute(attr);
			}
		}
	}

	/**
	 * Called by {@link #spriteAttribute(AttributeChangeEvent, Element, String, Object)} to
	 * add a sprite or modify its position or attributes.
	 */
	protected void addOrChangeSprite(AttributeChangeEvent event,
			Element element, String spriteId, Object value) {
		
		if (event == AttributeChangeEvent.ADD
		||  event == AttributeChangeEvent.CHANGE) {
			GraphicSprite sprite = styleGroups.getSprite(spriteId);

			if (sprite == null)
				sprite = addSprite_(spriteId);

			if (element != null) {
				if (element instanceof GraphicNode)
					sprite.attachToNode((GraphicNode) element);
				else if (element instanceof GraphicEdge)
					sprite.attachToEdge((GraphicEdge) element);
			}

			if (value != null && (!(value instanceof Boolean)))
				positionSprite(sprite, value);
		} else if (event == AttributeChangeEvent.REMOVE) {
			if (element == null) {
				if (styleGroups.getSprite(spriteId) != null) {
					removeSprite_(spriteId);
				}
			} else {
				GraphicSprite sprite = styleGroups.getSprite(spriteId);

				if (sprite != null)
					sprite.detach();
			}
		}
	}

	/**
	 * Directly add a sprite in this graphic graph. The graph attributes are modified accordingly.
	 * @param id The new sprite identifier.
	 * @return The created sprite.
	 */
	public GraphicSprite addSprite(String id) {
		String prefix = String.format("ui.sprite.%s", id);
		addAttribute(prefix, 0, 0, 0);

		GraphicSprite s = styleGroups.getSprite(id);
		assert (s != null);
		return s;
	}

	protected GraphicSprite addSprite_(String id) {
		GraphicSprite s = new GraphicSprite(id, this);
		styleGroups.addElement(s);
		graphChanged = true;

		return s;
	}

	/**
	 * Directly remove a sprite from this graphic graph. The graph attributes are modified
	 * accordingly.
	 * @param id The sprite identifier.
	 */
	public void removeSprite(String id) {
		String prefix = String.format("ui.sprite.%s", id);
		removeAttribute(prefix);
	}

	protected GraphicSprite removeSprite_(String id) {
		GraphicSprite sprite = (GraphicSprite) styleGroups.getSprite(id);

		if (sprite != null) {
			sprite.detach();
			styleGroups.removeElement(sprite);
			sprite.removed();

			graphChanged = true;
		}

		return sprite;
	}

	/**
	 * Decode a set the position of a sprite from an attribute value.
	 * @param sprite The sprite to position.
	 * @param value The value to decode.
	 */
	protected void positionSprite(GraphicSprite sprite, Object value) {
		if (value instanceof Object[]) {
			Object[] values = (Object[]) value;

			if (values.length == 4) {
				if (values[0] instanceof Number && values[1] instanceof Number
						&& values[2] instanceof Number
						&& values[3] instanceof Style.Units) {
					sprite.setPosition(((Number) values[0]).doubleValue(),
							((Number) values[1]).doubleValue(),
							((Number) values[2]).doubleValue(),
							(Style.Units) values[3]);
				} else {
					System.err
							.printf("GraphicGraph : cannot parse values[4] for sprite position.%n");
				}
			} else if (values.length == 3) {
				if (values[0] instanceof Number && values[1] instanceof Number
						&& values[2] instanceof Number) {
					sprite.setPosition(((Number) values[0]).doubleValue(),
							((Number) values[1]).doubleValue(),
							((Number) values[2]).doubleValue(), Units.GU);
				} else {
					System.err
							.printf("GraphicGraph : cannot parse values[3] for sprite position.%n");
				}
			} else if (values.length == 1) {
				if (values[0] instanceof Number) {
					sprite.setPosition(((Number) values[0]).doubleValue());
				} else {
					System.err
							.printf("GraphicGraph : sprite position percent is not a number.%n");
				}
			} else {
				System.err
						.printf("GraphicGraph : cannot transform value '%s' (length=%d) into a position%n",
								Arrays.toString(values), values.length);
			}
		} else if (value instanceof Number) {
			sprite.setPosition(((Number) value).doubleValue());
		} else if (value instanceof Value) {
			sprite.setPosition(((Value) value).value);
		} else if (value instanceof Values) {
			sprite.setPosition((Values) value);
		} else if (value == null) {
			throw new RuntimeException("What do you expect with a null value ?");
		} else {
			System.err
					.printf("GraphicGraph : cannot place sprite with posiiton '%s' (instance of %s)%n",
							value, value.getClass().getName());
		}
	}

	// Redefinition of the attribute setting mechanism to filter attributes.

	@Override
	public void addAttribute(String attribute, Object... values) {
		Matcher matcher = GraphicElement.acceptedAttribute.matcher(attribute);

		if (matcher.matches())
			super.addAttribute(attribute, values);
	}

	public void clearAttributeSinks() {
		listeners.clearAttributeSinks();
	}

	public void clearElementSinks() {
		listeners.clearElementSinks();
	}

	public void clearSinks() {
		listeners.clearSinks();
	}
	
	// stubs for the new methods

	public <T extends Edge> T addEdge(String id, int index1, int index2) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T addEdge(String id, int fromIndex, int toIndex,
			boolean directed) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T addEdge(String id, Node node1, Node node2) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T addEdge(String id, Node from, Node to,
			boolean directed) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T getEdge(int index)
			throws IndexOutOfBoundsException {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Node> T getNode(int index)
			throws IndexOutOfBoundsException {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T removeEdge(int index) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T removeEdge(int fromIndex, int toIndex) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T removeEdge(Node node1, Node node2) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Edge> T removeEdge(Edge edge) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Node> T removeNode(int index) {
		throw new RuntimeException("not implemented !");
	}

	public <T extends Node> T removeNode(Node node) {
		throw new RuntimeException("not implemented !");
	}
}