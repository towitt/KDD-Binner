package org.kdd;

import org.knime.core.node.NodeView;

/**
 * <code>NodeView</code> for the "KDD" Node.
 * LUCS-KDD DN (Discretisation/ Normalisation) 
 *
 * @author Tobias Witt
 */
public class KDDNodeView extends NodeView<KDDNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link KDDNodeModel})
     */
    protected KDDNodeView(final KDDNodeModel nodeModel) {
        super(nodeModel);

        // TODO instantiate the components of the view here.

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {

        // TODO retrieve the new model from your nodemodel and 
        // update the view.
        KDDNodeModel nodeModel = 
            (KDDNodeModel)getNodeModel();
        assert nodeModel != null;
        
        // be aware of a possibly not executed nodeModel! The data you retrieve
        // from your nodemodel could be null, emtpy, or invalid in any kind.
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
    
        // TODO things to do when closing the view
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {

        // TODO things to do when opening the view
    }

}

