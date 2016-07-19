package org.kdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataCell;

import com.google.common.collect.Range;


public class Bucket {

	// number of elements in bucket
	private int size; 		
	
	// range of bucket
	private Range<Double> range; 
	
	// frequency table for class variable in bucket
	private Map<DataCell, Integer> classFreqTable; 
	
	// dominant class in bucket
	private DataCell dominantClass;
	
	// constructor
	public Bucket(Range<Double> range){
				
		this.range = range;
		this.size = 0;
		
		// initialize class frequency table
		this.classFreqTable = new HashMap<DataCell, Integer> () {					
			private static final long serialVersionUID = 1L;
			@Override			
			public Integer get(Object key) {
				return containsKey(key) ? super.get(key) : 0;
			}
		};
	}
	
	
	/**
	 * Add an instance (row identifier and class value) to the bucket
	 * @param key
	 * @param classVal
	 */
	protected void add(DataCell classVal){
		
		// update entry counter
		this.size++;
		
		// update class frequency table
		this.classFreqTable.put(classVal, this.classFreqTable.get(classVal) + 1);			
	}
		
	
	/**
	 * Return the dominant class in the bucket (if it exists)
	 * @return
	 */
	protected void determineDominantClass(){
		
		// return null if bucket has no dominant class
		if(!this.hasDominantClass()) this.dominantClass = null;					
		
		// get class with maximum frequency
		int max = 0;
		DataCell dominantClass = null;		
		for(Entry<DataCell, Integer> entry : this.classFreqTable.entrySet()){
			if(entry.getValue() > max){
				max = entry.getValue();
				dominantClass = entry.getKey();        		
        	}
		}		
        this.dominantClass = dominantClass;
	}
		
	
	/**
	 * Check if bucket has a dominant class
	 * @param bucket
	 * @return
	 */
	protected boolean hasDominantClass(){
		
		// if no observation in bucket return false
		if(this.size == 0) return false;
		
		// if only observations of one class are in the bucket return true
		if(this.classFreqTable.size() == 1) return true;
		
		// else, check if last and next-to-last element in sorted frequency array are equal
		else{
		ArrayList<Integer> values = new ArrayList<Integer>(this.classFreqTable.values());		
		Collections.sort(values);				
		if(values.get(values.size() - 1) == values.get(values.size() - 2)){
			return false;
		}	
		}
		return true;				
	}
	
	
	/**
	 * Merge bucket with another bucket
	 * @param bucket
	 */
	protected void merge(Bucket bucket){
		
		// add all entries of other bucket		
		this.size = this.size + bucket.getSize();
		
		// update frequency table
		bucket.getClassFreqTable().forEach((k, v) -> this.classFreqTable.merge(k, v, (v1, v2) -> v1 + v2));
				
		// update bucket borders
		this.setRange(this.range.span(bucket.getRange()));
	}
	
	
	/**
	 * Calculate the probability of the dominant class in the bucket (if existent)
	 * @return
	 */
	protected double determineDominantClassProb(){
		
		// return zero if no dominant class exists
		if(!this.hasDominantClass()) return 0;
		
		// calculate proportion of dominant class in the bucket
		if(this.dominantClass == null) this.determineDominantClass();
		double ndom = (double) this.getClassFreqTable().get(this.dominantClass);		
		double n = (double) this.getSize();
		return ndom/n;
	}	
	
	
	/**
	 * Check if instance is in bucket
	 * @param key
	 * @return
	 */
	protected boolean contains(double value){
		if(this.range.contains(value)){
			return true;
		}
		return false;
	}
		
	public int getSize(){
		return this.size;
	}	
	
	public DataCell getDominantClass(){
		return this.dominantClass;
	}
	
	public void setDominantClass(DataCell dominantClass){
		this.dominantClass = dominantClass;
	}
	
	public Map<DataCell, Integer> getClassFreqTable(){
		return this.classFreqTable;
	}
	
	public Range<Double> getRange(){
		return this.range;
	}
	
	public void setRange(Range<Double> range){
		this.range = range;
	}
	
}
