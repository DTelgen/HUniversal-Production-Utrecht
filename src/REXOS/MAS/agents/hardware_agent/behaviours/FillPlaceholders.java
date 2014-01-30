/**
 * @file src/REXOS/MAS/agents/hardware_agent/behaviours/FillPlaceholders.java
 * @brief
 * @date Created: 12-04-13
 * 
 * @author Thierry Gerritse
 * @author Hessel Meulenbeld
 * @author Wouter Veen
 * 
 * @section LICENSE
 *          License: newBSD
 * 
 *          Copyright � 2013, HU University of Applied Sciences Utrecht.
 *          All rights reserved.
 * 
 *          Redistribution and use in source and binary forms, with or without
 *          modification, are permitted provided that the following conditions
 *          are met:
 *          - Redistributions of source code must retain the above copyright
 *          notice, this list of conditions and the following disclaimer.
 *          - Redistributions in binary form must reproduce the above copyright
 *          notice, this list of conditions and the following disclaimer in the
 *          documentation and/or other materials provided with the distribution.
 *          - Neither the name of the HU University of Applied Sciences Utrecht
 *          nor the names of its contributors may be used to endorse or promote
 *          products derived from this software without specific prior written
 *          permission.
 * 
 *          THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *          "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *          LIMITED TO,
 *          THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 *          PARTICULAR PURPOSE
 *          ARE DISCLAIMED. IN NO EVENT SHALL THE HU UNIVERSITY OF APPLIED
 *          SCIENCES UTRECHT
 *          BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 *          OR
 *          CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *          SUBSTITUTE
 *          GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *          INTERRUPTION)
 *          HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 *          STRICT
 *          LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *          ANY WAY OUT
 *          OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *          SUCH DAMAGE.
 **/
package agents.hardware_agent.behaviours;

import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

import java.util.List;

import libraries.blackboard_client.BlackboardClient;
import libraries.blackboard_client.data_classes.GeneralMongoException;
import libraries.blackboard_client.data_classes.InvalidDBNamespaceException;
import libraries.utillities.log.LogLevel;
import libraries.utillities.log.Logger;

import org.bson.types.ObjectId;

import agents.hardware_agent.EquipletStep;
import agents.hardware_agent.HardwareAgent;
import agents.hardware_agent.Module;
import agents.hardware_agent.ModuleFactory;
import agents.service_agent.ServiceStep;
import agents.shared_behaviours.ReceiveBehaviour;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * Class for the receivebehaviour receiving messages with the ontology FillPlaceholders.
 */
public class FillPlaceholders extends ReceiveBehaviour {
	/**
	 * @var long serialVersionUID
	 *      The serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @var MessageTemplate MESSAGE_TEMPLATE
	 *      The messageTemplate to match the messages to.
	 */
	private static MessageTemplate MESSAGE_TEMPLATE = MessageTemplate.MatchOntology("FillPlaceholders");

	/**
	 * @var HardwareAgent hardwareAgent
	 *      The hardwareAgent for this behaviour.
	 */
	private HardwareAgent hardwareAgent;
	/**
	 * @var ModuleFactory moduleFactory
	 *      The moduleFactory
	 */
	private ModuleFactory moduleFactory;

	/**
	 * Instantiates a new schedule step.
	 * 
	 * @param a the agent
	 * @param moduleFactory the moduleFactory
	 */
	public FillPlaceholders(HardwareAgent hardwareAgent, ModuleFactory moduleFactory) {
		super(hardwareAgent, MESSAGE_TEMPLATE);
		this.hardwareAgent = hardwareAgent;
		this.moduleFactory = moduleFactory;
	}

	/**
	 * @see ReceiveBehaviour#handle(ACLMessage)
	 */
	@Override
	public void handle(ACLMessage message) {
		try {
			ObjectId serviceStepId = (ObjectId) message.getContentObject();
			Logger.log(LogLevel.DEBUG, "%s received message from %s%n", myAgent.getLocalName(), message.getSender().getLocalName(),
					message.getOntology());
			FillStepPlaceholders(serviceStepId);
		} catch(UnreadableException e) {
			Logger.log(LogLevel.ERROR, "", e);
			myAgent.doDelete();
		}
	}

	/**
	 * Function for filling the placeholders of the equipletSteps for an serviceStepId
	 * 
	 * @param serviceStepId The serviceStepId to fill the equipletsSteps for.
	 */
	public void FillStepPlaceholders(ObjectId serviceStepId) 
	{
		try 
		{
			// Get the serviceStep
			ServiceStep serviceStep = new ServiceStep((BasicDBObject) 
					hardwareAgent.getServiceStepsBBClient().findDocumentById(serviceStepId));
			BlackboardClient equipletStepBBClient = hardwareAgent.getEquipletStepsBBClient();
			BasicDBObject query = new BasicDBObject("serviceStepID", serviceStep.getId());
			// Get the equipletSteps
			List<DBObject> steps = equipletStepBBClient.findDocuments(query);
			EquipletStep[] equipletSteps = new EquipletStep[steps.size()];
			
			for(int i = 0; i < steps.size(); i++) 
			{
				equipletSteps[i] = new EquipletStep((BasicDBObject) steps.get(i));
			}

			// Get the leadingModule
			int leadingModule = hardwareAgent.getLeadingModule(serviceStep.getServiceId());
			Module module = moduleFactory.getModuleById(leadingModule);
			module.setConfiguration(hardwareAgent.getConfiguration());

			// Fill the placeholders
			equipletSteps = module.fillPlaceHolders(equipletSteps, serviceStep.getParameters());
			Logger.log(LogLevel.DEBUG, "Saving updated instructionData of %d equipletSteps%n", equipletSteps.length);
			
			for(EquipletStep step : equipletSteps)
			{
				equipletStepBBClient.updateDocumentsUnsafe(new BasicDBObject("_id", step.getId()), new BasicDBObject("$set",
						new BasicDBObject("instructionData", step.getInstructionData().toBasicDBObject())));
			}
			
			// if the serviceStep has a nextStep fill the placeholders for that one to.
			if(serviceStep.getNextServiceStep() != null) 
			{
				FillStepPlaceholders(serviceStep.getNextServiceStep());
			}
		}
		catch(InvalidDBNamespaceException | GeneralMongoException e) 
		{
			Logger.log(LogLevel.ERROR, "", e);
			hardwareAgent.doDelete();
		}
	}
}
