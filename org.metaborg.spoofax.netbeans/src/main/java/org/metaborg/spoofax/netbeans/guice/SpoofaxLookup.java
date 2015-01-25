package org.metaborg.spoofax.netbeans.guice;

import com.google.inject.Guice;
import java.io.File;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = SpoofaxLookup.class)
public class SpoofaxLookup extends GuiceLookup {

    private final IResourceService resourceService;

    public SpoofaxLookup() {
        super(Guice.createInjector(new SpoofaxNetbeansModule()));
        this.resourceService = lookup(IResourceService.class);
    }

    public org.apache.commons.vfs2.FileObject toVFS(FileObject fo) {
        return resourceService.resolve(fo.toURL().toString());
    }

    public FileObject fromVFS(org.apache.commons.vfs2.FileObject fo) {
        return FileUtil.toFileObject(new File(fo.getName().getPath()));
    }

}
