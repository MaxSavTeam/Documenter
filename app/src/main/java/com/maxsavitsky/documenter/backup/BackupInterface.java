package com.maxsavitsky.documenter.backup;

public interface BackupInterface {
	void onSuccess(long timeOfCreation);

	void onException(final Exception e);
}
