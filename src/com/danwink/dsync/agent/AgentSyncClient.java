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
			try
			{
				E a = (E)Class.forName(p.agentClassId).newInstance();
				a.syncId = p.id;
				a.processMessage( p.payload );
				
				agents.add( a );
				agentMap.put( a.syncId, a );
				
				addListenerManager.call( l -> {
					l.event( a );
				});
			}
			catch( InstantiationException | IllegalAccessException | ClassNotFoundException e )
			{
				e.printStackTrace();
			}
		});
		
		client.on( AgentSyncServer.add, (Packet p) -> {
			try
			{
				E a = (E)Class.forName(p.agentClassId).newInstance();
				a.syncId = p.id;
				a.processMessage( p.payload );
				
				agents.add( a );
				agentMap.put( a.syncId, a );
				
				addListenerManager.call( l -> {
					l.event( a );
				});
			}
			catch( InstantiationException | IllegalAccessException | ClassNotFoundException e )
			{
				e.printStackTrace();
			}
		});
		
		client.on( AgentSyncServer.update, (Packet p) -> {
			agentMap.get( p.id ).processMessage( p.payload );
		});
		
		client.on( AgentSyncServer.remove, (Integer id) -> {

			agents.remove( agentMap.remove( id ) );
		});
	}
	
	public List<E> getAgents()
	{
		return agents;
	}
	
	public <A> void onAdd( AgentListener<A> listener )
	{
		addListenerManager.on( listener );
	}
	
	public interface AgentListener<A>
	{
		public void event( A a );
	}
}
