import java.io.RandomAccessFile;
import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import org.hackyourlife.gcn.dsp.BFSTM;
import org.hackyourlife.gcn.dsp.BRSTM;
import org.hackyourlife.gcn.dsp.RS02;
import org.hackyourlife.gcn.dsp.RS03;
import org.hackyourlife.gcn.dsp.DSP;
import org.hackyourlife.gcn.dsp.Stream;
import org.hackyourlife.gcn.dsp.AsyncDecoder;
import org.hackyourlife.gcn.dsp.FileFormatException;

public class player {
	public static void main(String[] args) throws Exception {
		int track = -1;
		if(args.length < 1) {
			System.err.println("Usage: player FILE [track]");
			System.exit(1);
		}
		if(args.length > 1) {
			try {
				track = Integer.parseInt(args[1]);
			} catch(NumberFormatException e) {
				System.err.println("Invalid number: " + args[1]);
				System.exit(1);
			}
		}
		try {
			String filename = args[0];
			String filenameLeft = null;
			String filenameRight = null;
			int lext = filename.lastIndexOf('.');
			if(lext > 1) {
				char[] data = filename.toCharArray();
				char c = data[lext - 1];
				if(c == 'L') {
					data[lext - 1] = 'R';
					filenameLeft = filename;
					filenameRight = new String(data);
				} else if(c == 'R') {
					data[lext - 1] = 'L';
					filenameLeft = new String(data);
					filenameRight = filename;
				}
			}
			RandomAccessFile file = new RandomAccessFile(filename, "r");
			Stream stream = null;
			try {
				stream = new BRSTM(file);
			} catch(FileFormatException e) {
				try {
					stream = new BFSTM(file);
				} catch(FileFormatException ex) {
					try {
						stream = new RS03(file);
					} catch(FileFormatException exc) {
						if(filenameLeft != null
								&& new File(filenameLeft).exists()
								&& new File(filenameRight).exists()) {
							file.close();
							RandomAccessFile left = new RandomAccessFile(filenameLeft, "r");
							RandomAccessFile right = new RandomAccessFile(filenameRight, "r");
							try {
								stream = new DSP(left, right);
							} catch(FileFormatException exce) {
								left.close();
								right.close();
								file = new RandomAccessFile(filename, "r");
								stream = new DSP(file);
							}
						} else {
							try {
								stream = new RS02(file);
							} catch(FileFormatException exce) {
								stream = new DSP(file);
							}
						}
					}
				}
			}
			System.out.printf("%d Channels, %d Hz\n", stream.getChannels(), stream.getSampleRate());
			AsyncDecoder decoder = new AsyncDecoder(stream);
			decoder.start();
			play(decoder, track);
			stream.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static byte[] sum(byte[] data, int channels, int track) {
		if(channels == 1 || channels == 2) {
			return data;
		}
		int samples = data.length / (channels * 2);
		byte[] result = new byte[samples * 4]; // 2 channels, 16bit

		// extract single (stereo) track?
		if(track != -1) {
			int ch = track * 2;
			for(int i = 0; i < samples; i++) {
				int lidx = (i * channels + ch) * 2;
				int ridx = (i * channels + ch + 1) * 2;
				result[i * 4    ] = data[lidx];
				result[i * 4 + 1] = data[lidx + 1];
				result[i * 4 + 2] = data[ridx];
				result[i * 4 + 3] = data[ridx + 1];
			}
			return result;
		}

		// sum up all channels
		for(int i = 0; i < samples; i++) {
			int l = 0;
			int r = 0;
			for(int ch = 0; ch < channels; ch++) {
				int idx = (i * channels + ch) * 2;
				short val = (short) (Byte.toUnsignedInt(data[idx]) << 8 | Byte.toUnsignedInt(data[idx + 1]));
				if((ch & 1) == 0) {
					l += val;
				} else {
					r += val;
				}
			}
			// clamp
			if(l < -32768) {
				l = -32768;
			} else if(l > 32767) {
				l = 32767;
			}
			if(r < -32768) {
				r = -32768;
			} else if(r > 32767) {
				r = 32767;
			}
			// write back
			result[i * 4] = (byte) (l >> 8);
			result[i * 4 + 1] = (byte) l;
			result[i * 4 + 2] = (byte) (r >> 8);
			result[i * 4 + 3] = (byte) r;
		}
		return result;
	}

	private static void play(Stream stream, int track) throws Exception {
		int channels = stream.getChannels();
		if(channels > 2) {
			channels = 2;
		}
		AudioFormat format = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,	// encoding
				stream.getSampleRate(),			// sample rate
				16,					// bit/sample
				channels,				// channels
				2 * channels,
				stream.getSampleRate(),
				true					// big-endian
		);

		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if(!AudioSystem.isLineSupported(info)) {
			throw new Exception("Line matching " + info + " not supported");
		}

		SourceDataLine waveout;
		waveout = (SourceDataLine) AudioSystem.getLine(info);
		waveout.open(format, 16384);

		waveout.start();
		while(stream.hasMoreData()) {
			byte[] buffer = stream.decode();
			buffer = sum(buffer, stream.getChannels(), track);
			waveout.write(buffer, 0, buffer.length);
		}
		waveout.stop();
	}
}
