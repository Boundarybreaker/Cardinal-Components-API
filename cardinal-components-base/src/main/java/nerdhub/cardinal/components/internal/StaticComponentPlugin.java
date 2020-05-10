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

import nerdhub.cardinal.components.internal.asm.AnnotationData;
import nerdhub.cardinal.components.internal.asm.NamedMethodDescriptor;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.regex.Pattern;

public interface StaticComponentPlugin {
    /**
     * A pattern that should be used to check that component type ids are valid
     */
    Pattern IDENTIFIER_PATTERN = Pattern.compile("([a-z0-9_.-]+:)?[a-z0-9/._-]+");

    /**
     * @return the annotation used by this plugin
     */
    Class<? extends Annotation> annotationType();

    /**
     * @param factoryDescriptor descriptor of the annotated method
     * @param data ASM data about the annotation being processed
     * @param method the method node being processed
     * @return a valid identifier string for a recognized component type
     */
    String scan(NamedMethodDescriptor factoryDescriptor, AnnotationData data, MethodNode method) throws IOException;

    void generate() throws IOException;
}
