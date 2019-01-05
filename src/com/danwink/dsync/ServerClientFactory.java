package com.danwink.dsync;

public abstract class ServerClientFactory
{
	@SuppressWarnings( "rawtypes" )
	public abstract Class[] getClasses();
	
	public DServer server()
	{
		DServer server = new DServer();
		
		server.register( getClasses() );
		
		return server;
	}
	
	public DClient client()
	{
		DClient client = new DClient();
		
		client.register( getClasses() );
		
		return client;
	}
}
