package com.danwink.dsync.agent;

public abstract class SyncAgent<Type extends AgentInstantiator>
{
	public int syncId;
	public Type type;
	public boolean alive = true;
	public Object syncMessage;
	
	public void setMessage( Object o )
	{
		syncMessage = o;
	}
	
	public abstract Object initial();
	public abstract void processMessage( Type type, Object o );
}
