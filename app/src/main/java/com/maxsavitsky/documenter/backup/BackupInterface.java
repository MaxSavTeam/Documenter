package com.maxsavitsky.documenter.backup;

public interface BackupInterface {
	void successfully(long timeOfCreation);

	void failed();

	void exceptionOccurred(final Exception e);
}
