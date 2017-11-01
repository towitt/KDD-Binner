package org.knime.base.node.preproc.binner.lucs_kdd;

import org.knime.core.data.NominalValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;


/**
 * <code>NodeDialog</code> for the "KDD" Node.
 * LUCS-KDD DN (Discretisation/ Normalisation) 
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author Tobias Witt
 */
public class KDDNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring KDD node dialog.
     * This is just a suggestion to demonstrate possible default dialog
     * components.
     */
	@SuppressWarnings({ "unchecked"})
    protected KDDNodeDialog() {
        super();
        
        // class attribute
    	addDialogComponent(new DialogComponentColumnNameSelection(
    			KDDNodeModel.createClassColModel(), 
    			"Class Column",
    			0, true, NominalValue.class));
    	
    	// desired number of divisions
    	addDialogComponent(new DialogComponentNumber(
    			KDDNodeModel.createNumDivisionsModel(), "Max. number of divisions:", 1));
    	
    	// choose features
    	addDialogComponent(new DialogComponentColumnFilter2(
    		KDDNodeModel.createIncludedFeaturesModel(), 0));                     
    }
}

