package org.metaborg.spoofax.netbeans.editor;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.messages.IMessage;
import org.metaborg.spoofax.core.resource.IResourceService;
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
import org.metaborg.spoofax.core.syntax.ISyntaxService;
import org.metaborg.spoofax.core.syntax.ParseResult;
import org.metaborg.spoofax.netbeans.SpoofaxLookup;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.HintsController;
import org.netbeans.spi.editor.hints.Severity;
import org.openide.util.Lookup;
import org.spoofax.interpreter.terms.IStrategoTerm;

@MimeRegistration(mimeType = "application/spoofax", service = HighlightsLayerFactory.class)
public class SpoofaxHighlightsLayerFactory implements HighlightsLayerFactory {

    private static Logger log = LoggerFactory.getLogger(SpoofaxHighlightsLayerFactory.class);

    @Override
    public HighlightsLayer[] createLayers(Context cntxt) {
        Document document = cntxt.getDocument();
        DataObject dataObject = NbEditorUtilities.getDataObject(document);
        if ( dataObject != null ) {
            ILanguage language = dataObject.getLookup().lookup(ILanguage.class);
            if ( language != null ) {
                return new HighlightsLayer[]{
                    HighlightsLayer.create(SpoofaxSyntaxHighlighter.class.getName(),
                            ZOrder.SYNTAX_RACK, true,
                            getSyntaxHighlighter(cntxt.getDocument(), dataObject, language).getHighlightsBag())
                };
            }
        }
        return new HighlightsLayer[0];
    }
    
    private SpoofaxSyntaxHighlighter getSyntaxHighlighter(Document document, DataObject dataObject, ILanguage language) {
        SpoofaxSyntaxHighlighter sh = (SpoofaxSyntaxHighlighter) document.getProperty(SpoofaxSyntaxHighlighter.class);
        if ( sh == null ) {
            document.putProperty(SpoofaxSyntaxHighlighter.class,
                    sh = new SpoofaxSyntaxHighlighter(document, dataObject, language));
        }
        return sh;
    }

    private static class SpoofaxSyntaxHighlighter implements DocumentListener {

        private final Document document;
        private final OffsetsBag bag;
        private final ISyntaxService<IStrategoTerm> syntaxService;
        private final DataObject dataObject;
        private final ILanguage language;
        private final IResourceService resourceService;

        public SpoofaxSyntaxHighlighter(Document document, DataObject dataObject, ILanguage language) {
            this.document = document;
            this.dataObject = dataObject;
            this.language = language;
            this.bag = new OffsetsBag(document);
            SpoofaxLookup spoofax = Lookup.getDefault().lookup(SpoofaxLookup.class);
            this.syntaxService = spoofax.lookup(Key.get(new TypeLiteral<ISyntaxService<IStrategoTerm>>(){}));
            this.resourceService = spoofax.lookup(IResourceService.class);
            document.addDocumentListener(this);
            update();
        }

        private void update() {
            // TODO this should be async
            try {
                String text = document.getText(0, document.getLength());
                FileObject vfo = resourceService.resolve(dataObject.getPrimaryFile().toURL().toExternalForm());
                ParseResult<IStrategoTerm> result = syntaxService.parse(text, vfo, language);
                HintsController.setErrors(document, "Parse Errors", getErrorDescriptions(result));
                OffsetsBag newBag = new OffsetsBag(document);
                SimpleAttributeSet as = new SimpleAttributeSet();
                as.addAttribute(StyleConstants.Italic, true);
                newBag.addHighlight(0, Math.min(5, document.getLength()), as);
                bag.setHighlights(newBag);
            } catch (BadLocationException ex) {
                log.error("Problem getting document text.",ex);
            } catch (IOException ex) {
                log.error("Failed to parse text.",ex);
            }
        }

        private List<ErrorDescription> getErrorDescriptions(ParseResult<IStrategoTerm> result) {
            List<ErrorDescription> errorDescriptions = new ArrayList<ErrorDescription>();
            for ( IMessage message : result.messages ) {
                int s = message.region().startOffset();
                int e = message.region().endOffset();
                errorDescriptions.add(
                        ErrorDescriptionFactory.createErrorDescription(getSeverity(message),
                        message.message(),
                        dataObject.getPrimaryFile(),
                        s, e < 0 ? Integer.MAX_VALUE : e));
            }
            return errorDescriptions;
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

        public HighlightsContainer getHighlightsBag() {
            return bag;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update();
        }

    }

}
