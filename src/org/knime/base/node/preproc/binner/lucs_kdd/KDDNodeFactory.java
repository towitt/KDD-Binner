package org.knime.base.node.preproc.binner.lucs_kdd;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "LUCS-KDD DN" Node.
 * LUCS-KDD DN (Discretisation/ Normalisation) 
 *
 * @author Tobias Witt
 */
public class KDDNodeFactory 
        extends NodeFactory<KDDNodeModel> {

    @Override
    public KDDNodeModel createNodeModel() {
        return new KDDNodeModel();
    }

    @Override
    public int getNrNodeViews() {
        return 0;
    }

    @Override
    public NodeView<KDDNodeModel> createNodeView(final int viewIndex,
            final KDDNodeModel nodeModel) {
        return null;
    }

    @Override
    public boolean hasDialog() {
        return true;
    }

    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new KDDNodeDialog();
    }

}

