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

import nerdhub.cardinal.components.api.ItemComponentFactory;
import nerdhub.cardinal.components.api.component.ComponentContainer;
import nerdhub.cardinal.components.internal.asm.AnnotationData;
import nerdhub.cardinal.components.internal.asm.CcaAsmHelper;
import nerdhub.cardinal.components.internal.asm.NamedMethodDescriptor;
import nerdhub.cardinal.components.internal.asm.StaticComponentLoadingException;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

public final class StaticItemComponentPlugin implements StaticComponentPlugin {
    public static final StaticItemComponentPlugin INSTANCE = new StaticItemComponentPlugin();

    private static String getSuffix(String itemId) {
        if (itemId.equals(ItemComponentFactory.WILDCARD)) {
            return "ItemStackImpl_All";
        }
        return "ItemStackImpl_" + itemId.replace(':', '$').replace('/', '$');
    }

    private final Map</*Identifier*/String, Map</*ComponentType*/String, NamedMethodDescriptor>> componentFactories = new HashMap<>();
    private final String itemStackClass = FabricLoader.getInstance().getMappingResolver().mapClassName("intermediary", "net.minecraft.class_1799");
    private final Map</*Identifier*/String, Class<? extends FeedbackContainerFactory<?, ?>>> factoryClasses = new HashMap<>();

    @Nullable
    public Class<? extends FeedbackContainerFactory<?, ?>> getFactoryClass(String itemId) {
        Class<? extends FeedbackContainerFactory<?, ?>> specificFactory = this.factoryClasses.get(itemId);
        if (specificFactory != null) {
            return specificFactory;
        }
        return this.factoryClasses.get(ItemComponentFactory.WILDCARD);
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return ItemComponentFactory.class;
    }

    @Override
    public String scan(NamedMethodDescriptor factoryDescriptor, AnnotationData data, MethodNode method) {
        Type[] factoryArgs = factoryDescriptor.descriptor.getArgumentTypes();
        if (factoryArgs.length > 1) {
            throw new StaticComponentLoadingException("Too many arguments in method " + factoryDescriptor + ". Should be either no-args or a single " + this.itemStackClass + " argument.");
        }
        List<String> targets = data.get("targets", List.class);
        HashSet<String> resolvedTargets = targets.stream().map(id -> id.indexOf(':') >= 0 || id.equals(ItemComponentFactory.WILDCARD) ? id : "minecraft:" + id).collect(Collectors.toCollection(HashSet::new));
        if (targets.size() != resolvedTargets.size()) {
            throw new StaticComponentLoadingException("ItemStack component factory '" + factoryDescriptor + "' is trying to subscribe with duplicate item ids (" + String.join(", ", targets) + ")");
        }
        String value = data.get("value", String.class);
        for (String target : resolvedTargets) {
            if (target.equals(ItemComponentFactory.WILDCARD)) {
                if (resolvedTargets.size() > 1) {
                    throw new StaticComponentLoadingException("ItemStack component factory '" + factoryDescriptor + "' is trying to subscribe with both wildcard and specific ids (" + String.join(", ", targets) + ")");
                }
            } else if (!IDENTIFIER_PATTERN.matcher(target).matches()) {
                throw new StaticComponentLoadingException("ItemStack component factory '" + factoryDescriptor + "' is subscribing with invalid id: " + target);
            }
            Map<String, NamedMethodDescriptor> specializedMap = this.componentFactories.computeIfAbsent(target, t -> new HashMap<>());
            NamedMethodDescriptor previousFactory = specializedMap.get(value);
            if (previousFactory != null) {
                throw new StaticComponentLoadingException("Duplicate factory declarations for " + value + " on item id '" + target + "': " + factoryDescriptor + " and " + previousFactory);
            }
            specializedMap.put(value, factoryDescriptor);
        }
        return value;
    }

    @Override
    public void generate() {
        Type itemType = Type.getObjectType(this.itemStackClass.replace('.', '/'));
        Map<String, NamedMethodDescriptor> wildcardMap = this.componentFactories.getOrDefault(ItemComponentFactory.WILDCARD, Collections.emptyMap());
        for (Map.Entry</*Identifier*/String, Map</*ComponentType*/String, NamedMethodDescriptor>> entry : this.componentFactories.entrySet()) {
            Map<String, NamedMethodDescriptor> compiled = new HashMap<>(entry.getValue());
            wildcardMap.forEach(compiled::putIfAbsent);
            String implSuffix = getSuffix(entry.getKey());
            Class<? extends ComponentContainer<?>> containerCls = CcaAsmHelper.defineContainer(compiled, implSuffix, itemType);
            this.factoryClasses.put(entry.getKey(), CcaAsmHelper.defineSingleArgFactory(implSuffix, Type.getType(containerCls), itemType));
        }
    }
}
