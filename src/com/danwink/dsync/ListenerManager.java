package com.danwink.dsync;

import java.util.ArrayList;
import java.util.HashMap;

public class ListenerManager<E>
{
	HashMap<Object, ArrayList<E>> listeners = new HashMap<>();
	
	public ListenerManager()
	{
		
	}
	
	public void on( Object key, E e )
	{
		ArrayList<E> listenerList = this.listeners.get( key );
		if( listenerList == null ) 
		{
			listenerList = new ArrayList<E>();
			listeners.put( key, listenerList );
		}
		listenerList.add( e );
	}
	
	public void call( Object key, ListenerCaller<E> caller )
	{
		ArrayList<E> callbackList = this.listeners.get( key );
		if( callbackList != null ) 
		{
			callbackList.forEach( c -> {
				caller.call( c );
			});
		}
	}
	
	public interface ListenerCaller<E>
	{
		public void call( E e );
	}

	public void clear()
	{
		listeners.clear();
	}
}
