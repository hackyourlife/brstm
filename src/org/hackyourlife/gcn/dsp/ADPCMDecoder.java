package org.hackyourlife.gcn.dsp;

public class ADPCMDecoder {
	int adpcm_history1;
	int adpcm_history2;

	int[] adpcm_coef;

	public void setCoef(int[] coef) {
		this.adpcm_coef = coef;
	}

	public void setHistory(int h1, int h2) {
		adpcm_history1 = h1;
		adpcm_history2 = h2;
	}

	public static int nibble_to_int[] = {0,1,2,3,4,5,6,7,-8,-7,-6,-5,-4,-3,-2,-1};

	public static int get_high_nibble_signed(int n) {
		/*return(((n & 0x70) - (n & 0x80)) >> 4);*/
		return(nibble_to_int[n >> 4]);
	}

	public static int get_low_nibble_signed(int n) {
		/*return((n & 7) - (n & 8));*/
		return(nibble_to_int[n & 0xf]);
	}

	public static int clamp16(int val) {
		if(val > 32767)
			return(32767);
		if(val < -32768)
			return(-32768);
		return(val);
	}

	public final static int unsigned2signed16bit(int x) {
		int sign = x & (1 << 15);
		int value = x & ~(1 << 15);
		return (sign != 0) ? -(~x & 0xFFFF) : value;
	}

	public int[] decode_ngc_dsp(int offset, long first_sample, int samples_to_do, byte[] mem) {
		long i = first_sample;
		int sample_count;
		int channelspacing = 1;

		int framesin = (int)(first_sample / 14);

		int header = mem[offset + framesin * 8] & 0xFF;
		int scale = 1 << (header & 0xf);
		int coef_index = (header >> 4) & 0xf;
		int hist1 = adpcm_history1;
		int hist2 = adpcm_history2;
		int coef1 = adpcm_coef[coef_index*2];
		int coef2 = adpcm_coef[coef_index*2 + 1];

		first_sample = first_sample % 14;

		int[] samples = new int[samples_to_do * channelspacing];

		for(i = first_sample, sample_count = 0; i < (first_sample + samples_to_do); i++, sample_count += channelspacing) {
			int sample_byte = mem[offset + framesin*8 + 1 + (int) (i/2)] & 0xFF;

			samples[sample_count] = clamp16((
				(((((i & 1) != 0) ?
				   get_low_nibble_signed(sample_byte):
				   get_high_nibble_signed(sample_byte)
				   ) * scale) << 11) + 1024 +
				 (coef1 * hist1 + coef2 * hist2)) >> 11
				);

			hist2 = hist1;
			hist1 = samples[sample_count];
		}

		adpcm_history1 = hist1;
		adpcm_history2 = hist2;

		return(samples);
	}
}
