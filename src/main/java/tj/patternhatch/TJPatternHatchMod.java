package tj.patternhatch;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import tj.patternhatch.registry.PatternHatchMetaTileEntities;
import tj.patternhatch.registry.PatternHatchRecipes;
import zone.rong.mixinbooter.ILateMixinLoader;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Mod(
        modid = TJPatternHatchMod.MODID,
        name = TJPatternHatchMod.NAME,
        version = TJPatternHatchMod.VERSION,
        dependencies = "required-after:gregtech;required-after:appliedenergistics2;required-after:mixinbooter;"
)
public class TJPatternHatchMod implements ILateMixinLoader {

    public static final String MODID = "patternhatch";
    public static final String NAME = "Pattern Hatch (TJ)";
    public static final String VERSION = "0.1.37";

    @Mod.Instance(MODID)
    public static TJPatternHatchMod instance;

    private static File configDir;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configDir = event.getModConfigurationDirectory();
        PatternHatchMetaTileEntities.init();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        if (configDir != null) {
            PatternHatchRecipes.register(configDir);
        }
    }

    @Override
    public List<String> getMixinConfigs() {
        return Arrays.asList("mixins.patternhatch.json", "mixins.patternhatch.nae2.json");
    }
}
