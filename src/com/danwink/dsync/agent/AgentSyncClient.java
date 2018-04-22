package com.danwink.dsync.agent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.danwink.dsync.DClient;
import com.danwink.dsync.ListenerManager;
import com.danwink.dsync.agent.AgentSyncServer.Packet;

public class AgentSyncClient<E extends SyncAgent>
{
	List<E> agents;
	HashMap<Integer, SyncAgent> agentMap;
	
	DClient client;
	
	ListenerManager<AgentListener> addListenerManager;
	
	public AgentSyncClient( DClient client )
	{
		this.client = client;
		client.register( AgentSyncServer.registerClasses );
		
		agents = new ArrayList<>();
		agentMap = new HashMap<>();
		
		addListenerManager = new ListenerManager<>();
		
		// DRY these two out
		client.on( AgentSyncServer.initial, (Packet p) -> {
			E a = (E)p.type.create();
			a.syncId = p.id;
			a.processMessage( p.type, p.payload );
			
			agents.add( a );
			agentMap.put( a.syncId, a );
			
			addListenerManager.call( p.type, l -> {
				l.event( a );
			});
		});
		
		client.on( AgentSyncServer.add, (Packet p) -> {
			E a = (E)p.type.create();
			a.syncId = p.id;
			a.processMessage( p.type, p.payload );
			
			agents.add( a );
			agentMap.put( a.syncId, a );
			
			addListenerManager.call( p.type, l -> {
				l.event( a );
			});
		});
		
		client.on( AgentSyncServer.update, (Packet p) -> {
			agentMap.get( p.id ).processMessage( p.type, p.payload );
		});
		
		client.on( AgentSyncServer.remove, (Integer id) -> {

			agents.remove( agentMap.remove( id ) );
		});
	}
	
	public List<E> getAgents()
	{
		return agents;
	}
	
	public <A> void onAdd( AgentInstantiator c, AgentListener<A> listener )
	{
		addListenerManager.on( c, listener );
	}
	
	public interface AgentListener<A>
	{
		public void event( A a );
	}
}
