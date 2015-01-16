package org.metaborg.spoofax.netbeans;

import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.netbeans.options.SpoofaxPreferences;
import org.openide.modules.ModuleInstall;
import org.openide.util.Lookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Installer extends ModuleInstall implements PreferenceChangeListener {

    private static final Logger log = LoggerFactory.getLogger(Installer.class);

    @Override
    public void restored() {
        discoverLanguages();
        SpoofaxPreferences.getDefault().addPreferenceChangeListener(this);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        discoverLanguages();
    }

    public static void discoverLanguages() {
        log.info("Discovering languages");
        Lookup lookup = Lookup.getDefault();
        SpoofaxLookup spoofax = lookup.lookup(SpoofaxLookup.class);
        IResourceService resourceService = spoofax.lookup(IResourceService.class);
        ILanguageDiscoveryService languageDiscoveryService = spoofax.lookup(ILanguageDiscoveryService.class);
        List<String> paths = SpoofaxPreferences.getDefault().getLanguagePaths();
        for ( String path : paths ) {
            try {
                FileObject dir = resourceService.resolve(path);
                Iterable<ILanguage> langs = languageDiscoveryService.discover(dir);
                for ( ILanguage lang : langs ) {
                    log.info("Discovered language: {}", lang.name());
                }
            } catch (Exception ex) {
                log.error("Language discovery problem.", ex);
            }
        }
    }

}
