New Features In the "Gobelin" version of UI
===========================================

* ``ui.color`` with AWT Colors
* ``ui.size`` with units. Merely pass a number and a Unit.PX or Unit.GU to tell in which units the size is given. You can use the Values class as well as a descendant class named Pixels. For example ``node.addAttribute("ui.size", 5, Units.PX)`` or ``node.addAttribute("ui.size", new Pixels(5))``.
* ``ui.log`` attribute on the graph with as value a file name string. This feature tells the viewer to create a file that will contain statistics on the frames per second the viewer is actually capable of.
* ``ui.fps`` attribute that changes the refresh frequency of the viewer. For example ``graph.addAtribute("ui.fps", 70)`` will tell the viewer to try to draw 70 frames per second. By default the viewer tries to draw 25 frames per second.
* ``ui.hide`` To hide a node or edge.
* ``CameraManager`` to control the camera on a distant Viewer.
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

Le problème des taille dynamiques:
	- Pour le moment, les tailles dynamiques sont stockées dans les skeleton en GU.
	- Elles ne sont changées que si l'attribut change.
	- Cependant, comme elle sont souvent données en pixels, il faut les convertir en GU avant de les stocker.
	- Si les bornes du graphe changent, les valeurs stockées en GU ne seront plus les bonnes (car en PX la taille varie en fonction des bornes du graphe).
	- Mais comme les tailles stockées ne sont mises à jour que si on change l'attribut, il est possible d'avoir des tailles dynamiques incohérentes !
	Solutions:
		- Actuellement, à chaque changement de bornes du graphe, on prévient tous les skeletes des groupes dynamiques pour qu'au prochain accès à leur taille, celle-ci soit remise à jour.
		- Mais ne serait-il pas plus simple de toujours stocker la taille dans l'unité où elle a été spécifiée ?
		- Ainsi la conversion se produit au moment où on accède au données, ce qui est cohérent avec les pixels dont la taille en GU
		varie en fonction des bornes du graphe.

Le problème des conversions GU <-> PX:
	- Actuellement on crée pas mal de nouveaux objets Point3 lors d'une simple convertion.
	- Ne pourrais-t-on pas améliorer cela ?
	- Dans HobGobelin déjà on pourrait créer une classe HobMatrix4 qui hérite de matrix4 et permet les multiplcations à base de gs.Point3 plutot qu'à base de vecteurs3.
