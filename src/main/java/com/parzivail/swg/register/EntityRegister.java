package com.parzivail.swg.register;

import com.parzivail.swg.Resources;
import com.parzivail.swg.StarWarsGalaxy;
import com.parzivail.swg.entity.EntityShip;
import com.parzivail.util.entity.EntityRegUtil;

public class EntityRegister
{
	public static void register()
	{
		int entityId = 0;
		EntityRegUtil.registerEntity(StarWarsGalaxy.instance, Resources.MODID, EntityShip.class, "ship", entityId++, 80, 1, true);
	}
}
