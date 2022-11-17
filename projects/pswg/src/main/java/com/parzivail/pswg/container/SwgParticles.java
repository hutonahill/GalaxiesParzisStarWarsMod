package com.parzivail.pswg.container;

import com.mojang.serialization.Codec;
import com.parzivail.pswg.Resources;
import com.parzivail.pswg.client.particle.ScorchParticle;
import com.parzivail.pswg.client.particle.SlugTrailParticle;
import com.parzivail.pswg.client.particle.SparkParticle;
import com.parzivail.pswg.client.particle.WakeParticle;
import com.parzivail.util.client.particle.PParticleType;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class SwgParticles
{
	public static final PParticleType SLUG_TRAIL = register(Resources.id("slug_trail"), true, SlugTrailParticle.Factory::new);
	public static final PParticleType SPARK = register(Resources.id("spark"), true, SparkParticle.Factory::new);
	public static final PParticleType SCORCH = register(Resources.id("scorch"), true, ScorchParticle.Factory::new);
	public static final ParticleType<BlockStateParticleEffect> WAKE = registerBlockStateBased(Resources.id("wake"), true, new WakeParticle.Factory());

	private static PParticleType register(Identifier name, boolean alwaysShow, ParticleFactoryRegistry.PendingParticleFactory<PParticleType> factory)
	{
		var particleType = Registry.register(Registry.PARTICLE_TYPE, name, new PParticleType(alwaysShow));
		ParticleFactoryRegistry.getInstance().register(particleType, factory);
		return particleType;
	}

	private static ParticleType<BlockStateParticleEffect> registerBlockStateBased(Identifier name, boolean alwaysShow, ParticleFactory<BlockStateParticleEffect> factory)
	{
		var particleType = Registry.register(Registry.PARTICLE_TYPE, name, new ParticleType<BlockStateParticleEffect>(alwaysShow, BlockStateParticleEffect.PARAMETERS_FACTORY)
		{
			public Codec<BlockStateParticleEffect> getCodec()
			{
				return BlockStateParticleEffect.createCodec(this);
			}
		});
		ParticleFactoryRegistry.getInstance().register(particleType, factory);
		return particleType;
	}

	public static void register()
	{
	}
}
