package minecrafttransportsimulator.sound;

import java.util.HashMap;
import java.util.Map;

import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.packets.components.InterfacePacket;
import minecrafttransportsimulator.packets.instances.PacketRadioStateChange;
import minecrafttransportsimulator.sound.RadioManager.RadioSources;

/**Base class for radios.  Used to provide a common set of tools for all radio implementations.
 * This class keeps track of the radio station currently selected, as well as the
 * current volume for the source and equalization settings.
*
* @author don_bruce
*/
public class Radio{
	public static final Map<Integer, Radio> createdClientRadios = new HashMap<Integer, Radio>();
	public static final Map<Integer, Radio> createdServerRadios = new HashMap<Integer, Radio>();
	/**Internal counter for radio IDs.  Increments each time a radio is created**/
	private static int idCounter = 1;
	public final int radioID;
	
	//Public variables for modifying state.
	public int preset;
	public int volume;
	public String displayText;
	public RadioStation currentStation;
	
	//Private runtime variables.
	private final IRadioProvider provider;
	private RadioSources currentSource;
	private SoundInstance currentSound;
	
	public Radio(IRadioProvider provider, WrapperNBT data){
		this.provider = provider;
		this.radioID = provider.getProviderWorld().isClient() ? data.getInteger("radioID") : idCounter++;
		if(provider.getProviderWorld().isClient()){
			if(data.getBoolean("savedRadio")){
				changeSource(RadioSources.values()[data.getInteger("currentSource")], false);
				pressPreset(data.getInteger("preset"), false);
				changeVolume(data.getInteger("volume"), false);
			}else{
				changeSource(RadioSources.LOCAL, false);
				changeVolume(10, false);
			}
			createdClientRadios.put(radioID, this);
		}else{
			setProperties(RadioSources.values()[data.getInteger("currentSource")], data.getInteger("volume"), data.getInteger("preset"));
			createdServerRadios.put(radioID, this);
		}
	}
	
	/**
	 * Starts radio playback, making a new sound instance to do so.
	 * This command comes from the currently-selected radio station when
	 * it has connected and is ready to play sound.
	 */
	public void start(){
		currentSound = new SoundInstance(provider, "Radio_" + radioID, false, this);
		currentSound.volume = volume/10F;
	}
	
	/**
	 * Stops radio playback, disconnecting it from its source.
	 * This command comes from the stop button or the audio system if the
	 * radio station has stopped.
	 */
	public void stop(){
		if(currentStation != null){
			currentStation.removeRadio(this);
			currentStation = null;
			if(currentSound != null){
				currentSound.stop();
			}
			displayText = "Radio turned off.";
		}
	}
	
	/**
	 * Changes the radio's source.
	 */
	public void changeSource(RadioSources source, boolean sendPacket){
		stop();
		this.currentSource = source;
		switch(source){
			case LOCAL : displayText = "Ready to play from files on your PC.\nPress a station number to start.\nFiles are in folders in the mts_music directory."; break;
			case SERVER : displayText = "Ready to play from files on the server.\nPress a station number to start."; break;
			case INTERNET : displayText = "Ready to play from internet streams.\nPress a station number to start.\nOr press SET to set a station URL."; break;
		}
		if(provider.getProviderWorld().isClient() && sendPacket){
			InterfacePacket.sendToServer(new PacketRadioStateChange(this, currentSource, volume, preset));
		}
	}
	
	/**
	 * Gets the radio's source.
	 */
	public RadioSources getSource(){
		return currentSource;
	}
	
	/**
	 * Changes the volume of this radio, and sets the currentSound's volume to that volume.
	 */
	public void changeVolume(int setVolume, boolean sendPacket){
		this.volume = setVolume == 0 ? 10 : setVolume;
		if(currentSound != null){
			currentSound.volume = setVolume/10F;
		}
		if(provider.getProviderWorld().isClient() && sendPacket){
			InterfacePacket.sendToServer(new PacketRadioStateChange(this, currentSource, setVolume, preset));
		}
	}
	
	/**
	 * Returns true if the radio is currently playing.
	 */
	public boolean isPlaying(){
		return currentSound != null && !currentSound.stopSound;
	}
	
	/**
	 * Returns the sound the radio is currently playing, or null if it isn't playing anything.
	 */
	public SoundInstance getPlayingSound(){
		return currentSound;
	}
	
	/**
	 * Sets the station for this radio.  Station is responsible for starting playback of sounds.
	 */
	public void pressPreset(int index, boolean sendPacket){
		//First stop the radio from playing any stations.
		stop();
		
		//Set the preset and playback source.
		preset = index;
		if(preset > 0){
			if(currentSource.equals(RadioSources.SERVER)){
				displayText = "This method of playback is not supported .... yet!";
			}else{
				currentStation = RadioManager.getStation(currentSource, preset - 1);
				currentStation.addRadio(this);
			}
		}
		if(provider.getProviderWorld().isClient() && sendPacket){
			InterfacePacket.sendToServer(new PacketRadioStateChange(this, currentSource, volume, preset));
		}
	}
	
	/**
	 * Sets the properties without doing any operations.  Used on servers to track state changes.
	 */
	public void setProperties(RadioSources source, int volume, int preset){
		this.currentSource = source;
		this.volume = volume;
		this.preset = preset;
	}
	
	
	/**
	 * Saves the radio state to NBT for creation later.
	 */
	public void save(WrapperNBT data){
		data.setInteger("radioID", radioID);
		data.setInteger("currentSource", currentSource.ordinal());
		data.setBoolean("savedRadio", true);
		data.setInteger("preset", preset);
		data.setInteger("volume", volume);
	}
}