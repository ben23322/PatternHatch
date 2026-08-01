package tj.patternhatch.registry;

import appeng.api.AEApi;
import gregtech.api.GTValues;
import gregtech.api.recipes.RecipeMaps;
import gregtech.common.metatileentities.MetaTileEntities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.config.Configuration;
import tj.patternhatch.pattern.PatternCacheInventory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembler recipe for the Pattern Hatch.
 * NOTE: the assembler recipe map accepts at most 9 item inputs in this fork,
 * so the sum of all counts below must stay <= 9.
 */
public final class PatternHatchRecipes {

    private PatternHatchRecipes() {
    }

    public static void register(File configDir) {
        Configuration config = new Configuration(new File(configDir, "patternhatch.cfg"));
        config.load();
        boolean enabled = config.getBoolean("enabled", "recipe", true,
                "Register the assembler recipe for the Pattern Hatch");
        int outputCount = config.getInt("outputCount", "recipe", 2, 1, 64,
                "Pattern Hatch output count per craft");
        int duration = config.getInt("duration", "recipe", 20, 1, 72000,
                "Recipe duration in ticks (1 second = 20 ticks)");
        int eut = config.getInt("eut", "recipe", (int) GTValues.V[GTValues.IV], 1, Integer.MAX_VALUE,
                "Recipe EU/t (IV = 8192)");
        int hvBus = config.getInt("hvInputBusCount", "recipe", 2, 0, 9,
                "Count of HV input buses (max total inputs is 9)");
        int hvHatch = config.getInt("hvInputHatchCount", "recipe", 2, 0, 9,
                "Count of HV fluid input hatches");
        int card = config.getInt("patternCapacityCardCount", "recipe", 2, 0, 9,
                "Count of Pattern Capacity Cards");
        int meIface = config.getInt("meInterfaceCount", "recipe", 1, 0, 9,
                "Count of ME Interfaces");
        int fluidIface = config.getInt("fluidInterfaceCount", "recipe", 1, 0, 9,
                "Count of Fluid Interfaces");
        PatternCacheInventory.CACHE_ITEM_CAP = config.getInt("cacheCapItems", "cache", Integer.MAX_VALUE, 9,
                Integer.MAX_VALUE,
                "Per-pattern-slot item cache cap: AE stops pushing when the cache reaches this, "
                        + "preventing cache pileup / over-production");
        String inputBusOverride = config.getString("hvInputBusItem", "recipe", "",
                "Override HV input bus item (modid:item:meta@count), empty = default");
        String inputHatchOverride = config.getString("hvInputHatchItem", "recipe", "",
                "Override HV fluid input hatch item");
        String cardOverride = config.getString("patternCapacityCardItem", "recipe", "",
                "Override Pattern Capacity Card item");
        String ifaceOverride = config.getString("meInterfaceItem", "recipe", "",
                "Override ME Interface item");
        String fluidIfaceOverride = config.getString("fluidInterfaceItem", "recipe", "",
                "Override Fluid Interface item");
        config.save();

        if (!enabled) {
            return;
        }

        ItemStack bus = MetaTileEntities.ITEM_IMPORT_BUS[GTValues.HV] == null
                ? ItemStack.EMPTY : MetaTileEntities.ITEM_IMPORT_BUS[GTValues.HV].getStackForm(1);
        ItemStack hatch = MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.HV] == null
                ? ItemStack.EMPTY : MetaTileEntities.FLUID_IMPORT_HATCH[GTValues.HV].getStackForm(1);
        bus = parseItem(inputBusOverride, bus);
        hatch = parseItem(inputHatchOverride, hatch);
        ItemStack cardStack = parseItem(cardOverride,
                AEApi.instance().definitions().materials().cardCapacity().maybeStack(1).orElse(ItemStack.EMPTY));
        ItemStack meIfaceStack = parseItem(ifaceOverride,
                AEApi.instance().definitions().blocks().iface().maybeStack(1)
                        .orElseGet(() -> new ItemStack(Item.getByNameOrId("appliedenergistics2:interface"))));
        ItemStack fluidIfaceStack = parseItem(fluidIfaceOverride,
                AEApi.instance().definitions().blocks().fluidIface().maybeStack(1)
                        .orElseGet(() -> new ItemStack(Item.getByNameOrId("appliedenergistics2:fluid_interface"))));

        if (bus.isEmpty()) System.out.println("[PatternHatch] missing ingredient: HV input bus");
        if (hatch.isEmpty()) System.out.println("[PatternHatch] missing ingredient: HV fluid input hatch");
        if (cardStack.isEmpty()) System.out.println("[PatternHatch] missing ingredient: Pattern Capacity Card");
        if (meIfaceStack.isEmpty()) System.out.println("[PatternHatch] missing ingredient: ME Interface");
        if (fluidIfaceStack.isEmpty()) System.out.println("[PatternHatch] missing ingredient: Fluid Interface");
        if (bus.isEmpty() || hatch.isEmpty() || cardStack.isEmpty() || meIfaceStack.isEmpty() || fluidIfaceStack.isEmpty()) {
            System.out.println("[PatternHatch] assembler recipe skipped: missing ingredient");
            return;
        }

        List<ItemStack> inputs = new ArrayList<>();
        add(inputs, bus, hvBus);
        add(inputs, hatch, hvHatch);
        add(inputs, cardStack, card);
        add(inputs, meIfaceStack, meIface);
        add(inputs, fluidIfaceStack, fluidIface);
        if (inputs.isEmpty() || inputs.size() > 9) {
            System.out.println("[PatternHatch] assembler recipe skipped: input count " + inputs.size()
                    + " must be 1..9 (see config)");
            return;
        }

        RecipeMaps.ASSEMBLER_RECIPES.recipeBuilder()
                .inputs(inputs)
                .outputs(PatternHatchMetaTileEntities.PATTERN_HATCH.getStackForm(outputCount))
                .duration(duration)
                .EUt(eut)
                .buildAndRegister();
        System.out.println("[PatternHatch] assembler recipe registered, inputs=" + inputs.size());
    }

    private static void add(List<ItemStack> list, ItemStack stack, int count) {
        if (stack == null || stack.isEmpty() || count <= 0) {
            return;
        }
        for (int i = 0; i < count; i++) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            list.add(copy);
        }
    }

    private static ItemStack parseItem(String value, ItemStack fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            String trimmed = value.trim();
            int count = 1;
            int at = trimmed.lastIndexOf('@');
            if (at >= 0) {
                count = Math.max(1, Integer.parseInt(trimmed.substring(at + 1)));
                trimmed = trimmed.substring(0, at);
            }
            String id = trimmed;
            int meta = 0;
            int colon = trimmed.lastIndexOf(':');
            if (colon >= 0) {
                String maybeMeta = trimmed.substring(colon + 1);
                if (maybeMeta.matches("\\d+")) {
                    meta = Integer.parseInt(maybeMeta);
                    id = trimmed.substring(0, colon);
                }
            }
            Item item = Item.getByNameOrId(id);
            if (item == null) {
                return fallback;
            }
            ItemStack stack = new ItemStack(item, count, meta);
            return stack.isEmpty() ? fallback : stack;
        } catch (Exception e) {
            return fallback;
        }
    }
}
