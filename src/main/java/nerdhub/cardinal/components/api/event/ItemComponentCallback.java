package nerdhub.cardinal.components.api.event;

import nerdhub.cardinal.components.api.component.ComponentContainer;
import nerdhub.cardinal.components.internal.ItemCaller;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

@FunctionalInterface
public interface ItemComponentCallback<T extends Item> {
    static Event<ItemComponentCallback> event(Item item) {
        return ((ItemCaller)item).getItemComponentEvent();
    }

    /**
     * Example code: {@code ItemComponentCallback.EVENT.register(i -> i == DIAMOND_PICKAXE ? (stack, cc) -> cc.put(TYPE, new MyComponent()) : null)}
     */
    ComponentGatherer<ItemStack> getComponentGatherer(Item item);

}
