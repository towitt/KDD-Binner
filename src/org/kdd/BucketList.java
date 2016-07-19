package org.kdd;

import java.util.ArrayList;


import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.data.DoubleValue;

import com.google.common.collect.Range;

public class BucketList {
	
	public ArrayList<Bucket> buckets;
	private DataCell[] dominantClasses;		
	public String feature;
	public String classAttr;
	public BufferedDataTable inData;
		
	// constructor
	public BucketList(String feature, String classAttr, BufferedDataTable inData){
		this.buckets = new ArrayList<Bucket>();
		this.feature = feature;
		this.classAttr = classAttr;
		this.inData = inData;
	}
		
	/**
	 * Divide the range of the attribute into N discrete sub-ranges (buckets). 
	 * For integers with a range less than 100, N is equivalent to the range. In all other cases N is 
	 * equivalent to 100. Afterwards, the buckets are "filled". However, no instances are actually added to
	 * the buckets, but for each bucket only the total number of elements and the distribution of
	 * class values within the bucket are updated.  
	 */
	protected void fill(){
		
		// get smallest and largest value of the feature	
		DataTableSpec inSpec = inData.getDataTableSpec();	
		DataColumnDomain attributeRange = inSpec.getColumnSpec(getColIndex(this.feature)).getDomain();
		double low = ((DoubleValue) attributeRange.getLowerBound()).getDoubleValue();
		double upp = ((DoubleValue) attributeRange.getUpperBound()).getDoubleValue();				
						
		// get data type of feature (integer or double)
		String type = inSpec.getColumnSpec(getColIndex(this.feature)).getType().getCellClass().getSimpleName();	
			
		// determine N 
		int N = 100;
		switch(type) {		
			
			// for integers determine the range and use the range as N if it's smaller than 100
			case "IntCell":  
				int r = (int) Math.ceil(upp - low);
				if(r < 100) N = r;
				else N = 100;	
				break;
				
			// for doubles, always set to 100
			case "DoubleCell":
				N = 100;
				break;
		}					
		
		// calculate size of each sub-interval
		double interval = (upp - low)/N;				
				
		// add empty buckets defined by lower and upper bound		
		Range<Double> range;
		double lower = round(low);		
		for(int i = 0; i < N; i++) {
									
			// first bucket is open infinite interval (-infinity, a]
			if(i == 0) range = Range.atMost(round(lower + interval));
			
			// last bucket is an open infinite interval (a, +infinity)
			else if(i == (N - 1)) range = Range.greaterThan(round(lower));
			
			// all other buckets are half-open intervals (a, b]
			else range = Range.openClosed(round(lower), round(lower + interval));									
			
			// create bucket 
			this.buckets.add(new Bucket(range));
			lower += interval;
		}
		
		// handle zero-variance attributes 
		if(N == 0){
			range = Range.all();
			this.buckets.add(new Bucket(range));
		}
		
		// loop over instances	
		for(DataRow r: this.inData){		
			
			// get class value (ignore if missing)
			DataCell c = r.getCell(getColIndex(this.classAttr));			
			if(c.isMissing()) continue;				
			
			
			// get double value of feature (ignore if missing)
			DataCell val = r.getCell(getColIndex(this.feature));
			if(val.isMissing()) continue;
			double x = ((DoubleValue) val).getDoubleValue();
			
			// map in interval [0, 1]
			double z = (x - low)/(upp - low);
						
			// choose the correct bucket
			int b;
			if(z <= 0) b = 0;
			else b = (int) Math.ceil(((N * z) - 1));							
			
			// put in bucket
			this.buckets.get(b).add(c);			
		}		
	}

	
	/**
	 * For each division identify the dominant class. Where there is no dominant class, because no 
	 * records fall into a particular sub-range or where two or more counts are equivalent, 
	 * impute the dominant class with the class of the nearest neighboring bucket.
	 */
	public void determineDominantClasses() {
		
		int lastDominantIndex=-1;
		int i;
		boolean atStart=true;

		// Loop through buckets
		for(i = 0; i < this.buckets.size(); i++) {
												
			// Test if column has a dominant class (it may not) 
			// if so, set the last element to the dominant class value.
			if(this.buckets.get(i).hasDominantClass()){
				
				// determine dominant class in bucket						
				this.buckets.get(i).determineDominantClass();
				
				// If previous column (index-1) without a dominant class
				if (lastDominantIndex != (i-1)) {
						
					// If at start, assign current dominant class to all foregoing columns
					if (atStart) {
						for (int j = 0; j < i; j++){
							this.buckets.get(j).setDominantClass(this.buckets.get(i).getDominantClass());							
						}			  
					}
					
					// Otherwise assign last dominant class to columns nearest to last column with a dominant 
					// class, and assign current class to columns nearest to current column
					else {
						
                        int newDominantIndex=i;                       
                        DataCell newClass = this.buckets.get(newDominantIndex).getDominantClass();                    
                        DataCell oldClass = this.buckets.get(lastDominantIndex).getDominantClass();
                                              
                        lastDominantIndex++;
                        newDominantIndex--;
                        while (lastDominantIndex <= newDominantIndex) {                        	
                        	this.buckets.get(lastDominantIndex).setDominantClass(oldClass);                        	
                        	this.buckets.get(newDominantIndex).setDominantClass(newClass);
                        	lastDominantIndex++;
                        	newDominantIndex--;
                        }
					}
				}
				atStart=false;
				lastDominantIndex=i;
			}
			
		// Otherwise no dominant class --> continue
			
		}
					
		// If count array contains no dominant classes at all, set entire array to arbitrary class		
		if (lastDominantIndex == -1) {				
			DataTableSpec inSpec = this.inData.getDataTableSpec();	
			DataColumnDomain classDomain = inSpec.getColumnSpec(getColIndex(this.classAttr)).getDomain();
			DataCell classVal = (DataCell) classDomain.getValues().toArray()[0];		
			for (i = 0; i < this.buckets.size(); i++){				
				this.buckets.get(i).setDominantClass(classVal);
			}
			
	    }
		
		// If last dominant column is not at end set dominant class for remaining columns to current class
        else if (lastDominantIndex != (i-1)) {	           	
        	for (int j = lastDominantIndex; j < (i - 0); j++){ // i - 1?        	
        		this.buckets.get(j).setDominantClass(this.buckets.get(lastDominantIndex).getDominantClass());
        	}        			                                       
 	    }		
		
	}
	
	
	/**
	 * Merge successive buckets with identical dominant classes to form a set of divisions.
	 */
	public void formDivisions(){
		
		int i = 0; int j;				
		while(i < this.buckets.size()){							
								
			// get dominant class of bucket
			DataCell dclass = this.buckets.get(i).getDominantClass();	
			
			// while dominant class of neighboring buckets is equal, merge buckets		
			j = i + 1;
			while(j < this.buckets.size() && this.buckets.get(j).getDominantClass().equals(dclass)){				
				this.buckets.get(i).merge(this.buckets.get(j));
				this.buckets.remove(j);				
			}
			
			// move on to next division
			i++;						
		}		
	}
	
	
	/**
	 * Merge two divisions 
	 * A division is selected for merging as follows:
	 * (1) For each possible merge calculate the combined probability for the resulting dominant class
	 * (2) Select the merge with the highest probability
	 */
	public void mergeDivisions(){
		
		// search for the merge of two buckets for which the probability of the dominant class is maximal
        int[] bestMerge = this.selectBestMerge();                
        	
        // merge both buckets and remove one from the list
        this.buckets.get(bestMerge[0]).merge(this.buckets.get(bestMerge[1]));
        this.buckets.remove(bestMerge[1]);        	
        	
        // check if dominant class for merged bucket is the same as for the previous and next bucket 
        // if yes --> merge
        int current = bestMerge[0];
        int prev = current - 1;
        int next = current + 1;       
        
        // check for next division
        if(next < this.buckets.size() && this.buckets.get(next).getDominantClass().toString().compareTo(this.buckets.get(current).getDominantClass().toString()) == 0){
        	this.buckets.get(current).merge(this.buckets.get(next));
        	this.buckets.remove(next);        	
        }
        
        // check for previous division        
        if(prev >= 0 && this.buckets.get(prev).getDominantClass().toString().compareTo(this.buckets.get(current).getDominantClass().toString()) == 0){
        	this.buckets.get(prev).merge(this.buckets.get(current));
        	this.buckets.remove(current);                	       	
        }	        	
	}
	
	
	/**
	 * Select the optimal merge, i.e., the merge for which the combined probability for the 
	 * resulting dominant class is maximal
	 * @return the positions of the buckets involved in the optimal merge
	 */
	public int[] selectBestMerge(){
		
		// saves best possible merge of buckets (i, j)
		// if all possible merges produce buckets with no dominant class (prob = 0), 
		// but the number of buckets should be further reduced, then merge the first and the second
		int[] bestMerge = {0, 1};
		
		// number of buckets in the list
		int n = this.buckets.size();
		
		// search for merge that produces the highest probability of the resulting dominant class
		double maxProb = 0;				
		for(int i = 0; i < (n - 1); i++){		
			int j = i + 1;	
			double prob = getDominantClassProb(this.buckets.get(i), this.buckets.get(j));					
			if(prob > maxProb){
				bestMerge[0] = i;
				bestMerge[1] = j;		
				maxProb = prob;
			}
		}
		return bestMerge;
	}
	
	
	/**
	 * Calculate the combined probability/proportion of the resulting dominant class after merging 
	 * the two buckets
	 * @param b1
	 * @param b2
	 * @return proportion of the dominant class in the merged bucket
	 */
	public double getDominantClassProb(Bucket b1, Bucket b2){
		
		// create a new bucket		
		Bucket bucket = new Bucket(b1.getRange());
		
		// merge new bucket with b1 and b2
		bucket.merge(b1);
		bucket.merge(b2);			
		
		// return probability of the dominant class
		return bucket.determineDominantClassProb();
	}
	
	
	/**
	 * Return the position of the bucket that contains the input double value. 
	 * The position is returned as a string of the form "Interval_bucketNumber"
	 * @param value
	 * @return 
	 */
	public String getCategory(double value){
		
        int category = -1;
        int i = 0;
        while(i < this.buckets.size() && category < 0){        
        	if(this.buckets.get(i).contains(value)){
        		category = i;
        	}
        	i++;
        }
        return ("Interval_" + category);       
	}

	
	public int getColIndex(String attribute){		
		return this.inData.getDataTableSpec().findColumnIndex(attribute);		
	}	
	
	public int size(){
		return this.buckets.size();
	}
		
	public ArrayList<Bucket> getBuckets(){
		return this.buckets;
	}
			
	public DataCell[] getDominantClasses(){
		return this.dominantClasses;
	}
	
	public double round(double a){
		return Math.round(a * 100.0)/100.0;
	}
		
}
