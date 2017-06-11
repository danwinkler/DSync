package com.danwink.dsync;

import java.util.HashMap;

import com.danwink.dsync.SyncServer.AddPacket;
import com.danwink.dsync.SyncServer.PartialPacket;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;

public class SyncClient
{
	public static final String JOIN = "com.danwink.dsync.SyncClient.JOIN";
	
	DClient client;
	@SuppressWarnings( "rawtypes" )
	ListenerManager<ObjectListener> addLm;
	
	@SuppressWarnings( "rawtypes" )
	ListenerManager<ObjectListener> initLm;
	
	ListenerManager<IdListener> removeLm;
	
	@SuppressWarnings( "rawtypes" )
	HashMap<Integer, SyncObject> syncies;
	
	public SyncClient( DClient client )
	{
		this( client, DEndPoint.DEFAULT_STATE );
	}
	
	@SuppressWarnings( { "unchecked", "rawtypes" } )
	public SyncClient( DClient client, Object state )
	{
		this.client = client;
		
		addLm = new ListenerManager<>();
		initLm = new ListenerManager<>();
		removeLm = new ListenerManager<>();
		
		syncies = new HashMap<>();
		
		client.on( state, SyncServer.add, (AddPacket p) -> {
			syncies.put( p.object.syncId, p.object );
			addLm.call( p.classHash, l -> {
				l.object( p.object );
			});
		});
		
		client.on( state, SyncServer.initial, (AddPacket p) -> {
			syncies.put( p.object.syncId, p.object );
			addLm.call( p.classHash, l -> {
				l.object( p.object );
			});
		});
		
		client.on( state, SyncServer.partial, (PartialPacket p) -> {
			PartialUpdatable pu = (PartialUpdatable)syncies.get( p.id );
			if( pu == null ) return;
			if( p.partial instanceof KeepAlive )
			{
				System.out.println( "A KEEP ALIVE WAS FOUND AS A PART OF A MESSAGE PACKET" );
				return;
			}
			pu.partialReadPacket( p.partial );
		});
		
		client.on( state, SyncServer.update, (SyncObject so) -> {
			SyncObject s = syncies.get( so.syncId );
			s.set( so );
		});
		
		client.on( state, SyncServer.remove, (Integer id) -> {
			SyncObject so = syncies.remove( id );
			so.remove = true;
			
			removeLm.call( so.getClass().getSimpleName().hashCode(), l -> {
				l.id( id );
			});
		});
	}
	
	public <E> void onAdd( Class<E> c, ObjectListener<E> listener )
	{
		addLm.on( c.getSimpleName().hashCode(), listener );
	}
	
	public <E> void onJoin( Class<E> c, ObjectListener<E> listener )
	{
		initLm.on( c.getSimpleName().hashCode(), listener );
	}
	
	public <E> void onAddAndJoin( Class<E> c, ObjectListener<E> listener )
	{
		addLm.on( c.getSimpleName().hashCode(), listener );
		initLm.on( c.getSimpleName().hashCode(), listener );
	}
	
	public <E> void onRemove( Class<E> c, IdListener listener )
	{
		removeLm.on( c.getSimpleName().hashCode(), listener );
	}
	
	public void clearListeners()
	{
		addLm.clear();
		initLm.clear();
		removeLm.clear();
	}
	
	public void remove( int id )
	{
		syncies.remove( id );
	}
	
	@SuppressWarnings( "rawtypes" )
	public SyncObject get( int id )
	{
		return syncies.get( id );
	}
	
	public interface ObjectListener<E>
	{
		public void object( E o );
	}
	
	public interface IdListener
	{
		public void id( int id );
	}
}
