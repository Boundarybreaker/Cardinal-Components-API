package nerdhub.cardinal.components.mixins.common;

import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.Component;
import nerdhub.cardinal.components.api.component.ComponentContainer;
import nerdhub.cardinal.components.api.provider.ComponentProvider;
import nerdhub.cardinal.components.api.util.ComponentHelper;
import nerdhub.cardinal.components.api.util.impl.IndexedComponentContainer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

@Mixin(Entity.class)
public abstract class MixinEntity implements ComponentProvider {

    private ComponentContainer components = new IndexedComponentContainer();

    @Shadow
    public abstract EntityType<?> getType();

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void initDataTracker(CallbackInfo ci) {
        // Mixin classes can be referenced from other mixin classes
        //noinspection ReferenceToMixin,ConstantConditions
        ((MixinEntityType)(Object)this.getType()).cardinal_fireComponentEvents((Entity) (Object) this, this.components);
    }

    @Inject(method = "toTag", at = @At("RETURN"))
    private void toTag(CompoundTag inputTag, CallbackInfoReturnable<CompoundTag> cir) {
        ComponentHelper.writeToTag(this.components, cir.getReturnValue());
    }

    @Inject(method = "fromTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V", shift = At.Shift.AFTER))
    private void fromTag(CompoundTag tag, CallbackInfo ci) {
        ComponentHelper.readFromTag(this.components, tag);
    }

    @Override
    public boolean hasComponent(ComponentType<?> type) {
        return this.components.containsKey(type);
    }

    @Nullable
    @Override
    public Component getComponent(ComponentType<?> type) {
        return this.components.getOrDefault(type, null);
    }

    @Override
    public Set<ComponentType<? extends Component>> getComponentTypes() {
        return Collections.unmodifiableSet(this.components.keySet());
    }
}
