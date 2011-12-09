package org.dyndns.pamelloes.Lifeless.serializable;

import java.io.Serializable;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationSerializable implements Serializable {
	private static final long serialVersionUID = -6057281732207926248L;
	
	UUID world;
	double x,y,z;
	float yaw,pitch;
	transient Location location = null;
	
	public LocationSerializable(Location location) {
		world = location.getWorld().getUID();
		x=location.getX();
		y=location.getY();
		z=location.getZ();
		yaw=location.getYaw();
		pitch=location.getPitch();
		this.location=location;
	}
	
	public Location getLocation() {
		if(location==null) location =  new Location(Bukkit.getWorld(world),x,y,z,yaw,pitch);
		return location;
	}
}
