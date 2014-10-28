import java.io.RandomAccessFile;
import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import org.hackyourlife.gcn.dsp.BRSTM;
import org.hackyourlife.gcn.dsp.Stream;
import org.hackyourlife.gcn.dsp.AsyncDecoder;

public class player {
	public static void main(String[] args) throws Exception {
		if(args.length < 1) {
			System.err.println("Usage: player FILE");
			System.exit(1);
		}
		try {
			RandomAccessFile file = new RandomAccessFile(args[0], "r");
			BRSTM rstm = new BRSTM(file);
			AsyncDecoder decoder = new AsyncDecoder(rstm);
			decoder.start();
			play(decoder);
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
