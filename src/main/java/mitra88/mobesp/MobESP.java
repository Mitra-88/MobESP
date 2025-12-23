package mitra88.mobesp;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = "craftpresence", name = "CraftPresence", version = "2.7.0", clientSideOnly = true)
public class MobESP {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new RenderESP());
    }
}
