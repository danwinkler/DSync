package com.danwink.dsync.agent;

public abstract class SyncAgent
{
	public int syncId;
	public boolean alive = true;
	public Object syncMessage;
	
	public void setMessage( Object o )
	{
		syncMessage = o;
	}
	
	public abstract Object initial();
	public abstract void processMessage( Object o );
}
