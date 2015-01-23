package org.metaborg.spoofax.netbeans.filetype;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.util.concurrent.Callable;
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
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;
import rx.Observable;
import rx.Observer;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

public class SpoofaxFileService {

    private static final Logger log = LoggerFactory.getLogger(SpoofaxFileService.class);

    private final FileObject fileObject;
    private final IResourceService resourceService;
    private final ILanguage language;
    private final ISyntaxService<IStrategoTerm> syntaxService;
    private final ICategorizerService<IStrategoTerm, IStrategoTerm> categorizerService;
    private final IStylerService<IStrategoTerm, IStrategoTerm> stylerService;

    private final Subject<String,String> text = BehaviorSubject.create();
    private final Observable<ParseResult<IStrategoTerm>> parseResult;
    private final Observable<Iterable<IRegionStyle<IStrategoTerm>>> highlights;

    private static final RequestProcessor RP = new RequestProcessor(SpoofaxFileService.class);

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
        parseResult = text.flatMap(new ParseFunc());
        highlights = parseResult.map(new HighlightFunc());
        fileObject.addFileChangeListener(fcl);
        setTextFromFile();
    }

    private final FileChangeListener fcl = new FileChangeAdapter() {
        @Override
        public void fileChanged(FileEvent fe) {
            setTextFromFile();
        }
    };

    private void setTextFromFile() {
        try {
            text.onNext(fileObject.asText());
        } catch (IOException ex) {
            log.error("Problem reading changed file.", ex);
        }
    }

    private org.apache.commons.vfs2.FileObject getVFO() throws FileNotFoundException {
        try {
            return  resourceService.resolve(fileObject.toURL().toString());
        } catch (RuntimeException ex) {
            throw new FileNotFoundException(ex);
        }
    }

    public ILanguage getLanguage() {
        return language;
    }

    public Observable<ParseResult<IStrategoTerm>> parseResult() {
        return parseResult;
    }

    public Observable<Iterable<IRegionStyle<IStrategoTerm>>> highlights() {
        return highlights;
    }

    public Observer<String> text() {
        return text;
    }

    private class ParseFunc implements Func1<String,Observable<ParseResult<IStrategoTerm>>> {
        @Override
        public Observable<ParseResult<IStrategoTerm>> call(final String text) {
            return Observable.from(RP.submit(new Callable<ParseResult<IStrategoTerm>>(){
                @Override
                public ParseResult<IStrategoTerm> call() throws Exception {
                    return syntaxService.parse(text, getVFO(), language);
                }
            }));
        }
    }

    private class HighlightFunc implements Func1<ParseResult<IStrategoTerm>,Iterable<IRegionStyle<IStrategoTerm>>> {
        @Override
        public Iterable<IRegionStyle<IStrategoTerm>> call(ParseResult<IStrategoTerm> result) {
            Iterable<IRegionCategory<IStrategoTerm>> categories =
                    categorizerService.categorize(language, result);
            return stylerService.styleParsed(language, categories);
        }
    };

}
