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
package org.graphstream.ui.swingViewer.basicRenderer.skeletons;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.Camera;
import org.graphstream.ui.swingViewer.util.GraphMetrics;

/**
 * Skeleton for sprites.
 *
 * <p>
 * This skeleton handles the absolute position of the sprite, computed from its relative coordinates
 * center. Each time the center is changed, a flag indicate the position should be recomputed. When
 * first accessed, the position is recomputed if needed.
 * </p>
 */
public class SpriteSkeleton extends NodeSkeleton {
	/**
	 * Should the position be recomputed.
	 */
	protected boolean positionDirty = true;
	
	/**
	 * Computed position, on the canvas.
	 * The center of a sprite is a set of values that need interpretation depending on a possible
	 * attachment. The position is the computed coordinates according to the center and the
	 * attachment. It is expressed in graph units.
	 */
	protected Point3 position = new Point3();
	
	/**
	 * Remember to which element the sprite was attached.
	 */
	protected GraphicElement attachment = null;
	
	@Override
	public void uninstalled() {
		super.uninstalled();
		
		if(attachment != null) {
			attachment.removeAttachment(element);
			attachment = null;
		}
	}
	
	@Override
	public void positionChanged() {
		positionDirty = true;
		
		GraphicSprite sprite = (GraphicSprite)element;
		
		if(sprite.getAttachment() != attachment) {
			if(attachment != null)
				attachment.removeAttachment(sprite);
			attachment = sprite.getAttachment();
			if(attachment != null)
				attachment.addAttachment(sprite);
		}
	}
	
	@Override
	public Point3 getPosition(Camera camera, Point3 pos, Units units) {
		if(positionDirty) {
			position = computeSpritePosition(camera, pos, Units.GU);
			positionDirty = false;
		}
		
		switch(units) {
			case GU: return position;
			case PX: return camera.transformGuToPx(new Point3(position));
			case PERCENTS: throw new RuntimeException("TODO");
			default: throw new RuntimeException("WTF?");
		}
	}
		
	/**
	 * Compute the sprite absolute position, storing the result in "pos" if non null
	 * in the units given.
	 * @param camera The camera.
	 * @param pos The memory location where the store the computed position or null. 
	 * @param units The units in which the position is expected.
	 * @return The computed position, either a reference to "pos" or a new point is
	 * "pos" was null.
	 */
	protected Point3 computeSpritePosition(Camera camera, Point3 pos, Units units) {
		GraphicSprite sprite = (GraphicSprite) element;
		
		if(pos == null)
			pos = new Point3();
		
		if (sprite.isAttachedToNode())
			return computeSpritePositionNode(camera, sprite, pos, units);
		else if (sprite.isAttachedToEdge())
			return computeSpritePositionEdge(camera, sprite, pos, units);

		return computeSpritePositionFree(camera, sprite, pos, units);
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
	protected Point3 computeSpritePositionFree(Camera camera, GraphicSprite sprite, Point3 pos, Units units) {
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
			pos.x = (sprite.center.x / 100f) * metrics.surfaceSize.data[0];
			pos.y = (sprite.center.y / 100f) * metrics.surfaceSize.data[1];
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
	protected Point3 computeSpritePositionNode(Camera camera, GraphicSprite sprite, Point3 pos, Units units) {
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
	protected Point3 computeSpritePositionEdge(Camera camera, GraphicSprite sprite, Point3 pos, Units units) {
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