package tj.patternhatch.api;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;

/** 自定义多方块能力：机器结构成型后通过 getAbilities() 收集样板仓。 */
public final class PatternHatchAbilities {

    public static final MultiblockAbility<IPatternHatch> PATTERN_HATCH = new MultiblockAbility<>();

    private PatternHatchAbilities() {
    }
}

