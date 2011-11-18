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

import java.util.Iterator;

import org.graphstream.graph.Node;
import org.graphstream.stream.SourceBase.ElementType;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.stylesheet.Selector;
import org.graphstream.ui.graphicGraph.stylesheet.Style;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.Values;

/**
 * Graphic data about a sprite, a small gentle sprite.
 */
public class GraphicSprite extends GraphicElement {
	/**
	 * The node this sprite is attached to.
	 */
	protected GraphicNode node;

	/**
	 * The edge this sprite is attached to.
	 */
	protected GraphicEdge edge;

	/**
	 * Sprite position.
	 */
	public Point3 center = new Point3(0, 0, 0);
	
//	public Values position = new Values(StyleConstants.Units.GU, 0, 0, 0);

	/**
	 * Sprite position units (this can be specified by the user), be careful !
	 */
	public StyleConstants.Units units = StyleConstants.Units.GU;
	
	/**
	 * New sprite.
	 * 
	 * @param id
	 *            The sprite unique identifier.
	 * @param graph
	 *            The graph containing this sprite.
	 */
	public GraphicSprite(String id, GraphicGraph graph) {
		super(id, graph);

		// Get the position of a random node.

		if (graph.getNodeCount() > 0) {
			Iterator<? extends Node> nodes = graph.getNodeIterator();

			GraphicNode node = (GraphicNode) nodes.next();

			center.copy(node.center);
		}

		String myPrefix = String.format("ui.sprite.%s", id);

		if (mygraph.getAttribute(myPrefix) == null)
			mygraph.addAttribute(myPrefix, new Values(units, center.x, center.y, center.z));
	}

	@Override
	public Skeleton getSkeleton() {
		if(skeleton == null && mygraph.skeletonFactory != null) {
			setSkeleton(mygraph.skeletonFactory.newSpriteSkeleton());
		}
		
		return skeleton;
	}

	/**
	 * The node this sprite is attached to or null if not attached to an edge.
	 * 
	 * @return A graphic node.
	 */
	public GraphicNode getNodeAttachment() {
		return node;
	}

	/**
	 * The edge this sprite is attached to or null if not attached to an edge.
	 * 
	 * @return A graphic edge.
	 */
	public GraphicEdge getEdgeAttachment() {
		return edge;
	}
	
	@Override
	public void attachMoved(GraphicElement element) {
		if(skeleton != null)
			skeleton.positionChanged();
	}

	/**
	 * Return the graphic object this sprite is attached to or null if not
	 * attached.
	 * 
	 * @return A graphic object or null if no attachment.
	 */
	public GraphicElement getAttachment() {
		GraphicNode n = getNodeAttachment();

		if (n != null)
			return n;

		return getEdgeAttachment();
	}

	/**
	 * True if the sprite is attached to a node or edge.
	 */
	public boolean isAttached() {
		return (edge != null || node != null);
	}

	/**
	 * True if the sprite is attached to a node.
	 */
	public boolean isAttachedToNode() {
		return node != null;
	}

	/**
	 * True if the node is attached to an edge.
	 */
	public boolean isAttachedToEdge() {
		return edge != null;
	}

	@Override
	public Selector.Type getSelectorType() {
		return Selector.Type.SPRITE;
	}

	@Override
	public Point3 getCenter() {
		return center;
	}

	/**
	 * The units in which the position is expressed.
	 */
	public Style.Units getUnits() {
		return units;
	}

	@Override
	public void move(double x, double y, double z) {
		setPosition(x, y, z, Style.Units.GU);
	}

	/**
	 * Attach this sprite to the given node.
	 * 
	 * @param node
	 *            A graphic node.
	 */
	public void attachToNode(GraphicNode node) {
		this.edge = null;
		this.node = node;

		String prefix = String.format("ui.sprite.%s", getId());

		if (this.node.getAttribute(prefix) == null)
			this.node.addAttribute(prefix);

		mygraph.graphChanged = true;

		if(skeleton != null)
			skeleton.positionChanged();
	}

	/**
	 * Attach this sprite to the given edge.
	 * 
	 * @param edge
	 *            A graphic edge.
	 */
	public void attachToEdge(GraphicEdge edge) {
		this.node = null;
		this.edge = edge;

		String prefix = String.format("ui.sprite.%s", getId());

		if (this.edge.getAttribute(prefix) == null)
			this.edge.addAttribute(prefix);

		mygraph.graphChanged = true;

		if(skeleton != null)
			skeleton.positionChanged();
	}

	/**
	 * Detach this sprite from the edge or node it was attached to.
	 */
	public void detach() {
		String prefix = String.format("ui.sprite.%s", getId());

		if (this.node != null)
			this.node.removeAttribute(prefix);
		else if (this.edge != null)
			this.edge.removeAttribute(prefix);

		this.edge = null;
		this.node = null;
		mygraph.graphChanged = true;

		if(skeleton != null)
			skeleton.positionChanged();
	}

	/**
	 * Reposition this sprite.
	 * 
	 * @param value
	 *            The coordinate.
	 */
	public void setPosition(double value) {
		setPosition(value, 0, 0, units);
	}

	/**
	 * Reposition this sprite.
	 * 
	 * @param x
	 *            First coordinate.
	 * @param y
	 *            Second coordinate.
	 * @param z
	 *            Third coordinate.
	 * @param units
	 *            The units to use for lengths and radii.
	 */
	public void setPosition(double x, double y, double z, Style.Units units) {
		if (edge != null) {
			if (x < 0)
				x = 0;
			else if (x > 1)
				x = 1;
		}

		boolean changed = false;

		if (center.x != x) {
			changed = true;
			center.x = x;
		}
		if (center.y != y) {
			changed = true;
			center.y = y;
		}
		if (center.z != z) {
			changed = true;
			center.z = z;
		}
		if (this.units != units) {
			changed = true;
			this.units = units;
		}

		if (changed) {
			mygraph.graphChanged = true;
			mygraph.boundsChanged = true;

			String prefix = String.format("ui.sprite.%s", getId());

			if(mygraph.feedbackXYZ)
				mygraph.setAttribute(prefix, new Values(this.units, center.x, center.y, center.z));
			
			if(skeleton != null)
				skeleton.positionChanged();
		}
	}

	public void setPosition(Values values) {
		double x = center.x;
		double y = center.y;
		double z = center.z;

		if (values.getValueCount() > 0)
			x = values.get(0);
		if (values.getValueCount() > 1)
			y = values.get(1);
		if (values.getValueCount() > 2)
			z = values.get(2);

		setPosition(x, y, z, values.units);
	}

	protected double checkAngle(double angle) {
		if (angle > Math.PI * 2)
			angle = angle % (Math.PI * 2);
		else if (angle < 0)
			angle = (Math.PI * 2) - (angle % (Math.PI * 2));

		return angle;
	}

	@Override
	protected void attributeChanged(String sourceId, long timeId,
			String attribute, AttributeChangeEvent event, Object oldValue,
			Object newValue) {
		super.attributeChanged(sourceId, timeId, attribute, event, oldValue,
				newValue);

		String completeAttr = String.format("ui.sprite.%s.%s", getId(),
				attribute);

		mygraph.listeners.sendAttributeChangedEvent(sourceId, timeId,
				mygraph.getId(), ElementType.GRAPH, completeAttr, event,
				oldValue, newValue);
	}
}