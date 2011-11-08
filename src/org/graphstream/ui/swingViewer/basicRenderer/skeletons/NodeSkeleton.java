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
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicNode;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.swingViewer.util.Camera;

public class NodeSkeleton extends BaseSkeleton {

	protected boolean dirty = true;
	
	protected Point3 size = new Point3();
	
	@Override
	public void installed(GraphicElement element) {
		super.installed(element);
		dirty = true;
	}
	
	@Override
	public void styleChanged() {
		dirty = true;
	}
	
	@Override
	public void sizeChanged(Object newValue) {
		dirty = true;
	}
	
	public Point3 getSizeGU(Camera camera) {
		if(dirty) {
			recomputeGeometry(camera);
		}
		
		return size;
	}
	
	public Point3 getSize(Camera camera, Units units) {
		if(dirty) {
			recomputeGeometry(camera);
		}
		
		if(units == Units.GU) {
			return size;
		} else if(units == Units.PX) {
			Point3 s = new Point3();
			s.x = camera.getMetrics().lengthToPx(size.x, Units.GU);
			s.y = camera.getMetrics().lengthToPx(size.y, Units.GU);
			return s;
		} else {
			throw new RuntimeException("TODO");
		}
	}
	
	public Point3 getPosition(Camera camera, Units units) {
		return getPosition(camera, null, units);
	}
	
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
	
	public boolean contains(Camera camera, double x, double y, Units units) {
		getSizeGU(camera);
		
		Point3 pos = getPosition(camera, Units.GU);
		Point3 p   = new Point3(x, y, 0);
		
		if(units == Units.PX)
			p = camera.transformPxToGu(p);
		
		if (x < (pos.x - size.x/2)) return false;
		if (y < (pos.y - size.y/2)) return false;
		if (x > (pos.x + size.x/2)) return false;
		if (y > (pos.y + size.y/2)) return false;

		return true;
	}
	
	public boolean visibleIn(Camera camera, double X1, double Y1, double X2, double Y2, Units units) {
		Point3 s  = getSize(camera, Units.PX);
		double w2 = s.x;
		double h2 = s.y;
		Point3 p  = getPosition(camera, null, Units.PX);

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
	
	protected void recomputeGeometry(Camera camera) {
		GraphicNode node = (GraphicNode) element;
		StyleGroup style = node.style;
		Values sizes = style.getSize();

		size.x = camera.getMetrics().lengthToGu(sizes, 0);
		size.y = sizes.size() > 1 ? camera.getMetrics().lengthToGu(sizes, 1) : size.x;
		
		if(style.getSizeMode() == StyleConstants.SizeMode.DYN_SIZE && node.hasNumber("ui.size")) {
			double ratio = size.x / size.y;
			size.x = node.getNumber("ui.size");
			size.y = size.x * ratio;
		}
		
		dirty = false;
	}
}