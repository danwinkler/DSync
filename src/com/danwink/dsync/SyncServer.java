package com.danwink.dsync;

import java.util.ArrayList;
import java.util.Arrays;

public class SyncServer
{
	public static final String add = "SYNC.ADD";
	public static final String update = "SYNC.UPDATE";
	public static final String initial = "SYNC.INITIAL"; //Sent when syncobject isnt new, but when a user joins so they are seeing it for the first time
	public static final String partial = "SYNC.PARTIAL";
	public static final String remove = "SYNC.REMOVE";
	
	public static final Class[] registerClasses = new Class[] { SyncObject.class, PartialPacket.class, AddPacket.class };
	
	DServer server;
	
	int nextId = 0;
	
	ArrayList<SyncObject> syncies = new ArrayList<SyncObject>();
	
	public SyncServer( DServer server )
	{
		this( server, DEndPoint.DEFAULT_STATE );
	}
	
	public SyncServer( DServer server, Object state )
	{
		this.server = server;
		Arrays.asList( registerClasses ).forEach( c -> {
			server.server.getKryo().register( c );
		});
		
		server.on( state, DServer.CONNECTED, (id, o) -> {
			for( int i = 0; i < syncies.size(); i++ )
			{
				SyncObject so = syncies.get( i );
				server.sendTCP( id, initial, new AddPacket( so.getClass().getSimpleName().hashCode(), so ) );
			}
		});
	}
	
	public void add( SyncObject so )
	{
		so.syncId = nextId++;
		syncies.add( so );
		server.broadcastTCP( add, new AddPacket( so.getClass().getSimpleName().hashCode(), so ) );
	}
	
	public void update()
	{
		for( int i = 0; i < syncies.size(); i++ )
		{
			SyncObject so = syncies.get( i );
			if( so.remove ) 
			{
				server.broadcastTCP( remove, so.syncId );
				syncies.remove( i );
				i--;
			} 
			else if( so.update ) 
			{
				server.broadcastTCP( update, so );
				so.update = false;
			}
			else if( so.partial ) 
			{
				server.broadcastTCP( partial, new PartialPacket( so.syncId, ((PartialUpdatable)so).partialMakePacket() ) );
				so.partial = false;
			} 
		}
	}
	
	public static class PartialPacket<E>
	{
		int id;
		E partial;
		
		public PartialPacket() {}
		
		public PartialPacket( int id, E partial )
		{
			this.id = id;
			this.partial = partial;
		}
	}
	
	public static class AddPacket
	{
		int classHash;
		SyncObject object;
		
		public AddPacket() {}
		
		public AddPacket( int classHash, SyncObject object )
		{
			this.classHash = classHash;
			this.object = object;
		}
	}
}
