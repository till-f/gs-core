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
 * 
 * <p>
 * This skeleton handles the geometry of the edge. It can handle four types of geometry:
 * <ol>
 * 		<li>A straight line.</li>
 * 		<li>A cubic curve (mostly used for multi-edges and loop-edges).</li>
 * 		<li>A set of absolute points.</li>
 * 		<li>A set of vectors (relative).</li>
 * </ol>
 * </p>
 * 
 * <p>
 * The geometry is used by example by sprites to compute a position on the edge. A flag is set
 * each time the edge moved or the style changed or the "ui.points" attribute is changed. When the
 * geometry is first accessed, it is computed or recomputed if this flag is set. 
 * </p>
 * 
 * <p>
 * This is the reason why several method take a {@link Camera} as argument, since they need it
 * to recompute correctly the geometry (metrics, etc.).
 * </p>
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
	 * The kind of geometry.
	 */
	protected Kind kind = Kind.LINE;
	
	/**
	 * Set to true each time the geometry (points) needs to be recomputed.
	 */
	protected boolean geomDirty = true;
	
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
	protected ArrayList<Point3> points = null;

	/**
	 * The position of the arrow. XXX TODO
	 */
	public Point3 arrowPos = null;
	
	@Override
	public Point3 getPosition(Camera camera, Point3 pos, Units units) {
		switch(units) {
			case GU: return element.getCenter();
			case PX: return camera.transformGuToPx(new Point3(element.getCenter()));
			case PERCENTS: throw new RuntimeException("TODO");
			default: throw new RuntimeException("WTF?");
		}
	}
	
	@Override
	public void installed(GraphicElement element) {
		super.installed(element);
		geomDirty = true;
		
		GraphicEdge edge = (GraphicEdge)element;
		edge.from.addAttachment(edge);
		edge.to.addAttachment(edge);
	}
	
	@Override
	public void uninstalled() {
		GraphicEdge edge = (GraphicEdge)element;
		edge.from.removeAttachment(edge);
		edge.to.removeAttachment(edge);
	}
	
	@Override
	public void pointsChanged(Object values) {
		setupPoints(values);
	}
	
	@Override
	public void positionChanged() {
		geomDirty = true;
	}
	
	/**
	 * Number of points in the geometry.
	 * @param camera The camera.
	 */
	public int pointCount(Camera camera) {
		if(geomDirty)
			recomputeGeometry(camera);
		if(points != null)
			return points.size();
		
		return 0;
	}

	/**
	 * I-th point in the geometry.
	 * @param i The point index.
	 * @param camera The camera.
	 */
	public Point3 getPoint(int i, Camera camera) {
		if(geomDirty)
			recomputeGeometry(camera);

		return points.get(i);
	}
	
	/**
	 * The kind of geometry the edge is using.
	 * @param camera The camera.
	 */
	public Kind getKind(Camera camera) {
		if(geomDirty)
			recomputeGeometry(camera);
		
		return kind;
	}
	
	/**
	 * The source point of the edge, this point is never included in the set of
	 * points of the geometry.
	 */
	public Point3 getSourcePoint() {
		return ((GraphicEdge)element).from.getCenter();
	}

	/**
	 * The target point of the edge, this point is never included in the set of
	 * points of the geometry.
	 */
	public Point3 getTargetPoint() {
		return ((GraphicEdge)element).to.getCenter();
	}

	/**
	 * Compute the absolute coordinates of a point at "percent" percents on the geometry, starting
	 * from the origin node of the edge. Use "pos" to store the result and return it. If "pos" is
	 * null a new point is created and returned. The "units" allow to request the result to be
	 * expressed in given units. The offset allows to compute a point that is perpendicular to the
	 * position on the geometry at the "offset" distance.
	 * @param camera The camera.
	 * @param percent The position on the geometry.
	 * @param offset The perpendicular offset from the geometry at the position.
	 * @param pos Memory location where to store the result, if non null.
	 * @param units The units in which the result is expressed.
	 * @return The absolute position at percent + offset on the geometry.
	 */
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
	
	/**
	 * Read the "ui.points" attribute and store the result as a geometry for this skeleton.
	 * @param values The data to decode as points.
	 */
	protected void setupPoints(Object values) {
		if(values == null) {
			points = null;
		} else {
			// XXX TODO XXX
			throw new RuntimeException("TODO");
		}
		
		geomDirty = true;
	}
	
	/**
	 * Compute or recompute the geometry of the edge.
	 * @param camera The camera.
	 */
	protected void recomputeGeometry(Camera camera) {
		GraphicEdge edge = (GraphicEdge) element;
		
		switch(edge.style.getShape()) {
			case POLYLINE:
				if(points != null) {
					kind = Kind.POINTS;
					recomputeGeometryLines();
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
				if(edge.getNode0() == edge.getNode1()) {
					recomputeGeometryLoop(edge, camera);
					kind = Kind.CUBIC_CURVE;
				} else if(edge.getMultiCount() > 1) {
					recomputeGeometryMulti(edge);
					kind = Kind.CUBIC_CURVE;
				} else {
					recomputeGeometryCubicCurve(edge, camera);
					kind = Kind.CUBIC_CURVE;
				}
				break;
			default:
				kind = Kind.LINE;
				if(edge.getNode0() == edge.getNode1()) {
					recomputeGeometryLoop(edge, camera);
					kind = Kind.CUBIC_CURVE;
				} else if(edge.getMultiCount() > 1) {
					recomputeGeometryMulti(edge);
					kind = Kind.CUBIC_CURVE;
				}
				break;
		}

		geomDirty = false;
	}
	
	protected void recomputeGeometryVectors() {
		throw new RuntimeException("TODO geometry vectors");
	}
	
	protected void recomputeGeometryLines() {
		throw new RuntimeException("TODO geometry points");
	}
	
	protected void recomputeGeometryCubicCurve(GraphicEdge edge, Camera camera) {
		if(edge.getNode0() == edge.getNode1()) {
			recomputeGeometryLoop(edge, camera);
		} else if(edge.multi > 1) {
			recomputeGeometryMulti(edge);
		} else {
			checkPointsArraySize(2);

			Point3 from = edge.from.center;
			Point3 to   = edge.to.center;
			Point3 c1   = points.get(0);
			Point3 c2   = points.get(1);

	        Vector2 mainDir = new Vector2(from, to);
	        double length  = mainDir.length();
	        double angle   = mainDir.data[1] / length;
	
	        if (angle > 0.707107f || angle < -0.707107f) {
	            // North or south.
	            c1.x = from.x + mainDir.data[0] / 2;
	            c2.x = c1.x;
	            c1.y = from.y;
	            c2.y = to.y;
	        } else {
	            // East or west.
	            c1.x = from.x;
	            c2.x = to.x;
	            c1.y = from.y + mainDir.data[1] / 2;
	            c2.y = c1.y;
	        }
		}
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
  
		checkPointsArraySize(2);
		Point3 c0 = points.get(0);
		Point3 c1 = points.get(1);
		
		c0.x = from.x + vx;
		c0.y = from.y + vy;
		c1.x = to.x - vx;
		c1.y = to.y - vy;
  
		int m = multi + (edge.from == main.from ? 0 : 1);
  
		if(m % 2 == 0) {
			c0.x += vx2 + ox;
			c0.y += vy2 + oy;
			c1.x += vx2 + ox;
			c1.y += vy2 + oy;
		} else {
			c0.x -= vx2 - ox;
			c0.y -= vy2 - oy;
			c1.x -= vx2 - ox;
			c1.y -= vy2 - oy;	  
		}
	}
	
	protected void recomputeGeometryLoop(GraphicEdge edge, Camera camera) {
		int multi = edge.multi;
		double x = edge.from.center.x;
		double y = edge.from.center.y;
		double m = 1f + multi * 0.2;
		double s = camera.getMetrics().lengthToGu(edge.from.style.getSize(), 0);
		NodeSkeleton nodeSkel = (NodeSkeleton)edge.from.getSkeleton(); 
		if(nodeSkel != null) {
			Point3 nodeSize = nodeSkel.getSizeGU(camera);
			s = (nodeSize.x + nodeSize.y) / 2;
		}
		double d = s / 2 * m + 4 * s * m;

		checkPointsArraySize(2);
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
	
	protected void checkPointsArraySize(int minSize) {
		if(points == null) {
			points = new ArrayList<Point3>();
		}
		while(points.size() < minSize) {
			points.add(new Point3());
		}
	}
}