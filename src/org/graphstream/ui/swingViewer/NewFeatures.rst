New Features In the "Gobelin" version of UI
===========================================

* ``ui.color`` with AWT Colors
* ``ui.size`` with units. Merely pass a number and a Unit.PX or Unit.GU to tell in which units the size is given. You can use the Values class as well as a descendant class named Pixels. For example ``node.addAttribute("ui.size", 5, Units.PX)`` or ``node.addAttribute("ui.size", new Pixels(5))``.
* ``ui.log`` attribute on the graph with as value a file name string. This feature tells the viewer to create a file that will contain statistics on the frames per second the viewer is actually capable of.
* ``ui.fps`` attribute that changes the refresh frequency of the viewer. For example ``graph.addAtribute("ui.fps", 70)`` will tell the viewer to try to draw 70 frames per second. By default the viewer tries to draw 25 frames per second.
* Lots of speed improvements.
	* In the visibility test.
	* In the iteration on elements to draw.
	* In the way the geometry of complex elements (curve, polygons, dynamic sizes) is handled.
	* In the way sprites are positionned.
	* In the way the text is drawn.
* New support for multi-graph and edge loop drawing.
* New support for cubic-curve edges shapes.
* New support for node and sprite plain strokes.
* New support for MouseManager and ShortcutManager classes allowing to tell the View what to do when the user interacts with it.
* The viewer now by default (default MouseManager) zooms using the mouse scroll wheel.
* The MouseManager and KeyboardManager shortcuts.
    * Shift-R, Home and Escape to reset the camera to defaults.
    * Click-Drag anywhere to move the view.
    * Shift-Click-Drag pour sélection-box.
    * Click on node to activate it or drag it.
    * Shift-Click on node to select or unselect it individually.
    * Double-click anywhere will zoom.
