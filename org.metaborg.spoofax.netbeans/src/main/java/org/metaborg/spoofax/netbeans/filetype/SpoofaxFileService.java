package org.metaborg.spoofax.netbeans.filetype;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.vfs2.FileNotFoundException;
import org.apache.commons.vfs2.FileTypeHasNoContentException;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageIdentifierService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.core.style.ICategorizerService;
import org.metaborg.spoofax.core.style.IRegionCategory;
import org.metaborg.spoofax.core.style.IRegionStyle;
import org.metaborg.spoofax.core.style.IStylerService;
import org.metaborg.spoofax.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.netbeans.SpoofaxLookup;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.util.Cancellable;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;

public class SpoofaxFileService {

    private static final Logger log = LoggerFactory.getLogger(SpoofaxFileService.class);

    private final FileObject fileObject;
    private final IResourceService resourceService;
    private final ILanguage language;
    private final ISyntaxService<IStrategoTerm> syntaxService;
    private final ICategorizerService<IStrategoTerm, IStrategoTerm> categorizerService;
    private final IStylerService<IStrategoTerm, IStrategoTerm> stylerService;
    private final List<ParseListener> listeners = new ArrayList<ParseListener>();

    private ParseResult<IStrategoTerm> parseResult;

    public SpoofaxFileService(FileObject fileObject) throws IOException {
        this.fileObject = fileObject;
        SpoofaxLookup spoofax = Lookup.getDefault().lookup(SpoofaxLookup.class);
        resourceService = spoofax.lookup(IResourceService.class);
        ILanguageIdentifierService languageIdentifierService =
                spoofax.lookup(ILanguageIdentifierService.class);
        this.language = languageIdentifierService.identify(getVFO());
        if ( this.language == null ) {
            throw new FileTypeHasNoContentException("Unable to identify language.");
        }
        this.syntaxService = spoofax.lookup(
                Key.get(new TypeLiteral<ISyntaxService<IStrategoTerm>>(){}));
        this.categorizerService = spoofax.lookup(
                Key.get(new TypeLiteral<ICategorizerService<IStrategoTerm, IStrategoTerm>>() {}));
        this.stylerService = spoofax.lookup(
                Key.get(new TypeLiteral<IStylerService<IStrategoTerm, IStrategoTerm>>() {}));
        fileObject.addFileChangeListener(fcl);
        setTextFromFile();
    }

    public ILanguage getLanguage() {
        return language;
    }

    public ParseResult<IStrategoTerm> getParseResult() {
        return parseResult;
    }

    public Iterable<IRegionStyle<IStrategoTerm>> getHighlights(ParseResult<IStrategoTerm> result) {
        Iterable<IRegionCategory<IStrategoTerm>> categories =
                categorizerService.categorize(language, result);
        return stylerService.styleParsed(language, categories);
    }

    private org.apache.commons.vfs2.FileObject getVFO() throws FileNotFoundException {
        try {
            return  resourceService.resolve(fileObject.toURL().toString());
        } catch (RuntimeException ex) {
            throw new FileNotFoundException(ex);
        }
    }

    private final FileChangeListener fcl = new FileChangeAdapter() {
        @Override
        public void fileChanged(FileEvent fe) {
            setTextFromFile();
        }
    };

    private void setTextFromFile() {
        try {
            setText(fileObject.asText());
        } catch (IOException ex) {
            log.error("Problem reading changed file.", ex);
        }
    }

    public void setText(String text) {
        RequestProcessor.getDefault().execute(new ParseTask(text));
    }

    private class ParseTask implements Runnable {

        private final String text;

        public ParseTask(String text) {
            this.text = text;
        }

        @Override
        public void run() {
            try {
                parseResult = syntaxService.parse(text, getVFO(), language);
                notifyParseResult();
            } catch (IOException ex) {
                log.error("Parse failed.", ex);
            }
        }
    };

    public Cancellable addParseListener(final ParseListener listener) {
        listeners.add(listener);
        if ( parseResult != null ) {
            listener.parseResult(parseResult);
        }
        return new Cancellable() {
            @Override
            public boolean cancel() {
                return listeners.remove(listener);
            }
        };
    }

    private void notifyParseResult() {
        for ( ParseListener listener : new ArrayList<ParseListener>(listeners) ) {
            listener.parseResult(parseResult);
        }
    }

    public interface ParseListener {
        public void parseResult(ParseResult<IStrategoTerm> parseResult);
    }

}
