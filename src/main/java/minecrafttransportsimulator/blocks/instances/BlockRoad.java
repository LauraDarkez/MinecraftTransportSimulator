package minecrafttransportsimulator.blocks.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.BoundingBox;
import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase;
import minecrafttransportsimulator.blocks.components.IBlockTileEntity;
import minecrafttransportsimulator.blocks.tileentities.components.RoadLane;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;

public class BlockRoad extends ABlockBase implements IBlockTileEntity<TileEntityRoad>{
	
    public BlockRoad(){
    	super(10.0F, 5.0F);
	}
    
    @Override
    public void addCollisionBoxes(WrapperWorld world, Point3i location, List<BoundingBox> collidingBoxes){
    	//Get collision box from saved instance in the TE.
    	TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
    	if(road != null){
    		collidingBoxes.add(road.boundingBox);
    	}else{
			super.addCollisionBoxes(world, location, collidingBoxes);
		}
	}

	@Override
	public boolean onClicked(WrapperWorld world, Point3i location, Axis axis, WrapperPlayer player){
		if(!world.isClient()){
			//Check if we aren't active.  If not, try to spawn collision again.
			TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
	    	if(road != null && !road.isActive()){
	    		road.spawnCollisionBlocks(player);
	    	}
		}
		return true;
	}
	
	@Override
    public void onBroken(WrapperWorld world, Point3i location){
		TileEntityRoad road = (TileEntityRoad) world.getTileEntity(location);
		if(road != null && road.isActive()){
			//Set the TE to inactive and remove all road connections.
			road.setActive(false);
			for(RoadLane lane : road.lanes){
				lane.removeConnections();
			}
			
			//Now remove all collision blocks.
			for(Point3i blockOffset : road.collisionBlockOffsets){
				Point3i blockLocation = location.copy().add(blockOffset);
				//Check to make sure we don't destroy non-road blocks.
				//This is required in case our TE is corrupt or someone messes with it.
				if(world.getBlock(blockLocation) instanceof BlockCollision){
					world.destroyBlock(blockLocation);
				}
			}
		}
	}
    
    @Override
	public TileEntityRoad createTileEntity(WrapperWorld world, Point3i position, WrapperNBT data){
		return new TileEntityRoad(world, position, data);
	}

	@Override
	public Class<TileEntityRoad> getTileEntityClass(){
		return TileEntityRoad.class;
	}
}
