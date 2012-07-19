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
package org.graphstream.ui.swingViewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.Timer;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.stream.ProxyPipe;
import org.graphstream.stream.Source;
import org.graphstream.stream.thread.ThreadProxyPipe;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.layout.Layout;
import org.graphstream.ui.layout.LayoutRunner;
import org.graphstream.ui.layout.Layouts;
import org.graphstream.ui.swingViewer.basicRenderer.SwingBasicGraphRenderer;

/**
 * Set of views on a graphic graph.
 * 
 * <p>
 * The viewer is an object whose role is to maintain a set of {@link View}s. Each view
 * is a graphical representation of a graph. Each {@link View} owns a specific
 * {@link GraphRenderer} that is in charge of really rendering the graph. Graph
 * renderers can be chosen when adding a view.
 * </p>
 * 
 * <h2>Graphic Graph</h2>
 * 
 * <p>
 * The viewer is in charge of maintaining a {@link GraphicGraph}. The graphic graph is
 * a special representation of the graph that defines the visual properties of nodes
 * and edges and adds the notion of sprite. It contains a style sheet and a set of
 * "style groups" that contains {@link GraphicElement}s having the same style.
 * </p>
 * 
 * <p>
 * The graphic graph is updated from the real graph to display by being a sink
 * of this graph. The graphic graph can be created by the viewer or given at construction (to
 * share it with another viewer).
 * </p>
 * 
 * <h2>Threads</h2>
 * 
 * <p>
 * The viewer ALWAYS runs in the Swing thread. Be very careful, the Swing thread
 * is not the thread your application runs at start (called the main thread). When
 * designing a GUI, you often have all your code running from GUI events in the
 * Swing thread, and your real graph is also accessed from this thread and there are
 * no problems. When you are designing a small application that uses no GUI but only
 * calls {@link Graph#display()}, you have two threads, the main thread and the Swing
 * thread where the viewer runs. 
 * </p>
 * 
 * <p>
 * The viewer is able to cope with these two threads by interposing a special
 * {@link ProxyPipe} (instance of {@link ThreadProxyPipe}) so that the events from the real graph
 * flow to the graphic graph in a safe way. But you have to tell it to the viewer. By default
 * the viewer assumes your real graph is in the main thread and therefore creates a proxy pipe
 * to protect from concurrent accesses. If you know your application runs in the Swing thread,
 * you can inform the viewer that a proxy pipe is not needed and gain a lot more speed.
 * </p>
 * 
 * <p>
 * Most of the methods of the viewer are protected from concurrent accesses, when it makes sense,
 * but not all. This is documented individually in each method. For example the {@link #getGraphicGraph()}
 * will allow you an access to the underlying graphic graph, but this graph is ALWAYS used in
 * the Swing Thread. 
 * </p>
 *
 * <h2>Animation and rendering loop</h2>
 * 
 * <p>
 * Once created, the viewer runs in a loop inside the Swing thread. The viewer will activate
 * at a given rate to redraw the graph. By default this rate is 25 frames per second. But
 * this can be changed. However the viewer will avoid to repaint the graph is nothing changed
 * either in the view (the camera object did not changed) or in the graph (the graphic
 * graph object was not updated) since the last frame drawn. This mechanism is here to
 * avoid consuming unneeded CPU cycles. 
 * </p>
 * 
 * <p>
 * Be very careful: due to the nature of graph events in GraphStream, the viewer
 * is not aware of events that occured on the graph <u>before</u> its creation.
 * There is a special mechanism that replay the graph if you use a proxy pipe or
 * if you pass the graph directly. However, when you create the viewer by yourself
 * and only pass a {@link Source}, the viewer <u>will not</u> display the events
 * that occured on the source before it is connected to it.
 * </p>
 */
public class Viewer implements ActionListener {
	/**
	 * Name of the default view.
	 */
	protected String defaultViewId = "defView"+((int)(Math.random()*10000));

	/**
	 * What to do when a view frame is closed.
	 */
	public static enum CloseFramePolicy {
		CLOSE_VIEWER, HIDE_ONLY, EXIT
	};

	/**
	 * How does the viewer synchronise its internal graphic graph with the graph
	 * displayed. The graph we display can be in the Swing thread (as will be
	 * the viewer, therefore in the same thread as the viewer), in another
	 * thread, or on a distant machine.
	 */
	public enum ThreadingModel {
		GRAPH_IN_SWING_THREAD, GRAPH_IN_ANOTHER_THREAD, GRAPH_ON_NETWORK
	};

	// Attribute

	/**
	 * If true the graph we display is in another thread, the synchronisation
	 * between the graph and the graphic graph must therefore use thread
	 * proxies.
	 */
	protected boolean graphInAnotherThread = true;

	/**
	 * The graph observed by the views.
	 */
	protected GraphicGraph graph;

	/**
	 * If we have to pump events by ourself.
	 */
	protected ProxyPipe pumpPipe;

	/**
	 * If we take graph events from a source in this thread.
	 */
	protected Source sourceInSameThread;

	/**
	 * Timer in the Swing thread.
	 */
	protected Timer timer;

	/**
	 * Delay in milliseconds between frames, approximatively 25 fps.
	 */
	protected int delay = 40;

	/**
	 * The set of views.
	 */
	protected HashMap<String, View> views = new HashMap<String, View>();

	/**
	 * What to do when a view frame is closed.
	 */
	protected CloseFramePolicy closeFramePolicy = CloseFramePolicy.EXIT;

	// Attribute

	/**
	 * Optional layout algorithm running in another thread.
	 */
	protected LayoutRunner optLayout = null;

	/**
	 * If there is a layout in another thread, this is the pipe coming from it.
	 */
	protected ProxyPipe layoutPipeIn = null;
	
	/**
	 * Set of listeners for events like node click, node selection, etc.
	 */
	protected ArrayList<ViewerListener> listeners = new ArrayList<ViewerListener>();

	/**
	 * The graph or source of graph events is in another thread or on another
	 * machine, but the pipe already exists. The graphic graph displayed by this
	 * viewer is created.
	 * 
	 * @param source
	 *            The source of graph events.
	 */
	public Viewer(ProxyPipe source) {
		graphInAnotherThread = true;
		init(new GraphicGraph(newGGId()), source, (Source) null);
	}

	/**
	 * We draw a pre-existing graphic graph. The graphic graph is maintained by
	 * its creator.
	 * 
	 * @param graph
	 *            THe graph to draw.
	 */
	public Viewer(GraphicGraph graph) {
		graphInAnotherThread = false;
		init(graph, (ProxyPipe) null, (Source) null);
	}

	/**
	 * New viewer on an existing graph.
	 * 
	 * <p>The viewer always run in the Swing
	 * thread, therefore, you must specify how it will take graph events from
	 * the graph you give. If the graph you give will be accessed only from the
	 * Swing thread use {@link ThreadingModel#GRAPH_IN_SWING_THREAD}. If the graph you
	 * use is accessed in another thread use {@link ThreadingModel#GRAPH_IN_ANOTHER_THREAD}.
	 * Most of the time, when you are designing a GUI all your code will be invoked by
	 * events, and therefore all your code will run in the Swing thread.
	 * </p>
	 * 
	 * @param graph
	 *            The graph to render.
	 * @param threadingModel
	 *            The threading model.
	 */
	public Viewer(Graph graph, ThreadingModel threadingModel) {
		switch (threadingModel) {
		case GRAPH_IN_SWING_THREAD:
			graphInAnotherThread = false;
			init(new GraphicGraph(newGGId()), (ProxyPipe) null, graph);
			//enableXYZfeedback(true);
			break;
		case GRAPH_IN_ANOTHER_THREAD:
			graphInAnotherThread = true;
			init(new GraphicGraph(newGGId()), new ThreadProxyPipe(graph, true),
					(Source) null);
			//enableXYZfeedback(false);
			break;
		case GRAPH_ON_NETWORK:
			throw new RuntimeException("TO DO, sorry !:-)");
		}
	}

	/**
	 * Create a new unique identifier for a graph.
	 * 
	 * @return The new identifier.
	 */
	protected String newGGId() {
		return String.format("GraphicGraph_%d", (int) (Math.random() * 10000));
	}

	/**
	 * Initialize the viewer.
	 * 
	 * @param graph
	 *            The graphic graph.
	 * @param ppipe
	 *            The source of events from another thread or machine (null if
	 *            source != null).
	 * @param source
	 *            The source of events from this thread (null if ppipe != null).
	 */
	protected void init(GraphicGraph graph, ProxyPipe ppipe, Source source) {
		this.graph = graph;
		this.pumpPipe = ppipe;
		this.sourceInSameThread = source;

		assert ((ppipe != null && source == null) || (ppipe == null && source != null));

		if (pumpPipe != null)
			pumpPipe.addSink(graph);
		if (sourceInSameThread != null) {
			if(source instanceof Graph)
				replayGraph((Graph) source);
			sourceInSameThread.addSink(graph);
		}

		if(pumpPipe != null)
			pumpPipe.pump();
		
		if(graph.hasNumber("ui.fps")) {
			double fps = graph.getNumber("ui.fps");
			delay = (int) (1000/fps);
			graph.removeAttribute("ui.fps");
		}
		
		this.timer = new Timer(delay, this);

		timer.setCoalesce(true);
		timer.setRepeats(true);
		timer.start();
	}

	/**
	 * Close definitively this viewer and all its views. This can be called from any thread.
	 */
	public void close() {
		synchronized (this) {
			disableAutoLayout();
			timer.stop();
			timer.removeActionListener(this);

			if (pumpPipe != null)
				pumpPipe.removeSink(graph);
			if (sourceInSameThread != null)
				sourceInSameThread.removeSink(graph);

			for (View view : views.values())
				view.close(graph);

			graph = null;
			pumpPipe = null;
			sourceInSameThread = null;
			timer = null;
		}
	}

	// Access

	/**
	 * Create a new instance of the default graph renderer. The default graph
	 * renderer class is given by the "gs.ui.renderer" system property. If the
	 * class indicated by this property is not usable (not in the class path,
	 * not of the correct type, etc.) or if the property is not present a
	 * SwingBasicGraphRenderer is returned.
	 */
	public static GraphRenderer newGraphRenderer() {
		String rendererClassName = System.getProperty("gs.ui.renderer");

		if (rendererClassName == null)
			return new SwingBasicGraphRenderer();

		try {
			Class<?> c = Class.forName(rendererClassName);
			Object object = c.newInstance();

			if (object instanceof GraphRenderer) {
				return (GraphRenderer) object;
			} else {
				System.err.printf("class '%s' is not a 'GraphRenderer'%n",
						object.getClass().getName());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err
					.printf("Cannot create graph renderer, 'GraphRenderer' %s class not found : %s%n",
							rendererClassName, e.getMessage());
		} catch (InstantiationException e) {
			e.printStackTrace();
			System.err.printf("Cannot create graph renderer, class '%s' error : %s%n",
					rendererClassName, e.getMessage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			System.err.printf("Cannot create graph renderer, class '%s' illegal access : %s%n",
					rendererClassName, e.getMessage());
		}

		return new SwingBasicGraphRenderer();
	}
	
	public View newView(String identifier, GraphRenderer renderer) {
		View view = null;
		
		String viewClassName = System.getProperty("gs.ui.view");
		
		if(viewClassName != null) {
			try {
				Class<?> c = Class.forName(viewClassName);
				Object object = c.newInstance();
				
				if(object instanceof View) {
					view = (View) object;
				} else {
					System.err.printf("class '%s' is not a 'View'%n", object.getClass().getName());
				}
			} catch(ClassNotFoundException e) {
				System.err.printf("Cannot create a view, %s class not found : %s.%n", viewClassName, e.getMessage());
			} catch(InstantiationException e) {
				System.err.printf("Cannot create view '%s' error : %s%n", viewClassName, e.getMessage());
			} catch(IllegalAccessException e) {
				System.err.printf("Cannot create view '%' illegal access : %s%n", viewClassName, e.getMessage());
			}
		}
		
		if(view == null) {
			view = new DefaultView();
		}
		
		view.open(identifier, this, renderer);
		
		return view;
	}

	/**
	 * What to do when a frame is closed. This can be used from any thread.
	 */
	public CloseFramePolicy getCloseFramePolicy() {
		synchronized(this) {
			return closeFramePolicy;
		}
	}

	/**
	 * New proxy pipe on events coming from the viewer through a thread. This can be used
	 * from any thread;
	 * 
	 * @return The new proxy pipe.
	 */
	public ProxyPipe newThreadProxyOnGraphicGraph() {
		synchronized(this) {
			return new ThreadProxyPipe(graph);
		}
	}

	/**
	 * New viewer pipe on the events coming from the viewer through a thread. This can be used
	 * from any thread.
	 * 
	 * @return The new viewer pipe.
	 */
	public ViewerPipe newViewerPipe() {
		synchronized(this) {
			return new ViewerPipe(String.format("viewer_%d",
				(int) (Math.random() * 10000)), new ThreadProxyPipe(graph,
				false));
		}
	}

	/**
	 * The underlying graphic graph. <em>Caution: Use the returned graph only
	 * in the Swing thread !!</em>
	 */
	public GraphicGraph getGraphicGraph() {
		return graph;
	}

	/**
	 * The view that correspond to the given identifier. This can be used from any thread.
	 * 
	 * @param id
	 *            The view identifier.
	 * @return A view or null if not found.
	 */
	public View getView(String id) {
		synchronized(this) {
			return views.get(id);
		}
	}
	
	/**
	 * Name of the default view.
	 * @return The name of the default view.
	 */
	public String getDefaultViewId() {
		return defaultViewId;
	}
	
	/**
	 * The default view. This is a shortcut to a call to {@link #getView(String)}
	 * with {@link #getDefaultViewId()} as parameter. This can be used from any thread.
	 * 
	 * @return The default view or null if no default view has been installed.
	 */
	public View getDefaultView() {
		return getView(defaultViewId);
	}

	// Command

	/**
	 * Build the default graph view and insert it. The view identifier is
	 * {@link #getDefaultViewId()}. You can request the view to be open in its own
	 * frame. You can do this only once. This can be used from any thread.
	 * 
	 * @param openInAFrame
	 *            It true, the view is placed in a frame, else the view is only
	 *            created and you must embed it yourself in your application.
	 */
	public View addDefaultView(boolean openInAFrame) {
		synchronized(this) {
			View view = getDefaultView();
			if(view == null) {
				view = newView(defaultViewId, newGraphRenderer());
				addView(view);

				if (openInAFrame)
					view.openInAFrame(true);
			}
			
			return view;
		}
	}

	/**
	 * Add a view using its identifier. If there was already a view with this
	 * identifier, it is closed and returned (if different of the one added). This
	 * can be used from any thread.
	 * 
	 * @param view
	 *            The view to add.
	 * @return The old view that was at the given identifier, if any, else null.
	 */
	public View addView(View view) {
		synchronized(this) {
			View old = views.put(view.getId(), view);

			if (old != null && old != view)
				old.close(graph);

			return old;
		}
	}

	/**
	 * Add a new default view with a specific renderer. If a view with the same
	 * id exists, it is removed and closed. By default the view is open in a
	 * frame. This can be used from any thread.
	 * 
	 * @param id
	 *            The new view identifier.
	 * @param renderer
	 *            The renderer to use.
	 * @return The created view.
	 */
	public View addView(String id, GraphRenderer renderer) {
		return addView(id, renderer, true);
	}

	/**
	 * Same as {@link #addView(String, GraphRenderer)} but allows to specify
	 * that the view uses a frame or not. This can be used from any thread.
	 * 
	 * @param id
	 *            The new view identifier.
	 * @param renderer
	 *            The renderer to use.
	 * @param openInAFrame
	 *            If true the view is open in a frame, else the returned view is
	 *            a JPanel that can be inserted in a GUI.
	 * @return The created view.
	 */
	public View addView(String id, GraphRenderer renderer, boolean openInAFrame) {
		synchronized(this) {
			View view = newView(id, renderer);
			addView(view);

			if (openInAFrame)
				view.openInAFrame(true);

			return view;
		}
	}

	/**
	 * Remove a view. This can be used from any thread.
	 * 
	 * @param id
	 *            The view identifier.
	 */
	public void removeView(String id) {
		synchronized(this) {
			views.remove(id);
		}
	}
	
	/**
	 * Iterable view on the set of viewer listeners.
	 * @return An iterable on the set of listeners.
	 */
	public Iterable<ViewerListener> getEachListener() {
		return listeners;
	}

	/**
	 * This method is called by the Viewer timer to redraw automatically the views
	 * at a given frame rate (by default 25 fps).
	 * 
	 * <p>
	 * This is the main redrawing method of
	 * the viewer. Each view is also a Swing component whose repaint() method can be
	 * called to redraw it at any time. Basically the {@link View#display(GraphicGraph, boolean)}
	 * method only call the JComponent.repaint() method.
	 * </p>
	 * 
	 * <p>
	 * Never call this method explicitly, it is public only because the ActionListener interface
	 * forces this.
	 * </p>
	 */
	public void actionPerformed(ActionEvent e) {
		synchronized(this) {
			if (pumpPipe != null)
				pumpPipe.pump();

			if (layoutPipeIn != null)
				layoutPipeIn.pump();

			checkFPS();
			
			boolean changed = graph.graphChangedFlag();

			if (changed)
				computeGraphMetrics();

			for (View view : views.values()) {
				if(view.getCamera().cameraChangedFlag() || changed) {
					view.display(graph, changed);
				}
			}
			
			graph.resetGraphChangedFlag();
		}
	}

	/**
	 * Check if an "ui.fps" attribute tells which refresh frequency to adopt, and it present use
	 * it and then remove it.
	 */
	protected void checkFPS() {
		if(graph.hasNumber("ui.fps")) {
			double fps = graph.getNumber("ui.fps");
			delay = (int) (1000/fps);
			timer.setDelay(delay);
			graph.removeAttribute("ui.fps");
		}
	}
	
	/**
	 * Compute the overall bounds of the graphic graph according to the nodes
	 * and sprites positions. We can only compute the graph bounds from the
	 * nodes and sprites centers since the node and graph bounds may in certain
	 * circumstances be computed according to the graph bounds. The bounds are
	 * stored in the graphic graph .
	 */
	protected void computeGraphMetrics() {
		graph.computeBounds();
	}

	/**
	 * What to do when the frame containing one or more views is closed. This can be
	 * used from any thread.
	 * 
	 * @param policy
	 *            The close frame policy.
	 */
	public void setCloseFramePolicy(CloseFramePolicy policy) {
		synchronized(this) {
			closeFramePolicy = policy;
		}
	}

	// Optional layout algorithm

	/**
	 * Launch an automatic layout process that will position nodes in the
	 * background. This can be used from any thread.
	 */
	public void enableAutoLayout() {
		enableAutoLayout(Layouts.newLayoutAlgorithm());
	}

	/**
	 * Launch an automatic layout process that will position nodes in the
	 * background. This can be used from any thread.
	 * 
	 * @param layoutAlgorithm
	 *            The algorithm to use (see Layouts.newLayoutAlgorithm() for the
	 *            default algorithm).
	 */
	public void enableAutoLayout(Layout layoutAlgorithm) {
		synchronized(this) {
			if (optLayout == null) {
//				optLayout = new LayoutRunner(graph, layoutAlgorithm, true, true);
				optLayout = new LayoutRunner(graph, layoutAlgorithm, true, false);
				graph.replay();
				layoutPipeIn = optLayout.newLayoutPipe();
				layoutPipeIn.addAttributeSink(graph);
			}
		}
	}

	/**
	 * Disable the running automatic layout process, if any. This can be used from
	 * any thread.
	 */
	public void disableAutoLayout() {
		synchronized(this) {
			if (optLayout != null) {
				((ThreadProxyPipe) layoutPipeIn).unregisterFromSource();
				layoutPipeIn.removeSink(graph);
				layoutPipeIn = null;
				optLayout.release();
				optLayout = null;
			}
		}
	}
	
	/**
	 * Add a listener for events on the views, occurring in the Swing thread.
	 * 
	 * <p>
	 * Be very careful: this listener is to be used when you watch events in the Swing
	 * thread, and all your program runs in the Swing thread (for example you are coding
	 * a GUI). If you use the viewer in the main java thread (if you do not know what thread
	 * you use, you are in the main thread), you should use the {@link ViewerPipe} and
	 * the {@link ViewerPipeListener}. Use the {@link Viewer#newViewerPipe()} method to
	 * create a pipe, optionally add your graph as a sink of the pipe, and you can then
	 * register a {@link ViewerPipeListener} in it.
	 * </p>
	 * 
	 * @param listener The listener to register.
	 */
	public void addViewerListener(ViewerListener listener) {
		listeners.add(listener);
	}
	
	/**
	 * Remove a listener for events on the views. This can be used only in the Swing thread.
	 * @param listener The listener to remove.
	 */
	public void removeViewerListener(ViewerListener listener) {
		int pos = listeners.lastIndexOf(listener);
		
		if(pos >= 0) {
			listeners.remove(pos);
		}
	}

	/** Dirty replay of the graph. */
	protected void replayGraph(Graph graph) {
		// Replay all graph attributes.

		if (graph.getAttributeKeySet() != null)
			for (String key : graph.getAttributeKeySet()) {
				this.graph.addAttribute(key, graph.getAttribute(key));
			}

		// Replay all nodes and their attributes.

		for (Node node : graph) {
			Node n = this.graph.addNode(node.getId());

			if (node.getAttributeKeySet() != null) {
				for (String key : node.getAttributeKeySet()) {
					n.addAttribute(key, node.getAttribute(key));
				}
			}
		}

		// Replay all edges and their attributes.

		for (Edge edge : graph.getEachEdge()) {
			Edge e = this.graph.addEdge(edge.getId(), edge.getSourceNode().getId(), edge.getTargetNode().getId(), edge.isDirected());
			
			if (edge.getAttributeKeySet() != null) {
				for (String key : edge.getAttributeKeySet()) {
					e.addAttribute(key, edge.getAttribute(key));
				}
			}
		}
	}
}