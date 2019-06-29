package nerdhub.cardinal.components.api.util.component.container;

import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.Component;
import nerdhub.cardinal.components.api.component.ComponentContainer;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.AbstractMap;

/**
 * This class provides a skeletal implementation of the {@code ComponentContainer} interface,
 * to minimize the effort required to implement this interface.
 *
 * <p> To implement an unmodifiable container, the programmer needs only to extend this class and
 * provide an implementation for the {@code entrySet} method, which returns a set-view of the container's
 * mappings. It is however recommended to provide a specific implementation of the {@link ComponentContainer#get(ComponentType)}
 * method, for performance reasons.
 *
 * <p> To implement a modifiable container, the programmer must additionally override the
 * {@link ComponentContainer#put(ComponentType, Component)} method (which otherwise throws an
 * {@code UnsupportedOperationException}).
 *
 * @see AbstractMap
 * @see FastComponentContainer
 * @see IndexedComponentContainer
 */
public abstract class AbstractComponentContainer extends AbstractMap<ComponentType<?>, Component> implements ComponentContainer {

    @SuppressWarnings("deprecation")    // overriding the deprecated method to avoid the compiler's warning...
    @Deprecated
    @Override
    public Component remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(Object key) {
        if (key != null && key.getClass() == ComponentType.class) {
            return this.containsKey((ComponentType<?>) key);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Component get(@Nullable Object key) {
        if (key != null && key.getClass() == ComponentType.class) {
            return get((ComponentType<?>) key);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public <V extends Component> V put(ComponentType<V> key, V value) {
        throw new UnsupportedOperationException("put");
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation first checks if {@code tag} has a tag list
     * mapped to the "cardinal_components" key; if not it returns immediately.
     * Then it iterates over the list's tags, casts them to {@code CompoundTag},
     * and passes them to the associated component's {@code fromTag} method.
     * If this container lacks a corresponding component for a serialized component
     * type, the component tag is skipped.
     */
    @Override
    public void fromTag(CompoundTag tag) {
        if(tag.containsKey("cardinal_components", NbtType.LIST)) {
            ListTag componentList = tag.getList("cardinal_components", NbtType.COMPOUND);
            componentList.stream().map(CompoundTag.class::cast).forEach(nbt -> {
                ComponentType<?> type = ComponentRegistry.INSTANCE.get(new Identifier(nbt.getString("componentId")));
                if (type != null) {
                    Component component = this.get(type);
                    if (component != null) {
                        component.fromTag(nbt);
                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation first checks if the container is empty; if so it
     * returns immediately. Then, it iterates over this container's mappings, and creates
     * a compound tag for each component. The component type's identifier is stored
     * in that tag using the "componentId" key. The tag is then passed to the component's
     * {@link Component#toTag(CompoundTag)} method. Every such serialized component is appended
     * to a {@code ListTag}, that is added to the given tag using the "cardinal_components" key.
     */
    @Override
    public CompoundTag toTag(CompoundTag tag) {
        if(!this.isEmpty()) {
            ListTag componentList = new ListTag();
            this.forEach((type, component) -> {
                CompoundTag componentTag = new CompoundTag();
                componentTag.putString("componentId", type.getId().toString());
                componentList.add(component.toTag(componentTag));
            });
            tag.put("cardinal_components", componentList);
        }
        return tag;
    }
}
