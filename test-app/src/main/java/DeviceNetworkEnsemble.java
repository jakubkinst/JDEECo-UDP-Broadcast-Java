import cz.cuni.mff.d3s.deeco.annotations.*;
import cz.cuni.mff.d3s.deeco.task.ParamHolder;

import java.util.Set;


@Ensemble
@PeriodicScheduling(period = 200)
public class DeviceNetworkEnsemble {

	@Membership
	public static boolean membership(@In("member.name") String memberName, @In("coord.name") String coordName) {
		return true;
	}

	@KnowledgeExchange
	public static void map(@InOut("member.otherDevices") ParamHolder<Set<String>> memberOtherDevices, @In("coord.name") String coordName) {
		memberOtherDevices.value.add(coordName);
	}

}
