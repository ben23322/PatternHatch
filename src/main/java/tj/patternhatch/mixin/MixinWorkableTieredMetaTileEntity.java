package tj.patternhatch.mixin;

import gregtech.api.metatileentity.WorkableTieredMetaTileEntity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 单方块机器补丁（M4）。
 * TODO M4：对照 fork 反编译源码确定注入点（配方查找/输入消耗路径），
 * 复制“活动槽视图”方案。先保留空 Mixin 保持配置可用。
 */
@Mixin(WorkableTieredMetaTileEntity.class)
public abstract class MixinWorkableTieredMetaTileEntity {
}

