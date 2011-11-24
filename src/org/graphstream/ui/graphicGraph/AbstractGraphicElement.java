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

package org.graphstream.ui.graphicGraph;

import org.graphstream.graph.implementations.AbstractElement;

/**
 * Base for all graphic elements (graphic graph, graphic node, graphic edge, and graphic sprite).
 * 
 * <p>
 * Basically, all graphic elements have a style, in addition of the abstract element.
 * </p>
 */
public abstract class AbstractGraphicElement extends AbstractElement {
	/**
	 * The style of this element as well as all the other elements
	 * with the same style.
	 */
	public StyleGroup style;

	/**
	 * New graphic element with the given unique identifier.
	 * @param id Unique identifier.
	 */
	public AbstractGraphicElement(String id) {
		super(id);
	}
	
	/**
	 * Style group of this element.
	 * 
	 * <p>
	 * This defines the style of this element.
	 * </p>
	 * 
	 * <p>
	 * A style group may reference several elements, therefore all
	 * elements having the same style will be referenced by this
	 * group.
	 * </p>
	 */
	public StyleGroup getStyle() {
		return style;
	}
	
	
	/**
	 * Change the style of this graphic element.
	 * @param style The new style.
	 */
	public void changeStyle(StyleGroup style) {
		this.style = style;
	}
	
	/**
	 *  Make this method accessible to this package. The index of
	 *  graphic elements is handled by the {@link StyleGroup} class,
	 *  you should never use this method, excepted if the element is
	 *  not in a style group.
	 */
	protected void reindex(int newIndex) {
		setIndex(newIndex);
	}
}