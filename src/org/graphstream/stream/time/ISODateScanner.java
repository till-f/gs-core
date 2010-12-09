/*
 * This file is part of GraphStream.
 * 
 * GraphStream is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * GraphStream is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GraphStream.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2006 - 2010
 * 	Julien Baudry
 * 	Antoine Dutot
 * 	Yoann Pigné
 * 	Guilhelm Savin
 */
package org.graphstream.stream.time;

import java.text.ParseException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.graphstream.stream.time.ISODateComponent.AliasComponent;
import org.graphstream.stream.time.ISODateComponent.AMPMHandler;
import org.graphstream.stream.time.ISODateComponent.EpochHandler;
import org.graphstream.stream.time.ISODateComponent.IntegerFieldHandler;
import org.graphstream.stream.time.ISODateComponent.TextComponent;
import org.graphstream.stream.time.ISODateComponent.UTFOffsetHandler;

/**
 * Scanner for date in ISO/IEC 9899:1999 format. The scanner takes a format and
 * then is able to parse timestamp in the given format.
 * 
 * The <i>parse()</i> return a {@link java.util.Calendar} for convenience.
 * 
 * Format of the scanner can be composed of %? directive which define components
 * of the time. These directives are listed below. For example, the format
 * "%F %T", which is equivalent to "%Y-%m-%d %H:%M:%S" can parse the following
 * timestamp: "2010-12-09 03:45:39";
 * 
 * <dl>
 * <dt>%a</dt>
 * <dd>locale's abbreviated weekday name</dd>
 * <dt>%A</dt>
 * <dd>locale's weekday name</dd>
 * <dt>%b</dt>
 * <dd>locale's abbreviated month name</dd>
 * <dt>%B</dt>
 * <dd>locale's month name</dd>
 * <dt>%c</dt>
 * <dd>locale's date and time representation</dd>
 * <dt>%C</dt>
 * <dd>two first digits of full year as an integer (00-99)</dd>
 * <dt>%d</dt>
 * <dd>day of the month (01-31)</dd>
 * <dt>%D</dt>
 * <dd>%m/%d/%y</dd>
 * <dt>%e</dt>
 * <dd>day of the month (1-31)</dd>
 * <dt>%F</dt>
 * <dd>%Y-%m-%d</dd>
 * <dt>%g</dt>
 * <dd>last 2 digits of the week-based year (00-99)</dd>
 * <dt>%G</dt>
 * <dd>"week-based year as a decimal number</dd>
 * <dt>%h</dt>
 * <dd>%b</dd>
 * <dt>%H</dt>
 * <dd>hour (24-hour clock) as a decimal number (00-23)</dd>
 * <dt>%I</dt>
 * <dd>hour (12-hour clock) as a decimal number (01-12)</dd>
 * <dt>%j</dt>
 * <dd>day of the year as a decimal number (001-366)</dd>
 * <dt>%k</dt>
 * <dd>milliseconds as a decimal number (001-999)</dd>
 * <dt>%K</dt>
 * <dd>milliseconds since the epoch</dd>
 * <dt>%m</dt>
 * <dd>month as a decimal number (01-12)</dd>
 * <dt>%M</dt>
 * <dd>minute as a decimal number (00-59)</dd>
 * <dt>%n</dt>
 * <dd>\n</dd>
 * <dt>%p</dt>
 * <dd>locale-s equivalent of the AM/PM</dd>
 * <dt>%r</dt>
 * <dd>locale's 12-hour clock time</dd>
 * <dt>%R</dt>
 * <dd>%H:%M</dd>
 * <dt>%S</dt>
 * <dd>second as a decimal number (00-60)</dd>
 * <dt>%t</dt>
 * <dd>\t</dd>
 * <dt>%T</dt>
 * <dd>%H:%M:%S</dd>
 * <dt>%u</dt>
 * <dd>ISO 8601 weekday as a decimal number (1-7)</dd>
 * <dt>%U</dt>
 * <dd>week number of the year as a decimal number (00-53)</dd>
 * <dt>%V</dt>
 * <dd>ISO 8601 week number as a decimal number (01-53)</dd>
 * <dt>%w</dt>
 * <dd>weekday as a decimal number (0-6)</dd>
 * <dt>%W</dt>
 * <dd>week number of the year as a decimal number (00-53)</dd>
 * <dt>%x</dt>
 * <dd>locale's date representation</dd>
 * <dt>%X</dt>
 * <dd>locale's time representation</dd>
 * <dt>%y</dt>
 * <dd>last 2 digits of the year as a decimal number (00-99)</dd>
 * <dt>%Y</dt>
 * <dd>year as a decimal number</dd>
 * <dt>%z</dt>
 * <dd>offset from UTC in the ISO 8601 format</dd>
 * <dt>%Z</dt>
 * <dd>locale's time zone name of abbreviation or empty</dd>
 * </dl>
 * 
 * @author Guilhelm Savin
 */
public class ISODateScanner {

	private static final ISODateComponent[] KNOWN_COMPONENTS = {
			new ISODateComponent("%a", "\\w+[.]", null),
			new ISODateComponent("%A", "\\w+", null),
			new ISODateComponent("%b", "\\w+[.]", null),
			new ISODateComponent("%B", "\\w+", null),
			new ISODateComponent("%c", null, null),
			new ISODateComponent("%C", "\\d\\d", new IntegerFieldHandler(
					Calendar.YEAR)),
			new ISODateComponent("%d", "[012]\\d|3[01]",
					new IntegerFieldHandler(Calendar.DAY_OF_MONTH)),
			new AliasComponent("%D", "%m/%d/%y"),
			new ISODateComponent("%e", "\\d|[12]\\d|3[01]",
					new IntegerFieldHandler(Calendar.DAY_OF_MONTH)),
			new AliasComponent("%F", "%Y-%m-%d"),
			new ISODateComponent("%g", "\\d\\d", new IntegerFieldHandler(
					Calendar.YEAR)),
			new ISODateComponent("%G", "\\d{4}", new IntegerFieldHandler(
					Calendar.YEAR)),
			new AliasComponent("%h", "%b"),
			new ISODateComponent("%H", "[01]\\d|2[0123]",
					new IntegerFieldHandler(Calendar.HOUR_OF_DAY)),
			new ISODateComponent("%I", "0\\d|1[012]", new IntegerFieldHandler(
					Calendar.HOUR)),
			new ISODateComponent("%j", "[012]\\d\\d|3[0-5]\\d|36[0-6]",
					new IntegerFieldHandler(Calendar.DAY_OF_YEAR)),
			new ISODateComponent("%k", "\\d{3}", new IntegerFieldHandler(
					Calendar.MILLISECOND)),
			new ISODateComponent("%K", "\\d+", new EpochHandler()),
			new ISODateComponent("%m", "0\\d|1[012]", new IntegerFieldHandler(
					Calendar.MONTH, -1)),
			new ISODateComponent("%M", "[0-5]\\d", new IntegerFieldHandler(
					Calendar.MINUTE)),
			new AliasComponent("%n", "\n"),
			new ISODateComponent("%p", "", new AMPMHandler()),
			new ISODateComponent("%r", "", null),
			new AliasComponent("%R", "%H:%M"),
			new ISODateComponent("%S", "[0-5]\\d|60", new IntegerFieldHandler(
					Calendar.SECOND)),
			new AliasComponent("%t", "\t"),
			new AliasComponent("%T", "%H:%M:%S"),
			new ISODateComponent("%u", "[1-7]", new IntegerFieldHandler(
					Calendar.DAY_OF_WEEK)),
			new ISODateComponent("%U", "[0-4]\\d|5[0123]",
					new IntegerFieldHandler(Calendar.WEEK_OF_YEAR, 1)),
			new ISODateComponent("%V", "0[1-9]|[2-4]\\d|5[0123]", null),
			new ISODateComponent("%w", "[0-6]", new IntegerFieldHandler(
					Calendar.DAY_OF_WEEK)),
			new ISODateComponent("%W", "[0-4]\\d|5[0123]",
					new IntegerFieldHandler(Calendar.WEEK_OF_YEAR)),
			new ISODateComponent("%x", "", null),
			new ISODateComponent("%X", "", null),
			new ISODateComponent("%y", "\\d\\d", new IntegerFieldHandler(
					Calendar.YEAR)),
			new ISODateComponent("%Y", "\\d{4}", new IntegerFieldHandler(
					Calendar.YEAR)),
			new ISODateComponent("%z", "[-+]\\d{4}", new UTFOffsetHandler()),
			new ISODateComponent("%Z", "\\w*", null),
			new AliasComponent("%%", "%") };

	/**
	 * List of components, build from a string format. Some of these components
	 * can just be text.
	 */
	protected LinkedList<ISODateComponent> components;
	/**
	 * The regular expression builds from the components.
	 */
	protected Pattern pattern;

	/**
	 * Create a new scanner with a given format.
	 * 
	 * @param format
	 *            format of the scanner.
	 * @throws ParseException
	 *             if bad directives found
	 */
	public ISODateScanner(String format) throws ParseException {
		components = findComponents(format);
		buildRegularExpression();
	}

	/**
	 * Build a list of component from a string.
	 * 
	 * @param format
	 *            format of the scanner
	 * @return a list of components found in the string format
	 * @throws ParseException
	 *             if invalid component found
	 */
	protected LinkedList<ISODateComponent> findComponents(String format)
			throws ParseException {
		LinkedList<ISODateComponent> components = new LinkedList<ISODateComponent>();
		int offset = 0;

		while (offset < format.length()) {
			if (format.charAt(offset) == '%') {
				boolean found = false;
				for (int i = 0; !found && i < KNOWN_COMPONENTS.length; i++) {
					if (format.startsWith(KNOWN_COMPONENTS[i].getShortcut(),
							offset)) {
						found = true;
						if (KNOWN_COMPONENTS[i].isAlias()) {
							LinkedList<ISODateComponent> sub = findComponents(KNOWN_COMPONENTS[i]
									.getReplacement());
							components.addAll(sub);
						} else
							components.addLast(KNOWN_COMPONENTS[i]);

						offset += KNOWN_COMPONENTS[i].getShortcut().length();
					}
				}
				if (!found)
					throw new ParseException("unknown identifier", offset);
			} else {
				int from = offset;
				while (offset < format.length() && format.charAt(offset) != '%')
					offset++;
				components.addLast(new TextComponent(Pattern.quote(format
						.substring(from, offset))));
			}
		}

		return components;
	}

	/**
	 * Build a regular expression from the components of the scanner.
	 */
	protected void buildRegularExpression() {
		String pattern = "^";

		for (int i = 0; i < components.size(); i++) {
			Object c = components.get(i);
			String regexValue;
			if (c instanceof ISODateComponent)
				regexValue = ((ISODateComponent) c).getReplacement();
			else
				regexValue = c.toString();

			pattern += "(" + regexValue + ")";
		}

		this.pattern = Pattern.compile(pattern);
	}

	/**
	 * Parse a string which should be in the scanner format. If not, null is
	 * returned.
	 * 
	 * @param time
	 *            timestamp in the scanner format
	 * @return a calendar modeling the time value or null if invalid format
	 */
	public Calendar parse(String time) {
		Calendar cal = Calendar.getInstance();
		Matcher match = pattern.matcher(time);

		if (match.matches()) {
			for (int i = 0; i < components.size(); i++)
				components.get(i).set(match.group(i + 1), cal);
		} else
			return null;

		return cal;
	}
}
