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

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

/**
 * Defines components of {@link ISODateScanner}.
 *
 */
public class ISODateComponent {

	final String shortcut;
	final String replace;
	final Handler handler;

	public ISODateComponent(String shortcut,
			String replace, Handler handler) {
		this.shortcut = shortcut;
		this.replace = replace;
		this.handler = handler;
	}

	public String getShortcut() {
		return shortcut;
	}

	public boolean isAlias() {
		return false;
	}

	public String getReplacement() {
		return replace;
	}

	public void set(String value, Calendar calendar) {
		if (handler != null)
			handler.handle(value, calendar);
	}
	
	public static interface Handler {
		void handle(String value, Calendar calendar);
	}

	public static class AliasComponent extends ISODateComponent {

		public AliasComponent(String shortcut, String replace) {
			super(shortcut, replace, null);
		}

		public boolean isAlias() {
			return true;
		}
	}

	public static class TextComponent extends ISODateComponent {
		public TextComponent(String value) {
			super(null, value, null);
		}
	}

	public static class IntegerFieldHandler implements Handler {
		protected final int field;
		protected final int offset;

		public IntegerFieldHandler(int field) {
			this(field, 0);
		}

		public IntegerFieldHandler(int field, int offset) {
			this.field = field;
			this.offset = offset;
		}

		public void handle(String value, Calendar calendar) {
			while (value.charAt(0) == '0' && value.length() > 1)
				value = value.substring(1);
			int val = Integer.parseInt(value);
			calendar.set(field, val + offset);
		}
	}

	protected static abstract class LocaleDependentHandler implements Handler {
		protected Locale locale;
		protected DateFormatSymbols symbols;

		public LocaleDependentHandler() {
			this(Locale.getDefault());
		}

		public LocaleDependentHandler(Locale locale) {
			this.locale = locale;
			this.symbols = DateFormatSymbols.getInstance(locale);
		}
	}

	public static class AMPMHandler extends LocaleDependentHandler {
		public AMPMHandler() {
		}

		public void handle(String value, Calendar calendar) {
			if (value.equalsIgnoreCase(symbols.getAmPmStrings()[Calendar.AM]))
				calendar.set(Calendar.AM_PM, Calendar.AM);
			else if (value
					.equalsIgnoreCase(symbols.getAmPmStrings()[Calendar.PM]))
				calendar.set(Calendar.AM_PM, Calendar.PM);
		}
	}

	public static class UTFOffsetHandler implements Handler {
		public void handle(String value, Calendar calendar) {
			String hs = value.substring(1,3);
			String ms = value.substring(3,5);
			if(hs.charAt(0)=='0')
				hs = hs.substring(1);
			if(ms.charAt(0)=='0')
				ms = ms.substring(1);
			
			int i = value.charAt(0) == '+' ? 1 : -1;
			int h = Integer.parseInt(hs);
			int m = Integer.parseInt(ms);
			
			calendar.getTimeZone().setRawOffset(i*(h*60+m)*60000);
		}
	}

	public static class EpochHandler implements Handler {
		public void handle(String value, Calendar calendar) {
			long e = Long.parseLong(value);
			calendar.setTimeInMillis(e);
		}
	}
}
