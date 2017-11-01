package org.knime.base.node.preproc.binner.lucs_kdd;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.AbstractCellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;


/**
 * This is the model implementation of KDD.
 * LUCS-KDD DN (Discretisation/ Normalisation) 
 *
 * @author Tobias Witt
 */
public class KDDNodeModel extends NodeModel {
    
    // the logger instance
    private static final NodeLogger logger = NodeLogger
            .getLogger(KDDNodeModel.class);
        
    /** the settings key which is used to retrieve and 
        store the settings (from the dialog or from a settings file)    
       (package visibility to be usable from the dialog). */
    
	// class attribute
	private static final SettingsModelString m_class = createClassColModel();
	protected static SettingsModelString createClassColModel(){
    	return new SettingsModelString("Class Column", "");	
    }
	
	// desired number of divisions	
	private static SettingsModelIntegerBounded m_divisions = createNumDivisionsModel();
	protected static SettingsModelIntegerBounded createNumDivisionsModel(){
		return new SettingsModelIntegerBounded("Divisions", 5, 1, Integer.MAX_VALUE);
	}
	
    // select variables    
    private static final SettingsModelColumnFilter2 m_features = createIncludedFeaturesModel();
    @SuppressWarnings("unchecked")
	protected static SettingsModelColumnFilter2 createIncludedFeaturesModel(){
    	return new SettingsModelColumnFilter2("Included features",
    			DoubleValue.class, IntValue.class);
    }
    	
   
    
    /**
     * Constructor for the node model.
     */
    protected KDDNodeModel() {
    
        // TODO one incoming port and one outgoing port is assumed
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    /* (non-Javadoc)
     * @see org.knime.core.node.NodeModel#execute(org.knime.core.node.BufferedDataTable[], org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {    	
    	
        // TODO do something here
        logger.info("Node Model Stub... this is not yet implemented !");
        
        // input data
        BufferedDataTable input = inData[0];
        
        // input table scheme
        DataTableSpec inSpec = input.getDataTableSpec();
       
        // get features for discretization
        String[] features = m_features.applyTo(inSpec).getIncludes();
        
        // create the column re-arranger for the output table
        ColumnRearranger outputTable = new ColumnRearranger(inSpec);
        
        // cell factory
        AbstractCellFactory cellFactory;
        
        System.out.println("\n\nCREATED BUCKETS");
        System.out.println("------------------------------------------------");      
        
        // loop over features
        for(String feature : features){
        	
        	// create initial buckets and fill them
        	BucketList buckets = new BucketList(feature, m_class.getStringValue(), inData[0]);      	
        	buckets.fill();                     	
        	
        	// for each bucket determine the dominant class
        	// for buckets without dominant class, the dominant class is imputed from the nearest buckets
        	buckets.determineDominantClasses();              
       
        	// form division by merging subsequent buckets with the same dominant class	
        	buckets.formDivisions();            	         
        	        	        
        	// merge divisions until number of division is smaller or equal than the user-defined maximal number
        	while(buckets.size() > m_divisions.getIntValue()){            	
        		buckets.mergeDivisions();        		     
        	}
            
        	// create cell factory
        	cellFactory = new KDDCellFactory(createOutputColumnSpec(feature), buckets);
        
        	// replace column
        	outputTable.replace(cellFactory, feature);       
        	
        	int i = 0;
        	System.out.println(feature + ":");
        	System.out.format("%8s%20s%20s\n", "Category", "Class", "Range");
        	for(Bucket b : buckets.getBuckets()){        		
        		System.out.format("%8s%20s%20s\n", i, b.getDominantClass(), b.getRange().toString());        
        		i++;
        	}
        	System.out.println("------------------------------------------------");        	
        }
                
        // create the output table
        BufferedDataTable bufferedOutput = exec.createColumnRearrangeTable(input, outputTable, exec); 
        
        return new BufferedDataTable[]{bufferedOutput};	
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO Code executed on reset.
        // Models build during execute are cleared here.
        // Also data handled in load/saveInternals will be erased here.
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unused")
	@Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        
        // TODO: check if user settings are available, fit to the incoming
        // table structure, and the incoming types are feasible for the node
        // to execute. If the node can execute in its current state return
        // the spec of its output data table(s) (if you can, otherwise an array
        // with null elements), or throw an exception with a useful user message
    	DataTableSpec inputSpec = inSpecs[0];
    	
    	 // check if input table contains nominal class attribute
    	DataColumnSpec columnSpec = inputSpec.getColumnSpec(m_class.getStringValue());
    	if (columnSpec == null || !columnSpec.getType().isCompatible(NominalValue.class)) {
    		// if no useful column is selected guess one
    		// get the first useful one starting at the end of the table
    		for (int i = inputSpec.getNumColumns() - 1; i >= 0; i--) {   			
    			if (inputSpec.getColumnSpec(i).getType().isCompatible(NominalValue.class)) {
    				m_class.setStringValue(inputSpec.getColumnSpec(i).getName());
    				break;
    			}
    		throw new InvalidSettingsException("Table contains no nominal class attribute.");
    		}
    	}
        return new DataTableSpec[] {null};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {

        // TODO save user settings to the config object.
        
        m_divisions.saveSettingsTo(settings);	
        m_class.saveSettingsTo(settings);	
        m_features.saveSettingsTo(settings);	

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        // TODO load (valid) settings from the config object.
        // It can be safely assumed that the settings are valided by the 
        // method below.
        
        m_divisions.loadSettingsFrom(settings);
        m_class.loadSettingsFrom(settings);
        m_features.loadSettingsFrom(settings);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
            
        // TODO check if the settings could be applied to our model
        // e.g. if the count is in a certain range (which is ensured by the
        // SettingsModel).
        // Do not actually set any values of any member variables.
    	
        m_divisions.validateSettings(settings);
        m_class.validateSettings(settings);
        m_features.validateSettings(settings);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        
        // TODO load internal data. 
        // Everything handed to output ports is loaded automatically (data
        // returned by the execute method, models loaded in loadModelContent,
        // and user settings set through loadSettingsFrom - is all taken care 
        // of). Load here only the other internals that need to be restored
        // (e.g. data used by the views).

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
       
        // TODO save internal models. 
        // Everything written to output ports is saved automatically (data
        // returned by the execute method, models saved in the saveModelContent,
        // and user settings saved through saveSettingsTo - is all taken care 
        // of). Save here only the other internals that need to be preserved
        // (e.g. data used by the views).

    }
    
     private DataColumnSpec createOutputColumnSpec(String feature) {
    	    	
    	// creator for the discretized feature
    	DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(feature, StringCell.TYPE);
    	
    	// create the specification for the new column
    	DataColumnSpec newColumnSpec = colSpecCreator.createSpec();
    	    	 
    	return newColumnSpec;
    }

}

