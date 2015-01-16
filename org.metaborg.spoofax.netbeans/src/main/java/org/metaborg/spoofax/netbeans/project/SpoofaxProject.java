package org.metaborg.spoofax.netbeans.project;

import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public class SpoofaxProject implements Project {

    private final FileObject projectDirectory;
    private final ProjectState state;
    private final Lookup lookup;

    public SpoofaxProject(FileObject projectDirectory, ProjectState state) {
        this.projectDirectory = projectDirectory;
        this.state = state;
        this.lookup = Lookups.fixed(
                new SpoofaxProjectInfo(this),
                new SpoofaxProjectView(this));
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDirectory;
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }
    
}
