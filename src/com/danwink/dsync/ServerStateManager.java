package com.danwink.dsync;

import java.util.HashMap;

import com.danwink.dsync.DServer.Updateable;

public class ServerStateManager implements Updateable
{
	HashMap<Object, ServerState> states;
	
	ServerState current;
	
	public ServerStateManager( DServer server )
	{
		states = new HashMap<>();
		
		server.onState( o -> {
			if( current != null ) 
			{
				current.hide();
			}
			
			current = states.get( o );
			
			if( current != null ) 
			{
				current.show();
			}
		});
	}

	public void update( float dt )
	{
		if( current != null )
		{
			current.update( dt );
		}
	}

	public void add( Object o, ServerState s )
	{
		states.put( o, s );
	}
}
