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
package org.graphstream.ui.graphicGraph.stylesheet;

import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;

/**
 * A value and the units of the value.
 * 
 * <p>
 * As a style sheet may express values in several different units. This class
 * purpose is to pack the value and the units it is expressed in into a single
 * object.
 * </p>
 */
public class Value extends Number {
	private static final long serialVersionUID = 1L;

	/**
	 * The value.
	 */
	public double value;

	/**
	 * The value units.
	 */
	public Style.Units units;

	// Constructor

	/**
	 * New value.
	 * 
	 * @param units
	 *            The value units.
	 * @param value
	 *            The value.
	 */
	public Value(Style.Units units, double value) {
		this.value = value;
		this.units = units;
	}

	/**
	 * New copy of another value.
	 * 
	 * @param other
	 *            The other value to copy.
	 */
	public Value(Value other) {
		this.value = other.value;
		this.units = other.units;
	}

	@Override
	public float floatValue() {
		return (float)value;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public int intValue() {
		return (int)Math.round(value);
	}

	@Override
	public long longValue() {
		return Math.round(value);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append(value);

		switch (units) {
		case GU:
			builder.append("gu");
			break;
		case PX:
			builder.append("px");
			break;
		case PERCENTS:
			builder.append("%");
			break;
		default:
			builder.append("wtf");
			break;
		}

		return builder.toString();
	}

	public boolean equals(Value o) {
		if (o != this) {
			if (!(o instanceof Value))
				return false;

			Value other = (Value) o;

			if (other.units != units)
				return false;

			if (other.value != value)
				return false;
		}

		return true;
	}

	/**
	 * Try to convert the given object into a single numerical value.
	 * 
	 * <p>
	 * It accepts string containing
	 * a value, {@link Number} instances and {@link Value} instance. If the value is a string,
	 * it can be suffixed by "px", "gu" or "%" and the units will be used. This method can also
	 * understand values that are an array of objects. In this case it tries to interpret the first
	 * cell of the array as it would do with a normal value and looks in the second cell of the
	 * array if it contains a Units instance to change the units of the returned value.
	 * </p>
	 * 
	 * <p>
	 * Therefore, here are examples of understood attributes values, thanks to this method:
	 * <pre>
	 *     A.addAttribute("a", 1); Value v = getNumber(A.getAttribute("a"));  
	 *     B.addAttribute("b", Value(Units.PX, 1); Value v = getNumber(B.getAttribute("b"));
	 *     C.addAttribute("c", "1px"; Value v = getNumber(C.getAttribute("c"));
	 *     D.addAttribute("d", 1, Units.PX); Value v = getNumber(D.getAttribute("d"));
	 *     E.addAttribute("e", new Pixels(1)); Value v = getNumber(E.getNumber("e"));
	 * </pre>
	 * </p>
	 * 
	 * @param value The data to convert.
	 * @return A value with its number and units.
	 * @throw NumberFormatException if the value is a string that does not contain a number.
	 */
	public static Value getNumber(Object value) throws NumberFormatException {
		// Value is a Number so we must test it first.
		if(value instanceof Value) {
			return (Value)value;
		} else if(value instanceof Number) {
			return new Value(Units.GU, ((Number)value).doubleValue());
		} else if(value instanceof String) {
			double n = 0;
			String s = (String) value; s = s.toLowerCase().trim();
			Units  u = Units.GU;
				
			if(s.endsWith("px")) {
				u = Units.PX;
				s = s.substring(0, s.length()-2).trim();
			} else if(s.endsWith("%")) {
				u = Units.PERCENTS;
				s = s.substring(0, s.length()-2).trim();
			} else if(s.endsWith("gu")) {
				s = s.substring(0, s.length()-2).trim();
			}
				
			n = Double.parseDouble(s);
			
			return new Value(u, n);
		} else if(value instanceof Object[]) {
			Object[] t = (Object[])value;
			if(t.length > 0) {
				Value v = getNumber(t[0]);
				if(t.length > 1 && t[1] instanceof Units) {
					v.units = (Units)t[1];
				}
				return v;
			}
		}
		
		throw new NumberFormatException("object cannot be converted to a number");
	}
}