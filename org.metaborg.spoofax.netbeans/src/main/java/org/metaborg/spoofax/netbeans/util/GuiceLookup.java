package org.metaborg.spoofax.netbeans.util;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Collection;
import java.util.Collections;
import org.openide.util.Lookup;
import org.openide.util.LookupListener;

public class GuiceLookup extends Lookup {
    
    private final Injector injector;

    public GuiceLookup(Injector injector) {
        this.injector = injector;
    }

    @Override
    public <T> T lookup(final Class<T> type) {
        return injector.getInstance(type);
    }

    public <T> T lookup(final Key<T> type) {
        return injector.getInstance(type);
    }

    @Override
    public <T> Result<T> lookup(final Template<T> tmplt) {
        return new Result<T>() {
            @Override
            public void addLookupListener(LookupListener ll) {}

            @Override
            public void removeLookupListener(LookupListener ll) { }

            @Override
            public Collection<? extends T> allInstances() {
                T i = injector.getInstance(tmplt.getType());
                return i != null ? Collections.singleton(i) : Collections.EMPTY_SET;
            }
        };
    }

}
