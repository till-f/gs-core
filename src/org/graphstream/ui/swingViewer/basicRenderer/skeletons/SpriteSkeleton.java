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

package org.graphstream.ui.swingViewer.basicRenderer.skeletons;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.util.Camera;
import org.graphstream.ui.swingViewer.util.GraphMetrics;

public class SpriteSkeleton extends NodeSkeleton {

	@Override
	public Point3 getPosition(Camera camera, Point3 pos, Units units) {
		GraphicSprite sprite = (GraphicSprite) element;
		
		if(pos == null)
			pos = new Point3();
		
		if (sprite.isAttachedToNode())
			return getSpritePositionNode(camera, sprite, pos, units);
		else if (sprite.isAttachedToEdge())
			return getSpritePositionEdge(camera, sprite, pos, units);

		return getSpritePositionFree(camera, sprite, pos, units);
	}

	/**
	 * Compute the position of a sprite if it is not attached.
	 * 
	 * @param camera
	 *            The camera. 
	 * @param sprite
	 *            The sprite.
	 * @param pos
	 *            Where to stored the computed position, if null, the position
	 *            is created.
	 * @param units
	 *            The units the computed position must be given into.
	 * @return The same instance as pos, or a new one if pos was null.
	 */
	protected Point3 getSpritePositionFree(Camera camera, GraphicSprite sprite, Point3 pos, Units units) {
		if (pos == null)
			pos = new Point3();

		if (sprite.getUnits() == units) {
			pos.copy(sprite.center);
		} else if (units == Units.GU && sprite.getUnits() == Units.PX) {
			pos.copy(sprite.center);
			pos = camera.transformGuToPx(pos);
		} else if (units == Units.PX && sprite.getUnits() == Units.GU) {
			pos.copy(sprite.center);
			pos = camera.transformPxToGu(pos);
		} else if (units == Units.GU && sprite.getUnits() == Units.PERCENTS) {
			GraphMetrics metrics = camera.getMetrics();
			pos.x = metrics.lo.x + (sprite.center.x / 100f) * metrics.graphWidthGU();
			pos.y = metrics.lo.y + (sprite.center.y / 100f) * metrics.graphHeightGU();
			pos.z = 0;
		} else if (units == Units.PX && sprite.getUnits() == Units.PERCENTS) {
			GraphMetrics metrics = camera.getMetrics();
			pos.x = (sprite.center.x / 100f) * metrics.viewport.data[0];
			pos.y = (sprite.center.y / 100f) * metrics.viewport.data[1];
			pos.z = 0;
		} else {
			throw new RuntimeException("Unhandled yet sprite positioning.");
		}

		return pos;
	}

	/**
	 * Compute the position of a sprite if attached to a node.
	 * 
	 * @param camera
	 *            The camera. 
	 * @param sprite
	 *            The sprite.
	 * @param pos
	 *            Where to stored the computed position, if null, the position
	 *            is created.
	 * @param units
	 *            The units the computed position must be given into.
	 * @return The same instance as pos, or a new one if pos was null.
	 */
	protected Point3 getSpritePositionNode(Camera camera, GraphicSprite sprite, Point3 pos, Units units) {
		if (pos == null)
			pos = new Point3();

		GraphMetrics metrics = camera.getMetrics();
		GraphicNode node = sprite.getNodeAttachment();
		double radius = metrics.lengthToGu(sprite.center.x, sprite.getUnits());
		double z = sprite.center.y * (Math.PI / 180);

		pos.set(
			node.center.x + ((double) Math.cos(z) * radius),
			node.center.y + ((double) Math.sin(z) * radius), 0);

		if (units == Units.PX)
			pos = camera.transformGuToPx(pos);

		return pos;
	}

	/**
	 * Compute the position of a sprite if attached to an edge. The edge must have
	 * an {@link EdgeSkeleton} so that its geometry can be used to position the
	 * sprite on it.
	 * 
	 * @param camera
	 *            The camera. 
	 * @param sprite
	 *            The sprite.
	 * @param pos
	 *            Where to store the computed position, if null, the position
	 *            is created.
	 * @param units
	 *            The units the computed position must be given into.
	 * @return The same instance as pos, or a new one if pos was null.
	 */
	protected Point3 getSpritePositionEdge(Camera camera, GraphicSprite sprite, Point3 pos, Units units) {
		if (pos == null)
			pos = new Point3();

		GraphicEdge edge = sprite.getEdgeAttachment();
		EdgeSkeleton edgeSkel = (EdgeSkeleton) edge.getSkeleton();
		
		if(edgeSkel != null) {
			return edgeSkel.positionOnGeometry(camera, sprite.center.x, sprite.center.y, pos, units);
		} else {
			throw new RuntimeException("cannot position sprite on edge without skeleton");
		}
	}
}