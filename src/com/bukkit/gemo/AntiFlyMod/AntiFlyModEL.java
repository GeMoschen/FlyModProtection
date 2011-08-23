package com.bukkit.gemo.AntiFlyMod;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

public class AntiFlyModEL extends EntityListener
{
	/////////////////////////////////////
	//
	// ON DEATH
	//
	/////////////////////////////////////
	@Override
	public void onEntityDeath(EntityDeathEvent event)
	{
		if(event.getEntity() instanceof Player)
		{
			AntiFlyModCore.pListener.clearLists(((Player)event.getEntity()).getName());
		}
	}

	/////////////////////////////////////
	//
	// ON DAMAGE
	//
	/////////////////////////////////////
	@Override
	public void onEntityDamage(EntityDamageEvent event)
	{
		if(!(event.getEntity() instanceof Player))
			return;		
	
		if(event.getCause() == DamageCause.ENTITY_ATTACK
				|| event.getCause() == DamageCause.ENTITY_EXPLOSION
				|| event.getCause() == DamageCause.FALL
				|| event.getCause() == DamageCause.DROWNING)
		{
			AntiFlyModCore.pListener.clearLists(((Player)event.getEntity()).getName());
		}
	}
	
	
}
