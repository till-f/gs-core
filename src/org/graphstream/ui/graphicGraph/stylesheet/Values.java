/*
 * Copyright 2006 - 2012
 *      Stefan Balev       <stefan.balev@graphstream-project.org>
 *      Julien Baudry	<julien.baudry@graphstream-project.org>
 *      Antoine Dutot	<antoine.dutot@graphstream-project.org>
 *      Yoann Pigné	<yoann.pigne@graphstream-project.org>
 *      Guilhelm Savin	<guilhelm.savin@graphstream-project.org>
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

import java.util.ArrayList;
import java.util.Iterator;

import org.graphstream.ui.graphicGraph.stylesheet.StyleConstants.Units;

/**
 * Several values and the units of these values.
 * 
 * <p>
 * As a style sheet may express values in several different units. This class
 * purpose is to pack the value and the units it is expressed in into a single
 * object.
 * </p>
 */
public class Values implements Iterable<Double> {
	// Attributes

	/**
	 * The value.
	 */
	public ArrayList<Double> values = new ArrayList<Double>();

	/**
	 * The values units.
	 */
	public Style.Units units;

	// Constructor

	/**
	 * New value set with one initial value.
	 * 
	 * @param units
	 *            The values units.
	 * @param values
	 *            A variable count of values.
	 */
	public Values(Style.Units units, double... values) {
		this.units = units;

		for (double value : values)
			this.values.add(value);
	}

	/**
	 * New copy of another value set.
	 * 
	 * @param other
	 *            The other values to copy.
	 */
	public Values(Values other) {
		this.values = new ArrayList<Double>(other.values);
		this.units = other.units;
	}

	/**
	 * New set of one value.
	 * 
	 * @param value
	 *            The value to copy with its units.
	 */
	public Values(Value value) {
		this.values = new ArrayList<Double>();
		this.units = value.units;

		values.add(value.value);
	}

	/**
	 * Number of values in this set.
	 * 
	 * @return The number of values.
	 */
	public int size() {
		return values.size();
	}

	/**
	 * Number of values in this set.
	 * 
	 * @return The number of values.
	 */
	public int getValueCount() {
		return values.size();
	}

	/**
	 * The i-th value of this set. If the index is less than zero, the first
	 * value is given, if the index if greater or equal to the number of values,
	 * the last value is given.
	 * 
	 * @param i
	 *            The value index.
	 * @return The corresponding value.
	 */
	public double get(int i) {
		if (i < 0)
			return values.get(0);
		else if (i >= values.size())
			return values.get(values.size() - 1);
		else
			return values.get(i);
	}

	/**
	 * Values units.
	 * 
	 * @return The units used for each value.
	 */
	public Style.Units getUnits() {
		return units;
	}

	@Override
	public boolean equals(Object o) {
		if (o != this) {
			if (!(o instanceof Values))
				return false;

			Values other = (Values) o;

			if (other.units != units)
				return false;

			int n = values.size();

			if (other.values.size() != n)
				return false;

			for (int i = 0; i < n; i++) {
				if (!other.values.get(i).equals(values.get(i)))
					return false;
			}
		}

		return true;
	}

	public Iterator<Double> iterator() {
		return values.iterator();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append('(');
		for (double value : values) {
			builder.append(' ');
			builder.append(value);
		}
		builder.append(" )");

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
			builder.append("wtf (what's the fuck?)");
			break;
		}

		return builder.toString();
	}

	/**
	 * Copy the given values to this set. The units are also copied.
	 * 
	 * @param values
	 *            The values to copy.
	 */
	public void copy(Values values) {
		units = values.units;
		this.values.clear();
		this.values.addAll(values.values);
	}
	
	/**
	 * Append the given value at the end of this set.
	 * 
	 * @param value
	 * 			The value to add.
	 */
	public void addValue(double value) {
		this.values.add(value);
	}

	/**
	 * Append the given set of values at the end of this set.
	 * 
	 * @param values
	 *            The value set to append.
	 */
	public void addValues(double... values) {
		for (double value : values)
			this.values.add(value);
	}

	/**
	 * Insert the given value at the given index.
	 * 
	 * @param i
	 *            Where to insert the value.
	 * @param value
	 *            The value to insert.
	 */
	public void insertValue(int i, double value) {
		values.add(i, value);
	}

	/**
	 * Change the i-th value.
	 * 
	 * @param i
	 *            The value index.
	 * @param value
	 *            The value to put.
	 */
	public void setValue(int i, double value) {
		values.set(i, value);
	}

	/**
	 * Remove the i-th value.
	 * 
	 * @param i
	 *            The index at which the value is to be removed.
	 */
	public void removeValue(int i) {
		values.remove(i);
	}

	/**
	 * Change the values units.
	 * 
	 * @param units
	 *            The units.
	 */
	public void setUnits(Style.Units units) {
		this.units = units;
	}
	
	/**
	 * Try to convert the given object into one or more numerical value with units.
	 * 
	 * <p>
	 * It accepts string containing
	 * a value, {@link Number} instances and {@link Value} or {@link Values} instances.
	 * If the value is a string,
	 * it can be suffixed by "px", "gu" or "%" and the units will be used. This method can also
	 * understand values that are an array of objects. In this case it tries to interpret the first
	 * cell of the array as a unit if provided, then all the remaining cells as individual values.
	 * </p>
	 * 
	 * <p>
	 * Therefore, here are examples of understood attributes values, thanks to this method:
	 * <pre>
	 *     A.addAttribute("a", 1); Values v = getNumbers(A.getAttribute("a"));  
	 *     B.addAttribute("b", Value(Units.PX, 1); Values v = getNumbers(B.getAttribute("b"));
	 *     C.addAttribute("c", Values(Units.PX, 1, 2)); Values v = getNumbers(C.getAttribute("c"));
	 *     D.addAttribute("d", "1px"; Values v = getNumbers(D.getAttribute("d"));
	 *     E.addAttribute("e", Units.PX, 1); Values v = getNumbers(D.getAttribute("d"));
	 *     F.addAttribute("f", Units.PX, 1, 2); Values v = getNumbers(F.getAttribute("f"));
	 *     G.addAttribute("g", new Pixels(1)); Values v = getNumbers(G.getNumber("g"));
	 *     H.addAttribute("h", new Pixels(1, 2)); Values v = getNumbers(H.getNumber("h"));
	 * </pre>
	 * </p>
	 * 
	 * @param value The data to convert, maybe an array.
	 * @return values with its units.
	 * @throw NumberFormatException if the value is a string that does not contain a number.
	 */
	public static Values getNumbers(Object value) throws NumberFormatException {
		// Value is a Number so we must test it first.
		if(value instanceof Value) {
			return new Values((Value)value);
		} else if(value instanceof Values) {
			return (Values)value;
		} else if(value instanceof Number) {
			return new Values(Units.GU, ((Number)value).doubleValue());
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
			
			return new Values(u, n);
		} else if(value instanceof Object[]) {
			Object[] t = (Object[])value;
			int i = 0;
			int n = t.length;
			Units units = Units.GU;
		
			if(i < n && t[i] instanceof Units) {
				units = (Units)t[i];
				i++;
			}
			
			Values result = new Values(units);
			
			while(i < n) {
				if(t[i] instanceof Number) {
					result.addValue(((Number)t[i]).doubleValue());
				} else {
					Value v = Value.getNumber(t[i]);
					result.addValue(v.value);
				}
				i++;
			}
			
			return result;
		}
		
		throw new NumberFormatException("object "+value.getClass().getName()+" cannot be converted to a number");
	}
}