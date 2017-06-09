package com.danwink.dsync;

import java.io.IOException;
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
	ListenerManager<ClientMessageListener> listenerManager;
	
	public DClient() 
	{
		listenerManager = new ListenerManager<>();
		
		c = new Client( 128000, 32000 );
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
	
	@SuppressWarnings( { "unchecked" } )
	public void update()
	{
		while( hasMessages() )
		{
			Message m = messages.removeFirst();
			if( m.value instanceof FrameworkMessage.KeepAlive ) {
				System.out.println( "KEEP ALIVE INTERCEPTED ON BOT" );
				continue;
			}
			listenerManager.call( m.key, l -> {
				l.receive( m.value );
			});
		}
	}
	
	public <E> void on( Object key, ClientMessageListener<E> listener ) 
	{
		listenerManager.on( key, listener );
	}
	
	public void clearListeners()
	{
		listenerManager.clear();
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
}
