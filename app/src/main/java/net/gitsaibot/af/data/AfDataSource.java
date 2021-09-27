package net.gitsaibot.af.data;

import net.gitsaibot.af.util.AfLocationInfo;

public interface AfDataSource {

	public void update(AfLocationInfo afLocationInfo, long currentUtcTime) throws AixDataUpdateException;

}
