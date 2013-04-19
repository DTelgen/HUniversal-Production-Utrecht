/**
 * @file ProductStepMessage.java
 * @brief Provides a message for the productstep blackboard
 * @date Created: 2013-04-03
 *
 * @author Hessel Meulenbeld
 *
 * @section LICENSE
 * License: newBSD
 *
 * Copyright © 2012, HU University of Applied Sciences Utrecht.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of the HU University of Applied Sciences Utrecht nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE HU UNIVERSITY OF APPLIED SCIENCES UTRECHT
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 **/
package equipletAgent;

import java.io.Serializable;
import com.mongodb.BasicDBObject;
import jade.core.AID;
import newDataClasses.ScheduleData;

/**
 * Implementation of a message for the productstep blackboard
 */
public class ProductStepMessage implements Serializable{
	/**
	 * @var static final long serialVersionUID
	 * The serial version uid for this class.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * @var AID productAgentId
	 * The AID of the productAgent linked to this product step.
	 */
	private AID productAgentId;
	
	/**
	 * @var long type
	 * The type of the product step
	 */
	private long type;
	
	/**
	 * @var ParameterList parameters
	 * The parameterlist for this product step.
	 */
	private BasicDBObject parameters;
	
	/**
	 * @var Object inputParts
	 * The input parts needed for this product step.
	 */
	private long[] inputParts;
	
	/**
	 * @var long outputPart
	 * The result parts for this product step.
	 */
	private long outputPart;
	
	/**
	 * @var StepStatusCode status
	 * The status for this product step.
	 */
	private StepStatusCode status;
	
	/**
	 * @var basicDBObject statusData
	 * The extra data provided by the status for this product step.
	 */
	private BasicDBObject statusData;
	
	/**
	 * @var ScheduleData scheduleData
	 * The schedule for this product step.
	 */
	private ScheduleData scheduleData;

	/**
	 * The constructor for the product step entry.
	 * 
	 * @param productAgentId - AID of the product agent linked to the product step 
	 * @param type - The type of the product step
	 * @param parameters - The parameters for the product step
	 * @param inputParts - The input parts for the product step
	 * @param outputPart - The output parts for the product step
	 * @param status - The status for the product step
	 * @param statusData - The additional data for the status
	 * @param scheduleData - The schedule data
	 */
	public ProductStepMessage(AID productAgentId, long type,
			BasicDBObject parameters, long[] inputParts, long outputPart,
			StepStatusCode status, BasicDBObject statusData, ScheduleData scheduleData) {
		this.productAgentId = productAgentId;
		this.type = type;
		this.parameters = parameters;
		this.inputParts = inputParts;
		this.outputPart = outputPart;
		this.status = status;
		this.statusData = statusData;
		this.scheduleData = scheduleData;
	}

	/**
	 * Function to check if this productstep equals to another object.
	 * 
	 * @param obj - The object to compare with
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj == this)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProductStepMessage other = (ProductStepMessage) obj;
		return productAgentId.equals(other.productAgentId)
				&& type == other.type
				&& parameters.equals(other.parameters)
				&& inputParts.equals(other.inputParts)
				&& outputPart == other.outputPart
				&& status == other.status
				&& statusData.equals(other.statusData)
				&& scheduleData.equals(other.scheduleData);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}

	/**
	 * @return the productAgentId
	 */
	public AID getProductAgentId() {
		return productAgentId;
	}

	/**
	 * @param productAgentId the productAgentId to set
	 */
	public void setProductAgentId(AID productAgentId) {
		this.productAgentId = productAgentId;
	}

	/**
	 * @return the type
	 */
	public long getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	public void setType(long type) {
		this.type = type;
	}

	/**
	 * @return the parameters
	 */
	public BasicDBObject getParameters() {
		return parameters;
	}

	/**
	 * @param parameters the parameters to set
	 */
	public void setParameters(BasicDBObject parameters) {
		this.parameters = parameters;
	}

	/**
	 * @return the inputParts
	 */
	public long[] getInputParts() {
		return inputParts;
	}

	/**
	 * @param inputParts the inputParts to set
	 */
	public void setInputParts(long[] inputParts) {
		this.inputParts = inputParts;
	}

	/**
	 * @return the outputPart
	 */
	public long getOutputPart() {
		return outputPart;
	}

	/**
	 * @param outputPart the outputPart to set
	 */
	public void setOutputParts(long outputPart) {
		this.outputPart = outputPart;
	}

	/**
	 * @return the status
	 */
	public StepStatusCode getStatus() {
		return status;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(StepStatusCode status) {
		this.status = status;
	}

	/**
	 * @return the statusData
	 */
	public BasicDBObject getStatusData() {
		return statusData;
	}

	/**
	 * @param statusData the statusData to set
	 */
	public void setStatusData(BasicDBObject statusData) {
		this.statusData = statusData;
	}

	/**
	 * @return the scheduleData
	 */
	public ScheduleData getScheduleData() {
		return scheduleData;
	}

	/**
	 * @param scheduleData the scheduleData to set
	 */
	public void setScheduleData(ScheduleData scheduleData) {
		this.scheduleData = scheduleData;
	}
}