/**
 * @file EquipletNode.cpp
 * @brief Symbolizes an entire EquipletNode.
 * @date Created: 2012-10-12
 *
 * @author Dennis Koole
 * @author Alexander Streng
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

#include <unistd.h>
#include "equiplet_node/EquipletNode.h"
#include "equiplet_node/StateBlackboard.h"
#include "rexos_blackboard_cpp_client/FieldUpdateSubscription.h"
#include "rexos_blackboard_cpp_client/BasicOperationSubscription.h"
#include "rexos_blackboard_cpp_client/OplogEntry.h"
#include "rexos_utilities/Utilities.h"


#include <libjson/libjson.h>

using namespace equiplet_node;


/**
 * Create a new EquipletNode
 * @param id The unique identifier of the Equiplet
 **/
EquipletNode::EquipletNode(int id, std::string blackboardIp) :
		equipletId(id),
		EquipletStateMachine(nameFromId(id),id),
		equipletStepBlackboardClient(NULL),
		equipletCommandBlackboardClient(NULL),
		directMoveBlackBoardClient(NULL),
		scada(this, &moduleRegistry) 
{
	ROS_DEBUG("Subscribing to EquipletStepsBlackBoard");
	equipletStepBlackboardClient = new Blackboard::BlackboardCppClient(blackboardIp, std::string("EQ") + std::to_string(id), "EquipletStepsBlackBoard");
	equipletStepSubscription = new Blackboard::FieldUpdateSubscription("status", *this);
	equipletStepSubscription->addOperation(Blackboard::SET);
	equipletStepBlackboardClient->subscribe(*equipletStepSubscription);
	subscriptions.push_back(equipletStepSubscription);
	sleep(1);

	ROS_DEBUG("Subscribing to equipletCommands");
	equipletCommandBlackboardClient = new Blackboard::BlackboardCppClient(blackboardIp, STATE_BLACKBOARD, COLLECTION_EQUIPLET_COMMANDS);
	equipletCommandSubscription = new Blackboard::BasicOperationSubscription(Blackboard::INSERT, *this);
	equipletCommandSubscriptionSet = new Blackboard::BasicOperationSubscription(Blackboard::UPDATE, *this);
	equipletCommandBlackboardClient->subscribe(*equipletCommandSubscription);
	sleep(1);
	equipletCommandBlackboardClient->subscribe(*equipletCommandSubscriptionSet);
	subscriptions.push_back(equipletCommandSubscription);
	subscriptions.push_back(equipletCommandSubscriptionSet);
	sleep(1);

	ROS_DEBUG("Subscribing to equipletState");
	equipletStateBlackboardClient = new Blackboard::BlackboardCppClient(blackboardIp, STATE_BLACKBOARD, COLLECTION_EQUIPLET_STATE);
	sleep(1);

	ROS_DEBUG("Subscribing to DirectMoveStepsBlackBoard");
	directMoveBlackBoardClient = new Blackboard::BlackboardCppClient(blackboardIp, std::string("EQ") + std::to_string(id), "DirectMoveStepsBlackBoard");
	directMoveSubscription = new Blackboard::BasicOperationSubscription(Blackboard::INSERT, *this);
	directMoveBlackBoardClient->subscribe(*directMoveSubscription);
	subscriptions.push_back(directMoveSubscription);
	sleep(1);

	
	std::cout << "Connected equiplet_node." << std::endl;
}

/**
 * Destructor for the EquipletNode
 **/
EquipletNode::~EquipletNode(){
	delete equipletStepBlackboardClient;
	delete equipletStepBlackboardClient;
	delete directMoveBlackBoardClient;
	delete equipletCommandBlackboardClient;
	delete equipletStateBlackboardClient;

	for (std::vector<Blackboard::BlackboardSubscription *>::iterator iter = subscriptions.begin() ; iter != subscriptions.end() ; iter++) {
		delete *iter;
	}

	subscriptions.clear();
}

/**
 * This function is called when a new message on the Blackboard is received,
 * The command, destination and payload are read from the message, and the 
 * service specified in the message is called
 *
 * @param json The message parsed in the json format
 **/
void EquipletNode::onMessage(Blackboard::BlackboardSubscription & subscription, const Blackboard::OplogEntry & oplogEntry) {
	mongo::OID targetObjectId;
	oplogEntry.getTargetObjectId(targetObjectId);

	if(&subscription == equipletStepSubscription || &subscription == directEquipletStepSubscription) {
		JSONNode n = libjson::parse(equipletStepBlackboardClient->findDocumentById(targetObjectId).jsonString());
	    rexos_datatypes::EquipletStep * step = new rexos_datatypes::EquipletStep(n);
	    //We only need to handle the step if its status is 'WAITING'
	    if (step->getStatus().compare("WAITING") == 0) {
	    	std::cout << "handling step: " << n.write_formatted() << std::endl;
    		handleEquipletStep(step, targetObjectId);
		}
		
	} else if(&subscription == equipletCommandSubscription || &subscription == equipletCommandSubscriptionSet) {
		ROS_INFO("Received equiplet statemachine command");
    	handleEquipletCommand(libjson::parse(oplogEntry.getUpdateDocument().jsonString()));
	} else if(&subscription == directMoveSubscription) {
		handleDirectMoveCommand(1, targetObjectId);
	}
}

void EquipletNode::handleEquipletStep(rexos_datatypes::EquipletStep * step, mongo::OID targetObjectId){
	
	rexos_statemachine::Mode currentMode = getCurrentMode();
	if (currentMode == rexos_statemachine::MODE_NORMAL) {

		rexos_statemachine::State currentState = getCurrentState();
		if (currentState == rexos_statemachine::STATE_NORMAL || currentState == rexos_statemachine::STATE_STANDBY) {
			
			rexos_datatypes::InstructionData instructionData = step->getInstructionData();

						//we need to call the lookup handler first
			if(instructionData.getLook_up().length() > 0 && instructionData.getLook_up().compare("NULL") != 0) {
				std::cout << "Calling lookuphandler" << std::endl;
				map<std::string, std::string> newPayload = callLookupHandler(instructionData.getLook_up(), instructionData.getLook_up_parameters(), instructionData.getPayload());
				instructionData.setPayload(newPayload);
			}
			
				//we might still need to update the payload on the bb
		    ModuleProxy *prox = moduleRegistry.getModule(step->getModuleId());    
			//prox->changeState(rexos_statemachine::STATE_NORMAL);
		    equipletStepBlackboardClient->updateDocumentById(targetObjectId, "{ $set : {status: \"IN_PROGRESS\" }  }");	
		    prox->setInstruction(targetObjectId.toString(), libjson::parse(instructionData.toJSONString()));
		} else {
			equipletStepBlackboardClient->updateDocumentById(targetObjectId, "{ $set : {status: \"FAILED\" } } ");
		}
	} else {
		ROS_INFO("Instruction received but current mode is %s", rexos_statemachine::mode_txt[currentMode]);
		equipletStepBlackboardClient->updateDocumentById(targetObjectId, "{$set : {status: \"FAILED\"");
	}
}

void EquipletNode::handleDirectMoveCommand(int moduleId, mongo::OID targetObjectId){
		std::cout << "Got an update! : " << directMoveBlackBoardClient->findDocumentById(targetObjectId).jsonString() << std::endl;
		ModuleProxy *prox = moduleRegistry.getModule(moduleId);
	    prox->setInstruction(targetObjectId.toString(), libjson::parse(directMoveBlackBoardClient->findDocumentById(targetObjectId).jsonString()));
		//still need to remove the step tho
}

void EquipletNode::handleEquipletCommand(JSONNode n){
	JSONNode::const_iterator i = n.begin();

    while (i != n.end()){
        const char * node_name = i -> name().c_str();
    if (strcmp(node_name, "$set") == 0) {
	JSONNode set = i->as_node();
	JSONNode::const_iterator j = set.begin();
        while ( j != set.end()) {
		const char * node_name = j -> name().c_str();
		if (strcmp(node_name, "desiredState") == 0){
			ROS_INFO("ChangeState to %s", j -> as_string().c_str());
            		changeState((rexos_statemachine::State) atoi(j -> as_string().c_str()));
        		}else if (strcmp(node_name, "desiredMode") == 0){
            		ROS_INFO("ChangeMode to %s", j -> as_string().c_str());
            		changeMode((rexos_statemachine::Mode) atoi(j -> as_string().c_str()));
        		} else {
			ROS_INFO("Unknown field %s", node_name);
    		}
		j++;
	}
    } else if (strcmp(node_name, "desiredState") == 0){
	ROS_INFO("ChangeState to %s", i -> as_string().c_str());
            changeState((rexos_statemachine::State) atoi(i -> as_string().c_str()));
        }else if (strcmp(node_name, "desiredMode") == 0){
            ROS_INFO("ChangeMode to %s", i -> as_string().c_str());
            changeMode((rexos_statemachine::Mode) atoi(i -> as_string().c_str()));
        } else {
	ROS_INFO("Unknown field %s", node_name);
    }
        i++;
    }
}

//needed for callback ( from proxy )
void EquipletNode::onInstructionStepCompleted(ModuleProxy* moduleProxy, std::string id, bool completed){

	//moduleProxy->changeState(rexos_statemachine::STATE_STANDBY);
	mongo::OID targetObjectId(id);

	if(completed) {
    	equipletStepBlackboardClient->updateDocumentById(targetObjectId, "{ $set : {status: \"DONE\" } } ");
    	std::cout << "Done with step with id: " << id << std::endl << " Updated status on BB to done." << std::endl;
	} else {
    	equipletStepBlackboardClient->updateDocumentById(targetObjectId, "{ $set : {status: \"FAILED\" } } ");
	}

}

void EquipletNode::onStateChanged(){
	EquipletStateMachine::onStateChanged();
	updateEquipletStateOnBlackboard();
}

void EquipletNode::onModeChanged(){
	EquipletStateMachine::onModeChanged();
	updateEquipletStateOnBlackboard();
}

void EquipletNode::updateEquipletStateOnBlackboard(){
	JSONNode jsonUpdateQuery;
	jsonUpdateQuery.push_back(JSONNode("id",equipletId));

	std::ostringstream stringStream;
	stringStream << "{$set: { state: " << getCurrentState() << ",mode: " << getCurrentMode() << "}}";
	std::cout << "updating state on blackboard; {$set: { state: " << getCurrentState() << ",mode: " << getCurrentMode() << "}}" << std::endl;

	equipletStateBlackboardClient->updateDocuments(jsonUpdateQuery.write().c_str(),stringStream.str());
}

std::string EquipletNode::getName() {
	return nameFromId(equipletId);
}

ros::NodeHandle& EquipletNode::getNodeHandle() {
	return nh;
}

/**
 * Call the lookuphandler with the data from the blackboard to get data
 *
 * @param lookupType the type of the lookup
 * @param lookupID the ID of the lookup
 * @param payload the payload, contains data that will get combined with environmentcache data
 **/
std::map<std::string, std::string> EquipletNode::callLookupHandler(std::string lookupType, std::map<std::string, std::string> lookupParameters, std::map<std::string, std::string> payloadMap){
 	
 	lookup_handler::LookupServer msg;

	msg.request.lookupMsg.lookupType = lookupType;
	msg.request.lookupMsg.lookupParameters = createMessageFromMap(lookupParameters);
	msg.request.lookupMsg.payLoad = createMessageFromMap(payloadMap);

	ros::NodeHandle nodeHandle;
	ros::ServiceClient lookupClient = nodeHandle.serviceClient<lookup_handler::LookupServer>("LookupHandler/lookup");

	if(lookupClient.call(msg)){
		environment_communication_msgs::Map map = msg.response.lookupMsg.payLoad;
		return createMapFromMessage(map);
	} else {
		ROS_INFO("Could not find anything in the lookup handler");
		return payloadMap;
	}
}
/**
 * Create a Map message from a map with strings as keys and strings as values
 *
 * @param Map The map to convert
 *
 * @return environment_communication_msgs::Map The map message object
 **/
environment_communication_msgs::Map EquipletNode::createMessageFromMap(std::map<std::string, std::string> &Map){
	std::map<std::string, std::string>::iterator MapIterator;
	environment_communication_msgs::Map mapMsg;
	environment_communication_msgs::KeyValuePair prop;

	for(MapIterator = Map.begin(); MapIterator != Map.end(); MapIterator++){
		prop.key = (*MapIterator).first;
		prop.value = (*MapIterator).second;
		mapMsg.map.push_back(prop);
	}

	return mapMsg;
}

map<std::string, std::string> EquipletNode::createMapFromMessage(environment_communication_msgs::Map Message){
    std::map<std::string, std::string> msgMap;    

	for(int i = 0; i < (int)(Message.map.size()); i++){
		msgMap.insert(std::pair<std::string, std::string>(Message.map[i].key, Message.map[i].value));
	}

    return msgMap;
}