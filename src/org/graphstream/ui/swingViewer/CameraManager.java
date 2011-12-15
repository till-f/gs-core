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

package org.graphstream.ui.swingViewer;

import org.graphstream.graph.Graph;
import org.graphstream.ui.geom.Point3;

/**
 * Manage a set if views in all viewers on the same graph and allow to change the point of view of
 * each one, storing the values in the graph for later retrieving.
 */
public class CameraManager {
	protected Graph graph;
	
	public CameraManager(Graph graph) {
		this.graph = graph;
	}

	public void setViewAutoFit(View view) {
		graph.removeAttribute(String.format("ui.camera.%s.angle", view.getId()));
		graph.removeAttribute(String.format("ui.camera.%s.zoom", view.getId()));
		graph.removeAttribute(String.format("ui.camera.%s.center", view.getId()));
	}

	public void setViewCenter(View view, double x, double y, double z) {
		setViewCenter(view.getId(), x, y, z);
	}

	public void setViewPercent(View view, double percent) {
		setViewPercent(view.getId(), percent);
	}

	public void setViewRotation(View view, double rotation) {
		setViewRotation(view.getId(), rotation);
	}
	
	public void setViewAutoFit(String viewId) {
		graph.removeAttribute(String.format("ui.camera.%s.angle", viewId));
		graph.removeAttribute(String.format("ui.camera.%s.zoom", viewId));
		graph.removeAttribute(String.format("ui.camera.%s.center", viewId));
	}

	public void setViewCenter(String viewId, double x, double y, double z) {
		graph.setAttribute(String.format("ui.camera.%s.center", viewId), new Point3(x, y, z));
	}

	public void setViewPercent(String viewId, double percent) {
		graph.setAttribute(String.format("ui.camera.%s.zoom", viewId), percent);
	}

	public void setViewRotation(String viewId, double rotation) {
		graph.setAttribute(String.format("ui.camera.%s.angle", viewId), rotation);		
	}
}