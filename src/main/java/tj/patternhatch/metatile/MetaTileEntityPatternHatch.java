package tj.patternhatch.metatile;

import appeng.api.config.Actionable;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.IConfigManager;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.PatternHelper;
import appeng.me.helpers.AENetworkProxy;
import com.glodblock.github.common.item.ItemFluidCraftEncodedPattern;
import com.glodblock.github.common.item.ItemFluidEncodedPattern;
import com.glodblock.github.common.item.ItemFluidPacket;
import com.glodblock.github.common.item.fake.FakeItemRegister;
import com.glodblock.github.util.FluidCraftingPatternDetails;
import com.glodblock.github.util.FluidPatternDetails;
import com.google.common.collect.ImmutableSet;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.metatileentity.multiblock.IMultiAbilityProvider;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.render.ICubeRenderer;
import gregtech.api.render.SimpleCubeRenderer;
import gregtech.common.metatileentities.electric.multiblockpart.MetaTileEntityMultiblockPart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import tj.patternhatch.api.IPatternHatch;
import tj.patternhatch.api.PatternHatchAbilities;
import tj.patternhatch.gui.PatternSlotWidget;
import tj.patternhatch.gui.SyncedTextWidget;
import tj.patternhatch.pattern.PatternSlotEntry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/**
 * Pattern Hatch: a multiblock input part with 36 pattern slots, per-slot isolated
 * item/fluid caches, 4 catalyst slots and 1 circuit slot. Directly integrates with
 * AE2 (item + ae2fc fluid patterns) and feeds the host machine via abilities.
 */
public class MetaTileEntityPatternHatch extends MetaTileEntityMultiblockPart
        implements IPatternHatch, IMultiblockAbilityPart<IItemHandlerModifiable>, IMultiAbilityProvider, IInterfaceHost {

    public static final int PATTERN_SLOTS = 36;
    public static final int CATALYST_SLOTS = 4;
    public static final int CIRCUIT_SLOTS = 1;

    private static final ICubeRenderer PATTERN_HATCH_CASING = new SimpleCubeRenderer("machines/pattern_hatch");

    private final ItemStackHandler patternInventory = new ItemStackHandler(PATTERN_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            markDirty();
            notifyCraftingGrid();
        }
    };
    private final PatternSlotEntry[] patternSlots = new PatternSlotEntry[PATTERN_SLOTS];
    private final ItemStackHandler catalystInventory = new ItemStackHandler(CATALYST_SLOTS);
    private final ItemStackHandler circuitInventory = new ItemStackHandler(CIRCUIT_SLOTS);

    private IItemHandlerModifiable mergedItemView;
    private DualityInterface dualityInterface;
    private AENetworkProxy aeProxy;

    private final IConfigManager configManager = new IConfigManager() {
        private final EnumMap<Settings, Enum<?>> values = new EnumMap<>(Settings.class);

        @Override
        public java.util.Set<Settings> getSettings() {
            return values.keySet();
        }

        @Override
        public void registerSetting(Settings setting, Enum<?> defaultValue) {
            values.put(setting, defaultValue);
        }

        @Override
        public Enum<?> getSetting(Settings setting) {
            return values.get(setting);
        }

        @Override
        public Enum<?> putSetting(Settings setting, Enum<?> value) {
            return values.put(setting, value);
        }

        @Override
        public void writeToNBT(NBTTagCompound tag) {
        }

        @Override
        public void readFromNBT(NBTTagCompound tag) {
        }
    };

    public MetaTileEntityPatternHatch(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, tier);
        for (int i = 0; i < patternSlots.length; i++) {
            patternSlots[i] = new PatternSlotEntry(i);
        }
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityPatternHatch(metaTileEntityId, getTier());
    }

    // ---------- IPatternHatch ----------

    @Override
    public List<PatternSlotEntry> getPatternSlots() {
        List<PatternSlotEntry> list = new ArrayList<>(patternSlots.length);
        for (PatternSlotEntry entry : patternSlots) {
            list.add(entry);
        }
        return list;
    }

    public IItemHandlerModifiable getPatternInventory() {
        return patternInventory;
    }

    @Override
    public IItemHandler getCatalystInventory() {
        return catalystInventory;
    }

    @Override
    public IItemHandler getCircuitInventory() {
        return circuitInventory;
    }

    // ---------- Multiblock abilities ----------

    @Override
    public MultiblockAbility<IItemHandlerModifiable> getAbility() {
        return MultiblockAbility.IMPORT_ITEMS;
    }

    @Override
    public void registerAbilities(List<IItemHandlerModifiable> abilityList) {
        abilityList.add(getMergedItemView());
    }

    @Override
    public MultiblockAbility<?>[] getAbilities() {
        // This fork only collects abilities via IMultiAbilityProvider, so all three must be here.
        return new MultiblockAbility<?>[]{
                MultiblockAbility.IMPORT_ITEMS,
                MultiblockAbility.IMPORT_FLUIDS,
                PatternHatchAbilities.PATTERN_HATCH};
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerAbilityFor(MultiblockAbility<?> ability, List<Object> abilityList) {
        if (ability == MultiblockAbility.IMPORT_ITEMS) {
            abilityList.add(getMergedItemView());
        } else if (ability == MultiblockAbility.IMPORT_FLUIDS) {
            // One tank per fluid so GT recipe matching sees every fluid separately.
            for (PatternSlotEntry entry : patternSlots) {
                for (IFluidTank tank : entry.getFluidCache().getTanks()) {
                    abilityList.add(tank);
                }
            }
        } else if (ability == PatternHatchAbilities.PATTERN_HATCH) {
            abilityList.add(this);
        }
    }

    private IItemHandlerModifiable getMergedItemView() {
        if (mergedItemView == null) {
            List<IItemHandler> handlers = new ArrayList<>();
            for (PatternSlotEntry entry : patternSlots) {
                handlers.add(entry.getItemCache());
            }
            handlers.add(catalystInventory);
            handlers.add(circuitInventory);
            mergedItemView = new ItemHandlerList(handlers);
        }
        return mergedItemView;
    }

    public EnumSet<EnumFacing> getConnectableSides() {
        return EnumSet.allOf(EnumFacing.class);
    }

    // ---------- AE2 grid access (mirrors MetaTileEntityAEHostablePart) ----------

    @Override
    public AENetworkProxy getProxy() {
        if (aeProxy == null) {
            aeProxy = new AENetworkProxy(getHolder(), "mte_proxy", getStackForm(), true);
            aeProxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            aeProxy.setValidSides(getConnectableSides());
            aeProxy.setIdlePowerUsage(1.0);
        }
        if (!aeProxy.isReady() && getWorld() != null) {
            aeProxy.onReady();
        }
        return aeProxy;
    }

    @Override
    public AECableType getCableConnectionType(AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void update() {
        super.update();
        if (getWorld() != null && !getWorld().isRemote) {
            getProxy();
        }
    }

    @Override
    public void onAttached() {
        updateConnectableSides();
    }

    private void updateConnectableSides() {
        getProxy().setValidSides(getConnectableSides());
    }

    // ---------- AE2 IInterfaceHost ----------

    @Override
    public void provideCrafting(ICraftingProviderHelper craftingTracker) {
        World world = getWorld();
        if (world == null) {
            return;
        }
        syncPatternsToDuality();
        int registered = 0;
        for (int i = 0; i < patternInventory.getSlots(); i++) {
            ItemStack patternStack = patternInventory.getStackInSlot(i);
            if (patternStack.isEmpty()) {
                continue;
            }
            ICraftingPatternDetails details = null;
            // 1) ae2fc extended processing patterns (Cnt long counts)
            try {
                FluidPatternDetails fluidDetails = new FluidPatternDetails(patternStack);
                if (fluidDetails.readFromStack()) {
                    details = fluidDetails;
                }
            } catch (Exception ignored) {
            }
            // 2) ae2fc crafting-type fluid patterns
            if (details == null) {
                try {
                    details = FluidCraftingPatternDetails.GetFluidPattern(patternStack, world);
                } catch (Exception ignored) {
                }
            }
            // 3) vanilla item patterns
            if (details == null) {
                PatternHelper helper = new PatternHelper(patternStack, world);
                if (helper != null) {
                    details = helper;
                }
            }
            if (details != null) {
                logPatternDetails(details, "register");
                craftingTracker.addCraftingOption(this, details);
                registered++;
            }
        }
        System.out.println("[PatternHatch] provideCrafting called, patterns registered=" + registered);
    }

    @Override
    public boolean pushPattern(ICraftingPatternDetails patternDetails, InventoryCrafting craftingInv) {
        logPatternDetails(patternDetails, "push");
        ItemStack patternStack = patternDetails.getPattern();
        int slotIndex = -1;
        for (int i = 0; i < patternInventory.getSlots(); i++) {
            if (ItemStack.areItemStacksEqual(patternInventory.getStackInSlot(i), patternStack)) {
                slotIndex = i;
                break;
            }
        }
        if (slotIndex < 0) {
            return false;
        }
        System.out.println("[PatternHatch] pushPattern slot=" + slotIndex);
        PatternSlotEntry entry = patternSlots[slotIndex];
        for (int i = 0; i < craftingInv.getSizeInventory(); i++) {
            ItemStack input = craftingInv.getStackInSlot(i);
            if (input.isEmpty()) {
                continue;
            }
            if (input.getItem() instanceof ItemFluidPacket) {
                Object unpacked = FakeItemRegister.getStack(input);
                if (unpacked instanceof FluidStack) {
                    entry.getFluidCache().fill((FluidStack) unpacked, true);
                    System.out.println("[PatternHatch] pushPattern fluid=" + ((FluidStack) unpacked).getFluid().getName()
                            + " x" + ((FluidStack) unpacked).amount);
                }
            } else {
                System.out.println("[PatternHatch] pushPattern item=" + input.getItem().getRegistryName() + " x" + input.getCount());
                entry.getItemCache().forceInsert(input);
            }
        }
        // Fallback: unpack fluid packets directly from the pattern details.
        try {
            for (IAEItemStack fluidInput : patternDetails.getCondensedInputs()) {
                if (fluidInput == null) {
                    continue;
                }
                Object unpacked = FakeItemRegister.getStack(fluidInput);
                if (unpacked instanceof FluidStack) {
                    entry.getFluidCache().fill((FluidStack) unpacked, true);
                } else if (unpacked instanceof appeng.api.storage.data.IAEFluidStack) {
                    FluidStack fs = ((appeng.api.storage.data.IAEFluidStack) unpacked).getFluidStack();
                    if (fs != null) {
                        entry.getFluidCache().fill(fs, true);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        markDirty();
        return true;
    }

    @Override
    public boolean isBusy() {
        return false;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.of();
    }

    @Override
    public IAEItemStack injectCraftedItems(ICraftingLink input, IAEItemStack acceptedItems, Actionable mode) {
        return acceptedItems;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
    }

    @Override
    public IGridNode getActionableNode() {
        return getHolder().getActionableNode();
    }

    @Override
    public DualityInterface getInterfaceDuality() {
        if (dualityInterface == null) {
            dualityInterface = new DualityInterface(getProxy(), this);
        }
        return dualityInterface;
    }

    @Override
    public EnumSet<EnumFacing> getTargets() {
        return getConnectableSides();
    }

    @Override
    public TileEntity getTileEntity() {
        return getHolder();
    }

    @Override
    public void saveChanges() {
        markDirty();
    }

    @Override
    public int getInstalledUpgrades(Upgrades upgrades) {
        return 0;
    }

    @Override
    public TileEntity getTile() {
        return getHolder();
    }

    @Override
    public IConfigManager getConfigManager() {
        return configManager;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        return null;
    }

    private void syncPatternsToDuality() {
        DualityInterface duality = getInterfaceDuality();
        if (duality == null) {
            return;
        }
        IItemHandler dualityPatterns = duality.getPatterns();
        if (dualityPatterns instanceof IItemHandlerModifiable) {
            IItemHandlerModifiable modifiable = (IItemHandlerModifiable) dualityPatterns;
            for (int i = 0; i < Math.min(patternInventory.getSlots(), 36); i++) {
                modifiable.setStackInSlot(i, patternInventory.getStackInSlot(i));
            }
        }
    }

    private void notifyCraftingGrid() {
        if (getWorld() == null || getWorld().isRemote) {
            return;
        }
        try {
            getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, getProxy().getNode()));
        } catch (Exception ignored) {
        }
    }

    private void logPatternDetails(ICraftingPatternDetails details, String tag) {
        try {
            if (details != null && details.getPattern() != null && details.getPattern().getTagCompound() != null) {
                System.out.println("[PatternHatch] pattern " + tag + " nbt=" + details.getPattern().getTagCompound());
            }
            StringBuilder sb = new StringBuilder("[PatternHatch] pattern " + tag + " class="
                    + details.getClass().getSimpleName() + " inputs=");
            appendStackSummary(sb, details.getCondensedInputs());
            sb.append(" outputs=");
            appendStackSummary(sb, details.getCondensedOutputs());
            System.out.println(sb);
        } catch (Exception ignored) {
        }
    }

    private void appendStackSummary(StringBuilder sb, IAEItemStack[] stacks) {
        if (stacks == null) {
            sb.append("null; ");
            return;
        }
        for (IAEItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }
            try {
                Object unpacked = FakeItemRegister.getStack(stack);
                if (unpacked instanceof FluidStack) {
                    sb.append(((FluidStack) unpacked).getFluid().getName())
                            .append("x").append(((FluidStack) unpacked).amount).append(", ");
                } else {
                    ItemStack item = stack.createItemStack();
                    sb.append(item.getItem().getRegistryName())
                            .append("x").append(stack.getStackSize()).append(", ");
                }
            } catch (Exception e) {
                sb.append("?x").append(stack.getStackSize()).append(", ");
            }
        }
    }

    @Override
    protected boolean openGUIOnRightClick() {
        return true;
    }

    @Override
    public ICubeRenderer getBaseTexture() {
        if (getController() != null) {
            return super.getBaseTexture();
        }
        return PATTERN_HATCH_CASING;
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND_EXTENDED, 280, 236);
        builder.widget(new tj.patternhatch.gui.ShadowLabelWidget(8, 5, "container.patternhatch.pattern_hatch", -1));
        gregtech.api.gui.resources.TextureArea line =
                gregtech.api.gui.resources.TextureArea.fullImage("patternhatch:textures/gui/line.png");
        builder.widget(new tj.patternhatch.gui.FilledRectWidget(7, 13, 163, 77, 0xFFFFFFFF));
        builder.widget(new tj.patternhatch.gui.FilledRectWidget(8, 14, 161, 75, 0xFF8A90A0));
        builder.widget(new tj.patternhatch.gui.FilledRectWidget(9, 15, 159, 73, 0xFF3D3D47));
        for (int row = 1; row < 4; row++) {
            builder.image(8, 15 + row * 18, 160, 1, line);
        }
        for (int col = 1; col < 9; col++) {
            builder.image(7 + col * 18, 14, 1, 74, line);
        }
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 9; col++) {
                builder.widget(new PatternSlotWidget(patternInventory, row * 9 + col, 8 + col * 18, 16 + row * 18));
            }
        }
        builder.widget(new tj.patternhatch.gui.ShadowLabelWidget(8, 90, "container.patternhatch.catalysts", 0xFFFFFFFF));
        for (int i = 0; i < CATALYST_SLOTS; i++) {
            gregtech.api.gui.widgets.SlotWidget slot = new gregtech.api.gui.widgets.SlotWidget(catalystInventory, i, 8 + i * 18, 100, true, true);
            slot.setBackgroundTexture(GuiTextures.SLOT);
            builder.widget(slot);
        }
        builder.widget(new tj.patternhatch.gui.ShadowLabelWidget(8, 122, "container.patternhatch.circuit", 0xFFFFFFFF));
        gregtech.api.gui.widgets.SlotWidget circuitSlot = new gregtech.api.gui.widgets.SlotWidget(circuitInventory, 0, 8, 132, true, true);
        circuitSlot.setBackgroundTexture(GuiTextures.SLOT);
        builder.widget(circuitSlot);

        // Right-hand column showing the full cache summary (items + fluids), synced from the server.
        builder.widget(new SyncedTextWidget(178, 16, 96, 24, this::buildCacheSummaryText));

        builder.bindPlayerInventory(entityPlayer.inventory, GuiTextures.SLOT, 8, 156);
        return builder.build(getHolder(), entityPlayer);
    }

    private String buildCacheSummaryText() {
        StringBuilder sb = new StringBuilder();
        for (PatternSlotEntry entry : patternSlots) {
            for (int s = 0; s < entry.getItemCache().getSlots(); s++) {
                ItemStack stack = entry.getItemCache().getStackInSlot(s);
                if (!stack.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(entry.getSlotIndex() + 1).append(": ")
                            .append(stack.getDisplayName()).append("x").append(stack.getCount());
                }
            }
            for (IFluidTank tank : entry.getFluidCache().getTanks()) {
                if (tank.getFluid() != null && tank.getFluidAmount() > 0) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append(entry.getSlotIndex() + 1).append(": ")
                            .append(tank.getFluid().getLocalizedName()).append("x").append(tank.getFluidAmount());
                }
            }
        }
        return sb.toString();
    }

    // ---------- NBT ----------

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("PatternInventory", patternInventory.serializeNBT());
        data.setTag("CatalystInventory", catalystInventory.serializeNBT());
        data.setTag("CircuitInventory", circuitInventory.serializeNBT());
        NBTTagCompound slotsTag = new NBTTagCompound();
        for (PatternSlotEntry entry : patternSlots) {
            slotsTag.setTag("Slot_" + entry.getSlotIndex(), entry.writeToNBT(new NBTTagCompound()));
        }
        data.setTag("PatternSlots", slotsTag);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        if (data.hasKey("PatternInventory")) {
            patternInventory.deserializeNBT(data.getCompoundTag("PatternInventory"));
        }
        if (data.hasKey("CatalystInventory")) {
            catalystInventory.deserializeNBT(data.getCompoundTag("CatalystInventory"));
        }
        if (data.hasKey("CircuitInventory")) {
            circuitInventory.deserializeNBT(data.getCompoundTag("CircuitInventory"));
        }
        if (data.hasKey("PatternSlots")) {
            NBTTagCompound slotsTag = data.getCompoundTag("PatternSlots");
            for (PatternSlotEntry entry : patternSlots) {
                String key = "Slot_" + entry.getSlotIndex();
                if (slotsTag.hasKey(key)) {
                    entry.readFromNBT(slotsTag.getCompoundTag(key));
                }
            }
        }
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> drops) {
        super.clearMachineInventory(drops);
        for (int i = 0; i < patternInventory.getSlots(); i++) {
            addDrops(drops, patternInventory.getStackInSlot(i));
        }
        for (int i = 0; i < catalystInventory.getSlots(); i++) {
            addDrops(drops, catalystInventory.getStackInSlot(i));
        }
        for (int i = 0; i < circuitInventory.getSlots(); i++) {
            addDrops(drops, circuitInventory.getStackInSlot(i));
        }
        for (PatternSlotEntry entry : patternSlots) {
            for (int i = 0; i < entry.getItemCache().getSlots(); i++) {
                addDrops(drops, entry.getItemCache().getStackInSlot(i));
            }
        }
    }

    private void addDrops(NonNullList<ItemStack> drops, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        int max = stack.getMaxStackSize();
        int remaining = stack.getCount();
        while (remaining > 0) {
            ItemStack part = stack.copy();
            int take = Math.min(remaining, max);
            part.setCount(take);
            drops.add(part);
            remaining -= take;
        }
    }
}
