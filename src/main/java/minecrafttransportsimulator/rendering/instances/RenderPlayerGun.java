package minecrafttransportsimulator.rendering.instances;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.rendering.components.InterfaceRender;
import minecrafttransportsimulator.rendering.components.OBJParser;
import minecrafttransportsimulator.rendering.components.RenderableModelObject;
import minecrafttransportsimulator.vehicles.main.EntityPlayerGun;

public class RenderPlayerGun{
	private static final Map<String, Integer> displayListMap = new HashMap<String, Integer>();
	private static final Map<String, List<RenderableModelObject>> objectListMap = new HashMap<String, List<RenderableModelObject>>();
	
	public static void render(EntityPlayerGun entity, float partialTicks){
		//Get the render offset.
		//This is the interpolated movement, plus the prior position.
		Point3d gunPosition = entity.position.copy().subtract(entity.prevPosition).multiply(partialTicks).add(entity.prevPosition);
		
		//Subtract the entitie's position by the render entity position to get the delta for translating.
        //If the player is sneaking, then the eye height is wrong for the player's held gun.
        //Check if we are that, and if so, add to the render position to bring the gun up to the player's eyes.
		Point3d renderPosition = gunPosition.copy().subtract(InterfaceClient.getRenderViewEntity().getRenderedPosition(partialTicks));
		WrapperPlayer clientPlayer = InterfaceClient.getClientPlayer();
		if(clientPlayer.isSneaking() && InterfaceClient.inFirstPerson()){
			EntityPlayerGun clientGun = EntityPlayerGun.playerClientGuns.get(clientPlayer.getUUID());
			if(clientGun != null && clientPlayer.equals(clientGun.player) ){
				renderPosition.add(0.0, 5.0/16.0, 0);
			}
		}
		
		//Get the entity rotation.
		Point3d renderRotation = entity.angles.copy().subtract(entity.prevAngles).multiply(1D - partialTicks).multiply(-1D).add(entity.angles);
       
        //Set up lighting.
        InterfaceRender.setLightingToEntity(entity);
        
        //Use smooth shading for main model rendering.
		GL11.glShadeModel(GL11.GL_SMOOTH);
        
        //Push the matrix on the stack and translate and rotate to the player's position.
        GL11.glPushMatrix();
        GL11.glTranslated(renderPosition.x, renderPosition.y, renderPosition.z);
        GL11.glRotated(renderRotation.y, 0, 1, 0);
        GL11.glRotated(renderRotation.x, 1, 0, 0);
        GL11.glRotated(renderRotation.z, 0, 0, 1);
        
		if(entity.gun != null){
			String modelLocation = entity.gun.definition.getModelLocation();
			if(!displayListMap.containsKey(modelLocation)){
				Map<String, Float[][]> parsedModel = OBJParser.parseOBJModel(modelLocation);
				objectListMap.put(modelLocation, OBJParser.generateRenderables(entity, modelLocation, parsedModel, entity.gun.definition.rendering != null ? entity.gun.definition.rendering.animatedObjects : null));
				displayListMap.put(modelLocation, OBJParser.generateDisplayList(parsedModel));
			}
			
			//Bind the texture and render.
			//Don't render on the transparent pass.
			InterfaceRender.setTexture(entity.gun.definition.getTextureLocation(entity.currentSubName));
			if(InterfaceRender.getRenderPass() != 1){
				GL11.glCallList(displayListMap.get(modelLocation));
			}
			
			//The display list only renders static objects.  We need to render dynamic ones manually.
			List<RenderableModelObject> modelObjects = objectListMap.get(modelLocation);
			for(RenderableModelObject modelObject : modelObjects){
				if(modelObject.applyAfter == null){
					modelObject.render(entity, partialTicks, modelObjects);
				}
			}
		}
		
		//Set shading back to normal now that all model bits have been rendered.
		GL11.glShadeModel(GL11.GL_FLAT);
		
		//Pop translation matrix and reset all states.
		GL11.glPopMatrix();
		InterfaceRender.resetStates();
		
		//Spawn particles if required.
		if(InterfaceRender.getRenderPass() != 1 && !InterfaceClient.isGamePaused()){
			entity.spawnParticles();
		}
	}
}
