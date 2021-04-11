package net.gitsaibot.af.data;

import net.gitsaibot.af.util.AixLocationInfo;

public interface AixDataSource {

	public void update(AixLocationInfo aixLocationInfo, long currentUtcTime) throws AixDataUpdateException;

}
