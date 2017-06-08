package com.danwink.dsync;

import java.io.IOException;

import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;

public class FakeClient extends DClient
{
	DServer server;
	FakeConnection conn;

	public FakeClient( DServer server )
	{
		super();
		
		this.server = server;
		conn = new FakeConnection( this, c.getKryo() );
	}
	
	@Override
	public void connect( String address, int tcpPort, int udpPort ) throws IOException 
	{
		//Manually trigger methods on server and client
		server.connected( conn );
		connected( null );
	}
	
	@Override
	public void sendTCP( Object key, Object value ) 
	{
		server.messages.addLast( new Message( key, value, conn.id ) );
	}
	
	@Override
	public void sendTCP( Object key ) 
	{
		if( key instanceof KeepAlive ) return;
		sendTCP( key, null );
	} 
}
