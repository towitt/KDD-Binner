package org.kdd;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "KDD" Node.
 * LUCS-KDD DN (Discretisation/ Normalisation) 
 *
 * @author Tobias Witt
 */
public class KDDNodeFactory 
        extends NodeFactory<KDDNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public KDDNodeModel createNodeModel() {
        return new KDDNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<KDDNodeModel> createNodeView(final int viewIndex,
            final KDDNodeModel nodeModel) {
        return new KDDNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new KDDNodeDialog();
    }

}

