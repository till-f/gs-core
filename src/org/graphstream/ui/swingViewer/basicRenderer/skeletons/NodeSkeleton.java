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
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.Camera;

/**
 * Skeleton for nodes (and a base for sprites).
 * 
 * <p>
 * This skeleton handles tasks like computing the (absolute) position of the element (refined
 * in the sprite skeleton) and the inclusion test to know if a point is whithin the rectangular
 * bounds of the node/sprite.
 * </p>
 */
public class NodeSkeleton extends BaseSkeleton {
	public Point3 getPosition(Camera camera, Units units) {
		return getPosition(camera, null, units);
	}
	
	@Override
	public Point3 getPosition(Camera camera, Point3 pos, Units units) {
		if(pos == null)
			pos = new Point3();
		
		if(units == Units.GU) {
			pos.copy(element.getCenter());
			return pos;
		} else if(units == Units.PX) {
			pos.copy(element.getCenter());
			return camera.transformGuToPx(pos);
		} else {
			throw new RuntimeException("TODO");
		}
	}
	
	/**
	 * Does the rectangular bounds of the node contain the point (x,y), expressed
	 * in the given units?
	 * @param camera The camera.
	 * @param x Abscissa of the point to test. 
	 * @param y Ordinate of the point to test.
	 * @param units Units of the point to test.
	 * @return True if the point is in the rectangular bounds of the node.
	 */
	public boolean contains(Camera camera, double x, double y, Units units) {
		getSizeGU(camera);
		
		Point3 pos = getPosition(camera, Units.GU);
		Point3 p   = new Point3(x, y, 0);

		if(units == Units.PX)
			p = camera.transformPxToGu(p);
		
		double sx = size.x/2;
		double sy = size.y/2;

		if (p.x < (pos.x - sx)) return false;
		if (p.y < (pos.y - sy)) return false;
		if (p.x > (pos.x + sx)) return false;
		if (p.y > (pos.y + sy)) return false;

		return true;
	}
	
	/**
	 * True if the node rectangular bounds are visible (in intersection or inside) the given
	 * region bounds, expressed in the given units.
	 * @param camera The camera.
	 * @param X1 The region lowest abscissa.
	 * @param Y1 The region lowest ordinate.
	 * @param X2 The region highest abscissa. 
	 * @param Y2 The region highest ordinate.
	 * @param units The units the region is expressed in.
	 * @return True if the node intersects or is in the given region.
	 */
	public boolean visibleIn(Camera camera, double X1, double Y1, double X2, double Y2, Units units) {
		Point3 s  = getSize(camera, Units.PX);
		double w2 = s.x;
		double h2 = s.y;
		Point3 p  = getPosition(camera, null, Units.PX);

		X2 -= X1; Y2-=Y1;
		X1 = 0; Y1 = 0;
		
		double x1 = p.x - w2;
		double x2 = p.x + w2;
		double y1 = p.y - h2;
		double y2 = p.y + h2;

		if (x2 < X1) return false;
		if (y2 < Y1) return false;
		if (x1 > X2) return false;
		if (y1 > Y2) return false;

		return true;
	}
}