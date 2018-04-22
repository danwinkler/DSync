package com.danwink.dsync.agent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.danwink.dsync.DEndPoint;
import com.danwink.dsync.DServer;
import com.danwink.dsync.PartialUpdatable;
import com.danwink.dsync.sync.SyncObject;
import com.danwink.dsync.sync.SyncServer.AddPacket;
import com.danwink.dsync.sync.SyncServer.PartialPacket;

public class AgentSyncServer<E extends SyncAgent>
{
	public static final String add = "SYNC.ADD";
	public static final String update = "SYNC.UPDATE";
	public static final String initial = "SYNC.INITIAL"; //Sent when agentsync isnt new, but when a user joins so they are seeing it for the first time
	public static final String partial = "SYNC.PARTIAL";
	public static final String remove = "SYNC.REMOVE";
	
	public static Class<?>[] registerClasses = new Class[] { Packet.class };
	
	public List<E> agents;
	
	DServer server;
	
	int nextId = 1;
	
	public AgentSyncServer( DServer server )
	{
		this( server, new ArrayList<E>() );
	}
	
	public AgentSyncServer( DServer server, List<E> agents )
	{
		this.server = server;
		this.agents = agents;
		
		server.register( registerClasses );
		
		for( E e : agents )
		{
			e.syncId = nextId++;
		}
		
		server.on( DServer.CONNECTED, (id, o) -> {
			System.out.println( "Client connected" );
			for( SyncAgent a : agents )
			{
				server.sendTCP( id, initial, new Packet( a.syncId, a.type, a.initial() ) );
			}
		});
	}
	
	public void update()
	{
		Iterator<E> i = agents.iterator();
		while( i.hasNext() )
		{
			E a = i.next();
			if( !a.alive ) 
			{
				server.broadcastTCP( remove, a.syncId );
				i.remove();
			} 
			else if( a.syncMessage != null ) 
			{
				server.broadcastTCP( update, new Packet( a.syncId, a.type, a.syncMessage ) );
				a.syncMessage = null;
			}
			
		}
	}
	
	public void add( E a )
	{
		a.syncId = nextId++;
		agents.add( a );
		server.broadcastTCP( add, new Packet( a.syncId, a.type, a.initial() ) );
	}
	
	public static class Packet
	{
		public int id;
		public AgentInstantiator type;
		public Object payload;
		
		public Packet()
		{
			
		}
		
		public Packet( int id, AgentInstantiator type, Object payload )
		{
			this.id = id;
			this.type = type;
			this.payload = payload;
		}
	}
}
