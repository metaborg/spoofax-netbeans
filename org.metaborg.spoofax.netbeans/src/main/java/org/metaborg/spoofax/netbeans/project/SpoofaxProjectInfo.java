package org.metaborg.spoofax.netbeans.project;

import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.openide.util.ImageUtilities;

class SpoofaxProjectInfo implements ProjectInformation {

    @StaticResource
    public static final String ICON = "org/metaborg/spoofax/netbeans/icons/project_16x16.png";

    private final SpoofaxProject project;

    public SpoofaxProjectInfo(SpoofaxProject project) {
        this.project = project;
    }

    @Override
    public String getName() {
        return project.getProjectDirectory().getName();
    }

    @Override
    public String getDisplayName() {
        return getName() + " (Spoofax Language)";
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(ImageUtilities.loadImage(ICON));
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pl) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pl) {
    }
    
}
