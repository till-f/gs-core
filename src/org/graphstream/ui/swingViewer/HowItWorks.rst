The viewer
==========

This only discuss the architecture of this specific viewer that uses Swing as its toolkit.

The viewer is a layer above the GraphicGraph. The graphic graph knows nothing about it. The viewer
observes the graphic graph and each time it changes, and if it can, the viewer draws the graph.

This viewer is separated in two very distinct parts: the views and the renderer. The viewer manages
several `View` object that roughly can be seen as an AWT container (they inherit `JPanel` by
default). We call the views a *rendering surface*. The viewer is in charge of asking each view
to redraw itself at a given frame rate and if the graph changed since the last frame. Therefore
the viewer is in charge of handling the GraphicGraph, the pipelining with the real user graph,
eventually a layout thread, and the views.

The other part is the renderer. A renderer sole task is to render a graphic graph in a rendering
surface. It has no notion of views. The view gives the renderer a rendering surface.

This separation of tasks allows to reuse the renderers in another rendering system that do not
uses the views. The only restriction is that the renderer must render inside an AWT container.
However this is not a strict restriction. For example the OpenGL parts create a new canvas and
insert them in the AWT container.

Viewer
------

	* The viewer contains all the active views on a graphic graph. It handles the graphic graph
	  and calls the views when it changes.
	* The viewer is an iterative element, it registers a timer and activates at regular intervals
	  (these intervals can vary in time).
	* At regular intervals it calls the views.
	* The views may or may not redraw when activated by the viewer.
		* This allows to have "animated views" that redraw constantly, very like simulations or
		  even games (which indeed are kinds of simulations).
		* But also to have "refresh on demand views" that only change when the informations to
		  draw changed.

Listeners
---------

	* For some events the view is able to directly modify the graphic graph. Then each listener
	  on the graphic graph will be automatically triggered. This includes events like :
	  	* adding or removing nodes, edges and sprites.
	  	* adding, changing or removing attributes.
	* But there are other kinds of events, they are managed via predefined attributes.
		* click on an element (node, edge, sprite) :
			ui.clicked.
				During the click (after pushing the button, and before releasing it)
		* selection of one or more elements :
			ui.selected
		* displacement of an element
			x, y, z, xy, xyz
			The usual attributes.
		* Edition of the text of an element
			label
			The usual attribute.
		* etc.

Renderers
---------

	* Views only do "administrative" tasks like opening a window or a panel, doing some
	  actions when the mouse or keyboard is used, etc.
	* The rendering is done by a renderer that is not coupled with views. In other words, it can
	  be used apart for project that only need to render a graph but want to implement the view
	  control by themselves.

Cameras
-------

Each renderer owns a `Camera` object that tells how the graph is viewed. The camera allows to zoom
on the graph, pan the graph, and rotate it. The camera also allows to know which elements are visible
or not, therefore allowing to fasten the rendering process by drawing only the visible or partly
visible elements.

The viewer, JFrames, Swing components and integration
-----------------------------------------------------

By default the viewer is only a set of views. Each view inherits JPanel and therefore is a
JComponent and an AWT container.

Then several scenario can occur :
	1. One view only :
		a. Embedded in an application.
		b. The viewer creates a window for it.
			i. If the window is closed it can be opened back.
			ii. If the window is closed the viewers closes.
	2. Several views :
		a. Embedded in an application.
		b. The viewer creates a window for all views.
			i. If the window is closed it can be opened back.
			ii. If the window is closed the viewers and all the views are closed.
		c. The viewer creates a window per view. 
			i. If one window is closed it can be opened back.
			ii. If one window is closed, the whole viewer closes with all other views.

We do not want to provide a way to embed all views in one unique window, in any case the solution
chosen to do this would not be sufficiently configurable to be usable. Therefore this is let to
the application developer. However it should be possible to create a simple frame to contain
individual views. Then the viewer has a setting for the window close button :
	* Close definitively.
	* Hide only.			
This setting is respected by all views and the viewer. There will exist methods that setup the
viewer for some configurations :
	* addView()						Add a specific view.
	* createDefaultView()			Only create a default viewer that can be embedded.
	* openDefaultViewWithFrame()	Open only one default viewer in its frame.
	* openAllViewsInTheirFrame()	Open all views added via addView() or createDefaultView() in
									their own frame.

The automatic layout
--------------------

The automatic layout is now completely apart from the viewer. It consist in a something that looks
like that :
	
	Graph ----listener----> Layout --+
	  ^                              |
	  |                              |
	  +-------attributes-listener----+

The layout is an output. It can also serve as an input for attributes only (since its only purpose
is to change the xyz attributes of each node. Then the layout can run when one wants. Once when
called, regularly when called, in a thread thanks to a proxy, even on a distant machine.		
