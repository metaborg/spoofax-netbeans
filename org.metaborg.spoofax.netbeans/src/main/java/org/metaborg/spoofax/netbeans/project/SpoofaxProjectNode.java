package org.metaborg.spoofax.netbeans.project;

import java.awt.Image;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public class SpoofaxProjectNode extends FilterNode {

    @StaticResource
    public static final String ICON = "org/metaborg/spoofax/netbeans/icons/project_16x16.png";

    public SpoofaxProjectNode(Node original, SpoofaxProject project) {
        super(original,
                new FilterNode.Children(original),
                new ProxyLookup(original.getLookup(),
                        project.getLookup(), Lookups.singleton(project)));
    }

    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage(ICON);
    }
    
}
