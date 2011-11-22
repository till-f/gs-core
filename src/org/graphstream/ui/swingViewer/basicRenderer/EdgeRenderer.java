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
package org.graphstream.ui.swingViewer.basicRenderer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.geom.Vector2;
import org.graphstream.ui.graphicGraph.GraphicEdge;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.ArrowShape;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.SizeMode;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.BaseSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.EdgeSkeleton;
import org.graphstream.ui.swingViewer.util.Camera;
import org.graphstream.ui.swingViewer.util.GraphMetrics;

public class EdgeRenderer extends ElementRenderer {
	protected Line2D shape = new Line2D.Double();
	
	protected Path2D curve = new Path2D.Double();

	protected double width = 1;

	protected double arrowLength = 0;

	protected double arrowWidth = 0;
	
	protected Path2D arrowShape = null;

	@Override
	protected void setupRenderingPass(StyleGroup group, Graphics2D g,
			Camera camera) {
	}

	@Override
	protected void pushStyle(StyleGroup group, Graphics2D g, Camera camera) {
		GraphMetrics metrics = camera.getMetrics();
		width = metrics.lengthToGu(group.getSize(), 0);
		arrowLength = metrics.lengthToGu(group.getArrowSize(), 0);
		arrowWidth = metrics.lengthToGu(group.getArrowSize(), group.getArrowSize().size() > 1 ? 1 : 0);

		g.setColor(group.getFillColor(0));
		g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
	}

	@Override
	protected void pushDynStyle(StyleGroup group, Graphics2D g, Camera camera, GraphicElement element) {
		EdgeSkeleton skel = (EdgeSkeleton)element.getSkeleton();
		
		Color color = skel.getColor();
		Point3 size = skel.getSizeGU(camera);

		g.setColor(color);

		if (group.getSizeMode() == SizeMode.DYN_SIZE) {
			width = size.x;
			g.setStroke(new BasicStroke((float) width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
		}
	}

	@Override
	protected void elementInvisible(StyleGroup group, Graphics2D g,
			Camera camera, GraphicElement element) {
	}

	@Override
	protected void renderElement(StyleGroup group, Graphics2D g, Camera camera, GraphicElement element) {
		GraphicEdge edge = (GraphicEdge) element;
		EdgeSkeleton skel = (EdgeSkeleton)edge.getSkeleton();

		Point3 src = edge.from.center;
		Point3 trg = edge.to.center;
		
		switch(skel.getKind(camera)) {
			case CUBIC_CURVE:
				Point3 c0 = skel.getPoint(0, camera);
				Point3 c1 = skel.getPoint(1, camera);
				curve.reset();
				curve.moveTo(src.x, src.y);
				curve.curveTo(c0.x, c0.y, c1.x, c1.y, trg.x, trg.y);
				g.draw(curve);
				break;
			case POINTS:
			case VECTORS:
			case LINE:
			default:
				shape.setLine(src.x, src.y, trg.x, trg.y);
				g.draw(shape);
				break;
		}

		renderArrow(group, g, camera, edge, skel);// Does not modify the graphics.
		
		if(edge.label != null) {
			textRenderer.queueElement(element);
		}
	}

	protected void renderArrow(StyleGroup group, Graphics2D g, Camera camera,
			GraphicEdge edge, EdgeSkeleton skel) {
		if (edge.isDirected() && arrowWidth > 0 && arrowLength > 0) {
			if (group.getArrowShape() != ArrowShape.NONE) {
				if(arrowShape == null)
					arrowShape = new Path2D.Double();
				
				// XXX In theory, we can compute the position of the arrow attachment
				// automatically when the attached node moves, instead of evaluating it
				// each time we draw the arrow. XXX TODO see arrowPos in EdgeSkeleton.
				
				GraphicNode node0 = (GraphicNode) edge.getNode0();
				GraphicNode node1 = (GraphicNode) edge.getNode1();
				Point3 src = skel.getSourcePoint();
				Point3 trg = skel.getTargetPoint();
				double off = evalEllipseRadius(edge, node0, node1, camera);
				
				Vector2 theDirection = new Vector2(trg.x - src.x, trg.y - src.y);

				theDirection.normalize();

				double x = trg.x - (theDirection.data[0] * off);
				double y = trg.y - (theDirection.data[1] * off);
				Vector2 perp = new Vector2(theDirection.data[1], -theDirection.data[0]);

				perp.normalize();
				theDirection.scalarMult(arrowLength);
				perp.scalarMult(arrowWidth);

				// Create a polygon.

				arrowShape.reset();
				arrowShape.moveTo(x, y);
				arrowShape.lineTo(x - theDirection.data[0] + perp.data[0], y - theDirection.data[1] + perp.data[1]);
				arrowShape.lineTo(x - theDirection.data[0] - perp.data[0], y - theDirection.data[1] - perp.data[1]);
				arrowShape.closePath();

				g.fill(arrowShape);
			}
		}
	}

	protected double evalEllipseRadius(GraphicEdge edge, GraphicNode sourceNode, GraphicNode targetNode, Camera camera) {
		Point3 size = ((BaseSkeleton)(targetNode.getSkeleton())).getSizeGU(camera);
		double w = size.x;
		double h = size.y;

		if (w == h)
			return w / 2; // Welcome simplification for circles ...

		// Vector of the entering edge.

		Point3 p0 = sourceNode.getCenter();
		Point3 p1 = targetNode.getCenter();
		
		double dx = p1.x - p0.x;
		double dy = p1.y - p0.y;

		// The entering edge must be deformed by the ellipse ratio to find the
		// correct angle.

		dy *= (w / h);

		// Find the angle of the entering vector with (1,0).

		double d = Math.sqrt(dx * dx + dy * dy);
		double a = dx / d;

		// Compute the coordinates at which the entering vector and the ellipse
		// cross.

		a = Math.acos(a);
		dx = Math.cos(a) * w;
		dy = Math.sin(a) * h;

		// The distance from the ellipse center to the crossing point of the
		// ellipse and vector.

		return Math.sqrt(dx * dx + dy * dy);
	}

}