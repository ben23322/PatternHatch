package tj.patternhatch.mixin;

import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tj.patternhatch.machine.PatternMachineLogic;

/** 多方块控制器基类补丁：每 tick 驱动活动槽选择。 */
@Mixin(MultiblockControllerBase.class)
public abstract class MixinMultiblockControllerBase {

    @Inject(method = "update", at = @At("HEAD"))
    private void patternhatch$onControllerTick(CallbackInfo ci) {
        PatternMachineLogic.onTick((MultiblockControllerBase) (Object) this);
    }
}

