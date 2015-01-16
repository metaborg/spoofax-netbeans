package org.metaborg.spoofax.netbeans;

import com.google.inject.Guice;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.netbeans.util.GuiceLookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SpoofaxLookup.class)
public class SpoofaxLookup extends GuiceLookup {

    public SpoofaxLookup() {
        super(Guice.createInjector(new SpoofaxModule()));
    }

}
