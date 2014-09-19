import java.io.*;
import javax.sound.sampled.*;
import org.hackyourlife.gcn.dsp.RS03;

public class player03 {
	private static void playDSPFile(String name)
	    throws Exception {
		RS03 dspfile = new RS03();
		if(!dspfile.open(name)) {
			System.out.println("Error reading file!");
			return;
		}
		System.out.println("RS03 File: " + dspfile);
		// Open stream
		AudioFormat format = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED,	// encoding
			dspfile.getSampleRate(),		// sample rate
			16,					// bit/sample
			dspfile.getChannels(),			// channels
			2 * dspfile.getChannels(),
			dspfile.getSampleRate(),
			true					// big-endian
		);

		// define the required attributes for our line, 
		// and make sure a compatible line is supported.
		DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
		if(!AudioSystem.isLineSupported(info)) {
			System.err.println("Line matching " + info + " not supported.");
			return;
		}

		// get and open the source data line for playback.
		SourceDataLine waveout;
		try {
			waveout = (SourceDataLine) AudioSystem.getLine(info);
			waveout.open(format, 16384);
		} catch (LineUnavailableException ex) { 
			System.err.println("Unable to open the line: " + ex);
			return;
		}

		waveout.start();
		while(true) {
			if(!dspfile.hasMoreData())
				break;
			byte[] buffer = dspfile.decode();
			waveout.write(buffer, 0, buffer.length);
		}
		waveout.stop();
	}
	
	public static void main(String[] args) {
		if(args.length < 1) {
			System.err.println("Usage: player FILE");
			System.exit(1);
		}
		try {
			playDSPFile(args[0]);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		System.exit(0);
	}
}
