package com.danwink.dsync;

import java.util.concurrent.ConcurrentLinkedDeque;

import com.esotericsoftware.kryonet.Listener;

public class DEndPoint extends Listener
{
	public static final String CONNECTED = "net.CONNECTED";
	public static final String DISCONNECTED = "net.DISCONNECTED";
	
	public ConcurrentLinkedDeque<Message> messages = new  ConcurrentLinkedDeque<Message>();
	
	public Message getNextMessage()
	{	
		return messages.removeFirst();
	}
	
	public boolean hasMessages() 
	{
		return !messages.isEmpty();
	}
}
