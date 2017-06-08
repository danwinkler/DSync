package com.danwink.dsync;

public interface PartialUpdatable<E>
{
	public void partialReadPacket( E e );
	public E partialMakePacket();
}
