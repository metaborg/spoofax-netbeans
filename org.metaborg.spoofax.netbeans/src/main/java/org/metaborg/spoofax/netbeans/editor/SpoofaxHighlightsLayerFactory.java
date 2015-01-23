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
import org.netbeans.spi.editor.highlighting.support.OffsetsBag;
import org.openide.loaders.DataObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.netbeans.filetype.SpoofaxFileService;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.spoofax.interpreter.terms.IStrategoTerm;

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
                addUpdateListener(document, fileService);
                addErrorListener(document, fileService);
                return new HighlightsLayer[]{
                    HighlightsLayer.create("Spoofax Syntax",
                            ZOrder.SYNTAX_RACK, true,
                            getSyntaxHighlighter(document, fileService).getHighlights())
                };
            }
        }
        return new HighlightsLayer[0];
    }

    private void addUpdateListener(final Document document, final SpoofaxFileService fileService) {
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
                    fileService.setText(document.getText(0, document.getLength()));
                } catch (BadLocationException ex) {
                    log.error("Problem getting document text.",ex);
                }
            }

        });
    }

    private void addErrorListener(final Document document, final SpoofaxFileService fileService) {
        fileService.addParseListener(new SpoofaxFileService.ParseListener() {

            @Override
            public void parseResult(ParseResult<IStrategoTerm> parseResult) {
                List<ErrorDescription> errorDescriptions = new ArrayList<ErrorDescription>();
                for ( IMessage message : parseResult.messages ) {
                    errorDescriptions.add(
                            ErrorDescriptionFactory.createErrorDescription(
                                        getSeverity(message),
                                        message.message(),
                                        document,
                                        getStartPosition(message.region()),
                                        getEndPosition(message.region())));
                }
                HintsController.setErrors(document, "Parse Errors", errorDescriptions);
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

        });
    }

    private SpoofaxSyntaxHighlighter getSyntaxHighlighter(final Document document,
            final SpoofaxFileService fileService) {
        SpoofaxSyntaxHighlighter sh = (SpoofaxSyntaxHighlighter) document.getProperty(SpoofaxSyntaxHighlighter.class);
        if ( sh == null ) {
            document.putProperty(SpoofaxSyntaxHighlighter.class,
                    sh = new SpoofaxSyntaxHighlighter(document, fileService));
            fileService.addParseListener(sh);
        }
        return sh;
    }

    private static class SpoofaxSyntaxHighlighter implements SpoofaxFileService.ParseListener {

        private final SpoofaxFileService fileService;
        private final OffsetsBag bag;

        public SpoofaxSyntaxHighlighter(Document document, SpoofaxFileService fileService) {
            this.fileService = fileService;
            this.bag = new OffsetsBag(document);
        }

        @Override
        public void parseResult(final ParseResult<IStrategoTerm> parseResult) {
            bag.clear();
            for ( IRegionStyle<IStrategoTerm> h : fileService.getHighlights(parseResult) ) {
                bag.addHighlight(getStartOffset(h.region()), getEndOffset(h.region()),
                        getAttributeSet(h.style()));
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

    private static int getStartOffset(final ISourceRegion region) {
        return region.startOffset() >= 0 ? region.startOffset() : 0;
    }

    private static int getEndOffset(final ISourceRegion region) {
        return region.endOffset() >= 0 ? region.endOffset() + 1 : getStartOffset(region);
    }

    private static Position getStartPosition(final ISourceRegion region) {
        return new Position() {
            @Override
            public int getOffset() {
                return getStartOffset(region);
            }
        };
    }

    private static Position getEndPosition(final ISourceRegion region) {
        return new Position() {
            @Override
            public int getOffset() {
                return getEndOffset(region);
            }
        };
    }

}
