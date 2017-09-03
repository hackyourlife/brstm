package org.hackyourlife.gcn.dsp;

import java.io.RandomAccessFile;
import java.io.IOException;

public class BFSTM implements Stream {
	long	sample_count;
	long	nibble_count;
	long	sample_rate;
	int	loop_flag;
	long	loop_start_sample;
	long	loop_end_sample;
	long	loop_start_offset;
	long	loop_end_offset;
	int	channel_count;
	int	coef[][];

	RandomAccessFile file;
	long	start_offset;
	long	filepos;
	long	filesize;

	long	channel_start_offset[];
	long	channel_offset[];

	long	current_sample;
	long	current_byte;

	long	interleave_block_size;
	long	interleave_smallblock_size;

	int	codec;

	ADPCMDecoder decoder[];

	public final static int CODEC_PCM8 = 0;
	public final static int CODEC_PCM16BE = 1;
	public final static int CODEC_ADPCM = 2;

	public BFSTM(RandomAccessFile file) throws FileFormatException, IOException {
		this.file = file;
		this.filesize = file.length();
		readHeader();
		reset();
	}

	public final static int unsigned2signed16bit(int x) {
		int sign = x & (1 << 15);
		int value = x & ~(1 << 15);
		return (sign != 0) ? -(~x & 0xFFFF) : value;
	}

	public int read_8bit(long offset) throws IOException {
		file.seek(offset);
		return file.read();
	}

	public int read_16bitBE(long offset) throws IOException {
		file.seek(offset);
		byte[] data = new byte[2];
		file.read(data);
		return endianess.get_16bitBE(data);
	}

	public long read_32bitBE(long offset) throws IOException {
		file.seek(offset);
		byte[] data = new byte[4];
		file.read(data);
		return endianess.get_32bitBE(data);
	}

	@Override
	public long getSampleRate() {
		return(sample_rate);
	}

	@Override
	public int getChannels() {
		return(channel_count);
	}

	public long getInterleaveSize() {
		return interleave_block_size;
	}

	public long getPreferedBufferSize() {
		return getChannels() * (long)(getInterleaveSize() / 8.0 * 14.0);
	}

	@Override
	public void close() throws Exception {
		file.close();
	}

	@Override
	public boolean hasMoreData() {
		return((loop_flag != 0) || (filepos < filesize));
	}

	private void seek(long pos) throws IOException {
		file.seek(start_offset + pos);
		filepos = start_offset + pos;
	}

	public void reset() throws IOException {
		seek(0);
		for(int i = 0; i < channel_count; i++)
			decoder[i].setHistory(0, 0);
	}

	private void readHeader() throws FileFormatException, IOException {
		boolean atlus_shrunken_head = false;
		if(read_32bitBE(0) != 0x4653544D) // "FSTM"
			throw new FileFormatException("not a bfstm file");

		if(read_16bitBE(4) != 0xFEFF)
			throw new FileFormatException("not a bfstm file");

		int section_count = read_16bitBE(0x10);
		long info_offset = -1;
		long info_size = -1;
		long seek_offset = -1;
		long seek_size = -1;
		long data_offset = -1;
		long data_size = -1;
		for(int i = 0; i < section_count; i++) {
			int id = read_16bitBE(0x14 + i * 0xC);
			switch(id) {
				case 0x4000:
					info_offset = read_32bitBE(0x18 + i * 0xC);
					info_size = read_32bitBE(0x1C + i * 0xC);
					break;
				case 0x4001:
					seek_offset = read_32bitBE(0x18 + i * 0xC);
					seek_size = read_32bitBE(0x1C + i * 0xC);
					break;
				case 0x4002:
					data_offset = read_32bitBE(0x18 + i * 0xC);
					data_size = read_32bitBE(0x1C + i * 0xC);
					break;
			}
		}

		if(read_32bitBE(info_offset) != 0x494E464F) // "INFO"
			throw new FileFormatException("not a bfstm file");

		int codec_number = read_8bit(info_offset + 0x20);
		this.loop_flag = read_8bit(info_offset + 0x21);
		this.channel_count = read_8bit(info_offset + 0x22);
		switch(codec_number) {
			case CODEC_ADPCM:
				break;
			case CODEC_PCM8:
			case CODEC_PCM16BE:
			default:
				throw new FileFormatException("unknown codec");
		}
		this.codec = codec_number;
		if(this.channel_count < 1) {
			throw new FileFormatException("no channel");
		}

		this.sample_count = read_32bitBE(info_offset + 0x2C);
		this.sample_rate = read_16bitBE(info_offset + 0x26);
		this.loop_start_sample = read_32bitBE(info_offset + 0x28);
		this.loop_end_sample = this.sample_count;
		this.loop_start_offset = (long)(this.loop_start_sample * 8.0 / 14.0);
		this.loop_end_offset = (long)(this.loop_end_sample * 8.0 / 14.0);

		this.interleave_block_size = read_32bitBE(info_offset + 0x34);
		this.interleave_smallblock_size = read_32bitBE(info_offset + 0x44);

		if(this.codec == CODEC_ADPCM) {
			long coef_ptr_table = read_32bitBE(info_offset + 0x1C) + info_offset + 8;

			this.coef = new int[this.channel_count][16];
			this.decoder = new ADPCMDecoder[this.channel_count];
			for(int j = 0; j < this.channel_count; j++) {
				long tmp = read_32bitBE(coef_ptr_table + 8 + j * 8);
				long coef_offset = tmp + coef_ptr_table;
				coef_offset += read_32bitBE(coef_offset + 4);
				for(int i = 0; i < 16; i++)
					this.coef[j][i] = unsigned2signed16bit(read_16bitBE(coef_offset + i * 2));
				this.decoder[j] = new ADPCMDecoder();
				this.decoder[j].setCoef(this.coef[j]);
				this.decoder[j].setHistory(0, 0);
			}
		}

		this.start_offset = data_offset + 0x20;

		this.channel_offset = new long[this.channel_count];
		this.channel_start_offset = new long[this.channel_count];
		for(int i = 0; i < this.channel_count; i++)
			this.channel_start_offset[i] = this.channel_offset[i] = this.start_offset + i * this.interleave_block_size;

		seek(0);
		current_sample = 0;
		current_byte = 0;
	}

	private int[] doDecode() throws Exception {
		long start_offset = current_byte;
		if((loop_flag != 0) && (current_byte >= loop_end_offset))
			start_offset = loop_start_offset;
		long blocks = start_offset / interleave_block_size;
		if((loop_flag != 0) && (current_byte >= loop_end_offset))
			current_byte = blocks * interleave_block_size;
		else
			current_byte = start_offset;
		seek(blocks * interleave_block_size * channel_count);

		start_offset = start_offset - (blocks * interleave_block_size);
		start_offset = (start_offset / 8) * 8;
		int startsample = (int)(start_offset / 8.0 * 14.0);

		long interleave = interleave_block_size;
		if((filesize - filepos) < (interleave_block_size * channel_count))
			interleave = interleave_smallblock_size;

		long end_offset = current_byte + interleave;
		if((loop_flag != 0) && ((current_byte + interleave) > loop_end_offset))
			end_offset = loop_end_offset;

		int endsample = (int)((end_offset - current_byte) / 8.0 * 14.0);

		byte[] rawdata = new byte[(int)(interleave * channel_count)];
		int samplecnt = endsample - startsample;
		int[] samples = new int[samplecnt * channel_count];
		int read = file.read(rawdata);
		filepos += read;
		current_byte += read / channel_count;
		for(int ch = 0; ch < channel_count; ch++) {
			for(int i = 0; i < samplecnt; i += 14) {
				int samplestodo = samplecnt - i;
				if(samplestodo > 14)
					samplestodo = 14;
				int[] buf = decoder[ch].decode_ngc_dsp((int)(interleave * ch), startsample + i, samplestodo, rawdata);
				for(int x = 0; x < buf.length; x++)
					samples[(i + x) * channel_count + ch] = buf[x];
			}
		}
		return samples;
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
		for(int i = 0; i < samples.length; i++)
			buffer[i] = (short) samples[i];
		return(buffer);
	}

	@Override
	public String toString() {
		return(new String("BFSTM[" + sample_rate + "Hz,16bit," + sample_count + " samples,loop:" + ((loop_flag != 0) ? "yes" : "no") + "," + channel_count + "ch]"));
	}
}
