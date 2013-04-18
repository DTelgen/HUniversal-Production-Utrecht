/**
 *
 * Project: product-agents
 *
 * Package: newDataClasses
 *
 * File: ProductionStep.java
 *
 * Author: Mike Schaap
 *
 * Version: 1.0
 *
 */
package newDataClasses;

import java.io.Serializable;

public class ProductionStep implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -832835122145455883L;

	private int _requiredTimeSlots;
	private int _id;
	private long _capability;
	private ProductionStepStatus _status;
	
	private ParameterList _parameters;

	public ProductionStep() {
		this._parameters = new ParameterList();
		this._status = ProductionStepStatus.STATE_TODO;
	}
	
	public ProductionStep(int id, long capability,  ParameterList parameterList) {
		this._id = id;
		this._capability = capability;
		this._parameters = parameterList;
		this._status = ProductionStepStatus.STATE_TODO;
	}
	
	public int getId(){
		return this._id;
	}
	
	public ProductionStepStatus getStatus(){
		return this._status;
	}
	
	public void setStatus(ProductionStepStatus status){
		this._status = status;
	}
	
	public void setRequiredTimeSlots(int timeSlots) {
		this._requiredTimeSlots = timeSlots;
	}
	
	public int getRequiredTimeSlots() {
		return this._requiredTimeSlots;
	}
	
	public ParameterList getParameterList() {
		return _parameters;
	}

	public long getCapability() {
		return _capability;
	}
	
}
