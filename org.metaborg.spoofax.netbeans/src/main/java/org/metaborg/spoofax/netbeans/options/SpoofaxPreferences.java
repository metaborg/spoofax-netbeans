package org.metaborg.spoofax.netbeans.options;

import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import org.metaborg.spoofax.netbeans.guice.SpoofaxLookup;
import org.openide.util.Exceptions;
import org.openide.util.NbPreferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpoofaxPreferences {
    private static final Logger log = LoggerFactory.getLogger(SpoofaxPreferences.class);

    private final Preferences prefs = NbPreferences.forModule(SpoofaxLookup.class);

    private SpoofaxPreferences() {
    }

    // Language paths

    private static final String PREF_LANGPATH = "languagePath";

    public List<String> getLanguagePaths() {
        List<String> paths = new ArrayList<String>();
        int count = prefs.getInt(PREF_LANGPATH+"_n", 0);
        for ( int i = 0; i < count; i++ ) {
            String path = prefs.get(PREF_LANGPATH+"_"+i, "");
            if ( !path.isEmpty() ) {
                paths.add(path);
            }
        }
        return paths;
    }

    public void setLanguagePaths(List<String> paths) {
        cleanKeysWithPrefix(PREF_LANGPATH);
        prefs.putInt(PREF_LANGPATH+"_n", paths.size());
        for ( int i = 0; i < paths.size(); i++ ) {
            prefs.put(PREF_LANGPATH+"_"+i, paths.get(i));
        }
    }

    private void cleanKeysWithPrefix(String prefix) {
        try {
            for ( String key : prefs.keys() ) {
                if ( key.startsWith(prefix)) {
                    prefs.remove(key);
                }
            }
        } catch (BackingStoreException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    // Change listening

    public void addPreferenceChangeListener(PreferenceChangeListener pcl) {
        prefs.addPreferenceChangeListener(pcl);
    }

    public void removePreferenceChangeListener(PreferenceChangeListener pcl) {
        prefs.removePreferenceChangeListener(pcl);
    }

    // Singleton stuff

    private static volatile SpoofaxPreferences instance = null;

    public static synchronized SpoofaxPreferences getDefault() {
        if ( instance == null ) {
            instance = new SpoofaxPreferences();
        }
        return instance;
    }
}
