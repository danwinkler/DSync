package com.danwink.dsync;

import java.io.IOException;
import java.util.HashMap;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage;


/**
 * Client class to connect to DServer. 
 * 
 * Essentially a wrapper around kryonet's Client class, but where every packet 
 * sent is a Message object.
 * 
 * A thread is spawned when connect is called (through kryonet). This thread 
 * receives messages and adds them to the messages queue. These can either be
 * directly fetched through hasClientMessages and getNextClientMessage, but if
 * using callbacks, the update method should be called from the main thread,
 * which will simplify concurrency issues.
 * 
 * 
 * @author Daniel Winkler
 *
 */
public class DClient extends DEndPoint
{
	Client c;
	
	@SuppressWarnings( "rawtypes" )
	
	ListenerManager<ClientMessageListener> globalListeners;
	HashMap<Object, ListenerManager<ClientMessageListener>> listeners;
	ListenerManager<StateChangedListener> stateListeners;
	
	public DClient() 
	{
		globalListeners = new ListenerManager<>();
		listeners = new HashMap<>();
		stateListeners = new ListenerManager<>();
		
		c = new Client( 128000, DServer.OBJECT_BUFFER );
		c.getKryo().register( Message.class );
		
		c.addListener( this );
	}
	
	public void connect( String address, int tcpPort, int udpPort ) throws IOException 
	{
		c.start();
		c.connect( 2500, address, tcpPort, udpPort );	
	}
	
	@SuppressWarnings( "rawtypes" )
	public void register( Class...classes )
	{
		for( Class cToR : classes ) 
		{
			c.getKryo().register( cToR );
		}
	}
	
	public void sendTCP( Object key, Object value ) 
	{
		c.sendTCP( new Message( key, value ) );
	}
	
	public void sendTCP( Object key ) 
	{
		sendTCP( key, null );
	}
	
	public interface ClientMessageListener<E>
	{
		public void receive( E message );
	}
	
	public interface StateChangedListener
	{
		public void changed( Object state );
	}
	
	@SuppressWarnings( { "unchecked" } )
	public void update()
	{
		while( hasMessages() )
		{
			Message m = messages.removeFirst();
			if( m.value instanceof FrameworkMessage.KeepAlive ) {
				System.out.println( "KEEP ALIVE INTERCEPTED ON BOT" );
				continue;
			} else if( m.key.equals( SET_STATE ) ) {
				state = m.value;
				stateListeners.call( l -> {
					l.changed( state );
				});
			} 
			
			ListenerManager<ClientMessageListener> lm = listeners.get( state );
			if( lm != null )
			{
				lm.call( m.key, l -> {
					l.receive( m.value );
				});
			}
			globalListeners.call( m.key, l-> {
				l.receive( m.value );
			});
		}
	}
	
	public <E> void on( Object key, ClientMessageListener<E> listener ) 
	{
		globalListeners.on( key, listener );
	}
	
	public <E> void on( Object state, Object key, ClientMessageListener<E> listener ) 
	{
		if( state == null )
		{
			on( key, listener );
		} 
		else
		{
			ListenerManager<ClientMessageListener> lm = listeners.get( state );
			if( lm == null )
			{
				lm = new ListenerManager<>();
				listeners.put( state, lm );
			}
			lm.on( key, listener );
		}
	}
	
	public void onStateChanged( StateChangedListener listener )
	{
		stateListeners.on( listener );
	}
	
	public void clearListeners()
	{
		listeners.clear();
	}
	
	//Listener
	public void received( Connection c, Object o ) 
	{
		if( o instanceof Message )
		{
			messages.addLast( (Message)o );
		}
	}
	
	public void connected( Connection c )
	{
		messages.addLast( new Message( CONNECTED, null ) );
	}
	
	public void disconnected( Connection c )
	{
		messages.addLast( new Message( DISCONNECTED, null ) );
	}

	public void stop()
	{
		c.close();
		c.stop();
	}
}
