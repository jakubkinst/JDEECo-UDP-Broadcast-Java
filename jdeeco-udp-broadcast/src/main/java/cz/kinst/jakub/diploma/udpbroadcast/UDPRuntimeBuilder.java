package cz.kinst.jakub.diploma.udpbroadcast;

import java.util.Random;

import cz.cuni.mff.d3s.deeco.DeecoProperties;
import cz.cuni.mff.d3s.deeco.executor.Executor;
import cz.cuni.mff.d3s.deeco.executor.SameThreadExecutor;
import cz.cuni.mff.d3s.deeco.knowledge.CloningKnowledgeManagerFactory;
import cz.cuni.mff.d3s.deeco.knowledge.KnowledgeManagerContainer;
import cz.cuni.mff.d3s.deeco.knowledge.KnowledgeManagerFactory;
import cz.cuni.mff.d3s.deeco.model.runtime.api.RuntimeMetadata;
import cz.cuni.mff.d3s.deeco.model.runtime.custom.TimeTriggerExt;
import cz.cuni.mff.d3s.deeco.network.KnowledgeDataManager;
import cz.cuni.mff.d3s.deeco.network.PublisherTask;
import cz.cuni.mff.d3s.deeco.runtime.RuntimeFramework;
import cz.cuni.mff.d3s.deeco.runtime.RuntimeFrameworkImpl;
import cz.cuni.mff.d3s.deeco.scheduler.SingleThreadedScheduler;

/**
 * Created by jakubkinst on 12/11/14.
 */
public class UDPRuntimeBuilder {


	public RuntimeFramework build(String ipAddress, RuntimeMetadata model, UDPBroadcast udpBroadcast) {
		if (model == null) {
			throw new IllegalArgumentException("Model must not be null");
		}

		UDPBroadcastHost host = new UDPBroadcastHost(ipAddress, udpBroadcast);

		KnowledgeDataManager knowledgeDataManager = new NonRebroadcastingKnowledgeDataManager();

		KnowledgeManagerFactory knowledgeManagerFactory = new CloningKnowledgeManagerFactory();

		// Set up the executor
		Executor executor = new SameThreadExecutor();

		// Set up the simulation scheduler
		SingleThreadedScheduler scheduler = new SingleThreadedScheduler();
		scheduler.setExecutor(executor);

		// Set up the host container
		KnowledgeManagerContainer container = new KnowledgeManagerContainer(knowledgeManagerFactory);
		knowledgeDataManager.initialize(container, host.getKnowledgeDataSender(), host.getHostId(), scheduler);
		host.setKnowledgeDataReceiver(knowledgeDataManager);
		// Set up the publisher task
		TimeTriggerExt publisherTrigger = new TimeTriggerExt();
		publisherTrigger.setPeriod(Integer.getInteger(
				DeecoProperties.PUBLISHING_PERIOD,
				PublisherTask.DEFAULT_PUBLISHING_PERIOD));
		long seed = 0;
		for (char c : host.getHostId().toCharArray())
			seed = seed * 32 + (c - 'a');
		Random rnd = new Random(seed);
		publisherTrigger.setOffset(rnd.nextInt((int) publisherTrigger
				.getPeriod()) + 1);
		PublisherTask publisherTask = new PublisherTask(scheduler, knowledgeDataManager,
				publisherTrigger, host.getHostId());

		// Add publisher task to the scheduler
		scheduler.addTask(publisherTask);

		return new RuntimeFrameworkImpl(model, scheduler, executor, container);
	}
}
