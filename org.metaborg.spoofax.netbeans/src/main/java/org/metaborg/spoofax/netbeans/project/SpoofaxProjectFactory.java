package org.metaborg.spoofax.netbeans.project;

import java.io.IOException;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ProjectFactory.class)
public class SpoofaxProjectFactory implements ProjectFactory {

    @Override
    public boolean isProject(FileObject projectDirectory) {
        return hasSubFolder(projectDirectory, "editor")
                && hasSubFolder(projectDirectory, "syntax")
                && hasSubFolder(projectDirectory, "trans");
    }

    private boolean hasSubFolder(FileObject dir, String name) {
        FileObject sfo = dir.getFileObject(name);
        return sfo != null && sfo.isFolder();
    }

    @Override
    public Project loadProject(FileObject projectDirectory, ProjectState state) throws IOException {
        return isProject(projectDirectory) ? new SpoofaxProject(projectDirectory,state) : null;
    }

    @Override
    public void saveProject(Project baseProject) throws IOException, ClassCastException {
        SpoofaxProject project = (SpoofaxProject) baseProject;
    }
    
}
