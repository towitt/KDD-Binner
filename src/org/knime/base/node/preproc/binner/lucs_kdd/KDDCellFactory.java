package org.knime.base.node.preproc.binner.lucs_kdd;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;

/**
 * @author Tobias Witt, University of Konstanz
 *
 */
public class KDDCellFactory extends SingleCellFactory {
	
	private BucketList buckets;
	
	/**
	 * @param colSpec
	 * @param buckets
	 */
	public KDDCellFactory(DataColumnSpec colSpec, BucketList buckets) {
		super(colSpec);
		this.buckets = buckets;		
	}
	
	@Override
	public DataCell getCell(DataRow row) {	
		
		// position of feature
		int colIndex = this.buckets.getColIndex(this.buckets.getFeature());		
		
		DataCell category;
		
		// get value of feature
		DataCell val = row.getCell(colIndex);
		
		// check if value is missing
		if(val.isMissing()){
			category = new MissingCell(null);
		}
		
		else{					
			// obtain category
			double value = ((DoubleValue) row.getCell(colIndex)).getDoubleValue();	
			category = new StringCell(this.buckets.getCategory(value));		
		}	
		return category;		
	}
}

