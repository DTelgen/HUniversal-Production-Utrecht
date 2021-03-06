/**
 * @file src/REXOS/MAS/agents/service_agent/PickAndPlaceService.java
 * @brief Class for the pick&place service.
 * @date Created: 23 apr. 2013
 * 
 * @author Hessel Meulenbeld
 * @author Duncan Jenkins
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
package agents.service_agent;

import java.util.HashMap;
import java.util.Set;

import libraries.utillities.log.LogLevel;
import libraries.utillities.log.Logger;
import agents.data_classes.Part;
import agents.data_classes.Position;
import agents.data_classes.ScheduleData;
import agents.data_classes.StepStatusCode;

import com.mongodb.BasicDBObject;

/**
 * Instances of this class represent the pick & place service and is meant to
 * pick up objects like a cube and put them somewhere else on the working field.
 * It produces 2 service steps: "pick" and "place".
 */
public class PickAndPlaceService extends Service {
	
	/**
	 * @var int CRATE_HEIGHT
	 * A static value that contains the height of the crate.
	 */
	private static final double CRATE_HEIGHT = 17.56;
	
	/**
	 * @see agents.service_agent.Service#canDoStep(int,
	 *      com.mongodb.BasicDBObject)
	 */
	@Override
	public boolean canDoStep(int productStepType, BasicDBObject parameters) {
		try {
			if (parameters.containsField("parameterGroups")) {
				BasicDBObject parameterGroups = (BasicDBObject) parameters
						.get("parameterGroups");

				if (parameterGroups.containsField("loc")) {
					BasicDBObject location = (BasicDBObject) parameterGroups
							.get("loc");

					if (location.containsField("parameters")) {
						BasicDBObject locParameters = (BasicDBObject) location
								.get("parameters");
						if (locParameters.containsField("row")
								&& locParameters.containsField("column")) {
							return true;
						}
					}
				}
			} else {
				return false;
			}
		} catch (IllegalArgumentException e) {
			return false;
		}
		return false;
	}

	/**
	 * @see agents.service_agent.Service#getServiceSteps(int,
	 *      com.mongodb.BasicDBObject)
	 */
	@Override
	public ServiceStep[] getServiceSteps(int productStepType,
			BasicDBObject parameters) {
		BasicDBObject parameterGroups = (BasicDBObject) parameters
				.get("parameterGroups");

		BasicDBObject partGroup = (BasicDBObject) parameterGroups.get("part");
		BasicDBObject partParams = (BasicDBObject) partGroup.get("parameters");
		BasicDBObject partType = (BasicDBObject) partParams.get("type");

		Part part = new Part((int) (partType.get("value")));

		BasicDBObject pickParameters = new BasicDBObject();
		pickParameters.put("part", part.toBasicDBObject());
		pickParameters.put("row", "ROW-PLACEHOLDER");
		pickParameters.put("column", "COLUMN-PLACEHOLDER");
		pickParameters.put("crate", "CRATE-PLACEHOLDER");
		pickParameters.put("height", CRATE_HEIGHT);

		BasicDBObject loc = (BasicDBObject) parameterGroups.get("loc");
		BasicDBObject locParameters = (BasicDBObject) loc.get("parameters");
		BasicDBObject locColumn = (BasicDBObject) locParameters.get("column");
		BasicDBObject locRow = (BasicDBObject) locParameters.get("row");

		BasicDBObject placeParameters = new BasicDBObject();
		placeParameters.put("part", part.toBasicDBObject());
		placeParameters.put("row", locRow.get("value"));
		placeParameters.put("column", locColumn.get("value"));
		placeParameters.put("crate", "CRATE-PLACEHOLDER");
		placeParameters.put("height", CRATE_HEIGHT);

		return new ServiceStep[] {
				new ServiceStep(getId(), 1, pickParameters,
						StepStatusCode.EVALUATING, null, new ScheduleData()),
				new ServiceStep(getId(), 2, placeParameters,
						StepStatusCode.EVALUATING, null, new ScheduleData()) };
	}

	/**
	 * @see agents.service_agent.Service#updateParameters(java.util.HashMap,
	 *      rexos.mas.service_agent.ServiceStep[])
	 */
	@Override
	public ServiceStep[] updateParameters(
			HashMap<Part, Position> partParameters, ServiceStep[] serviceSteps) {
		Set<Part> parts = partParameters.keySet();
		Part supplyCrate = null, productCrate = null;
		for (Part part : parts) {
			Logger.log(LogLevel.DEBUG, "parts in partParameter: " + part);
			if (part.getId() == 100) {
				supplyCrate = part;
			} else if (part.getId() == 101) {
				productCrate = part;
			}
		}

		if (supplyCrate == null || productCrate == null) {
			// error = one or more crates were not returned
		}

		for (ServiceStep ss : serviceSteps) {
			BasicDBObject oldParameters = ss.getParameters();
			BasicDBObject newParameters = new BasicDBObject();

			// fill crate Ids
			switch (ss.getServiceStepType()) {
				case 1: // Pick - Supply
					// Grab a ball
					// Part ball =
					// partParameters.entrySet().iterator().next().getKey();
					// Position ballPosition = partParameters.remove(ball);
	
					Part ball = null;
					Position ballPosition = null;
	
					for (Part part : parts) {
						if (part.getType() == 1) {
							ball = part;
							ballPosition = partParameters.get(ball);
						}
					}
					if (ball != null && ballPosition != null) {
						if (oldParameters.containsField("crate")
								&& oldParameters.getString("crate").equals(
										"CRATE-PLACEHOLDER")) {
							newParameters.put("crate",
									supplyCrate.toBasicDBObject());
						} else {
							newParameters.put("crate", oldParameters.get("crate"));
						}
	
						if (oldParameters.containsField("row")
								&& oldParameters.getString("row").equals(
										"ROW-PLACEHOLDER")) {
							newParameters.put("row", ballPosition.getY());
						} else {
							newParameters
									.put("row", oldParameters.getDouble("row"));
						}
	
						if (oldParameters.containsField("column")
								&& oldParameters.getString("column").equals(
										"COLUMN-PLACEHOLDER")) {
							newParameters.put("column", ballPosition.getX());
						} else {
							newParameters.put("column",
									oldParameters.getDouble("column"));
						}
					} else {
						// error - no ball part
					}
					break;
				case 2: // Place - Product
					if (oldParameters.containsField("crate")
							&& oldParameters.getString("crate").equals(
									"CRATE-PLACEHOLDER")) {
						newParameters.put("crate", productCrate.toBasicDBObject());
					} else {
						newParameters.put("crate", oldParameters.get("crate"));
					}
	
					newParameters.put("row", oldParameters.getDouble("row"));
					newParameters.put("column", oldParameters.getDouble("column"));
					break;
			}

			newParameters.put("height", oldParameters.getDouble("height"));
			
			Logger.log(
					LogLevel.DEBUG,
					"After updating service step parameters: "
							+ newParameters.keySet());

			ss.setParameters(newParameters);
		}

		return serviceSteps;
	}

}
