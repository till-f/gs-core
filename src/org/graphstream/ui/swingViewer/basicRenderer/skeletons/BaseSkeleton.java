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

import java.awt.Color;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.StyleGroup;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.Value;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;
import org.graphstream.ui.swingViewer.util.Camera;

/**
 * Base skeleton for nodes, edges and sprites.
 * 
 * <p>
 * The skeleton makes a back reference to its element to ease the updating.
 * </p>
 * 
 * <p>
 * The base skeleton handles the fill color and the size of the element. It can update them
 * if the color or size is dynamic. It computes them only when needed (when an access is done
 * on them). Each time a change is made to the style or the corresponding "ui.color" or "ui.size"
 * attributes, a flag allows to mark them as obsolete so that they are recomputed at first
 * access when needed.
 * </p>
 */
public abstract class BaseSkeleton implements GraphicElement.Skeleton {
	/**
	 * Back reference on the element.
	 */
	protected GraphicElement element;
	
	/**
	 * Should the color be computed when accessed ?
	 */
	protected boolean colorDirty = true;

	/**
	 * The color of the element.
	 */
	protected Color color;

	/**
	 * Should the size be computed hen accessed ?
	 */
	protected boolean sizeDirty = true;
	
	/**
	 * The size of the element.
	 */
	protected Point3 size = new Point3();
	
	/**
	 * Color of the element.
	 */
	public Color getColor() {
		if(colorDirty) {
			computeColor();
		}
		
		return color;
	}

	/**
	 * Size of the element in graph units.
	 * @param camera The camera.
	 */
	public Point3 getSizeGU(Camera camera) {
		if(sizeDirty) {
			computeSize(camera);
		}
		
		return size;
	}

	/**
	 * Size of the element, in the units specified. 
	 * @param camera The camera.
	 * @param units The units you want the size in.
	 */
	public Point3 getSize(Camera camera, Units units) {
		if(sizeDirty) {
			computeSize(camera);
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

	/**
	 * The absolute coordinates of the element on the canvas in the units given. The position may be
	 * different from the center of an element. The center coordinates may be relative coordinates.
	 * The position are absolute coordinates. The center are the values specified by the user to
	 * position an element. The position is the real position on screen of the element. For example,
	 * sprites can be attached and their center is expressed in coordinates relative to the
	 * attachment, the position are absolute coordinates.
	 * @param camera The camera.
	 * @param pos The memory where to store the computed position, if null, a point is created.
	 * @param units The units in which you want the result.
	 * @return The computed position, either the point you passed as "pos" or a new point if "pos"
	 *   was null.
	 */
	abstract public Point3 getPosition(Camera camera, Point3 pos, Units units);
	
	public void installed(GraphicElement element) {
		this.element = element;
		sizeDirty = true;
		colorDirty = true;
	}

	public void uninstalled() {
		this.element = null;
	}

	public void positionChanged() {
	}

	public void pointsChanged(Object newValue) {
	}

	public void sizeChanged(Object newValue) {
		sizeDirty = true;
	}

	public void labelChanged() {
	}

	public void iconChanged(Object newValue) {
	}

	public void colorChanged(Object newValue) {
		colorDirty = true;
	}
	
	public void unknownUIAttributeChanged(String attribute, Object newValue) {
	}

	public void styleChanged() {
		colorDirty = true;
		sizeDirty = true;
	}
	
	/**
	 * Compute or recompute the size of the element, after a style change, a "ui.size" attribute
	 * change, etc.
	 * @param camera The camera.
	 */
	protected void computeSize(Camera camera) {
		StyleGroup style = element.style;
		Values sizes = style.getSize();

		size.x = camera.getMetrics().lengthToGu(sizes, 0);
		size.y = sizes.size() > 1 ? camera.getMetrics().lengthToGu(sizes, 1) : size.x;
		
		if(style.getSizeMode() == StyleConstants.SizeMode.DYN_SIZE ) {
			Object o = element.getAttribute("ui.size");
			
			if(o != null) {
				if(o instanceof Values) {
					Values val = (Values)o;
					size.x = camera.getMetrics().lengthToGu(val, 0);
					size.y = val.size() > 1 ? camera.getMetrics().lengthToGu(val, 1) : size.x;
				} else {
					try {
						double ratio = size.x / size.y;
						Value val = Value.getNumber(element.getAttribute("ui.size"));
						size.x = camera.getMetrics().lengthToGu(val);
						size.y = size.x * ratio;
					} catch(NumberFormatException e) {}
				}
			}
			
		}
		
		sizeDirty = false;
	}
	
	/**
	 * Compute or recompute the color of the element, after a style change, a "ui.color" attribute
	 * change, etc.
	 */
	protected void computeColor() {
		if(element.style.getFillMode() == StyleConstants.FillMode.DYN_PLAIN) {
			Object value = element.getAttribute("ui.color");
			
			if(value != null) {
				if(value instanceof Color) {
					color = (Color) value;
				} else if(value instanceof Number) {
					color = interpolateColor(((Number)value).doubleValue());
				}
			} else {
				color = element.style.getFillColor(0);
			}
		} else {
			color = element.style.getFillColor(0);
		}
		
		colorDirty = false;
	}
	
	/**
	 * Compute a color interpolated from the fill-color palette of an element,
	 * according to a value between 0 and 1.
	 * @param value A real number between 0 and 1.
	 * @return The interpolated color.
	 */
	protected Color interpolateColor(double value) {
		int n = element.style.getFillColorCount();
		value = value < 0 ? 0 : (value > 1 ? 1 : value);
		
		if (value == 1) {
			return element.style.getFillColor(n - 1);
		} else if(value == 0) {
			return element.style.getFillColor(0);
		} else {
			double div = 1f / (n - 1);
			int col = (int) (value / div);
			div = (value - (div * col)) / div;

			Color color0 = element.style.getFillColor(col);
			Color color1 = element.style.getFillColor(col + 1);

			double red   = ((color0.getRed()   * (1 - div)) + (color1 .getRed()   * div)) / 255.0;
			double green = ((color0.getGreen() * (1 - div)) + (color1 .getGreen() * div)) / 255.0;
			double blue  = ((color0.getBlue()  * (1 - div)) + (color1 .getBlue()  * div)) / 255.0;
			double alpha = ((color0.getAlpha() * (1 - div)) + (color1 .getAlpha() * div)) / 255.0;

			return new Color((float) red, (float) green, (float) blue, (float) alpha);
		}
	}
}