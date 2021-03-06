package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.jsondefs.JSONVehicle.VehicleConnection;
import minecrafttransportsimulator.mcinterface.WrapperPlayer;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.packets.components.APacketVehicle;
import minecrafttransportsimulator.vehicles.main.AEntityBase;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

/**Packet used to send signals to vehicles to connect/disconnect trailers.  Sent from the server to all clients when trailer
 * linking changes.
 * 
 * @author don_bruce
 */
public class PacketVehicleTrailerChange extends APacketVehicle{
	private final int linkedID;
	private final int activeHitchConnectionSavedIndex;
	private final int activeHookupConnectionSavedIndex;
	private final Point3d activeHitchPartSavedOffset;
	private final Point3d activeHookupPartSavedOffset;
	
	public PacketVehicleTrailerChange(EntityVehicleF_Physics vehicle, VehicleConnection hitchConnection, VehicleConnection hookupConnection, APart optionalHitchPart, APart optionalHookupPart){
		super(vehicle);
		EntityVehicleF_Physics trailer = vehicle.towedVehicle;
		if(trailer != null){
			this.linkedID = vehicle.towedVehicle.lookupID;
			if(vehicle.activeHitchPart != null){
				this.activeHitchConnectionSavedIndex = vehicle.activeHitchPart.definition.connections.indexOf(vehicle.activeHitchConnection);
				this.activeHitchPartSavedOffset = vehicle.activeHitchPart.placementOffset;
			}else{
				this.activeHitchConnectionSavedIndex = vehicle.definition.connections.indexOf(vehicle.activeHitchConnection);
				this.activeHitchPartSavedOffset = null;
			}
			if(trailer.activeHookupPart != null){
				this.activeHookupConnectionSavedIndex = trailer.activeHookupPart.definition.connections.indexOf(trailer.activeHookupConnection);
				this.activeHookupPartSavedOffset = trailer.activeHookupPart.placementOffset;
			}else{
				this.activeHookupConnectionSavedIndex = trailer.definition.connections.indexOf(trailer.activeHookupConnection);
				this.activeHookupPartSavedOffset = null;
			}
		}else{
			this.linkedID = -1;
			this.activeHitchConnectionSavedIndex = -1;
			this.activeHookupConnectionSavedIndex = -1;
			this.activeHitchPartSavedOffset = null;
			this.activeHookupPartSavedOffset = null;
		}
	}
	
	public PacketVehicleTrailerChange(ByteBuf buf){
		super(buf);
		this.linkedID = buf.readInt();
		if(linkedID != -1){
			this.activeHitchConnectionSavedIndex = buf.readInt();
			this.activeHookupConnectionSavedIndex = buf.readInt();
			if(buf.readBoolean()){
				this.activeHitchPartSavedOffset = readPoint3dFromBuffer(buf);
			}else{
				this.activeHitchPartSavedOffset = null;
			}
			if(buf.readBoolean()){
				this.activeHookupPartSavedOffset = readPoint3dFromBuffer(buf);
			}else{
				this.activeHookupPartSavedOffset = null;
			}
		}else{
			this.activeHitchConnectionSavedIndex = -1;
			this.activeHookupConnectionSavedIndex = -1;
			this.activeHitchPartSavedOffset = null;
			this.activeHookupPartSavedOffset = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(linkedID);
		buf.writeInt(activeHitchConnectionSavedIndex);
		buf.writeInt(activeHookupConnectionSavedIndex);
		if(activeHitchPartSavedOffset != null){
			buf.writeBoolean(true);
			writePoint3dToBuffer(activeHitchPartSavedOffset, buf);
		}else{
			buf.writeBoolean(false);
		}
		if(activeHookupPartSavedOffset != null){
			buf.writeBoolean(true);
			writePoint3dToBuffer(activeHookupPartSavedOffset, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	public boolean handle(WrapperWorld world, WrapperPlayer player, EntityVehicleF_Physics vehicle){
		if(linkedID != -1){
			for(AEntityBase entity : AEntityBase.createdClientEntities){
				if(entity.lookupID == linkedID){
					EntityVehicleF_Physics trailer = (EntityVehicleF_Physics) entity;
					
					APart hitchPart;
					VehicleConnection hitchConnection;
					if(activeHitchPartSavedOffset != null){
						hitchPart = vehicle.getPartAtLocation(activeHitchPartSavedOffset);
						hitchConnection = hitchPart.definition.connections.get(activeHitchConnectionSavedIndex);
					}else{
						hitchPart = null;
						hitchConnection = vehicle.definition.connections.get(activeHitchConnectionSavedIndex);
					}
					
					APart hookupPart;
					VehicleConnection hookupConnection;
					if(activeHookupPartSavedOffset != null){
						hookupPart = trailer.getPartAtLocation(activeHookupPartSavedOffset);
						hookupConnection = hookupPart.definition.connections.get(activeHookupConnectionSavedIndex);
					}else{
						hookupPart = null;
						hookupConnection = trailer.definition.connections.get(activeHookupConnectionSavedIndex);
					}
					
					vehicle.changeTrailer(trailer, hitchConnection, hookupConnection, hitchPart, hookupPart);
					break;
				}
			}
		}else{
			if(vehicle.towedVehicle != null){
				vehicle.changeTrailer(null, null, null, null, null);
			}
		}
		return true;
	}
}
