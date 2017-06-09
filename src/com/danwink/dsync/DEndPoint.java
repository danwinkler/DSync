package com.danwink.dsync;

import java.util.concurrent.ConcurrentLinkedDeque;

import com.esotericsoftware.kryonet.Listener;

public class DEndPoint extends Listener
{
	public static final String CONNECTED = "dsync.CONNECTED";
	public static final String DISCONNECTED = "dsync.DISCONNECTED";
	
	public static final String DEFAULT_STATE = "dsync.DEFAULT";
	public static final String SET_STATE = "dsync.SET_STATE";
	
	public ConcurrentLinkedDeque<Message> messages = new ConcurrentLinkedDeque<Message>();
	public Object state = DEFAULT_STATE;
	
	public Message getNextMessage()
	{	
		return messages.removeFirst();
	}
	
	public boolean hasMessages() 
	{
		return !messages.isEmpty();
	}
}
