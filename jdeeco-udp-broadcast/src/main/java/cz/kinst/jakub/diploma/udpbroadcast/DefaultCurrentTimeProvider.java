package cz.kinst.jakub.diploma.udpbroadcast;

import cz.cuni.mff.d3s.deeco.scheduler.CurrentTimeProvider;

/**
 * Created by jakubkinst on 14/11/14.
 */
public class DefaultCurrentTimeProvider implements CurrentTimeProvider{

	@Override
	public long getCurrentMilliseconds() {
		return System.currentTimeMillis();
	}
}
