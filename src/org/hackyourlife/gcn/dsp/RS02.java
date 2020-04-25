package org.hackyourlife.gcn.dsp;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RS02 implements Stream {
	long sample_count;
	long nibble_count;
	long sample_rate;
	int format;
	int loop_flag;
	long loop_start_offset;
	long loop_end_offset;
	long channel_count;
	int coef[][]; /* really 8x2 */

	RandomAccessFile file;
	long startoffset;
	long filepos;
	long filesize;

	long current_sample;
	long current_byte;

	ADPCMDecoder decoder[];

	public RS02(RandomAccessFile file) throws FileFormatException, IOException {
		this.file = file;
		this.filesize = file.length();
		if(!readHeader()) {
			throw new FileFormatException("not a RS02 file");
		}
		reset();
	}

	public final static int unsigned2signed16bit(int x) {
		int sign = x & (1 << 15);
		int value = x & ~(1 << 15);
		return (sign != 0) ? -(~x & 0xFFFF) : value;
	}


	public boolean read_dsp_header(byte[] header, RandomAccessFile in) throws IOException {
		channel_count = endianess.get_32bitBE(header, 0x00);
		if(channel_count != 2) {
			return(false);
		}

		sample_count = endianess.get_32bitBE(header, 0x04);
		sample_rate = endianess.get_32bitBE(header, 0x08);
		nibble_count = endianess.get_32bitBE(header, 0x0C);
		loop_flag = endianess.get_16bitBE(header, 0x10);
		format = endianess.get_16bitBE(header, 0x12);
		loop_start_offset = endianess.get_32bitBE(header, 0x14) / 2 * channel_count;
		loop_end_offset = endianess.get_32bitBE(header, 0x18) / 2 * channel_count;

		coef = new int[(int) channel_count][16];
		byte[] buf = new byte[(int) (0x20 * channel_count)];
		in.read(buf);
		for(int c = 0; c < channel_count; c++) {
			for(int i = 0; i < 16; i++) {
				coef[c][i] = unsigned2signed16bit(endianess.get_16bitBE(buf, (c * 0x20) + i * 2));
			}
		}

		return(true);
	}

	@Override
	public long getSampleRate() {
		return(sample_rate);
	}

	@Override
	public int getChannels() {
		return((int)channel_count);
	}

	public long getPreferedBufferSize() {
		return(getChannels() * (long) (channel_count * 14.0));
	}

	private boolean readHeader() throws IOException {
		seek(0);

		byte[] header = new byte[0x1C];
		file.read(header);
		if(!read_dsp_header(header, file)) {
			return(false);
		}

		startoffset = 0x60;

		decoder = new ADPCMDecoder[(int) channel_count];
		for(int i = 0; i < channel_count; i++) {
			decoder[i] = new ADPCMDecoder();
			decoder[i].setCoef(coef[i]);
			decoder[i].setHistory(0, 0);
		}

		filepos = startoffset;
		current_sample = 0;
		current_byte = 0;

		return(true);
	}

	@Override
	public void close() throws IOException {
		file.close();
	}

	@Override
	public boolean hasMoreData() {
		return((loop_flag != 0) || (filepos < filesize));
	}

	private void seek(long pos) throws IOException {
		file.seek(startoffset + pos);
		filepos = startoffset + pos;
	}

	public void reset() throws IOException {
		seek(0);
		for(int i = 0; i < channel_count; i++) {
			decoder[i].setHistory(0, 0);
		}
	}

	private int[] doDecode() throws IOException {
		int[] samples = new int[(int) (14 * channel_count)];
		byte[] rawdata = new byte[(int) (8 * channel_count)];
		filepos += file.read(rawdata);
		for(int ch = 0; ch < channel_count; ch++) {
			int[] buf = decoder[ch].decode_ngc_dsp_sub(0, 0,
					(int) (rawdata.length / 8.0 / channel_count * 14.0), ch,
					(int) channel_count, rawdata);
			for(int x = 0; x < buf.length; x++) {
				samples[(int) (x * channel_count + ch)] = buf[x];
			}
		}
		current_sample += samples.length;
		if((loop_flag != 0) && ((filepos - startoffset) >= loop_end_offset)) {
			filepos = startoffset + (loop_start_offset / 8) * 8;
			seek(filepos);
		}
		return(samples);
	}

	@Override
	public byte[] decode() throws Exception {
		int[] samples = doDecode();
		byte[] buffer = new byte[samples.length * 2];
		for(int i = 0; i < samples.length; i++)
			endianess.set16bit_BE(samples[i], buffer, i * 2);
		return(buffer);
	}

	public short[] decode16() throws Exception {
		int[] samples = doDecode();
		short[] buffer = new short[samples.length];
		for(int i = 0; i < samples.length; i++) {
			buffer[i] = (short) samples[i];
		}
		return(buffer);
	}

	@Override
	public String toString() {
		return("RS02[" + sample_rate + "Hz,16bit," + sample_count + " samples,loop:" +
				((loop_flag != 0) ? "yes" : "no") + "," + channel_count + "ch]");
	}
}
