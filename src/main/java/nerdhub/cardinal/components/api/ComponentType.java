package nerdhub.cardinal.components.api;

import nerdhub.cardinal.components.api.component.Component;
import nerdhub.cardinal.components.api.component.ComponentAccessor;
import net.minecraft.util.Identifier;

import javax.annotation.Nullable;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class ComponentType<T extends Component> {

    private final Class<T> componentClass;
    private final Identifier id;
    private final int rawId;

    /**
     * Constructs a new immutable ComponentType
     *
     * @see ComponentRegistry#registerIfAbsent(Identifier, Class)
     */

    /* package-private */ ComponentType(Identifier id, Class<T> componentClass, int rawId) {
        this.componentClass = componentClass;
        this.id = id;
        this.rawId = rawId;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private T apply(ComponentAccessor componentAccessor) {
        return (T) componentAccessor.getComponent(this);
    }

    public Identifier getId() {
        return this.id;
    }

    public int getRawId() {
        return this.rawId;
    }

    public Class<T> getComponentClass() {
        return componentClass;
    }

    public ObjectPath<ComponentAccessor, T> asComponentPath() {
        return this::apply;
    }

    /**
     * @param holder a component holder
     * @param <V>    the class of the component holder
     * @return the nonnull value of the held component of this type
     * @throws NoSuchElementException if the holder does not provide this type of component
     * @throws ClassCastException     if <code>holder</code> does not implement {@link ComponentAccessor}
     * @see #maybeGet(Object)
     */
    public <V> T get(V holder) {
        T component = this.apply(((ComponentAccessor) holder));
        if (component == null) {
            throw new NoSuchElementException();
        }
        return component;
    }

    /**
     *
     * @param holder a component holder
     * @param <V>    the class of the component holder
     * @return an {@code Optional} describing a component of this type, or an empty
     * {@code Optional} if {@code holder} does not have such a component.
     * @see #get(Object)
     */
    public <V> Optional<T> maybeGet(@Nullable V holder) {
        if (holder instanceof ComponentAccessor) {
            return Optional.ofNullable(this.apply(((ComponentAccessor) holder)));
        }
        return Optional.empty();
    }

}
