package org.hackyourlife.gcn.dsp;

public interface Stream {
	public boolean hasMoreData();
	public byte[] decode() throws Exception;
	public int getChannels();
	public long getSampleRate();
	public void close() throws Exception;
}
