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

import static org.graphstream.ui.graphicGraph.GraphPosLengthUtils.nodePosition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.stream.SourceBase.ElementType;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.stylesheet.Selector;

/**
 * Graphic data about a node.
 * 
 * <p>
 * A graphic node defines a position center (x,y,z) that allows to
 * draw its representation at this location. It can tell at which
 * time this position has been setup for the first time. Most
 * renderer will not render the node until it got a position
 * (see the {@link #positioned} flag).
 * </p>
 * 
 * @see GraphicGraph
 */
public class GraphicNode extends GraphicElement implements Node {
	/**
	 * The position of the node. In graph units.
	 */
	public Point3 center = new Point3();
	
	/**
	 * True as soon as the center has been modified for the first time.
	 */
	public boolean positioned = false;

	/**
	 * New graphic node.
	 * 
	 * @param id
	 *            The node identifier.
	 * @param attributes
	 *            The node attribute set (can be null).
	 */
	public GraphicNode(GraphicGraph graph, String id,
			HashMap<String, Object> attributes) {
		super(id, graph);

		if (attributes != null)
			addAttributes(attributes);
	}

	@Override
	public Selector.Type getSelectorType() {
		return Selector.Type.NODE;
	}

	@Override
	public Point3 getCenter() {
		return center;
	}

	protected void moveFromEvent(double x, double y, double z) {
		center.set(x, y, z);
		positionChanged();
	}
	
	protected void positionChanged() {
		positioned = true;

		mygraph.graphChanged = true;
		mygraph.boundsChanged = true;	// Maybe ...

//		if (mygraph.feedbackXYZ)
//			setAttribute("xyz", center.x, center.y, center.z);
		
		if(skeleton != null) {
			skeleton.positionChanged();

		}

		if(attached != null)
			for(GraphicElement elt: attached)
				elt.attachMoved(this);
	}
	
	@Override
	public void attachMoved(GraphicElement element) {
		// Nodes are never attached.
	}

	@Override
	public void move(double x, double y, double z) {
		center.set(x, y, z);
		positionChanged();
	}

	@Override
	protected void attributeChanged(String sourceId, long timeId,
			String attribute, AttributeChangeEvent event, Object oldValue,
			Object newValue) {
		super.attributeChanged(sourceId, timeId, attribute, event, oldValue,
				newValue);

		if (attribute.startsWith("ui.sprite.")) {
			// An attachment ?
			mygraph.spriteAttribute(event, this, attribute, newValue);
		} else if (event == AttributeChangeEvent.ADD
				|| event == AttributeChangeEvent.CHANGE) {
			if (attribute.equals("x")) {
				center.x = numberAttribute(newValue);
				positionChanged();
			} else if (attribute.equals("y")) {
				center.y = numberAttribute(newValue);
				positionChanged();
			} else if (attribute.equals("z")) {
				center.z = numberAttribute(newValue);
				positionChanged();
			} else if (attribute.equals("xy") || attribute.equals("xyz")) {
				nodePosition(this, center);
				positionChanged();
			}
		}

		mygraph.listeners.sendAttributeChangedEvent(sourceId, timeId, getId(),
				ElementType.NODE, attribute, event, oldValue, newValue);
	}

	/**
	 * Try to convert the object to a double.
	 * 
	 * @param value
	 *            The object to convert.
	 * @return The value.
	 */
	protected double numberAttribute(Object value) {
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		} else if (value instanceof CharSequence) {
			String xs = ((CharSequence) value).toString();

			try {
				return Double.parseDouble(xs);
			} catch (NumberFormatException e) {
			}
		}

		return 0;
	}

	// Node interface.

	/**
	 * Not implemented.
	 */
	public Iterator<Node> getBreadthFirstIterator() {
		throw new RuntimeException("not implemented !");
	}

	/**
	 * Not implemented.
	 */
	public Iterator<Node> getBreadthFirstIterator(boolean directed) {
		throw new RuntimeException("not implemented !");
	}

	/**
	 * Not implemented.
	 */
	public Iterator<Node> getDepthFirstIterator() {
		throw new RuntimeException("not implemented !");
	}

	/**
	 * Not implemented.
	 */
	public Iterator<Node> getDepthFirstIterator(boolean directed) {
		throw new RuntimeException("not implemented !");
	}

	public int getDegree() {
		ArrayList<GraphicEdge> edges = mygraph.connectivity.get(this);

		if (edges != null)
			return edges.size();

		return 0;
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> T getEdge(int i) {
		ArrayList<GraphicEdge> edges = mygraph.connectivity.get(this);

		if (edges != null && i >= 0 && i < edges.size())
			return (T)edges.get(i);

		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Edge> T getEdgeBetween(String id) {
		if(hasEdgeToward(id)) return (T)getEdgeToward(id);
		else return (T)getEdgeFrom(id);
	}

	@SuppressWarnings("all")
	public <T extends Edge> T getEdgeFrom(String id) {
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T extends Edge> Iterator<T> getEdgeIterator() {
		ArrayList<GraphicEdge> edges = mygraph.connectivity.get(this);

		if (edges != null)
			return (Iterator<T>)edges.iterator();

		return null;
	}

	@SuppressWarnings("all")
	public Iterator<Edge> iterator() {
		return (Iterator<Edge>) getEdgeIterator();
	}
	
	@SuppressWarnings("all")
	public <T extends Edge> Iterable<T> getEachEdge() {
		return (Iterable<T>)mygraph.connectivity.get(this);
	}

	@SuppressWarnings("all")
	public <T extends Edge> Collection<T> getEdgeSet() {
		return (Collection<T>)Collections.unmodifiableCollection(mygraph.connectivity.get(this));
	}

	@SuppressWarnings("all")
	public <T extends Edge> T getEdgeToward(String id) {
		ArrayList<? extends Edge> edges = mygraph.connectivity.get(this);

		for (Edge edge : edges) {
			if (edge.getOpposite(this).getId().equals(id))
				return (T)edge;
		}

		return null;
	}

	@SuppressWarnings("all")
	public <T extends Edge> Iterator<T> getEnteringEdgeIterator() {
		return getEdgeIterator();
	}
	
	@SuppressWarnings("all")
	public <T extends Edge> Iterable<T> getEachEnteringEdge() {
		return getEdgeSet();
	}

	@SuppressWarnings("all")
	public <T extends Edge> Collection<T> getEnteringEdgeSet() {
		return (Collection<T>)Collections.unmodifiableCollection(getEdgeSet());
	}

	public Graph getGraph() {
		return mygraph;
	}

	public String getGraphName() {
		throw new RuntimeException("impossible with GraphicGraph");
	}

	public String getHost() {
		throw new RuntimeException("impossible with GraphicGraph");
	}

	public int getInDegree() {
		return getDegree();
	}

	@SuppressWarnings("all")
	public <T extends Edge> Iterator<T> getLeavingEdgeIterator() {
		return getEdgeIterator();
	}

	@SuppressWarnings("all")
	public <T extends Edge> Iterable<T> getEachLeavingEdge() {
		return getEdgeSet();
	}

	@SuppressWarnings("all")
	public <T extends Edge> Collection<T> getLeavingEdgeSet() {
		return (Collection<T>)Collections.unmodifiableCollection(getEdgeSet());
	}

	public Iterator<Node> getNeighborNodeIterator() {
		return null;
	}

	public int getOutDegree() {
		return getDegree();
	}

	public boolean hasEdgeBetween(String id) {
		return( hasEdgeToward(id) || hasEdgeFrom(id) );
	}

	public boolean hasEdgeFrom(String id) {
		return false;
	}

	public boolean hasEdgeToward(String id) {
		return false;
	}

	public boolean isDistributed() {
		return false;
	}

	public void setGraph(Graph graph) {
		throw new RuntimeException("impossible with GraphicGraph");
	}

	public void setGraphName(String newHost) {
		throw new RuntimeException("impossible with GraphicGraph");
	}

	public void setHost(String newHost) {
		throw new RuntimeException("impossible with GraphicGraph");
	}
	
	// XXX stubs for the new methods
	
	public <T extends Edge> T getEdgeBetween(Node Node) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T extends Edge> T getEdgeBetween(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T extends Edge> T getEdgeFrom(Node Node) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T extends Edge> T getEdgeFrom(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T extends Edge> T getEdgeToward(Node Node) {
		// TODO Auto-generated method stub
		return null;
	}

	public <T extends Edge> T getEdgeToward(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean hasEdgeBetween(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasEdgeBetween(int index) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasEdgeFrom(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasEdgeFrom(int index) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasEdgeToward(Node node) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean hasEdgeToward(int index) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public <T extends Edge> T getEnteringEdge(int i) {
		// TODO Auto-generated method stub
		return null;		
	}

	public <T extends Edge> T getLeavingEdge(int i) {
		// TODO Auto-generated method stub
		return null;		
	}
}