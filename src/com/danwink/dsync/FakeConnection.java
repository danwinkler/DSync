package com.danwink.dsync;

import java.util.Random;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.FrameworkMessage.KeepAlive;
import com.esotericsoftware.kryonet.Listener;

public class FakeConnection extends Connection
{
	public int id = (new Random()).nextInt( 10000000 ) + 100;
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
