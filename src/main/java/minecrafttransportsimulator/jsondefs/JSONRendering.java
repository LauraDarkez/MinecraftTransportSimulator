package minecrafttransportsimulator.jsondefs;

import java.util.List;

import minecrafttransportsimulator.packloading.JSONParser.JSONDescription;

public class JSONRendering{
	@JSONDescription("Text objects are used to render text on models.  This can be used for license plates on cars, tail numbers on planes, status information on fuel pumps, etc.  Every entry is its own text section, and therefore you can have multiple objects for different things.  For example, you may want to make a bus with light-up route signs with multiple characters, but also with two license plates that are limited to 7 characters.")
	public List<JSONText> textObjects;
	
	@JSONDescription("Animated objects are the most complex part of rendering and will likely result in a few pack reloads before you get them right.  However, they are a powerful system that allows any type of rotation, including multi-axis for things like driveshafts and steering assemblies. The animated objects section is composed of a few fields, and a listing of one or more animations to apply on the object.  Objects require no special naming in the model, though some objects may require special names to work with other systems.  For example, a light would have to be named according to the light convention, but could also be specified in this section to rotate it.")
    public List<JSONAnimatedObject> animatedObjects;
	
	@JSONDescription("Camera objects allow you to define new camera points on entities.  These points, unlike the normal user camera, are locked to the position and rotation you specify, so they can be used for special areas, such as airplane landing gear, bus doors, and gun barrel edges.  To facilitate the last of these, camera objects may be rotated and translated via animations.")
    public List<JSONCameraObject> cameraObjects;
	
	@JSONDescription("A list of custom variable names.  Currently only supported on vehicles, and will appear as switches in the panel with the names below them.  These may be assigned any name, and are used for custom animation that don't fit neatly into the pre-defined definitions.  You may have up to 4 custom variables on any vehicle.  Surely, that's enough?")
    public List<String> customVariables;
}
