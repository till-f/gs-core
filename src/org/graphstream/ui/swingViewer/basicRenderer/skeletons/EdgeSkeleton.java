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
import org.graphstream.ui.geom.Vector2;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.util.Camera;
import org.graphstream.ui.swingViewer.util.CubicCurve;

/**
 * Skeleton for the edges.
 */
public class EdgeSkeleton extends BaseSkeleton {
	/**
	 * The possible geometries handled internally. This is mainly a guide for the skins to
	 * render a specific shape, and a path for sprites when attached to the edge.
	 */
	public enum Kind {
		LINE,
		CUBIC_CURVE,
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
		dirty = true;
	}
	
	@Override
	public void pointsChanged(Object values) {
		setupPoints(values);
	}
	
	@Override
	public void positionChanged() {
		dirty = true;
	}
	
	public int pointCount(Camera camera) {
		if(dirty)
			recomputeGeometry(camera);
		if(points != null)
			return points.size();
		
		return 0;
	}
	
	public Point3 getPoint(int i, Camera camera) {
		if(dirty)
			recomputeGeometry(camera);

		return points.get(i);
	}
	
	public Point3 positionOnGeometry(Camera camera, double percent, double offset, Point3 pos, Units units) {
		if(pos == null)
			pos = new Point3();
		
		switch(kind) {
			case LINE:
				return positionOnLine(camera, percent, offset, pos, units);
			case CUBIC_CURVE:
				return positionOnCubicCurve(camera, percent, offset, pos, units);
			case POINTS:
				return positionOnPoints(camera, percent, offset, pos, units);
			case VECTORS:
				return positionOnVectors(camera, percent, offset, pos, units);
			default:
				throw new RuntimeException("WTF?");
		}
	}
	
	protected void setupPoints(Object values) {
		if(values == null) {
			points = null;
		} else {
			// XXX TODO XXX
			throw new RuntimeException("TODO");
		}
		
		dirty = true;
	}
	
	protected void recomputeGeometry(Camera camera) {
		GraphicEdge edge = (GraphicEdge) element;
		
		switch(edge.style.getShape()) {
			case POLYLINE:
				if(points != null) {
					kind = Kind.POINTS;
				} else {
					kind = Kind.LINE;
				}
				break;
			case POLYLINE_SCALED:
				if(points != null) {
					kind = Kind.VECTORS;
					recomputeGeometryVectors();
				} else {
					kind = Kind.LINE;
				}
				break;
			case CUBIC_CURVE:
				kind = Kind.CUBIC_CURVE;
				recomputeGeometryCubicCurve();
				break;
			default:
				kind = Kind.LINE;
				if(edge.getNode0() == edge.getNode1()) {
					recomputeGeometryLoop(edge, camera);
				} else if(edge.multi > 1) {
					recomputeGeometryMulti(edge);
				}
				break;
		}

		dirty = false;
	}
	
	protected void recomputeGeometryVectors() {
		throw new RuntimeException("TODO geometry vectors");
	}
	
	protected void recomputeGeometryCubicCurve() {
		throw new RuntimeException("TODO geometry cubic-curve");
	}
	
	protected void recomputeGeometryMulti(GraphicEdge edge) {
		Point3 from = edge.from.center;
		Point3 to   = edge.to.center;
		int multi   = edge.multi;
		
		double vx  = to.x - from.x;
		double vy  = to.y - from.y;
		double vx2 =  vy * 0.6;
		double vy2 = -vx * 0.6;
		double gap = 0.2;
		double ox  = 0.0;
		double oy  = 0.0;
		double f   = ((1 + multi) / 2) * gap; // must be done on integers.
  
		vx *= 0.2;
		vy *= 0.2;
  
		GraphicEdge main = edge.group.getEdge(0);
 
		if(edge.group.getCount() %2 == 0) {
			ox = vx2 * (gap/2);
			oy = vy2 * (gap/2);
			if(edge.from != main.from) {	// Edges are in the same direction.
				ox = - ox;
				oy = - oy;
			}
		}
  
		vx2 *= f;
		vy2 *= f;
  
		double xx1 = from.x + vx;
		double yy1 = from.y + vy;
		double xx2 = to.x - vx;
		double yy2 = to.y - vy;
  
		int m = multi + (edge.from == main.from ? 0 : 1);
  
		if(m % 2 == 0) {
			xx1 += vx2 + ox;
			yy1 += vy2 + oy;
			xx2 += vx2 + ox;
			yy2 += vy2 + oy;
		} else {
			xx1 -= vx2 - ox;
			yy1 -= vy2 - oy;
			xx2 -= vx2 - ox;
			yy2 -= vy2 - oy;	  
		}
		
		if(points == null) {
			points = new ArrayList<Point3>();
		}
		while(points.size() < 2) {
			points.add(new Point3());
		}

		points.get(0).set(xx1, yy1, 0);
		points.get(1).set(xx2, yy2, 0);
	}
	
	protected void recomputeGeometryLoop(GraphicEdge edge, Camera camera) {
		int multi = edge.multi;
		double x = edge.from.center.x;
		double y = edge.from.center.y;
		double m = 1f + multi * 0.2;
		double s = 0;
		NodeSkeleton nodeSkel = (NodeSkeleton)edge.from.getSkeleton(); 
		if(nodeSkel != null) {
			Point3 nodeSize = nodeSkel.getSizeGU(camera);
			s = (nodeSize.x + nodeSize.y) / 2;
		}
		double d = s / 2 * m + 4 * s * m;

		if(points == null) {
			points = new ArrayList<Point3>();
		}
		while(points.size() < 2) {
			points.add(new Point3());
		}

		points.get(0).set(x+d, y, 0);
		points.get(1).set(x, y+d, 0);
	}
	
	protected Point3 positionOnLine(Camera camera, double percent, double offset, Point3 pos, Units units) {
		GraphicEdge edge = (GraphicEdge) element;

		double x  = edge.from.center.x;
		double y  = edge.from.center.y;
		double dx = edge.to.center.x - x;
		double dy = edge.to.center.y - y;

		offset  = camera.getMetrics().lengthToGu(offset, units);
		percent = percent > 1 ? 1 : percent;
		percent = percent < 0 ? 0 : percent;

		x += dx * percent;
		y += dy * percent;

		percent = Math.sqrt(dx * dx + dy * dy);
		dx /= percent;
		dy /= percent;

		x += -dy * offset;
		y +=  dx * offset;

		pos.x = x;
		pos.y = y;
		pos.z = 0;

		if (units == Units.PX)
			pos = camera.transformGuToPx(pos);
		
		return pos;
	}
	
	protected Point3 positionOnCubicCurve(Camera camera, double percent, double offset, Point3 pos, Units units) {
		GraphicEdge edge = (GraphicEdge) element;
		
		Point3 p0 = edge.from.center;
		Point3 p1 = points.get(0);
		Point3 p2 = points.get(1);
		Point3 p3 = edge.to.center;

		if(offset != 0) {
			Vector2 perp = CubicCurve.perpendicular(p0, p1, p2, p3, percent);
			double  y    = camera.getMetrics().lengthToGu(offset, units);

			perp.normalize();
			perp.scalarMult(y);

			pos.x = CubicCurve.eval(p0.x, p1.x, p2.x, p3.x, offset) - perp.data[0];
			pos.y = CubicCurve.eval(p0.y, p1.y, p2.y, p3.y, offset) - perp.data[1];
		} else {
			pos.x = CubicCurve.eval(p0.x, p1.x, p2.x, p3.x, offset);
			pos.y = CubicCurve.eval(p0.y, p1.y, p2.y, p3.y, offset);
		}
		
		pos.z = 0;

		return pos;
	}
	
	protected Point3 positionOnPoints(Camera camera, double percent, double offset, Point3 pos, Units units) {
		// XXX TODO XXX
		throw new RuntimeException("TODO");
	}
	
	protected Point3 positionOnVectors(Camera camera, double percent, double offset, Point3 pos, Units units) {
		// XXX TODO XXX
		throw new RuntimeException("TODO");
	}
}