/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.Container
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.neoforged.neoforge.items.IItemHandler
 *  net.neoforged.neoforge.items.IItemHandlerModifiable
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package com.likerain.roguelike.util;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessoriesCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessoriesCompat.class);
    private static boolean curiosChecked = false;
    private static boolean curiosAvailable = false;
    private static Class<?> curiosApiClass;
    private static Method getCuriosInventoryMethod;
    private static Method getEquippedCuriosMethod;
    private static boolean accessoriesChecked;
    private static boolean accessoriesAvailable;
    private static Class<?> accessoriesApiClass;
    private static Method getAccessoriesContainerMethod;
    private static Method isCurioMethod;
    private static Object curiosHelperInstance;
    private static Method isAccessoryMethod;

    private static void initCurios() {
        if (curiosChecked) {
            return;
        }
        curiosChecked = true;
        try {
            curiosApiClass = Class.forName("top.theillusivec4.curios.api.CuriosApi");
            getCuriosInventoryMethod = curiosApiClass.getMethod("getCuriosInventory", Player.class);
            Class<?> iCuriosItemHandlerClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
            getEquippedCuriosMethod = iCuriosItemHandlerClass.getMethod("getEquippedCurios", new Class[0]);
            try {
                Method getHelper = curiosApiClass.getMethod("getCuriosHelper", new Class[0]);
                curiosHelperInstance = getHelper.invoke(null, new Object[0]);
                Class<?> iCuriosHelperClass = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosHelper");
                isCurioMethod = iCuriosHelperClass.getMethod("isCurio", ItemStack.class);
            }
            catch (Exception e) {
                LOGGER.info("Curios helper is not available: " + e.getMessage());
            }
            curiosAvailable = true;
            LOGGER.info("Curios compatibility initialized.");
        }
        catch (Exception e) {
            LOGGER.info("Curios mod not detected: " + e.getMessage());
            curiosAvailable = false;
        }
    }

    private static void initAccessories() {
        if (accessoriesChecked) {
            return;
        }
        accessoriesChecked = true;
        try {
            accessoriesApiClass = Class.forName("io.wispforest.accessories.api.AccessoriesAPI");
            try {
                isAccessoryMethod = accessoriesApiClass.getMethod("isAccessory", ItemStack.class);
            }
            catch (Exception e) {
                LOGGER.info("Accessories isAccessory method is not available: " + e.getMessage());
            }
            Class<?> accessoriesCapabilityClass = Class.forName("io.wispforest.accessories.api.AccessoriesCapability");
            getAccessoriesContainerMethod = accessoriesCapabilityClass.getMethod("getOptionally", LivingEntity.class);
            accessoriesAvailable = true;
            LOGGER.info("Accessories compatibility initialized.");
        }
        catch (Exception e) {
            LOGGER.info("Accessories mod not detected: " + e.getMessage());
            accessoriesAvailable = false;
        }
    }

    public static boolean isAccessory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            boolean hasAccessoryTag = stack.getTags().anyMatch(tag -> {
                String namespace = tag.location().getNamespace();
                return "curios".equals(namespace) || "accessories".equals(namespace);
            });
            if (hasAccessoryTag) {
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        AccessoriesCompat.initCurios();
        if (curiosAvailable && isCurioMethod != null && curiosHelperInstance != null) {
            try {
                if (((Boolean)isCurioMethod.invoke(curiosHelperInstance, stack)).booleanValue()) {
                    return true;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        AccessoriesCompat.initAccessories();
        if (accessoriesAvailable && isAccessoryMethod != null) {
            try {
                if (((Boolean)isAccessoryMethod.invoke(null, stack)).booleanValue()) {
                    return true;
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }

    public static List<ItemStack> getEquippedAccessories(Player player) {
        ArrayList<ItemStack> list;
        block18: {
            Optional opt;
            list = new ArrayList<ItemStack>();
            AccessoriesCompat.initCurios();
            if (curiosAvailable) {
                try {
                    Object handler;
                    Object equipped;
                    opt = (Optional)getCuriosInventoryMethod.invoke(null, player);
                    if (opt != null && opt.isPresent() && (equipped = getEquippedCuriosMethod.invoke(handler = opt.get(), new Object[0])) instanceof IItemHandler) {
                        IItemHandler itemHandler = (IItemHandler)equipped;
                        for (int i = 0; i < itemHandler.getSlots(); ++i) {
                            ItemStack stack = itemHandler.getStackInSlot(i);
                            if (stack.isEmpty()) continue;
                            list.add(stack.copy());
                        }
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Failed to get Curios items", (Throwable)e);
                }
            }
            AccessoriesCompat.initAccessories();
            if (accessoriesAvailable) {
                try {
                    Object res;
                    opt = (Optional)getAccessoriesContainerMethod.invoke(null, player);
                    if (opt == null || !opt.isPresent()) break block18;
                    Object container = opt.get();
                    Method getAllEquipped = null;
                    try {
                        getAllEquipped = container.getClass().getMethod("getAllEquipped", new Class[0]);
                    }
                    catch (NoSuchMethodException e) {
                        for (Method m : container.getClass().getMethods()) {
                            if (!m.getName().equals("getAllEquipped") && !m.getName().equals("getEquipped")) continue;
                            getAllEquipped = m;
                            break;
                        }
                    }
                    if (getAllEquipped != null && (res = getAllEquipped.invoke(container, new Object[0])) instanceof List) {
                        List rawList = (List)res;
                        for (Object obj : rawList) {
                            if (obj instanceof ItemStack) {
                                ItemStack stack = (ItemStack)obj;
                                if (stack.isEmpty()) continue;
                                list.add(stack.copy());
                                continue;
                            }
                            if (obj == null) continue;
                            try {
                                Method getStack = obj.getClass().getMethod("stack", new Class[0]);
                                ItemStack stack = (ItemStack)getStack.invoke(obj, new Object[0]);
                                if (stack == null || stack.isEmpty()) continue;
                                list.add(stack.copy());
                            }
                            catch (Exception ex) {
                                try {
                                    Method getStack = obj.getClass().getMethod("getItemStack", new Class[0]);
                                    ItemStack stack = (ItemStack)getStack.invoke(obj, new Object[0]);
                                    if (stack == null || stack.isEmpty()) continue;
                                    list.add(stack.copy());
                                }
                                catch (Exception exception) {}
                            }
                        }
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Failed to get Accessories items", (Throwable)e);
                }
            }
        }
        return list;
    }

    public static void clearEquippedAccessories(Player player) {
        block13: {
            Optional opt;
            AccessoriesCompat.initCurios();
            if (curiosAvailable) {
                try {
                    Object handler;
                    Object equipped;
                    opt = (Optional)getCuriosInventoryMethod.invoke(null, player);
                    if (opt != null && opt.isPresent() && (equipped = getEquippedCuriosMethod.invoke(handler = opt.get(), new Object[0])) instanceof IItemHandlerModifiable) {
                        IItemHandlerModifiable itemHandler = (IItemHandlerModifiable)equipped;
                        for (int i = 0; i < itemHandler.getSlots(); ++i) {
                            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                        }
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Failed to clear Curios items", (Throwable)e);
                }
            }
            AccessoriesCompat.initAccessories();
            if (accessoriesAvailable) {
                try {
                    opt = (Optional)getAccessoriesContainerMethod.invoke(null, player);
                    if (opt == null || !opt.isPresent()) break block13;
                    Object container = opt.get();
                    Method resetMethod = null;
                    try {
                        resetMethod = container.getClass().getMethod("reset", boolean.class);
                    }
                    catch (NoSuchMethodException e) {
                        // empty catch block
                    }
                    if (resetMethod != null) {
                        resetMethod.invoke(container, true);
                    } else if (container instanceof Container) {
                        Container mcContainer = (Container)container;
                        mcContainer.clearContent();
                    }
                }
                catch (Exception e) {
                    LOGGER.error("Failed to clear Accessories items", (Throwable)e);
                }
            }
        }
    }

    static {
        accessoriesChecked = false;
        accessoriesAvailable = false;
        isCurioMethod = null;
        curiosHelperInstance = null;
        isAccessoryMethod = null;
    }
}

