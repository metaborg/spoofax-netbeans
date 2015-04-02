package org.metaborg.spoofax.netbeans.guice;

import com.google.inject.multibindings.Multibinder;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.spoofax.interpreter.library.AbstractPrimitive;
import org.spoofax.interpreter.library.IOperatorRegistry;

public class SpoofaxNetbeansModule extends SpoofaxModule {

    public SpoofaxNetbeansModule() {
        super(SpoofaxNetbeansModule.class.getClassLoader());
    }

    /*
    @Override
    protected void bindResource() {
        bind(IResourceService.class).to(NetbeansResourceService.class)
                .in(Singleton.class);
        bind(FileSystemManager.class).toProvider(NetbeansFileSystemManagerProvider.class)
                .in(Singleton.class);
    }

    @Override
    protected void bindLocalFileProviders(MapBinder<String, ILocalFileProvider> binder) {
        super.bindLocalFileProviders(binder);
    }
    */

    @Override
    protected void bindOther() {
        bindPrimitives();
    }

    private void bindPrimitives() {
        final Multibinder<AbstractPrimitive> primitiveBinder =
            Multibinder.newSetBinder(binder(), AbstractPrimitive.class);
        bindPrimitive(primitiveBinder, ProjectPathPrimitive.class);
        bindPrimitive(primitiveBinder, new DummyPrimitive("SSL_EXT_set_total_work_units", 0, 0));
        bindPrimitive(primitiveBinder, new DummyPrimitive("SSL_EXT_set_markers", 0, 1));
        bindPrimitive(primitiveBinder, new DummyPrimitive("SSL_EXT_refreshresource", 0, 1));
        bindPrimitive(primitiveBinder, new DummyPrimitive("SSL_EXT_queue_strategy", 0, 2));
        bindPrimitive(primitiveBinder, new DummyPrimitive("SSL_EXT_complete_work_unit", 0, 0));

        final Multibinder<IOperatorRegistry> libraryBinder =
            Multibinder.newSetBinder(binder(), IOperatorRegistry.class);
        bindPrimitiveLibrary(libraryBinder, SpoofaxNetbeansPrimitiveLibrary.class);
    }

}