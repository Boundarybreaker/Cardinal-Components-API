package nerdhub.cardinal.components.util;

import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.accessor.StackComponentAccessor;
import nerdhub.cardinal.components.api.component.Component;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.Set;

public class ComponentHelper {

    public static void writeToTag(Map<ComponentType<? extends Component>, Component> components, CompoundTag tag) {
        if(!components.isEmpty()) {
            ListTag componentList = new ListTag();
            components.forEach((type, component) -> {
                CompoundTag componentTag = new CompoundTag();
                componentTag.putString("id", type.getId().toString());
                componentTag.put("component", component.serialize(new CompoundTag()));
                componentList.add(componentTag);
            });
            tag.put("cardinal_components", componentList);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static boolean areComponentsEqual(ItemStack stack1, ItemStack stack2) {
        if(stack1.isEmpty() && stack2.isEmpty()) {
            return true;
        }
        StackComponentAccessor accessor = (StackComponentAccessor) (Object) stack1;
        StackComponentAccessor other = (StackComponentAccessor) (Object) stack2;
        Set<ComponentType<? extends Component>> types = accessor.getComponentTypes();
        if(types.size() == other.getComponentTypes().size()) {
            for(ComponentType<? extends Component> type : types) {
                if(!other.hasComponent(type) || !accessor.getComponent(type).isComponentEqual(other.getComponent(type))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static Component copyOf(Component toCopy) {
        Component ret = toCopy.newInstance();
        ret.deserialize(toCopy.serialize(new CompoundTag()));
        return ret;
    }

    public static void readFromTag(Map<ComponentType<? extends Component>, Component> components, CompoundTag tag) {
        if(tag.containsKey("cardinal_components", 9)) {
            ListTag componentList = tag.getList("cardinal_components", 10);
            componentList.stream().map(CompoundTag.class::cast).forEach(nbt -> {
                ComponentType<?> type = ComponentRegistry.get(new Identifier(nbt.getString("id")));
                components.get(type).deserialize(nbt.getCompound("component"));
            });
        }
    }
}
