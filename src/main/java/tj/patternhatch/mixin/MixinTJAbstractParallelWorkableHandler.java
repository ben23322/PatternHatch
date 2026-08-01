package tj.patternhatch.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * TJ 平行机崩溃保护（配合 MixinTJMultiblockControllerBase）：
 * TJ 允许结构在没有任何物品输入总线时成型（checkStructureComponents 只要求
 * Math.min(1, recipeMap.getMinInputs()) 个输入总线，纯流体配方图 min 为 0），
 * 且 distinct 模式一旦打开不会因总线被拆除/换成样板仓而自动关闭
 * （setDistinct 只在"开启"时挡 busCount<1，NBT 加载则直接写 isDistinct）。
 * 此时 startRecipe -> trySearchForRecipeDistinct -> checkRecipeInputsDirty
 * 会对长度 0 的 lastItemInputsMatrix 取 [0]，抛 ArrayIndexOutOfBoundsException 崩服。
 * 这里每 tick 在 update 入口检查：distinct 但无输入总线时自动关闭 distinct，
 * 与 TJ 自身"无总线不允许开 distinct"的意图一致，机器退回合并输入路径安静待机。
 * 使用 @Pseudo + targets：TJ 类缺失时本补丁自动跳过，不影响其他整合包。
 */
@Pseudo
@Mixin(targets = "tj.capability.AbstractParallelWorkableHandler")
public abstract class MixinTJAbstractParallelWorkableHandler {

    @Shadow
    protected boolean isDistinct;

    @Shadow
    protected int busCount;

    @Shadow
    public abstract void setDistinct(boolean distinct);

    @Inject(method = "update", at = @At("HEAD"))
    private void patternhatch$disableDistinctWithoutBuses(int layer, CallbackInfo ci) {
        try {
            if (isDistinct && busCount < 1) {
                setDistinct(false);
            }
        } catch (Exception ignored) {
        }
    }
}
