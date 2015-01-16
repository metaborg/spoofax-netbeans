package org.metaborg.spoofax.netbeans.filetype;

import java.io.IOException;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageIdentifierService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.core.terms.ITermFactoryService;
import org.metaborg.spoofax.netbeans.SpoofaxLookup;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.ITermFactory;

@Messages({
    "LBL_Spoofax_LOADER=Spoofax Language File"
})
@DataObject.Registration(
        mimeType = "application/spoofax",
        displayName = "#LBL_Spoofax_LOADER",
        iconBase = "org/metaborg/spoofax/netbeans/icons/filetype_16x16.png",
        position = 300
)
@ActionReferences({
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.OpenAction"),
            position = 100,
            separatorAfter = 200
    ),
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CutAction"),
            position = 300
    ),
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.CopyAction"),
            position = 400,
            separatorAfter = 500
    ),
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "Edit", id = "org.openide.actions.DeleteAction"),
            position = 600
    ),
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.RenameAction"),
            position = 700,
            separatorAfter = 800
    ),
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.FileSystemAction"),
            position = 1100,
            separatorAfter = 1200
    ),
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.ToolsAction"),
            position = 1300
    ),
    @ActionReference(
            path = "Loaders/application/spoofax/Actions",
            id = @ActionID(category = "System", id = "org.openide.actions.PropertiesAction"),
            position = 1400
    )
})
public class SpoofaxDataObject extends MultiDataObject {
    private static final Logger log = LoggerFactory.getLogger(SpoofaxDataObject.class);

    private final Lookup lookup;
    private final InstanceContent lookupContent = new InstanceContent();

    public SpoofaxDataObject(FileObject pf, MultiFileLoader loader)
            throws DataObjectExistsException, IOException {
        super(pf, loader);
        lookup = new ProxyLookup( getCookieSet().getLookup(),
                new AbstractLookup(lookupContent));
        initLanguageServices(pf);
        registerEditor(SpoofaxMIMEResolver.SPOOFAX_MIME_TYPE, false);
    }

    private void initLanguageServices(FileObject pf) throws IOException {
        SpoofaxLookup spoofax = Lookup.getDefault().lookup(SpoofaxLookup.class);
        if ( spoofax == null ) return;

        IResourceService resourceService =
                spoofax.lookup(IResourceService.class);
        org.apache.commons.vfs2.FileObject vfo;
        try {
            vfo = resourceService.resolve(pf.toURL().toString());
        } catch (RuntimeException ex) {
            log.error("Failed to resolve {}.",pf.toURL(),ex);
            return;
        }

        ILanguageIdentifierService languageIdentifierService =
                spoofax.lookup(ILanguageIdentifierService.class);
        ILanguage lang;
        lang = languageIdentifierService.identify(vfo);
        if ( lang == null ) {
            log.error("Failed to identify language for {}.",pf.toURL());
            return;
        }

        ITermFactoryService termFactoryService =
                spoofax.lookup(ITermFactoryService.class);
        ITermFactory termFactory = termFactoryService.get(lang);

        lookupContent.add(lang);
        lookupContent.add(termFactory);
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @Override
    protected Node createNodeDelegate() {
        return new DataNode(this, Children.LEAF, getLookup());
    }

}
