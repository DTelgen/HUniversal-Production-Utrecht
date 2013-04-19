/**
 * @file StartStep.java
 * @brief Behaviour for handling the messages with the ontology StartStep
 * @date Created: 2013-04-02
 *
 * @author Hessel Meulenbeld
 *
 * @section LICENSE
 * License: newBSD
 *
 * Copyright � 2013, HU University of Applied Sciences Utrecht.
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
package equipletAgent.behaviours;


import org.bson.types.ObjectId;

import newDataClasses.ScheduleData;
import nl.hu.client.BlackboardClient;
import nl.hu.client.GeneralMongoException;
import nl.hu.client.InvalidDBNamespaceException;

import behaviours.ReceiveBehaviour;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import equipletAgent.NextProductStepTimer;
import equipletAgent.StepStatusCode;
import equipletAgent.EquipletAgent;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

/**
 * The Class StartStep.
 */
public class StartStep extends ReceiveBehaviour {
	/**
	 * @var static final long serialVersionUID
	 * The serial version UID for this class
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * @var MessageTemplate messageTemplate
	 * The messageTemplate this behaviour listens to.
	 * This behaviour listens to the ontology: StartStep.
	 */
    private static MessageTemplate messageTemplate = MessageTemplate.MatchOntology("StartStep");
    
    /**
	 * @var EquipletAgent equipletAgent
	 * The equipletAgent related to this behaviour.
	 */
    private EquipletAgent equipletAgent;
    private BlackboardClient equipletBBClient;
	
	/**
	 * Instantiates a new can perform step.
	 *
	 * @param a The agent for this behaviour
	 */
	public StartStep(Agent a, BlackboardClient equipletBBClient) {
		super(a, -1, messageTemplate);
		equipletAgent = (EquipletAgent)a;
		this.equipletBBClient = equipletBBClient;
	}
	
	/**
	 * Function to handle the incoming messages for this behaviour.
	 * Handles the response to the StartStep.
	 * 
	 * @param message - The received message.
	 */
	@Override
	public void handle(ACLMessage message){
		Object contentObject = null;
		String contentString = message.getContent();

		try {
			contentObject = message.getContentObject();
		} catch (UnreadableException e) {
			System.out.println("Exception Caught, No Content Object Given");
		}
		System.out.format("%s received message from %s (%s:%s)%n",
				myAgent.getLocalName(), message.getSender().getLocalName(), message.getOntology(), contentObject == null ? contentString : contentObject);
		
		try {
			ObjectId productStepId = equipletAgent.getRelatedObjectId(message.getConversationId());
			equipletBBClient.updateDocuments(
					new BasicDBObject("_id", productStepId), 
					new BasicDBObject("$set", new BasicDBObject("status", StepStatusCode.WAITING)));			
		} catch (InvalidDBNamespaceException | GeneralMongoException e1) {
			e1.printStackTrace();
		}
		
		NextProductStepTimer timer = equipletAgent.getTimer();
		try {
			BasicDBObject query = new BasicDBObject("status", StepStatusCode.PLANNED);
			query.put("$order_by", new BasicDBObject("scheduleData", new BasicDBObject("startTime", "-1")));
			DBObject nextProductStep = equipletBBClient.findDocuments(query).get(0);
			ScheduleData scheduleData = (ScheduleData) nextProductStep.get("scheduleData");
			if (scheduleData.getStartTime() < timer.getNextUsedTimeSlot()) {
				timer.setNextUsedTimeSlot(scheduleData.getStartTime());
			}
		} catch (Exception e) {
			timer.setNextUsedTimeSlot(-1);
		e.printStackTrace();
		}
	}
}
