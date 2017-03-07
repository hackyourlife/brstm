import java.io.RandomAccessFile;
import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import org.hackyourlife.gcn.dsp.BFSTM;
import org.hackyourlife.gcn.dsp.BRSTM;
import org.hackyourlife.gcn.dsp.RS03;
import org.hackyourlife.gcn.dsp.DSP;
import org.hackyourlife.gcn.dsp.Stream;
import org.hackyourlife.gcn.dsp.AsyncDecoder;
import org.hackyourlife.gcn.dsp.FileFormatException;

public class player {
	public static void main(String[] args) throws Exception {
		if(args.length < 1) {
			System.err.println("Usage: player FILE");
			System.exit(1);
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
						} else
							stream = new DSP(file);
					}
				}
			}
			System.out.printf("%d Channels, %d Hz\n", stream.getChannels(), stream.getSampleRate());
			AsyncDecoder decoder = new AsyncDecoder(stream);
			decoder.start();
			play(decoder);
			stream.close();
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void play(Stream stream) throws Exception {
		AudioFormat format = new AudioFormat(
				AudioFormat.Encoding.PCM_SIGNED,	// encoding
				stream.getSampleRate(),			// sample rate
				16,					// bit/sample
				stream.getChannels(),			// channels
				2 * stream.getChannels(),
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
		while(true) {
			if(!stream.hasMoreData())
				break;
			byte[] buffer = stream.decode();
			waveout.write(buffer, 0, buffer.length);
		}
		waveout.stop();
	}
}
