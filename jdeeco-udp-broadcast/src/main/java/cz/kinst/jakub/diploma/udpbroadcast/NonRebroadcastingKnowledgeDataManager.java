package cz.kinst.jakub.diploma.udpbroadcast;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import cz.cuni.mff.d3s.deeco.DeecoProperties;
import cz.cuni.mff.d3s.deeco.knowledge.ChangeSet;
import cz.cuni.mff.d3s.deeco.knowledge.KnowledgeManager;
import cz.cuni.mff.d3s.deeco.knowledge.KnowledgeManagerContainer;
import cz.cuni.mff.d3s.deeco.knowledge.KnowledgeNotFoundException;
import cz.cuni.mff.d3s.deeco.knowledge.KnowledgeUpdateException;
import cz.cuni.mff.d3s.deeco.knowledge.ValueSet;
import cz.cuni.mff.d3s.deeco.logging.Log;
import cz.cuni.mff.d3s.deeco.model.runtime.api.KnowledgePath;
import cz.cuni.mff.d3s.deeco.model.runtime.custom.RuntimeMetadataFactoryExt;
import cz.cuni.mff.d3s.deeco.model.runtime.meta.RuntimeMetadataFactory;
import cz.cuni.mff.d3s.deeco.network.KnowledgeData;
import cz.cuni.mff.d3s.deeco.network.KnowledgeDataManager;
import cz.cuni.mff.d3s.deeco.network.KnowledgeDataSender;
import cz.cuni.mff.d3s.deeco.network.KnowledgeMetaData;
import cz.cuni.mff.d3s.deeco.network.NICType;
import cz.cuni.mff.d3s.deeco.scheduler.Scheduler;

public class NonRebroadcastingKnowledgeDataManager extends KnowledgeDataManager {

	/**
	 * Stores received KnowledgeMetaData for replicas (received ValueSet is deleted)
	 */
	protected final Map<KnowledgeManager, KnowledgeMetaData> replicaMetadata;
	/**
	 * Global version counter for all outgoing local knowledge.
	 */
	protected long localVersion;


	/**
	 * Empty knowledge path enabling convenient query for all knowledge
	 */
	protected final List<KnowledgePath> emptyPath;

	protected final boolean useIndividualPublishing;
	protected final boolean checkGossipCondition;
	protected final boolean checkBoundaryCondition;
	protected final Random random;


	public NonRebroadcastingKnowledgeDataManager() {
		this.localVersion = 0;
		this.replicaMetadata = new HashMap<>();

		RuntimeMetadataFactory factory = RuntimeMetadataFactoryExt.eINSTANCE;
		KnowledgePath empty = factory.createKnowledgePath();
		emptyPath = new LinkedList<>();
		emptyPath.add(empty);

		useIndividualPublishing = Boolean.getBoolean(DeecoProperties.USE_INDIVIDUAL_KNOWLEDGE_PUBLISHING);
		checkGossipCondition = !Boolean.getBoolean(DeecoProperties.DISABLE_GOSSIP_CONDITION);
		checkBoundaryCondition = !Boolean.getBoolean(DeecoProperties.DISABLE_BOUNDARY_CONDITIONS);


		random = new Random();
	}

	@Override
	public void initialize(
			KnowledgeManagerContainer kmContainer,
			KnowledgeDataSender knowledgeDataSender,
			String host,
			Scheduler scheduler) {
		super.initialize(kmContainer, knowledgeDataSender, host, scheduler);
		long seed = 0;
		for (char c : host.toCharArray())
			seed += c;
		random.setSeed(seed);

		Log.d(String.format("KnowledgeDataManager at %s uses %s publishing", host, useIndividualPublishing ? "individual" : "list"));
	}


	@Override
	public void publish() {
		// we re-publish periodically only local data
		List<KnowledgeData> data = prepareLocalKnowledgeData();

		if (!data.isEmpty()) {

			logPublish(data);

			if (useIndividualPublishing) {
				// broadcast each kd individually to minimize the message size and
				// thus reduce network collisions.
				for (KnowledgeData kd : data) {
					//System.out.println("Broadcasting data at " + host + kd);
					knowledgeDataSender.broadcastKnowledgeData(Arrays.asList(kd));
				}
			} else {
				//System.out.println("Broadcasting data at " + host + data);
				knowledgeDataSender.broadcastKnowledgeData(data);
			}
			localVersion++;
		}
	}

	@Override
	public void rebroacast(KnowledgeMetaData metadata, NICType nicType) {
		// not implemented here
	}

	@Override
	public void receive(List<? extends KnowledgeData> knowledgeData) {
		if (knowledgeData == null)
			Log.w("KnowledgeDataManager.receive: Received null KnowledgeData.");

		logReceive(knowledgeData);

		for (KnowledgeData kd : knowledgeData) {
			KnowledgeMetaData newMetadata = kd.getMetaData();
			if (kmContainer.hasLocal(newMetadata.componentId)) {
				if (Log.isDebugLoggable())
					Log.d("KnowledgeDataManager.receive: Dropping KnowledgeData for local component " + newMetadata.componentId);

				continue;
			}
			KnowledgeManager replica = kmContainer.createReplica(newMetadata.componentId);
			KnowledgeMetaData currentMetadata = replicaMetadata.get(replica);

			// accept only fresh knowledge data (drop if we have already a newer value)
			boolean haveOlder = (currentMetadata == null) || (currentMetadata.versionId < newMetadata.versionId);
			if (haveOlder) {
				try {
					replica.update(toChangeSet(kd.getKnowledge()));
				} catch (KnowledgeUpdateException e) {
					Log.w(String
							.format("KnowledgeDataManager.receive: Could not update replica of %s.",
									newMetadata.componentId), e);
				}
				//	store the metadata without the knowledge values
				replicaMetadata.put(replica, newMetadata);


				if (Log.isDebugLoggable()) {
					Log.d(String.format("Receive (%d) at %s got %sv%d after %dms and %d hops\n",
							timeProvider.getCurrentMilliseconds(),
							host,
							newMetadata.componentId,
							newMetadata.versionId,
							timeProvider.getCurrentMilliseconds() - newMetadata.createdAt,
							newMetadata.hopCount));
				}
			}
			//System.out.println("Received at " + host + " " + knowledgeData);
		}
	}

	protected List<KnowledgeData> prepareLocalKnowledgeData() {
		List<KnowledgeData> result = new LinkedList<>();
		for (KnowledgeManager km : kmContainer.getLocals()) {
			try {
				KnowledgeData kd = prepareLocalKnowledgeData(km);
				result.add(filterLocalKnowledgeForKnownEnsembles(kd));
			} catch (Exception e) {
				Log.e("prepareKnowledgeData error", e);
			}
		}
		return result;
	}


	protected KnowledgeData prepareLocalKnowledgeData(KnowledgeManager km)
			throws KnowledgeNotFoundException {
		return new KnowledgeData(
				getNonLocalKnowledge(km.get(emptyPath), km),
				new KnowledgeMetaData(km.getId(), localVersion, host, timeProvider.getCurrentMilliseconds(), 1));
	}

	protected ValueSet getNonLocalKnowledge(ValueSet toFilter, KnowledgeManager km) {
		ValueSet result = new ValueSet();
		for (KnowledgePath kp : toFilter.getKnowledgePaths()) {
			if (!km.isLocal(kp)) {
				result.setValue(kp, toFilter.getValue(kp));
			}
		}
		return result;
	}

	protected KnowledgeData filterLocalKnowledgeForKnownEnsembles(KnowledgeData kd) {
		// FIXME: make this generic
		// now we hardcode our demo (we filter the Leader knowledge to only
		// publish id, team, and position.
		if (kd.getMetaData().componentId.startsWith("L")) {
			ValueSet values = kd.getKnowledge();
			ValueSet newValues = new ValueSet();
			for (KnowledgePath kp : values.getKnowledgePaths()) {
				newValues.setValue(kp, values.getValue(kp));
			}
			return new KnowledgeData(newValues, kd.getMetaData());
		} else {
			return kd;
		}
	}

	protected ChangeSet toChangeSet(ValueSet valueSet) {
		if (valueSet != null) {
			ChangeSet result = new ChangeSet();
			for (KnowledgePath kp : valueSet.getKnowledgePaths())
				result.setValue(kp, valueSet.getValue(kp));
			return result;
		} else {
			return null;
		}
	}

	protected void logPublish(List<? extends KnowledgeData> data) {
		logPublish(data, "");
	}

	protected void logPublish(List<? extends KnowledgeData> data, String recipient) {
		if (Log.isDebugLoggable()) {
			StringBuilder sb = new StringBuilder();
			for (KnowledgeData kd : data) {
				sb.append(kd.getMetaData().componentId + "v" + kd.getMetaData().versionId);
				sb.append(", ");
			}
			if (recipient != null && !recipient.isEmpty())
				if (Log.isDebugLoggable()) {
					Log.d(String.format("Publish (%d) at %s, sending [%s] directly to %s\n",
							timeProvider.getCurrentMilliseconds(), host, sb.toString(), recipient));
				} else if (Log.isDebugLoggable()) {
					Log.d(String.format("Publish (%d) at %s, sending [%s]\n",
							timeProvider.getCurrentMilliseconds(), host, sb.toString()));
				}
		}
	}

	protected void logReceive(List<? extends KnowledgeData> knowledgeData) {
		if (Log.isDebugLoggable()) {
			StringBuilder sb = new StringBuilder();
			for (KnowledgeData kd : knowledgeData) {
				sb.append(kd.getMetaData().componentId + "v" + kd.getMetaData().versionId + "<-" + kd.getMetaData().sender);
				sb.append(", ");
			}

			if (Log.isDebugLoggable()) {
				Log.d(String.format("Receive (%d) at %s, received [%s]\n",
						timeProvider.getCurrentMilliseconds(), host, sb.toString()));
			}
		}
	}

}
