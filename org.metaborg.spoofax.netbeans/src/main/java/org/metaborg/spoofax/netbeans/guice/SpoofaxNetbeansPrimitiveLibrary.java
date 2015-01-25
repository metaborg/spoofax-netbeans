package org.metaborg.spoofax.netbeans.guice;

import com.google.inject.Inject;
import java.util.Set;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.library.AbstractStrategoOperatorRegistry;

class SpoofaxNetbeansPrimitiveLibrary extends AbstractStrategoOperatorRegistry {

    @Inject
    public SpoofaxNetbeansPrimitiveLibrary(Set<AbstractPrimitive> primitives) {
        for (AbstractPrimitive primitive : primitives) {
            add(primitive);
        }
    }


    @Override
    public String getOperatorRegistryName() {
        return SpoofaxNetbeansPrimitiveLibrary.class.getName();
    }
    
}
