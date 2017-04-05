package org.hackyourlife.gcn.dsp;

public class AsyncDecoder extends Thread implements Stream {
	private Stream stream;
	private byte[] data = null;
	private boolean moreData = false;
	private boolean closed = false;

	public AsyncDecoder(Stream stream) {
		this.stream = stream;
		this.moreData = stream.hasMoreData();
	}

	@Override
	public synchronized boolean hasMoreData() {
		return moreData || data != null;
	}

	@Override
	public synchronized byte[] decode() throws Exception {
		if(!hasMoreData()) {
			return null;
		}
		while(data == null) {
			wait();
		}
		byte[] tmp = data;
		data = null;
		notify();
		return tmp;
	}

	@Override
	public int getChannels() {
		return stream.getChannels();
	}

	@Override
	public long getSampleRate() {
		return stream.getSampleRate();
	}

	@Override
	public void close() throws Exception {
		closed = true;
		interrupt();
		stream.close();
	}

	public void run() {
		while(!closed && stream.hasMoreData()) {
			try {
				synchronized(this) {
					if(data != null) {
						wait();
					}
					data = stream.decode();
					moreData = stream.hasMoreData();
					notifyAll();
				}
			} catch(InterruptedException e) {
			} catch(Exception e) {
				e.printStackTrace();
				return;
			}
		}
	}
}
