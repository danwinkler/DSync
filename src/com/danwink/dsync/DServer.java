package com.danwink.dsync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.Pools;
import com.danwink.dsync.DClient.ClientMessageListener;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;


//TODO: user defined handshake protocol

/**
 * DServer and DClient are an abstraction over Kryonet's Server and Client 
 * classes. They provide a key/value message model with a callback interface, 
 * a message buffer, thread helpers, and state management.
 * 
 * Every message sent between client to server or server to client(s) is in the
 * form (Object key, Object value). Generics are (ab)used so that you can have 
 * message listeners like so:
 * <code>
 * server.on( "Hello", (Integer id, Float message) -> {
 *	//Do something
 * });
 * 
 * server.on( 3, (Integer id, String message) -> {
 *	//Do something
 * });
 * 
 * server.on( "Message", (id, o) -> {
 *	//Do something
 * });
 * </code>
 * 
 * Note that this interface foregoes type safety, so don't try to send a 
 * message of a certain type on one side while trying to listen for it as a 
 * different type on the other!
 * 
 * Specifying a null state in an "on" callback is the same as using the
 * on( Object key, ServerMessageListener<E> callback ) function, in that it
 * adds the callback as a global callback.
 * 
 * @author dan
 *
 */
public class DServer extends DEndPoint
{
	public static final int WRITE_BUFFER = 5000000;
	public static final int OBJECT_BUFFER = 32000;
	
	Thread messageSenderThread;
	public ThreadRunner tr;
	boolean running;
	
	ConcurrentLinkedDeque<MessagePacket> messagesToSend = new  ConcurrentLinkedDeque<MessagePacket>();
	HashMap<Integer, Connection> connections = new HashMap<Integer, Connection>();
	ArrayList<Connection> connectionsArr = new ArrayList<Connection>();
	
	Pool<MessagePacket> mpPool = Pools.get( MessagePacket.class );
	
	@SuppressWarnings( "rawtypes" )
	ListenerManager<ServerMessageListener> globalListeners;
	HashMap<Object, ListenerManager<ServerMessageListener>> listeners;
	
	Server server;
	
	public DServer()
	{
		globalListeners = new ListenerManager<>();
		listeners = new HashMap<>();
		
		server = new Server( WRITE_BUFFER, OBJECT_BUFFER );
		server.getKryo().register( Message.class );
		server.addListener( this );
	}
	
	//Always needs to be called to bind the server to the ports
	public void start( int tcpPort, int udpPort ) throws IOException
	{
		running = true;
		
		server.bind( tcpPort, udpPort );
		server.start();
		
		messageSenderThread = new Thread( new MessageSender() ) ;
		messageSenderThread.setName( "DServer MessageSender" );
		messageSenderThread.start();
	}
	
	//Creates a server game loop (optional)
	public void startThread( Updateable u, int fps ) 
	{	
		tr = new ThreadRunner( fps );
		tr.start( u );
	}
	
	public void sendTCP( int id, Object key, Object value )
	{
		MessagePacket mp = mpPool.obtain();
		mp.init( id, key, value );
		messagesToSend.addLast( mp );
	}
	
	public void broadcastTCP( Object key, Object value )
	{
		synchronized( connectionsArr ) 
		{
			for( int i = 0; i < connectionsArr.size(); i++ )
			{
				sendTCP( connectionsArr.get( i ).getID(), key, value );
			}
		}
	}
	
	private class ThreadRunner 
	{
		@SuppressWarnings( "unused" )
		public int framesPerSecond;
		Thread t;
		long lastFrame;
		long targetNanosPerTick;
		
		public ThreadRunner( int framesPerSecond ) 
		{
			this.framesPerSecond = framesPerSecond;
			targetNanosPerTick = 1000000000 / framesPerSecond;
		}
		
		public void start( Updateable u ) 
		{
			t = new Thread(()->{
				lastFrame = System.nanoTime();
				while( running ) {
					long now = System.nanoTime();
					long deltaTime = now - lastFrame;
					lastFrame = now;
					float dt = deltaTime / 1000000000.f;
					
					//This catches errors in user code, so we can try to keep plodding along
					//TODO: May be incorrect to do this.
					try
					{
						processMessages();
					}
					catch( Exception e )
					{
						e.printStackTrace();
					}
					
					u.update( dt );
					
					long remaining = targetNanosPerTick - (System.nanoTime() - now);
					if( remaining < 0 ) continue;
					try
					{
						Thread.sleep( remaining / 1000000 );
					}
					catch( Exception e )
					{
						e.printStackTrace();
					}
				}
			});
			t.setName( "DServer" );
			t.start();
		}
	}
	
	public interface Updateable
	{
		public void update( float dt );
	}
	
	public static class MessagePacket implements Poolable
	{
		int destination;
		Message m;
		
		public MessagePacket()
		{
			destination = -1;
			m = new Message();
		}
		
		public void init( int destination, Object key, Object value )
		{
			this.destination = destination;
			m.key = key;
			m.value = value;
		}
		
		public void reset()
		{
			m.key = null;
			m.value = null;
			this.destination = -1;
		}
	}
	
	//Key Callback helpers
	
	public interface ServerMessageListener<E>
	{
		public void receive( int id, E e );
	}
	
	@SuppressWarnings( { "unchecked" } )
	public void processMessages()
	{
		while( hasMessages() )
		{
			Message m = messages.removeFirst();
			listeners.get( state ).call( m.key, l -> {
				l.receive( m.sender, m.value );
			});
			globalListeners.call( m.key, l -> {
				l.receive( m.sender, m.value );
			});
		}
	}
	
	public <E> void on( Object key, ServerMessageListener<E> listener ) 
	{
		globalListeners.on( key, listener );
	}
	
	public <E> void on( Object state, Object key, ServerMessageListener<E> listener ) 
	{
		if( state == null )
		{
			on( key, listener );
		}
		else
		{
			ListenerManager<ServerMessageListener> lm = listeners.get( state );
			if( lm == null )
			{
				lm = new ListenerManager<>();
				listeners.put( state, lm );
			}
			lm.on( key, listener );
		}
	}
	
	public void setState( Object o )
	{
		state = o;
		server.sendToAllTCP( new Message( SET_STATE, o ) );
	}
	
	public void clearListeners()
	{
		listeners.clear();
	}
	
	@SuppressWarnings( "rawtypes" )
	public void register( Class...classes )
	{
		for( Class cToR : classes ) 
		{
			server.getKryo().register( cToR );
		}
	}
	
	//Kryonet Server Listeners	
	public void received( Connection c, Object o ) 
	{
		if( o instanceof Message )
		{
			Message m = (Message)o;
			m.sender = c.getID();
			messages.addLast( m );
		}
	}
	
	public void connected( Connection c )
	{
		Message m = new Message();
		m.key = CONNECTED;
		m.value = CONNECTED;
		m.sender = c.getID();
		messages.push( m );
		
		synchronized( connectionsArr )
		{
			if( connections.get( c.getID() ) == null )
			{
				connections.put( c.getID(), c );
				connectionsArr.add( c );
			}
		}
		
		sendTCP( c.getID(), SET_STATE, state );
	}
	
	public void disconnected( Connection c )
	{
		synchronized( connectionsArr )
		{
			Message m = new Message();
			m.key = DISCONNECTED;
			m.value = DISCONNECTED;
			m.sender = c.getID();
			messages.push( m );
			connections.remove( c.getID() );
			connectionsArr.remove( c );
		}
	}
	
	public class MessageSender implements Runnable
	{
		public void run()
		{
			while( running )
			{
				Iterator<MessagePacket> i = messagesToSend.iterator();
				while( i.hasNext() )
				{
					MessagePacket packet = i.next();
					synchronized( connections )
					{
						Connection c = connections.get( packet.destination );
						if( c == null )
						{
							i.remove();
							continue;
						}
						int bytes = 0;
						
						try 
						{
							bytes = c.getTcpWriteBufferSize();
						}
						catch( NullPointerException e ){}
						
						if( bytes < WRITE_BUFFER - OBJECT_BUFFER ) 
						{
							c.sendTCP( packet.m );
							i.remove();
							mpPool.free( packet );
						}
					}
				}
				
				try
				{
					Thread.sleep( 10 );
				}
				catch( InterruptedException e )
				{
					e.printStackTrace();
				}
			}
		}
	}

	public void stop()
	{
		server.stop();
		running = false;
	}
}
