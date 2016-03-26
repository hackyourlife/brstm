package org.hackyourlife.gcn.dsp;

public final class endianess {
	public final static int get_16bitBE(byte buffer[]) {
		return(get_16bitBE(buffer, 0));
	}
	public final static int get_16bitBE(byte buffer[], int offset) {
		return(((buffer[offset + 0] & 0xFF) << 8) | (buffer[offset + 1] & 0xFF));
	}

	public final static long get_32bitBE(byte buffer[]) {
		return(get_32bitBE(buffer, 0));
	}
	public final static long get_32bitBE(byte buffer[], int offset) {
		return(
			((buffer[offset + 0] & 0xFF) << 24) |
			((buffer[offset + 1] & 0xFF) << 16) |
			((buffer[offset + 2] & 0xFF) << 8) |
			 (buffer[offset + 3] & 0xFF)
		);
	}

	public final static long get_64bitBE(byte buffer[]) {
		return(get_64bitBE(buffer, 0));
	}
	public final static long get_64bitBE(byte buffer[], int offset) {
		return(
			((buffer[offset + 0] & 0xFF) << 56) |
			((buffer[offset + 1] & 0xFF) << 48) |
			((buffer[offset + 2] & 0xFF) << 40) |
			((buffer[offset + 3] & 0xFF) << 32) |
			((buffer[offset + 4] & 0xFF) << 24) |
			((buffer[offset + 5] & 0xFF) << 16) |
			((buffer[offset + 6] & 0xFF) <<  8) |
			 (buffer[offset + 7] & 0xFF)
		);
	}

	public final static int get_16bitLE(byte buffer[]) {
		return(get_16bitLE(buffer, 0));
	}
	public final static int get_16bitLE(byte buffer[], int offset) {
		return((buffer[offset + 0] & 0xFF) | ((buffer[offset + 1] & 0xFF) << 8));
	}

	public final static long get_32bitLE(byte buffer[]) {
		return(get_32bitLE(buffer, 0));
	}
	public final static long get_32bitLE(byte buffer[], int offset) {
		return(
			 (buffer[offset + 0] & 0xFF) |
			((buffer[offset + 1] & 0xFF) << 8) |
			((buffer[offset + 2] & 0xFF) << 16) |
			((buffer[offset + 3] & 0xFF) << 24)
		);
	}

	public final static long get_64bitLE(byte buffer[]) {
		return(get_64bitLE(buffer, 0));
	}
	public final static long get_64bitLE(byte buffer[], int offset) {
		return(
			 (buffer[offset + 0] & 0xFF) |
			((buffer[offset + 1] & 0xFF) <<  8) |
			((buffer[offset + 2] & 0xFF) << 16) |
			((buffer[offset + 3] & 0xFF) << 24) |
			((buffer[offset + 4] & 0xFF) << 32) |
			((buffer[offset + 5] & 0xFF) << 40) |
			((buffer[offset + 6] & 0xFF) << 48) |
			((buffer[offset + 7] & 0xFF) << 56)
		);
	}

	public final static byte[] set16bit_BE(int num, byte buffer[]) {
		return(set16bit_BE(num, buffer, 0));
	}

	public final static byte[] set32bit_BE(int num, byte buffer[]) {
		return(set32bit_BE(num, buffer, 0));
	}

	public final static byte[] set16bit_BE(int num, byte buffer[], int offset) {
		buffer[offset + 0] = (byte) ((num >> 8) & 0xFF);
		buffer[offset + 1] = (byte) (num & 0xFF);
		return(buffer);
	}

	public final static byte[] set32bit_BE(int num, byte buffer[], int offset) {
		buffer[offset + 0] = (byte) ((num >> 24) & 0xFF);
		buffer[offset + 1] = (byte) ((num >> 16) & 0xFF);
		buffer[offset + 2] = (byte) ((num >>  8) & 0xFF);
		buffer[offset + 3] = (byte) (num & 0xFF);
		return(buffer);
	}
}
