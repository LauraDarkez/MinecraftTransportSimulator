package minecrafttransportsimulator.rendering.components;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.mcinterface.BuilderEntity;
import minecrafttransportsimulator.mcinterface.BuilderItem;
import minecrafttransportsimulator.mcinterface.BuilderTileEntity;
import minecrafttransportsimulator.mcinterface.BuilderTileEntityRender;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packloading.PackResourceLoader;
import minecrafttransportsimulator.packloading.PackResourceLoader.ResourceType;
import minecrafttransportsimulator.systems.PackParserSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**Interface for handling events pertaining to loading models into MC.  These events are mainly for item models,
 * though events for Entity and Tile Entity model rendering classes are also included here as they are registered
 * like item models.
 *
 * @author don_bruce
 */
@EventBusSubscriber(Side.CLIENT)
public class InterfaceEventsModelLoader{
	private static final Map<BuilderEntity, RenderTickData> renderData = new HashMap<BuilderEntity, RenderTickData>();
    
	/**
	 *  Event that's called to register models.  We register our render wrapper
	 *  classes here, as well as all item JSONs.
	 */
	@SuppressWarnings("unchecked")
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event){
		//Register the global entity rendering class.
		RenderingRegistry.registerEntityRenderingHandler(BuilderEntity.class, new IRenderFactory<BuilderEntity>(){
			@Override
			public Render<? super BuilderEntity> createRenderFor(RenderManager manager){
			return new Render<BuilderEntity>(manager){
				@Override
				protected ResourceLocation getEntityTexture(BuilderEntity builder){
					return null;
				}
				
				@Override
				public void doRender(BuilderEntity builder, double x, double y, double z, float entityYaw, float partialTicks){
					if(builder.entity != null){
						//If we don't have render data yet, create one now.
						if(!renderData.containsKey(builder)){
							renderData.put(builder, new RenderTickData(builder.entity.world));
						}
						
						//Get render pass.  Render data uses 2 for pass -1 as it uses arrays and arrays can't have a -1 index.
						int renderPass = InterfaceRender.getRenderPass();
						if(renderPass == -1){
							renderPass = 2;
						}
						
						//If we need to render, do so now.
						if(renderData.get(builder).shouldRender(renderPass, partialTicks)){
							builder.entity.render(partialTicks);
						}
					}
				}
			};
		}});
		
		//Register the TESR wrapper.
		ClientRegistry.bindTileEntitySpecialRenderer(BuilderTileEntity.class, new BuilderTileEntityRender());
		
		//Get the list of default resource packs here to inject a custom parser for auto-generating JSONS.
		//FAR easier than trying to use the bloody bakery system.
		//Normally we'd add our pack to the current loader, but this gets wiped out during reloads and unless we add our pack to the main list, it won't stick.
		//To do this, we use reflection to get the field from the main MC class that holds the master list to add our custom ones.
		//((SimpleReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).reloadResourcePack(new PackResourcePack(MasterLoader.MODID + "_packs"));
		List<IResourcePack> defaultPacks = null;
		for(Field field : Minecraft.class.getDeclaredFields()){
			if(field.getName().equals("defaultResourcePacks") || field.getName().equals("field_110449_ao")){
				try{
					if(!field.isAccessible()){
						field.setAccessible(true);
					}
					
					defaultPacks = (List<IResourcePack>) field.get(Minecraft.getMinecraft());
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		//Check to make sure we have the pack list before continuing.
		if(defaultPacks == null){
			InterfaceCore.logError("Could not get default pack list. Item icons will be disabled.");
			return;
		}
		
		//Now that we have the custom resource pack location, add our built-in loader.
		//This one auto-generates item JSONs.
		defaultPacks.add(new PackResourcePack(MasterLoader.MODID + "_packs"));
		
		//Register the core item models.  Some of these are pack-based.
		//Don't add those as they get added during the pack registration processing. 
		for(Entry<AItemBase, BuilderItem> entry : BuilderItem.itemMap.entrySet()){
			try{
				//TODO remove this when we don't have non-pack items.
				if(!(entry.getValue().item instanceof AItemPack)){
					registerCoreItemRender(entry.getValue());
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//Now register items for the packs.
		//If we ever register a pack item from a non-external pack, we'll need to make a resource loader for it.
		//This is done to allow MC/Forge to play nice with item textures.
		for(AItemPack<?> packItem : PackParserSystem.getAllPackItems()){
			//TODO remove this when the internal system actually works.
			if(PackParserSystem.getPackConfiguration(packItem.definition.packID) == null || PackParserSystem.getPackConfiguration(packItem.definition.packID).internallyGenerated){
				ModelLoader.setCustomModelResourceLocation(packItem.getBuilder(), 0, new ModelResourceLocation(MasterLoader.MODID + "_packs:" + packItem.definition.packID + AItemPack.PACKID_SEPARATOR + packItem.getRegistrationName(), "inventory"));
			}else{
				if(!PackResourcePack.createdLoaders.containsKey(packItem.definition.packID)){
					defaultPacks.add(new PackResourcePack(packItem.definition.packID));
				}
				ModelLoader.setCustomModelResourceLocation(packItem.getBuilder(), 0, new ModelResourceLocation(MasterLoader.MODID + "_packs:" + packItem.getRegistrationName(), "inventory"));
			}
		}
		
		//Now that we've created all the pack loaders, reload the resource manager to add them to the systems.
		FMLClientHandler.instance().refreshResources(VanillaResourceType.MODELS);
	}
	
	/**
	 *  Helper method to register renders.
	 */
	private static void registerCoreItemRender(Item item){
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MasterLoader.MODID + ":" + item.getRegistryName().getPath(), "inventory"));
	}
	
	/**
	 *  Custom ResourcePack class for auto-generating item JSONs.
	 */
	private static class PackResourcePack implements IResourcePack{
	    private static final Map<String, PackResourcePack> createdLoaders = new HashMap<String, PackResourcePack>();
		private final String domain;
	    private final Set<String> domains;
		
		private PackResourcePack(String domain){
			this.domain = domain;
			domains = new HashSet<String>();
			domains.add(domain);
			createdLoaders.put(domain, this);
		}

		@Override
		public InputStream getInputStream(ResourceLocation location) throws IOException{
			//Create stream return variable and get raw data.
			InputStream stream;
			String rawPackInfo = location.getPath();
			
			//If we are for an item JSON, try to find that JSON, or generate one automatically.
			//If we are for an item PNG, just load the PNG as-is.  If we don't find it, then just let MC purple checker it.
			//Note that the internal mts_packs loader does not do PNG loading, as it re-directs the PNG files to the pack's loaders.
			if(rawPackInfo.endsWith(".json")){
				//Strip the suffix from the packInfo, and then test to see if it's an internal
				//JSON reference from an item JSON, or if it's the primary JSON for the item being loaded..
				String strippedSuffix = rawPackInfo.substring(0, rawPackInfo.lastIndexOf("."));
				if(!strippedSuffix.contains(AItemPack.PACKID_SEPARATOR)){
					//JSON reference.  Get the specified file.
					stream = getClass().getResourceAsStream("/assets/" + domain + "/" + rawPackInfo);
					if(stream == null){
						InterfaceCore.logError("Could not find JSON-specified file: " + rawPackInfo);
						throw new FileNotFoundException(rawPackInfo);
					}
				}else{
					String resourcePath = "";
					String itemTexturePath = "";
						
					//Strip off the auto-generated prefix.
					String combinedPackInfo = rawPackInfo;
					combinedPackInfo = strippedSuffix.substring("models/item/".length());
					
					//Get the pack information, and try to load the resource.
					try{
						String packID = combinedPackInfo.substring(0, combinedPackInfo.indexOf(AItemPack.PACKID_SEPARATOR));
						String systemName = combinedPackInfo.substring(combinedPackInfo.indexOf(AItemPack.PACKID_SEPARATOR) + 1);
						AItemPack<?> packItem = PackParserSystem.getItem(packID, systemName);
						resourcePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_JSON, systemName);
						
						//Try to load the item JSON, or create it if it doesn't exist.
						stream = getClass().getResourceAsStream(resourcePath);
						if(stream == null){
							//Get the actual texture path.
							itemTexturePath = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName);
							
							//Remove the "/assets/packID/" portion as it's implied with JSON.
							itemTexturePath = itemTexturePath.substring(("/assets/"  + packID + "/").length());
							
							//If the packloader is internal, remove the "textures/" prefix.  This is auto-generated.
							//If we don't do this, then the assets won't load right.
							//TODO remove this when generators aren't internal.
							if(PackParserSystem.getPackConfiguration(packID).internallyGenerated){
								itemTexturePath = itemTexturePath.substring("textures/".length());
							}
							
							//Remove the .png suffix as it's also implied.
							itemTexturePath = itemTexturePath.substring(0, itemTexturePath.length() - ".png".length());
							
							//Need to add packID domain to this to comply with JSON domains.
							//If we don't, the PNG won't get sent to the right loader.
							itemTexturePath = packID + ":" + itemTexturePath;
							
							//Generate fake JSON and return as stream to MC loader.
							String fakeJSON = "{\"parent\":\"mts:item/basic\",\"textures\":{\"layer0\": \"" + itemTexturePath + "\"}}";
							stream = new ByteArrayInputStream(fakeJSON.getBytes(StandardCharsets.UTF_8));
						}
					}catch(Exception e){
						InterfaceCore.logError("Could not parse out item JSON from: " + rawPackInfo + "  Looked for JSON at:" + resourcePath + (itemTexturePath.isEmpty() ? (", with fallback at:" + itemTexturePath) : ", but could not find it."));
						throw new FileNotFoundException(rawPackInfo);
					}
				}
			}else{
				try{
					//Strip off the auto-generated prefix and suffix data.
					String combinedPackInfo = rawPackInfo;
					combinedPackInfo = combinedPackInfo.substring("textures/".length(), combinedPackInfo.length() - ".png".length());
					
					//Get the pack information.
					//If we are ending in _item, it means we are getting a JSON for a modular-pack's item PNG.
					//Need to remove this suffix to get the correct systemName to look-up in the systems.
					String packID = domain;
					String systemName = combinedPackInfo.substring(combinedPackInfo.lastIndexOf('/') + 1);
					if(systemName.endsWith("_item")){
						systemName = systemName.substring(0, systemName.length() - "_item".length());
					}
					AItemPack<?> packItem = PackParserSystem.getItem(packID, systemName);
					
					String streamLocation = null;
					if(packItem != null){
						//Get the actual resource path for this resource and return its stream.
						streamLocation = PackResourceLoader.getPackResource(packItem.definition, ResourceType.ITEM_PNG, systemName);
						stream = getClass().getResourceAsStream(streamLocation);
					}else{
						stream = null;
					}
					
					if(stream == null){
						//We might not have this file, but we also might have a JSON-defined item here.
						//Try the JSON standards before throwing an error.
						String streamJSONLocation = "/assets/" + packID + "/" + rawPackInfo;
						stream = getClass().getResourceAsStream(streamJSONLocation);
						if(stream == null){
							if(streamLocation != null){
								InterfaceCore.logError("Could not find item PNG at specified location: " + streamLocation + "  Or potential JSON location: " + streamJSONLocation);
							}else{
								InterfaceCore.logError("Could not find JSON PNG: " + streamJSONLocation);
							}
							throw new FileNotFoundException(rawPackInfo);
						}
					}
				}catch(Exception e){
					if(e instanceof FileNotFoundException){
						throw e;
					}else{
						InterfaceCore.logError("Could not parse which item PNG to get from: " + rawPackInfo);
						throw new FileNotFoundException(rawPackInfo);
					}
				}
				
			}
			
			//Return whichever stream we found.
			return stream;
		}

		@Override
		public boolean resourceExists(ResourceLocation location){
			return domains.contains(location.getNamespace()) 
					&& !location.getPath().contains("blockstates") 
					&& !location.getPath().contains("armatures") 
					&& !location.getPath().contains("mcmeta")
					&& (location.getPath().startsWith("models/item/") || location.getPath().startsWith("textures/"));
		}

		@Override
		public Set<String> getResourceDomains(){
			return domains;
		}

		@Override
		public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer metadataSerializer, String metadataSectionName) throws IOException{
			return null;
		}

		@Override
		public BufferedImage getPackImage() throws IOException{
			return null;
		}

		@Override
		public String getPackName(){
			return "Internal:" + domain;
		}
	}
}
