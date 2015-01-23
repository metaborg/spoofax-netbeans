package org.metaborg.spoofax.netbeans.project;

import java.util.HashMap;
import java.util.Map;
import org.metaborg.spoofax.netbeans.filetype.SpoofaxFileService;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;

public class SpoofaxProjectService {
    
    private final Map<FileObject,SpoofaxFileService> files =
            new HashMap<FileObject,SpoofaxFileService>();

    public SpoofaxProjectService(FileObject projectDirectory) {
        projectDirectory.addRecursiveListener(fcl);
        scanForFiles(projectDirectory);
        // TODO : start analysis task
    }

    private void scanForFiles(FileObject folder) {
        for ( FileObject fo : folder.getChildren() ) {
            if ( fo.isFolder() ) {
                scanForFiles(fo);
            } else if ( fo.isData() ) {
                registerFile(fo);
            }
        }
    }

    private final FileChangeListener fcl = new FileChangeAdapter() {

        @Override
        public void fileDeleted(FileEvent fe) {
            files.remove(fe.getFile());
        }

        @Override
        public void fileChanged(FileEvent fe) {
            // TODO : isExpected indicates if change was internal or external?
            // TODO : start analysis task
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            registerFile(fe.getFile());
        }

    };

    private void registerFile(FileObject fo) {
        try {
            DataObject d = DataObject.find(fo);
            SpoofaxFileService fileService = d.getLookup().lookup(SpoofaxFileService.class);
            if ( fileService != null ) {
                files.put(fo, fileService);
            }
        } catch (DataObjectNotFoundException ex) {
        }
    }

}
