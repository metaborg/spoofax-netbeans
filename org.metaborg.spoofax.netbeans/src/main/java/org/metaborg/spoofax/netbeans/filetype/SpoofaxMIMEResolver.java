package org.metaborg.spoofax.netbeans.filetype;

import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageIdentifierService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.netbeans.SpoofaxLookup;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceProvider(service = MIMEResolver.class)
public class SpoofaxMIMEResolver extends MIMEResolver {

    private static final Logger log = LoggerFactory.getLogger(SpoofaxMIMEResolver.class);

    public static final String SPOOFAX_MIME_TYPE = "application/spoofax";

    public SpoofaxMIMEResolver() {
        super(new String[]{ SPOOFAX_MIME_TYPE });
    }

    @Override
    public String findMIMEType(FileObject fo) {
        log.info("Test if {} has a Spoofax language.", fo.getPath());
        SpoofaxLookup spoofax = Lookup.getDefault().lookup(SpoofaxLookup.class);
        if ( spoofax == null ) {
            log.error("Could not find SpoofaxService in Lookup.");
            return null;
        }
        IResourceService resourceService = spoofax.lookup(IResourceService.class);
        if ( resourceService == null ) {
            log.error("Could not find ResourceService in SpoofaxService.");
            return null;
        }
        ILanguageIdentifierService languageIdentifierService = spoofax.lookup(ILanguageIdentifierService.class);
        if ( languageIdentifierService == null ) {
            log.error("Could not find LanguageIdentificationService in SpoofaxService.");
            return null;
        }
        org.apache.commons.vfs2.FileObject vfo;
        try {
            vfo = resourceService.resolve(fo.toURL().toExternalForm());
        } catch (RuntimeException ex) {
            log.error("ResourceService could not resolve {}.", fo.getPath());
            return null;
        }
        try {
            ILanguage lang = languageIdentifierService.identify(vfo);
            if ( lang != null ) {
                log.info("Identified {} as {} file.", fo.getPath(), lang.name());
                return SPOOFAX_MIME_TYPE;
            }
        } catch (RuntimeException ex) {
            log.error("Error while identifying language.", ex);
        }
        return null;
    }
    
}
