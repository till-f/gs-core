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

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.GraphicSprite;
import org.graphstream.ui.graphicGraph.stylesheet.Style;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.BaseCamera;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.NodeSkeleton;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.SpriteSkeleton;

/**
 * Define how the graph is viewed.
 * 
 * <p>
 * In addition to the {@link BaseCamera}, this implementation defines how to pass from GU to PX and
 * the reverse using Java2D AffineTransform matrices. It is also in charge of modifying the
 * Graphics2D to setup the actual transform according to the camera settings
 * ({@link #pushView(GraphicGraph, Graphics2D)} then {@link #autoFitView(Graphics2D)} or
 * {@link #userView(Graphics2D)} and finally {@link #popView(Graphics2D)}).
 * </p>
 * 
 * <p>
 * It defines the visibility tests using the element skeletons.
 * </p>
 */
public class BasicCamera extends BaseCamera {
	/**
	 * The graph-space -> pixel-space transformation.
	 */
	protected AffineTransform Tx = new AffineTransform();

	/**
	 * The inverse transform of Tx.
	 */
	protected AffineTransform xT;

	/**
	 * The previous affine transform.
	 */
	protected AffineTransform oldTx;

	@Override
	public Point3 transformPxToGu(double x, double y) {
		Point2D.Double p = new Point2D.Double(x, y);
		xT.transform(p, p);
		return new Point3(p.x, p.y, 0);
	}

	@Override
	public Point3 transformGuToPx(double x, double y, double z) {
		Point2D.Double p = new Point2D.Double(x, y);
		Tx.transform(p, p);
		return new Point3(p.x, p.y, 0);
	}

	@Override
	public Point3 transformPxToGu(Point3 p) {
		Point2D.Double pp = new Point2D.Double(p.x, p.y);
		xT.transform(pp, pp);
		p.x = pp.x;
		p.y = pp.y;
		return p;
	}

	@Override
	public Point3 transformGuToPx(Point3 p) {
		Point2D.Double pp = new Point2D.Double(p.x, p.y);
		Tx.transform(pp, pp);
		p.x = pp.x;
		p.y = pp.y;
		return p;
	}

	/**
	 * Set the camera view in the and backup the previous
	 * transform of the graphics. Call {@link #popView(Graphics2D)} to restore
	 * the saved transform. You can only push one time the view.
	 * 
	 * @param g2
	 *            The Swing graphics to change.
	 */
	public void pushView(GraphicGraph graph, Graphics2D g2) {
		if (oldTx == null) {
			oldTx = g2.getTransform();

			if (autoFit)
				autoFitView(g2);
			else
				userView(g2);

			g2.setTransform(Tx);
		}
		
		checkVisibility(graph);
	}

	/**
	 * Restore the transform that was used before {@link #pushView(GraphicGraph, Graphics2D)}
	 * is used.
	 * 
	 * @param g2
	 *            The Swing graphics to restore.
	 */
	public void popView(Graphics2D g2) {
		if (oldTx != null) {
			g2.setTransform(oldTx);
			oldTx = null;
		}
	}

	/**
	 * Compute a transformation matrix that pass from graph units (user space)
	 * to pixel units (device space) so that the whole graph is visible.
	 * 
	 * @param g2
	 *            The Swing graphics.
	 */
	protected void autoFitView(Graphics2D g2) {
		double sx, sy;
		double tx, ty;
		double padXgu = getPaddingXgu() * 2;
		double padYgu = getPaddingYgu() * 2;
		double padXpx = getPaddingXpx() * 2;
		double padYpx = getPaddingYpx() * 2;

		sx = (metrics.surfaceSize.data[0] - padXpx) / (metrics.size.data[0] + padXgu); // Ratio along X
		sy = (metrics.surfaceSize.data[1] - padYpx) / (metrics.size.data[1] + padYgu); // Ratio along Y
		tx = metrics.lo.x + (metrics.size.data[0] / 2); // Center of graph in X
		ty = metrics.lo.y + (metrics.size.data[1] / 2); // Center of graph in Y

		if (sx > sy) // The least ratio.
			sx = sy;
		else
			sy = sx;

		Tx.setToIdentity();
		Tx.translate(metrics.surfaceSize.data[0] / 2, metrics.surfaceSize.data[1] / 2);
		if (rotation != 0)
			Tx.rotate(rotation / (180 / Math.PI));
		Tx.scale(sx, -sy);
		Tx.translate(-tx, -ty);

		xT = new AffineTransform(Tx);
		try {
			xT.invert();
		} catch (NoninvertibleTransformException e) {
			System.err.printf("cannot inverse gu2px matrix...%n");
		}

		zoom = 1;

		center.set(tx, ty, 0);
		metrics.setRatioPx2Gu(sx);
		metrics.loVisible.copy(metrics.lo);
		metrics.hiVisible.copy(metrics.hi);
	}

	/**
	 * Compute a transformation that pass from graph units (user space) to a
	 * pixel units (device space) so that the view (zoom and centre) requested
	 * by the user is produced.
	 * 
	 * @param g2
	 *            The Swing graphics.
	 */
	protected void userView(Graphics2D g2) {
		double sx, sy;
		double tx, ty;
		double padXgu = getPaddingXgu() * 2;
		double padYgu = getPaddingYgu() * 2;
		double padXpx = getPaddingXpx() * 2;
		double padYpx = getPaddingYpx() * 2;
		double gw = gviewport != null ? gviewport[2] - gviewport[0]
				: metrics.size.data[0];
		double gh = gviewport != null ? gviewport[3] - gviewport[1]
				: metrics.size.data[1];

		sx = (metrics.surfaceSize.data[0] - padXpx) / ((gw + padXgu) * zoom);
		sy = (metrics.surfaceSize.data[1] - padYpx) / ((gh + padYgu) * zoom);
		tx = center.x;
		ty = center.y;

		if (sx > sy) // The least ratio.
			sx = sy;
		else
			sy = sx;

		Tx.setToIdentity();
		Tx.translate(metrics.surfaceSize.data[0] / 2, metrics.surfaceSize.data[1] / 2); 
		if (rotation != 0)
			Tx.rotate(rotation / (180 / Math.PI));
		Tx.scale(sx, -sy);
		Tx.translate(-tx, -ty);

		xT = new AffineTransform(Tx);
		try {
			xT.invert();
		} catch (NoninvertibleTransformException e) {
			System.err.printf("cannot inverse gu2px matrix...%n");
		}

		metrics.setRatioPx2Gu(sx);

		double w2 = (metrics.surfaceSize.data[0] / sx) / 2;
		double h2 = (metrics.surfaceSize.data[1] / sx) / 2;

		metrics.loVisible.set(center.x - w2, center.y - h2);
		metrics.hiVisible.set(center.x + w2, center.y + h2);
	}

	@Override
	protected boolean isNodeVisibleIn(GraphicNode node, double X1, double Y1, double X2, double Y2) {
		NodeSkeleton nskel = (NodeSkeleton) node.getSkeleton();
		
		if(nskel == null) {
			nskel = new NodeSkeleton();
			node.setSkeleton(nskel);
		}
		
		return nskel.visibleIn(this, X1, Y1, X2, Y2, Units.PX);
	}

	@Override
	protected boolean isSpriteVisibleIn(GraphicSprite sprite, double X1, double Y1,
			double X2, double Y2) {
		if (sprite.isAttachedToNode() && nodeInvisible.contains(sprite.getNodeAttachment().getId())) {
			return false;	// To speed up things.
		} else if (sprite.isAttachedToEdge() && !isEdgeVisible(sprite.getEdgeAttachment())) {
			return false;	// To speed up things.
		} else {
			SpriteSkeleton sskel = (SpriteSkeleton) sprite.getSkeleton();
			
			if(sskel != null) {
				return sskel.visibleIn(this, X1, Y1, X2, Y2, Units.PX);
			} else {
				throw new RuntimeException("cannot compute visibility of node without skeleton");
			}
		}
	}

	@Override
	protected boolean nodeContains(GraphicElement elt, double x, double y) {
		NodeSkeleton skel = (NodeSkeleton) elt.getSkeleton();
		
		if(skel != null) {
			return skel.contains(this, x, y, Units.PX);
		}
		
		return false;
	}
	
	@Override
	protected boolean edgeContains(GraphicElement elt, double x, double y) {
		return false;
	}

	@Override
	protected boolean spriteContains(GraphicElement elt, double x, double y) {
		SpriteSkeleton skel = (SpriteSkeleton) elt.getSkeleton();
		
		if(skel != null) {
			return skel.contains(this, x, y, Units.PX);
		}
		
		return false;
	}
	
// Utility
	
	protected double getPaddingXgu() {
		if (padding.units == Style.Units.GU && padding.size() > 0)
			return padding.get(0);
	
		return 0;
	}
	
	protected double getPaddingYgu() {
		if (padding.units == Style.Units.GU && padding.size() > 1)
			return padding.get(1);
	
		return getPaddingXgu();
	}
	
	protected double getPaddingXpx() {
		if (padding.units == Style.Units.PX && padding.size() > 0)
			return padding.get(0);
	
		return 0;
	}
	
	protected double getPaddingYpx() {
		if (padding.units == Style.Units.PX && padding.size() > 1)
			return padding.get(1);
	
		return getPaddingXpx();
	}
}