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
package org.graphstream.ui.swingViewer.util;

import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.geom.Vector3;
import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants;
import org.graphstream.ui.graphicGraph.stylesheet.Value;
import org.graphstream.ui.graphicGraph.stylesheet.Values;
import org.graphstream.ui.swingViewer.Camera;

/**
 * Various geometric informations on the graphic graph.
 * 
 * <p>
 * This class provides not only metrics on the graphic graph (size in graph units) but also on the
 * rendering surface (size in pixels), and allows to convert from graph units (GU) to surface units
 * (PX) and the reverse. However, if you have a point in GU and want to convert it in PX it is
 * better to use the {@link Camera#transformGuToPx(Point3)} method.
 * </p>
 */
public class GraphMetrics {
	/**
	 * Graph lower position (bottom,left,front) in GU.
	 */
	public Point3 lo = new Point3();

	/**
	 * Graph higher position (top,right,back) in GU.
	 */
	public Point3 hi = new Point3();

	/**
	 * The lowest visible point in GU (the graph view port).
	 */
	public Point3 loVisible = new Point3();

	/**
	 * The highest visible point in GU (the graph viewport).
	 */
	public Point3 hiVisible = new Point3();

	/**
	 * Graph dimension in GU.
	 */
	public Vector3 size = new Vector3();

	/**
	 * The graph diagonal in GU.
	 */
	public double diagonal = 1;

	/**
	 * The view port size, in PX.
	 */
	public Vector3 surfaceSize = new Vector3();

	/**
	 * The scaling factor to pass from graph units to pixels.
	 */
	public double ratioPx2Gu;

	/**
	 * The length for one pixel, according to the current transformation, that is the length of
	 * one pixel in GU.
	 */
	public double px1;

	/**
	 * New canvas metrics with default values.
	 */
	public GraphMetrics() {
		setDefaults();
	}

	/**
	 * Set defaults value in the lo, hi and size fields to (-1) and (1)
	 * respectively.
	 */
	protected void setDefaults() {
		lo.set(-1, -1, -1);
		hi.set(1, 1, 1);
		size.set(2, 2, 2);

		diagonal = 1;
		ratioPx2Gu = 1;
		px1 = 1;
	}

	/**
	 * The graph diagonal (either in 2D or 3D, from the lowest point to the highest) in GU.
	 * 
	 * @return The diagonal in GU.
	 */
	public double getDiagonal() {
		return diagonal;
	}

	/**
	 * The graph bounds in GU.
	 * 
	 * @return The size in GU.
	 */
	public Vector3 getSize() {
		return size;
	}

	/**
	 * The graph lowest (bottom,left,front) point in GU.
	 * 
	 * @return The lowest point in GU.
	 */
	public Point3 getLowPoint() {
		return lo;
	}

	/**
	 * The graph highest (top,right,back) point in GU.
	 * 
	 * @return The highest point in GU.
	 */
	public Point3 getHighPoint() {
		return hi;
	}

	/**
	 * The graph width in GU.
	 */
	public double graphWidthGU() {
		return hi.x - lo.x;
	}

	/**
	 * The graph height in GU.
	 * @return
	 */
	public double graphHeightGU() {
		return hi.y - lo.y;
	}

	/**
	 * The graph depth in GU.
	 */
	public double graphDepthGU() {
		return hi.z - lo.z;
	}

	/**
	 * Convert a value in given units to graph units.
	 * 
	 * @param value
	 *            The value to convert.
	 * @param units
	 *            The units the value to convert is expressed in.
	 * @return The value converted to GU.
	 */
	public double lengthToGu(double value, StyleConstants.Units units) {
		switch (units) {
		case PX:
			return (value - 0.01f) / ratioPx2Gu;
		case PERCENTS:
			return (diagonal * value);
		case GU:
		default:
			return value;
		}
	}

	/**
	 * Convert a value in a given units to GU.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in pixels from to abscissa and ordinate in GU converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param value
	 *            The value to convert (it contains its own units).
	 */
	public double lengthToGu(Value value) {
		return lengthToGu(value.value, value.units);
	}

	/**
	 * Convert one of the given values in a given units to graph units.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in pixels from to abscissa and ordinate in GU converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param values
	 *            The values set containing the value to convert (it contains
	 *            its own units).
	 * @param index
	 *            Index of the value to convert.
	 */
	public double lengthToGu(Values values, int index) {
		return lengthToGu(values.get(index), values.units);
	}

	/**
	 * Convert a value in a given units to pixels.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in GU from to abscissa and ordinate in PX converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param value
	 *            The value to convert.
	 * @param units
	 *            The units the value to convert is expressed in.
	 * @return The value converted in pixels.
	 */
	public double lengthToPx(double value, StyleConstants.Units units) {
		switch (units) {
		case GU:
			return (value - 0.01f) * ratioPx2Gu;
		case PERCENTS:
			return (diagonal * value) * ratioPx2Gu;
		case PX:
		default:
			return value;
		}
	}

	/**
	 * Convert a value in a given units to pixels.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in GU from to abscissa and ordinate in PX converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param value
	 *            The value to convert (it contains its own units).
	 */
	public double lengthToPx(Value value) {
		return lengthToPx(value.value, value.units);
	}

	/**
	 * Convert one of the given values in a given units pixels.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in GU from to abscissa and ordinate in PX converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param values
	 *            The values set containing the value to convert (it contains
	 *            its own units).
	 * @param index
	 *            Index of the value to convert.
	 */
	public double lengthToPx(Values values, int index) {
		return lengthToPx(values.get(index), values.units);
	}

	/**
	 * Convert a value in given units to percents of the diagonal of the graph.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in GU from to abscissa and ordinate in percents converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param value
	 *            The value to convert.
	 * @param units
	 *            The units the value to convert is expressed in.
	 * @return The value converted to GU.
	 */
	public double lengthToPercents(double value, StyleConstants.Units units) {
		switch (units) {
			case PX:
				return (diagonal / ratioPx2Gu) / value;
			case GU:
				return diagonal / value;
			case PERCENTS:
			default:
				return value;
		}
	}

	/**
	 * Convert a value in a given units to percents of the diagonal of the graph.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in GU from to abscissa and ordinate in percents converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param value
	 *            The value to convert (it contains its own units).
	 */
	public double lengthToPercents(Value value) {
		return lengthToPercents(value.value, value.units);
	}

	/**
	 * Convert one of the given values in a given units to percents of the diagonal of the graph.
	 * 
	 * <p>
	 * Be very careful, a length is not the same as a position. You cannot find a point expressed
	 * in GU from to abscissa and ordinate in percents converted with this method. For example the
	 * GU space has its Y axis inverted compared to the pixel space. Use the {@link Camera#transformGuToPx(Point3)}
	 * for this.
	 * </p>
	 * 
	 * @param values
	 *            The values set containing the value to convert (it contains
	 *            its own units).
	 * @param index
	 *            Index of the value to convert.
	 */
	public double lengthToPercents(Values values, int index) {
		return lengthToGu(values.get(index), values.units);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(
				String.format("Graph Metrics :%n"));

		builder.append(String.format("        lo         = %s%n", lo));
		builder.append(String.format("        hi         = %s%n", hi));
		builder.append(String.format("        visible lo = %s%n", loVisible));
		builder.append(String.format("        visible hi = %s%n", hiVisible));
		builder.append(String.format("        size       = %s%n", size));
		builder.append(String.format("        diag       = %f%n", diagonal));
		builder.append(String.format("        viewport   = %s%n", surfaceSize));
		builder.append(String.format("        ratio      = %fpx = 1gu%n",
				ratioPx2Gu));

		return builder.toString();
	}

	/**
	 * Set the rendering surface size in PX.
	 * 
	 * <p>
	 * This is the size of the rendering
	 * canvas, it need to be set each time the rendering surface is changed.
	 * </p>
	 *
	 * <p>
	 * This is done by the camera automatically.
	 * </p>
	 * 
	 * @param surfaceWidth
	 *            The width in pixels of the view port.
	 * @param surfaceHeight
	 *            The width in pixels of the view port.
	 */
	public void setSurfaceSize(double surfaceWidth, double surfaceHeight) {
		surfaceSize.set(surfaceWidth, surfaceHeight, 0);
	}

	/**
	 * The ratio to pass by multiplication from pixels to graph units.
	 * 
	 * <p>This
	 * ratio must be larger than zero, else it is not taken into account by
	 * this method.
	 * </p>
	 *
	 * <p>
	 * This is done by the camera automatically.
	 * </p>
	 * 
	 * @param ratio
	 *            The ratio.
	 */
	public void setRatioPx2Gu(double ratio) {
		if (ratio > 0) {
			ratioPx2Gu = ratio;
			px1 = 0.95f / ratioPx2Gu;
		}
	}

	/**
	 * Set the graphic graph bounds (the lowest and highest points) in GU.
	 *
	 * <p>
	 * This is done by the camera automatically.
	 * </p>
	 * 
	 * @param minx
	 *            Lowest abscissa.
	 * @param miny
	 *            Lowest ordinate.
	 * @param minz
	 *            Lowest depth.
	 * @param maxx
	 *            Highest abscissa.
	 * @param maxy
	 *            Highest ordinate.
	 * @param maxz
	 *            Highest depth.
	 */
	public void setBounds(double minx, double miny, double minz, double maxx,
			double maxy, double maxz) {
		lo.x = minx;
		lo.y = miny;
		lo.z = minz;
		hi.x = maxx;
		hi.y = maxy;
		hi.z = maxz;

		size.data[0] = hi.x - lo.x;
		size.data[1] = hi.y - lo.y;
		size.data[2] = hi.z - lo.z;
		diagonal =  Math.sqrt(size.data[0] * size.data[0] + size.data[1]
				* size.data[1] + size.data[2] * size.data[2]);
	}
}