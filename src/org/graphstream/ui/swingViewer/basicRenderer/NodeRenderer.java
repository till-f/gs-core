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
import java.awt.geom.Ellipse2D;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.swingViewer.basicRenderer.skeletons.BaseSkeleton;
import org.graphstream.ui.swingViewer.util.Camera;
import org.graphstream.ui.swingViewer.util.GraphMetrics;

public class NodeRenderer extends ElementRenderer {
	protected Ellipse2D shape;

	protected double width, height, w2, h2;
	
	protected Color strokeColor = null;
	
	protected double strokeWidth = 0;
	
	@Override
	protected void setupRenderingPass(StyleGroup group, Graphics2D g,
			Camera camera) {
	}

	@Override
	protected void pushStyle(StyleGroup group, Graphics2D g, Camera camera) {
		GraphMetrics metrics = camera.getMetrics();
		Values size = group.getSize();
		shape = new Ellipse2D.Double();
		width = metrics.lengthToGu(size, 0);
		height = size.size() > 1 ? metrics.lengthToGu(size, 1) : width;
		w2 = width / 2;
		h2 = height / 2;

		g.setColor(group.getFillColor(0));
		
		if(group.getStrokeMode() != StyleConstants.StrokeMode.NONE) {
			strokeWidth = camera.getMetrics().lengthToGu(group.getStrokeWidth());
			if(strokeWidth > 0) {
				strokeColor = group.getStrokeColor(0);
				g.setStroke(new BasicStroke((float) strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
			}
		}
	}

	@Override
	protected void pushDynStyle(StyleGroup group, Graphics2D g, Camera camera,
			GraphicElement element) {
		BaseSkeleton skel = (BaseSkeleton)element.getSkeleton();
		Color color = skel.getColor();
		Point3 size = skel.getSizeGU(camera);

		g.setColor(color);

		width = size.x;
		height = size.y;
		w2 = width / 2;
		h2 = height / 2;
	}

	@Override
	protected void renderElement(StyleGroup group, Graphics2D g, Camera camera, GraphicElement element) {
		BaseSkeleton skel = (BaseSkeleton)element.getSkeleton();
		Point3 pos = skel.getPosition(camera, null, Units.GU);

		shape.setFrame(pos.x - w2, pos.y - h2, width, height);
		g.fill(shape);

		if(strokeWidth > 0) {
			Color c = g.getColor();
			g.setColor(strokeColor);
			g.draw(shape);
			g.setColor(c);
		}
		
		if(element.label != null)
			textRenderer.queueElement(element);
	}

	@Override
	protected void elementInvisible(StyleGroup group, Graphics2D g,
			Camera camera, GraphicElement element) {
	}
}