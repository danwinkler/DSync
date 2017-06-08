package com.danwink.dsync;

import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.Listener;

public class FakeConnection extends Connection
{
	public int id = MathUtils.random( 50, Integer.MAX_VALUE );
	public Listener l;
	public Kryo k;
	
	public FakeConnection( Listener l, Kryo k )
	{
		this.l = l;
		this.k = k;
	}
	
	public int getID()
	{
		return id;
	}
	
	public int sendTCP( Object o )
	{
		if( o instanceof KeepAlive ) return 0;
		l.received( this, k.copy( o ) );
		return 0;
	}
}
