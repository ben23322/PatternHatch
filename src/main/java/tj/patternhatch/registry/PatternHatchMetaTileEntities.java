package tj.patternhatch.registry;

import gregtech.api.GregTechAPI;
import net.minecraft.util.ResourceLocation;
import tj.patternhatch.TJPatternHatchMod;
import tj.patternhatch.metatile.MetaTileEntityPatternHatch;

public final class PatternHatchMetaTileEntities {

    /** addon 用高位 id：注册表上限 32767，GT/GCYL 已用到 32000，取 32766 避免冲突。 */
    private static final int META_ID = 32766;

    public static MetaTileEntityPatternHatch PATTERN_HATCH;

    private PatternHatchMetaTileEntities() {
    }

    public static void init() {
        // tier 5 = IV（GTCEu 电压等级索引：0 ULV ... 5 IV ... 6 LuV ...）
        PATTERN_HATCH = GregTechAPI.registerMetaTileEntity(
                META_ID,
                new MetaTileEntityPatternHatch(
                        new ResourceLocation(TJPatternHatchMod.MODID, "pattern_hatch"),
                        5));
    }
}
