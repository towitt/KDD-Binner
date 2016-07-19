package org.kdd;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.MissingCell;

public class KDDCellFactory extends SingleCellFactory {
	
	private BucketList buckets;
	
	public KDDCellFactory(DataColumnSpec newColSpec, BucketList buckets) {
		super(newColSpec);
		this.buckets = buckets;		
	}
	
	@Override
	public DataCell getCell(DataRow row) {	
		
		// position of feature
		int colIndex = this.buckets.getColIndex(this.buckets.feature);		
		
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

