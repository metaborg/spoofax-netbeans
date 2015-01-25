package org.metaborg.spoofax.netbeans.editor;

import java.util.ArrayList;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.metaborg.spoofax.core.analysis.AnalysisFileResult;
import org.metaborg.spoofax.core.analysis.AnalysisResult;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.messages.IMessage;
import org.metaborg.spoofax.core.messages.ISourceRegion;
import org.metaborg.spoofax.core.style.IRegionStyle;
import org.metaborg.spoofax.core.style.IStyle;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.netbeans.spi.editor.highlighting.HighlightsContainer;
import org.netbeans.spi.editor.highlighting.HighlightsLayer;
import org.netbeans.spi.editor.highlighting.HighlightsLayerFactory;
import org.netbeans.spi.editor.highlighting.ZOrder;
import org.openide.loaders.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.netbeans.guice.SpoofaxLookup;
import org.metaborg.spoofax.netbeans.filetype.SpoofaxFileService;
import org.metaborg.spoofax.netbeans.project.SpoofaxProjectService;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.editor.highlighting.support.PositionsBag;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.filesystems.FileObject;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.spoofax.interpreter.terms.IStrategoTerm;
import rx.functions.Action1;

@MimeRegistration(mimeType = "application/spoofax", service = HighlightsLayerFactory.class)
public class SpoofaxHighlightsLayerFactory implements HighlightsLayerFactory {

    private static final Logger log = LoggerFactory.getLogger(SpoofaxHighlightsLayerFactory.class);

    @Override
    public HighlightsLayer[] createLayers(Context cntxt) {
        Document document = cntxt.getDocument();
        DataObject dataObject = NbEditorUtilities.getDataObject(document);
        if ( dataObject != null ) {
            SpoofaxFileService fileService = dataObject.getLookup().lookup(SpoofaxFileService.class);
            if ( fileService != null ) {
                addTextUpdater(document, fileService);
                addParseResults(document, fileService);
                Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
                SpoofaxProjectService projectService = project.getLookup().lookup(SpoofaxProjectService.class);
                if ( projectService != null ) {
                    addAnalysisResults(document, dataObject.getPrimaryFile(),
                            fileService.getLanguage(), projectService);
                }
                return new HighlightsLayer[]{
                    HighlightsLayer.create("Spoofax Syntax",
                            ZOrder.SYNTAX_RACK, true,
                            getSyntaxHighlighter(document, fileService).getHighlights())
                };
            }
        }
        return new HighlightsLayer[0];
    }

    private void addTextUpdater(final Document document, final SpoofaxFileService fileService) {
        document.addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                notifyText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                notifyText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                notifyText();
            }

            private void notifyText() {
                try {
                    fileService.text().onNext(document.getText(0, document.getLength()));
                } catch (BadLocationException ex) {
                    log.error("Problem getting document text.",ex);
                }
            }

        });
    }

    private void addParseResults(final Document document, final SpoofaxFileService fileService) {
        fileService.parseResult().subscribe(new Action1<ParseResult<IStrategoTerm>>(){
            @Override
            public void call(ParseResult<IStrategoTerm> parseResult) {
                List<ErrorDescription> errorDescriptions = new ArrayList<ErrorDescription>();
                for ( IMessage message : parseResult.messages ) {
                    try {
                        errorDescriptions.add(
                                ErrorDescriptionFactory.createErrorDescription(
                                        getSeverity(message),
                                        message.message(),
                                        document,
                                        getStartPosition(document, message.region()),
                                        getEndPosition(document, message.region())));
                    } catch (BadLocationException ex) {
                        log.error("Problem creating hint.", ex);
                    }
                }
                HintsController.setErrors(document, "Spoofax Parsing", errorDescriptions);
            }
        });
    }

    private void addAnalysisResults(final Document document, final FileObject file,
            final ILanguage language, final SpoofaxProjectService projectService) {
        projectService.analysis().subscribe(new Action1<AnalysisResult<IStrategoTerm,IStrategoTerm>>() {
            @Override
            public void call(AnalysisResult<IStrategoTerm,IStrategoTerm> analysisResult) {
                if ( !analysisResult.language.equals(language) ) { return; }
                SpoofaxLookup spoofax = Lookup.getDefault().lookup(SpoofaxLookup.class);
                List<ErrorDescription> errorDescriptions = new ArrayList<ErrorDescription>();
                for ( AnalysisFileResult<IStrategoTerm,IStrategoTerm> fileResult : analysisResult.fileResults ) {
                    FileObject fo = spoofax.fromVFS(fileResult.file());
                    if ( fo.equals(file) ) {
                        for ( IMessage message : fileResult.messages() ) {
                            try {
                                errorDescriptions.add(
                                        ErrorDescriptionFactory.createErrorDescription(
                                                getSeverity(message),
                                                message.message(),
                                                document,
                                                getStartPosition(document, message.region()),
                                                getEndPosition(document, message.region())));
                            } catch (BadLocationException ex) {
                                log.error("Problem creating hint.", ex);
                            }
                        }
                    }
                }
                HintsController.setErrors(document, "Spoofax Analysis", errorDescriptions);
            }
        });
    }

    private SpoofaxSyntaxHighlighter getSyntaxHighlighter(final Document document,
            final SpoofaxFileService fileService) {
        SpoofaxSyntaxHighlighter sh = (SpoofaxSyntaxHighlighter) document.getProperty(SpoofaxSyntaxHighlighter.class);
        if ( sh == null ) {
            document.putProperty(SpoofaxSyntaxHighlighter.class,
                    sh = new SpoofaxSyntaxHighlighter(document));
            fileService.highlights().subscribe(sh);
        }
        return sh;
    }

    private static class SpoofaxSyntaxHighlighter implements Action1<Iterable<IRegionStyle<IStrategoTerm>>> {

        private final Document document;
        private final PositionsBag bag;

        public SpoofaxSyntaxHighlighter(Document document) {
            this.document = document;
            this.bag = new PositionsBag(document);
        }

        @Override
        public void call(final Iterable<IRegionStyle<IStrategoTerm>> highlights) {
            bag.clear();
            for ( IRegionStyle<IStrategoTerm> h : highlights ) {
                try {
                    bag.addHighlight(
                            getStartPosition(document, h.region()),
                            getEndPosition(document, h.region()),
                            getAttributeSet(h.style()));
                } catch (BadLocationException ex) {
                    log.error("Problem creating highlight.", ex);
                }
            }
        }

        private AttributeSet getAttributeSet(IStyle style) {
            SimpleAttributeSet as = new SimpleAttributeSet();
            if ( style.backgroundColor() != null ) {
                as.addAttribute(StyleConstants.Background, style.backgroundColor());
            }
            if ( style.color() != null ) {
                as.addAttribute(StyleConstants.Foreground, style.color());
            }
            as.addAttribute(StyleConstants.Bold, style.bold());
            as.addAttribute(StyleConstants.Italic, style.italic());
            as.addAttribute(StyleConstants.Underline, style.underscore());
            return as;
        }

        public HighlightsContainer getHighlights() {
            return bag;
        }

    }

    private static Position getStartPosition(Document doc, ISourceRegion region)
            throws BadLocationException {
        return NbDocument.createPosition(doc,
                region.startOffset() >= 0 ? region.startOffset() : 0,
                Position.Bias.Forward);
    }

    private static Position getEndPosition(Document doc, ISourceRegion region)
            throws BadLocationException {
        return region.endOffset() >= 0
                ?  NbDocument.createPosition(doc, region.endOffset() + 1, Position.Bias.Forward)
                : getStartPosition(doc, region);
    }

    private Severity getSeverity(IMessage message) {
        switch (message.severity()) {
            case ERROR:
                return Severity.ERROR;
            case WARNING:
                return Severity.WARNING;
            case NOTE:
                return Severity.HINT;
        }
        return Severity.VERIFIER;
    }

}
