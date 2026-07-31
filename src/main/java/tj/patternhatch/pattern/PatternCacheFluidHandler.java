package tj.patternhatch.pattern;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;

/** 单个样板槽的流体隔离缓存：默认 4 罐，每罐容量 int max。 */
public class PatternCacheFluidHandler implements IFluidHandler {

    public static final int DEFAULT_TANKS = 9;
    public static final int TANK_CAPACITY = Integer.MAX_VALUE;

    private final FluidTank[] tanks;

    public PatternCacheFluidHandler() {
        this(DEFAULT_TANKS);
    }

    public PatternCacheFluidHandler(int tankCount) {
        this.tanks = new FluidTank[tankCount];
        for (int i = 0; i < tankCount; i++) {
            this.tanks[i] = new FluidTank(TANK_CAPACITY);
        }
    }

    /** 直接暴露内部罐，供多方块机器按“一罐一流体”的方式匹配配方。 */
    public IFluidTank[] getTanks() {
        return tanks.clone();
    }

    @Override
    public IFluidTankProperties[] getTankProperties() {
        IFluidTankProperties[] properties = new IFluidTankProperties[tanks.length];
        for (int i = 0; i < tanks.length; i++) {
            properties[i] = new FluidTankProperties(tanks[i].getFluid(), tanks[i].getCapacity());
        }
        return properties;
    }

    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        for (FluidTank tank : tanks) {
            if (tank.getFluid() == null || tank.getFluid().isFluidEqual(resource)) {
                return tank.fill(resource, doFill);
            }
        }
        return 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, boolean doDrain) {
        if (resource == null) {
            return null;
        }
        for (FluidTank tank : tanks) {
            if (tank.getFluid() != null && tank.getFluid().isFluidEqual(resource)) {
                return tank.drain(resource.amount, doDrain);
            }
        }
        return null;
    }

    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        for (FluidTank tank : tanks) {
            if (tank.getFluid() != null && tank.getFluid().amount > 0) {
                return tank.drain(maxDrain, doDrain);
            }
        }
        return null;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        for (int i = 0; i < tanks.length; i++) {
            tag.setTag("Tank_" + i, tanks[i].writeToNBT(new NBTTagCompound()));
        }
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        for (int i = 0; i < tanks.length && tag.hasKey("Tank_" + i); i++) {
            tanks[i].readFromNBT(tag.getCompoundTag("Tank_" + i));
        }
    }
}
