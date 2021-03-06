/**
 * @file src/REXOS/MAS/agents/equiplet_agent/behaviours/InitialisationFinished.java
 * @brief Behaviour for handling the messages with the ontology
 *        InitialisationFinished
 * @date Created: 2013-04-02
 * 
 * @author Hessel Meulenbeld
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
package agents.equiplet_agent.behaviours;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import libraries.blackboard_client.data_classes.BlackboardSubscriber;
import libraries.blackboard_client.data_classes.FieldUpdateSubscription;
import libraries.blackboard_client.data_classes.GeneralMongoException;
import libraries.blackboard_client.data_classes.InvalidDBNamespaceException;
import libraries.blackboard_client.data_classes.MongoOperation;
import libraries.blackboard_client.data_classes.OplogEntry;
import libraries.blackboard_client.data_classes.FieldUpdateSubscription.MongoUpdateLogOperation;
import libraries.utillities.log.LogLevel;
import libraries.utillities.log.Logger;
import agents.data_classes.EquipletState;
import agents.data_classes.EquipletStateEntry;
import agents.equiplet_agent.EquipletAgent;
import agents.equiplet_agent.EquipletDirectoryEntry;
import agents.shared_behaviours.ReceiveOnceBehaviour;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

/**
 * A receive once behaviour for receiving messages with ontology: "InitialisationFinished".
 * When the message is received the equiplet agent of this behaviour posts itself on the EquipletDirectory to advertise
 * itself for the product agents.
 */
public class InitialisationFinished extends ReceiveOnceBehaviour implements BlackboardSubscriber {
	/**
	 * @var static final long serialVersionUID
	 *      The serial version UID for this class
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @var MessageTemplate MESSAGE_TEMPLATE
	 *      The messageTemplate this behaviour listens to.
	 *		This behaviour listens to the ontology: InitialisationFinished.
	 */
	private static MessageTemplate MESSAGE_TEMPLATE = MessageTemplate.MatchOntology("InitialisationFinished");

	/**
	 * @var EquipletAgent equipletAgent
	 *      The equipletAgent related to this behaviour.
	 */
	private EquipletAgent equipletAgent;

	private FieldUpdateSubscription stateUpdateSubscription;

	/**
	 * Instantiates a new can perform step.
	 * 
	 * @param equipletAgent The agent for this behaviour
	 */
	public InitialisationFinished(EquipletAgent equipletAgent) {
		super(equipletAgent, 3000, MESSAGE_TEMPLATE);
		this.equipletAgent = equipletAgent;
		stateUpdateSubscription = new FieldUpdateSubscription("state", this);
		stateUpdateSubscription.addOperation(MongoUpdateLogOperation.SET);
	}

	/**
	 * Function to handle the incoming messages for this behaviour. Handles the response to the InitialisationFinished.
	 * 
	 * @param message The received message.
	 */
	@Override
	public void handle(ACLMessage message) {
		if(message != null) {

			try {
				EquipletStateEntry equipletState = equipletAgent.getEquipletStateEntry();
				if(equipletState != null && equipletState.getEquipletState() != EquipletState.STANDBY && equipletState.getEquipletState() != EquipletState.NORMAL) {
					equipletAgent.setDesiredEquipletState(EquipletState.STANDBY);
					equipletAgent.getStateBBClient().subscribe(stateUpdateSubscription);
				} else {
					// inserts himself on the collective blackboard equiplet directory.
					EquipletDirectoryEntry directoryEntry =
							new EquipletDirectoryEntry(equipletAgent.getAID(), equipletAgent.getCapabilities(),
									equipletAgent.getDbData());
					equipletAgent.getCollectiveBBClient().insertDocument(directoryEntry.toBasicDBObject());
				}
			} catch(InvalidDBNamespaceException | GeneralMongoException e) {
				Logger.log(LogLevel.ERROR, "", e);
				equipletAgent.doDelete();
			}

			// starts the behaviour for receiving messages with the Ontology CanPerformStep.
			equipletAgent.addBehaviour(new CanPerformStep(equipletAgent, equipletAgent.getProductStepBBClient()));

			// starts the behaviour for receiving messages with the Ontology ProductionDuration.
			equipletAgent.addBehaviour(new ProductionDuration(equipletAgent));

			// starts the behaviour for receiving messages with the Ontology ScheduleStep.
			equipletAgent.addBehaviour(new ScheduleStep(equipletAgent, equipletAgent.getProductStepBBClient()));

			// starts the behaviour for receiving messages with the Ontology StartStep.
			equipletAgent.addBehaviour(new StartStep(equipletAgent));
			
		} else {
			Logger.log(LogLevel.ERROR, "timeout!");
			equipletAgent.doDelete();
		}
	}

	@Override
	public void onMessage(MongoOperation operation, OplogEntry entry) {
		try {
			// inserts himself on the collective blackboard equiplet directory.
			DBObject dbObject = equipletAgent.getStateBBClient().findDocumentById(entry.getTargetObjectId());
			EquipletStateEntry state = new EquipletStateEntry((BasicDBObject) dbObject);
			switch(state.getEquipletState()) {
				case STANDBY:
					Logger.log(LogLevel.DEBUG, "EquipletState changed to %s%n", state.getEquipletState().name());

					EquipletDirectoryEntry directoryEntry =
							new EquipletDirectoryEntry(equipletAgent.getAID(), equipletAgent.getCapabilities(),
									equipletAgent.getDbData());

					equipletAgent.getCollectiveBBClient().insertDocument(directoryEntry.toBasicDBObject());
					equipletAgent.getStateBBClient().unsubscribe(stateUpdateSubscription);
					break;
				default:
					Logger.log(LogLevel.DEBUG, "EquipletState changed to %s%n", state.getEquipletState().name());
					break;
			}
		} catch(InvalidDBNamespaceException | GeneralMongoException e) {
			Logger.log(LogLevel.ERROR, "", e);
			// Cannot add myself on the collective BB, so remove the agent since it cannot be found by product agents 
			equipletAgent.doDelete();
		}
	}
}
