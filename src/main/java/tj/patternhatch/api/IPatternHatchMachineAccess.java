package tj.patternhatch.api;

import gregtech.api.capability.impl.MultiblockRecipeLogic;

/** Mixin 注入到 RecipeMapMultiblockController 的访问接口（读取受保护的 workable）。 */
public interface IPatternHatchMachineAccess {
    MultiblockRecipeLogic patternhatch$getWorkable();
}

