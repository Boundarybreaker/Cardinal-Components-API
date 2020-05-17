/*
 * Cardinal-Components-API
 * Copyright (C) 2019-2020 OnyxStudios
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */
package nerdhub.cardinal.components.internal;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import nerdhub.cardinal.components.api.ComponentRegistry;
import nerdhub.cardinal.components.api.ComponentType;
import nerdhub.cardinal.components.api.component.Component;
import nerdhub.cardinal.components.api.event.ComponentRegisteredCallback;
import nerdhub.cardinal.components.api.util.LazyComponentType;
import net.minecraft.util.Identifier;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class ComponentRegistryImpl implements ComponentRegistry {

    private final Map<Identifier, ComponentType<?>> registry = new LinkedHashMap<>();
    private final ComponentTypeAccess access;
    private int nextRawId = 0;

    public ComponentRegistryImpl(ComponentTypeAccess access) {
        this.access = access;
    }

    @Override
    public <T extends Component> ComponentType<T> registerIfAbsent(Identifier componentId, Class<T> componentClass) {
        Preconditions.checkArgument(Component.class.isAssignableFrom(componentClass), "Component interface must extend " + Component.class.getCanonicalName());
        // make sure 2+ components cannot get registered at the same time
        synchronized (this.registry) {
            @SuppressWarnings("unchecked")
            ComponentType<T> existing = (ComponentType<T>) this.registry.get(componentId);
            if (existing != null) {
                if (existing.getComponentClass() != componentClass) {
                    throw new IllegalStateException("Registered component " + componentId + " twice with 2 different classes: " + existing.getComponentClass() + ", " + componentClass);
                }
                return existing;
            } else {
                ComponentType<T> registered;
                Class<? extends ComponentType<?>> generated = CcaBootstrap.INSTANCE.getGeneratedComponentTypeClass(componentId.toString());
                if (generated != null) {
                    registered = this.instantiateStaticType(generated, componentId, componentClass, this.nextRawId++);
                } else {
                    registered = this.access.create(componentId, componentClass, this.nextRawId++);
                }
                this.registry.put(componentId, registered);
                SharedComponentSecrets.registeredComponents.set(this.registry.values().toArray(new ComponentType[0]));
                ComponentRegisteredCallback.EVENT.invoker().onComponentRegistered(componentId, componentClass, registered);
                return registered;
            }
        }
    }

    private <T extends Component> ComponentType<T> instantiateStaticType(Class<? extends ComponentType<?>> generated, Identifier componentId, Class<T> componentClass, int rawId) {
        try {
            @SuppressWarnings("unchecked") ComponentType<T> ret = (ComponentType<T>) generated.getConstructor(Identifier.class, Class.class, int.class).newInstance(componentId, componentClass, rawId);
            return ret;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new IllegalStateException("Failed to create statically declared component type", e);
        }
    }

    @Override
    public ComponentType<?> get(Identifier id) {
        return this.registry.get(id);
    }

    @Override
    public LazyComponentType getLazy(Identifier id) {
        return LazyComponentType.create(id);
    }

    @Override
    public Stream<ComponentType<?>> stream() {
        return this.registry.values().stream();
    }

    @VisibleForTesting
    void clear() {
        this.registry.clear();
    }
}
