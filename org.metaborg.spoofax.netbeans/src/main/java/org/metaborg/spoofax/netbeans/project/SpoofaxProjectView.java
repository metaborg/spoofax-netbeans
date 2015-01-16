package org.metaborg.spoofax.netbeans.project;

import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.loaders.DataFolder;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class SpoofaxProjectView implements LogicalViewProvider {
    
    private final SpoofaxProject project;

    public SpoofaxProjectView(SpoofaxProject project) {
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        try {
            DataFolder folder = DataFolder.findFolder(project.getProjectDirectory());
            Node node = folder.getNodeDelegate();
            return new SpoofaxProjectNode(node, project);
        } catch (Exception ex) {
            return new AbstractNode(Children.LEAF, project.getLookup());
        }
    }

    @Override
    public Node findPath(Node node, Object o) {
        return null;
    }

}
