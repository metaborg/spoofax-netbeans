package org.metaborg.spoofax.netbeans.project;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.metaborg.spoofax.core.analysis.AnalysisResult;
import org.metaborg.spoofax.core.analysis.IAnalysisService;
import org.metaborg.spoofax.core.context.SpoofaxContext;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.netbeans.guice.SpoofaxLookup;
import org.metaborg.spoofax.netbeans.filetype.SpoofaxFileService;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spoofax.interpreter.terms.IStrategoTerm;
import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.Subject;

public class SpoofaxProjectService {
    
    private static Logger log = LoggerFactory.getLogger(SpoofaxProjectService.class);

    private static final RequestProcessor RP = new RequestProcessor(SpoofaxProjectService.class);

    private final FileObject projectDirectory;
    private final SpoofaxLookup spoofax;
    private final IAnalysisService<IStrategoTerm, IStrategoTerm> analysisService;

    private final Map<FileObject,SpoofaxFileService> files =
            new HashMap<FileObject, SpoofaxFileService>();
    private final ListMultimap<ILanguage,SpoofaxFileService> langFiles =
            ArrayListMultimap.create();
    private final Subject<LF,LF> lfs = BehaviorSubject.create();
    private final Observable<AnalysisResult<IStrategoTerm, IStrategoTerm>> analysis;

    public SpoofaxProjectService(FileObject projectDirectory) {
        this.projectDirectory = projectDirectory;
        this.spoofax = Lookup.getDefault().lookup(SpoofaxLookup.class);
        this.analysisService = spoofax.lookup(Key.get(
                new TypeLiteral<IAnalysisService<IStrategoTerm,IStrategoTerm>>(){}));
        this.analysis = setupAnalysis();
        projectDirectory.addRecursiveListener(fcl);
        scanForFiles(projectDirectory);
        for ( ILanguage lang : langFiles.keySet() ) {
            triggerAnalysis(lang);
        }
    }

    public Observable<AnalysisResult<IStrategoTerm,IStrategoTerm>> analysis() {
        return analysis;
    }

    private void scanForFiles(FileObject folder) {
        for ( FileObject fo : folder.getChildren() ) {
            if ( fo.isFolder() ) {
                scanForFiles(fo);
            } else if ( fo.isData() ) {
                addFile(fo);
            }
        }
    }

    private final FileChangeListener fcl = new FileChangeAdapter() {

        @Override
        public void fileDeleted(FileEvent fe) {
            removeFile(fe.getFile());
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            addFile(fe.getFile());
        }

    };

    private void addFile(FileObject fo) {
        try {
            DataObject d = DataObject.find(fo);
            SpoofaxFileService fileService = d.getLookup().lookup(SpoofaxFileService.class);
            if ( fileService != null ) {
                files.put(fo, fileService);
                ILanguage language = fileService.getLanguage();
                langFiles.put(language, fileService);
                triggerAnalysis(language);
            }
        } catch (DataObjectNotFoundException ex) {
        }
    }

    private void removeFile(FileObject fo) {
        SpoofaxFileService fileService = files.remove(fo);
        if ( fileService != null ) {
            ILanguage language = fileService.getLanguage();
            langFiles.remove(language, fileService);
            triggerAnalysis(language);
        }
    }

    private void triggerAnalysis(ILanguage lang) {
        lfs.onNext(new LF(lang,langFiles.get(lang)));
    }

    private Observable<AnalysisResult<IStrategoTerm,IStrategoTerm>> setupAnalysis() {
        return Observable.merge(
                lfs.groupBy((lf) -> lf.lang)
                        .map((lfo) -> {
                            return Observable.switchOnNext(lfo.flatMap((lf) -> {
                                return Observable.combineLatest(
                                        Lists.transform(lf.files, (f) -> f.parseResult()),
                                        (prs) -> {
                                            return Observable.from(RP.submit(() -> {
                                                try {
                                                    return (AnalysisResult<IStrategoTerm,IStrategoTerm>)
                                                            analysisService.analyze((List)Arrays.asList(prs),
                                                                    new SpoofaxContext(lf.lang, spoofax.toVFS(projectDirectory)));
                                                } catch (Exception ex) {
                                                    log.error("Analysis problem.",ex);
                                                    return (AnalysisResult<IStrategoTerm,IStrategoTerm>)null;
                                                }
                                            }));
                                        }
                                );
                            }));
                        })
        ).filter((a) -> a != null);
    }

    private static class LF {
        final ILanguage lang;
        final List<SpoofaxFileService> files;
        public LF(ILanguage lang, List<SpoofaxFileService> files) {
            this.lang = lang;
            this.files = files;
        }
    }

}
