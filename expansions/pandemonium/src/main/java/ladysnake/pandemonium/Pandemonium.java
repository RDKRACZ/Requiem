package ladysnake.pandemonium;

import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.world.WorldComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.world.WorldComponentInitializer;
import ladysnake.pandemonium.api.anchor.FractureAnchorManager;
import ladysnake.pandemonium.client.ClientAnchorManager;
import ladysnake.pandemonium.common.entity.PandemoniumEntities;
import ladysnake.pandemonium.common.impl.anchor.CommonAnchorManager;
import ladysnake.pandemonium.common.network.ServerMessageHandling;
import ladysnake.pandemonium.common.remnant.PlayerBodyTracker;
import ladysnake.requiem.api.v1.RequiemApi;
import ladysnake.requiem.api.v1.annotation.CalledThroughReflection;
import nerdhub.cardinal.components.api.util.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;

@CalledThroughReflection
public class Pandemonium implements ModInitializer, EntityComponentInitializer, WorldComponentInitializer {
    public static final String MOD_ID = "pandemonium";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    public static void tickAnchors(World world) {
        Profiler profiler = world.getProfiler();
        profiler.push("requiem_ethereal_anchors");
        FractureAnchorManager.get(world).updateAnchors(world.getLevelProperties().getTime());
        profiler.pop();
    }

    @Override
    public void onInitialize() {
        PandemoniumEntities.init();
        ServerMessageHandling.init();
        RequiemApi.registerPlugin(new PandemoniumRequiemPlugin());
        ServerTickEvents.END_WORLD_TICK.register(Pandemonium::tickAnchors);
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(PlayerBodyTracker.KEY, PlayerBodyTracker::new, RespawnCopyStrategy.ALWAYS_COPY);
    }

    @Override
    public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
        registry.register(FractureAnchorManager.KEY, world -> world.isClient
            ? new ClientAnchorManager(world)
            : new CommonAnchorManager(world)
        );
    }
}
