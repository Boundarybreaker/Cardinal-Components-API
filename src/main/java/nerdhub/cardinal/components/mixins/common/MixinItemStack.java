package nerdhub.cardinal.components.mixins.common;

import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.ItemComponentProvider;
import nerdhub.cardinal.components.api.accessor.StackComponentAccessor;
import nerdhub.cardinal.components.api.component.ItemComponent;
import nerdhub.cardinal.components.mixins.accessor.ItemstackComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@Mixin(ItemStack.class)
public abstract class MixinItemStack implements StackComponentAccessor, ItemstackComponents {

    private final Map<ComponentType<? extends ItemComponent>, ItemComponent> components = new IdentityHashMap<>();

    @Inject(method = "areTagsEqual", at = @At("RETURN"))
    private static void areTagsEqual(ItemStack stack1, ItemStack stack2, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValueZ() && !compare(stack1, stack2)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isEqual", at = @At("RETURN"))
    private void isEqual(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if(cir.getReturnValueZ() && !compare((ItemStack) (Object) this, stack)) {
            cir.setReturnValue(false);
        }
    }

    /**
     * private helper method
     */
    private static boolean compare(ItemStack stack1, ItemStack stack2) {
        if(stack1.isEmpty()) {
            return true;
        }
        StackComponentAccessor accessor = (StackComponentAccessor) (Object) stack1;
        StackComponentAccessor other = (StackComponentAccessor) (Object) stack2;
        Set<ComponentType<? extends ItemComponent>> types = accessor.getComponentTypes();
        if(types.size() == other.getComponentTypes().size()) {
            for(ComponentType<? extends ItemComponent> type : types) {
                if(!other.hasComponent(type) || !accessor.getComponent(type).isEqual(other.getComponent(type))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void copy(CallbackInfoReturnable<ItemStack> cir) {
        ItemstackComponents other = (ItemstackComponents) (Object) cir.getReturnValue();
        components.forEach((type, component) -> {
            ItemComponent copy = copyOf(component);
            copy.fromTag(component.toTag(new CompoundTag()));
            other.setComponentValue(type, copy);
        });
    }

    /**
     * private helper method
     */
    private static ItemComponent copyOf(ItemComponent toCopy) {
        ItemComponent ret = toCopy.newInstance();
        ret.fromTag(toCopy.toTag(new CompoundTag()));
        return ret;
    }

    @Inject(method = "toTag", at = @At("RETURN"))
    private void serialize(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if(!components.isEmpty()) {
            ListTag componentList = new ListTag();
            components.forEach((type, component) -> {
                CompoundTag componentTag = new CompoundTag();
                componentTag.putString("id", type.getID());
                componentTag.put("component", component.toTag(new CompoundTag()));
                componentList.add(componentTag);
            });
            tag.put("cardinal_components", componentList);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/item/ItemProvider;I)V", at = @At("RETURN"))
    private void initComponents(ItemProvider item, int amount, CallbackInfo ci) {
        ((ItemComponentProvider) this.getItem()).initComponents((ItemStack) (Object) this);
    }

    @Shadow
    public abstract Item getItem();

    @Shadow
    public abstract void removeDisplayName();

    @Inject(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("RETURN"))
    private void initComponentsNBT(CompoundTag tag, CallbackInfo ci) {
        ((ItemComponentProvider) this.getItem()).initComponents((ItemStack) (Object) this);
        if(tag.containsKey("cardinal_components", 9)) {
            ListTag componentList = tag.getList("cardinal_components", 9);
            componentList.stream().map(CompoundTag.class::cast).forEach(data -> {
                ComponentType type = ComponentRegistry.get(data.getString("id"));
                if(this.hasComponent(type)) {
                    this.getComponent(type).fromTag(data.getCompound("component"));
                }
            });
        }

    }

    @Override
    public <T extends ItemComponent> void setComponentValue(ComponentType<T> type, ItemComponent obj) {
        components.put(type, obj);
    }

    @Override
    public <T extends ItemComponent> boolean hasComponent(ComponentType<T> type) {
        return components.containsKey(type);
    }

    @Nullable
    @Override
    public <T extends ItemComponent> T getComponent(ComponentType<T> type) {
        return components.containsKey(type) ? type.cast(components.get(type)) : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Set<ComponentType<? extends ItemComponent>> getComponentTypes() {
        return Collections.unmodifiableSet(components.keySet());
    }
}
