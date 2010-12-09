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
package org.graphstream.stream.file;

import java.io.IOException;
import java.util.Calendar;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.DefaultGraph;
import org.graphstream.stream.sync.SourceTime;
import org.graphstream.stream.time.ISODateScanner;
import org.graphstream.stream.time.RealTimePipe;

/**
 * This is an attempt of a realtime dynamic graph file format.
 * 
 * This format (DGS004T) is the same that DGS003/DGS004 but with two addition.
 * First, the header contains a datetime format ({@see
 * org.graphstream.stream.time.ISODateScanner}). Next, each event of the file
 * should start with a timestamp in the format given in the header.
 * 
 * For example:
 * <code>
 * DGS004T
 * realtime 0 0 "%F %T"
 * "2010-10-11 00:00:00" an "..."
 * "2010-10-11 00:00:03" an "..."
 * ...
 * </code>
 */
public class FileSourceDGST extends FileSourceDGS {

	private static class TimestampSourceTime extends SourceTime {
		public TimestampSourceTime(String sourceId) {
			super(sourceId);
		}

		public void newTimestamp(long time) {
			this.currentTimeId = time;
		}

		public long newEvent() {
			return currentTimeId;
		}
	}

	protected ISODateScanner dateScanner;

	protected long timeId;

	/**
	 * New reader for the DGS graph file format version 3.
	 */
	public FileSourceDGST() {
		super();
		sourceTime = new TimestampSourceTime(sourceTime.getSourceId());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.graphstream.stream.file.FileSourceDGS#next(boolean, boolean)
	 */
	protected boolean next(boolean readSteps, boolean stop) throws IOException {
		String key = null, timestamp;
		boolean loop = readSteps;
		Calendar cal;
		// Sorted in probability of appearance ...

		do {
			timestamp = getString();
			cal = dateScanner.parse(timestamp);

			if (cal == null)
				parseError("invalid timestamp: \"" + timestamp + "\"");

			if (cal.getTimeInMillis() == timeId)
				parseError("two events have the same timestamp");
			else if (cal.getTimeInMillis() < timeId)
				parseError("events must be chronologically ordered");

			timeId = cal.getTimeInMillis();
			((TimestampSourceTime) sourceTime).newTimestamp(timeId);

			key = getWordOrSymbolOrStringOrEolOrEof();

			if (key.equals("ce")) {
				readCE();
			} else if (key.equals("cn")) {
				readCN();
			} else if (key.equals("ae")) {
				readAE();
			} else if (key.equals("an")) {
				readAN();
			} else if (key.equals("de")) {
				readDE();
			} else if (key.equals("dn")) {
				readDN();
			} else if (key.equals("cg")) {
				readCG();
			} else if (key.equals("st")) {
				if (readSteps) {
					if (stop) {
						loop = false;
						pushBack();
					} else {
						stop = true;
						readST();
					}
				} else {
					readST();
				}
			} else if (key.equals("#")) {
				eatAllUntilEol();
				return next(readSteps, stop);
			} else if (key.equals("EOL")) {
				// Probably an empty line.
				// NOP
				return next(readSteps, stop);
			} else if (key.equals("EOF")) {
				finished = true;
				return false;
			} else {
				parseError("unknown token '" + key + "'");
			}
		} while (loop);

		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.graphstream.stream.file.FileSourceDGS#begin()
	 */
	protected void begin() throws IOException {
		st.parseNumbers();
		eatWords("DGS004T");

		version = 3;

		eatEol();
		graphName = getWordOrString();
		stepCountAnnounced = (int) getNumber();// Integer.parseInt( getWord() );
		eventCountAnnounced = (int) getNumber();// Integer.parseInt( getWord());
		try {
			dateScanner = new ISODateScanner(getString());
		} catch (Exception e) {
			parseError(e.getMessage());
		}

		eatEol();

		if (graphName != null) {
		}
		// sendGraphAttributeAdded(graphName, "label", graphName);
		else
			graphName = "DGS_";

		graphName = String.format("%s_%d", graphName,
				System.currentTimeMillis() + ((long) Math.random() * 10));
	}

	public static void main(String... args) {
		FileSourceDGST dgst = new FileSourceDGST();
		RealTimePipe rt = new RealTimePipe();
		Graph g = new DefaultGraph("g");

		dgst.addSink(rt);
		rt.addSink(g);

		g.display(true);

		try {
			dgst.begin("src/org/graphstream/stream/file/test.dgst");
			while (dgst.nextEvents())
				;
			dgst.end();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}