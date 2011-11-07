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

import java.util.ArrayList;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;

/**
 * Skeleton for the edges.
 */
public class EdgeSkeleton extends BaseSkeleton {
	/**
	 * The possible geometries handled by this simple renderer.
	 */
	public enum Kind {
		AUTO_LOOP,
		AUTO_MULTI,
		LINE,
		POINTS,
		VECTORS
	}
	
	/**
	 * The kind of edge.
	 */
	protected Kind kind = Kind.LINE;
	
	/**
	 * Set to true each time the geometry (points) needs to be recomputed.
	 */
	protected boolean dirty = true;
	
	/**
	 * The set of points making up the geometry.
	 * 
	 * The way this is interpreted differs according to the kind of skeleton:
	 * <ul>
	 * 		<li>For the LINE type it is null, nothing is stored,
	 * 			the two end points are given by the edge nodes.</li>
	 * 		<li>For the AUTO_LOOP and AUTO_MULTI types, this contains
	 * 			two "control" points for a cubic curve. The two node
	 * 			centers playing the role of the first and last points.</li>
	 * 		<li>For the POINTS type, the points define line segments. There
	 * 			can be one ore more points.
	 * 			For example if there are one point, this defines two
	 * 			line segments one going from the start node to the point
	 * 			and one going from the point to the end node.</li>
	 * 		<li>For the VECTORS type, the points define a vector
	 * 			shape that contains the first and last end-points
	 * 			(the nodes are not considered). This means that there
	 * 			are at least two points in the set.</li>
	 * </ul> 
	 */
	public ArrayList<Point3> points = null;
	
	@Override
	public void installed(GraphicElement element) {
		super.installed(element);
	}
	
	@Override
	public void pointsChanged(Object values) {
		setupPoints(values);
	}
	
	@Override
	public void positionChanged() {
		dirty = true;
	}
	
	public int pointCount() {
		if(points != null)
			return points.size();
		
		return 0;
	}
	
	public Point3 getPoint(int i) {
		if(dirty) {
			setupPoints(element.getAttribute("ui.points"));
		}
		return points.get(i);
	}
	
	protected void setupPoints(Object values) {
		
		dirty = false;
	}
}