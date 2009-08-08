/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package plugins.Library.serial;

import plugins.Library.serial.Serialiser.*;
import plugins.Library.serial.TaskAbortException;
import plugins.Library.serial.Packer;
import plugins.Library.io.ObjectStreamReader;
import plugins.Library.io.ObjectStreamWriter;

import freenet.keys.FreenetURI;

import java.util.Map;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileLock;

/**
** Converts between a map of {@link String} to {@link Object}, and a file on
** disk. An {@link ObjectStreamReader} and an {@link ObjectStreamWriter} is
** used to do the hard work once the relevant streams have been established.
**
** This class expects {@link Task#meta} to be of type {@link String}, or an
** array whose first element is of type {@link String}.
**
** @see Yaml
** @see Loader
** @see Dumper
** @author infinity0
*/
public class FileArchiver<T extends Map<String, Object>>
implements Archiver<T>,
           LiveArchiver<T, SimpleProgress> {

	// DEBUG
	private static boolean testmode = false;
	public static void setTestMode() { System.out.println("FileArchiver will now randomly pause 5-10s for each task, to simulate network speeds"); testmode = true; }
	public static void randomWait(SimpleProgress p) {
		int t = (int)(Math.random()*5+5);
		p.addTotal(t, true);
		for (int i=0; i<t; ++i) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// nothing
			} finally {
				p.addPartDone();
			}
		}
	}

	/**
	** DEBUG: whether to generate random file names
	*/
	final protected boolean random;

	/**
	** Prefix of filename.
	*/
	final protected String prefix;

	/**
	** Suffix of filename.
	*/
	final protected String suffix;

	/**
	** Extension of filename (no leading dot).
	*/
	final protected String extension;

	final protected ObjectStreamReader reader;
	final protected ObjectStreamWriter writer;

	public <S extends ObjectStreamReader & ObjectStreamWriter> FileArchiver(S rw, String pre, String suf, String ext) {
		this(rw, rw, pre, suf, ext);
	}

	public <S extends ObjectStreamReader & ObjectStreamWriter> FileArchiver(S rw, boolean rnd, String ext) {
		this(rw, rw, rnd, ext);
	}

	public FileArchiver(ObjectStreamReader r, ObjectStreamWriter w, String pre, String suf, String ext) {
		reader = r;
		writer = w;
		prefix = (pre == null)? "": pre;
		suffix = (suf == null)? "": suf;
		extension = (ext == null)? "": ext;
		random = false;
	}

	public FileArchiver(ObjectStreamReader r, ObjectStreamWriter w, boolean rnd, String ext) {
		reader = r;
		writer = w;
		suffix = prefix = "";
		extension = (ext == null)? "": ext;
		random = rnd;
	}

	protected String[] getFileParts(Object meta) {
		String[] m = new String[]{"", ""};
		if (meta instanceof String) {
			m[0] = (String)(meta);
		} else if (meta instanceof Object[]) {
			Object[] arr = (Object[])meta;
			if (arr.length > 0 && arr[0] instanceof String) {
				m[0] = (String)arr[0];
				if (arr.length > 1) {
					StringBuilder str = new StringBuilder(arr[1].toString());
					for (int i=2; i<arr.length; ++i) {
						str.append('.').append(arr[i].toString());
					}
					m[1] = str.toString();
				}
			} else {
				throw new IllegalArgumentException("FileArchiver does not support such metadata: " + java.util.Arrays.deepToString(arr));
			}
		} else if (meta != null) {
			throw new IllegalArgumentException("FileArchiver does not support such metadata: " + meta);
		}

		return m;
	}

	/*========================================================================
	  public interface LiveArchiver
	 ========================================================================*/

	@Override public void pull(PullTask<T> t) throws TaskAbortException {
		String[] s = getFileParts(t.meta);
		File file = new File(prefix + s[0] + suffix + s[1] + extension);
		try {
			FileInputStream is = new FileInputStream(file);
			try {
				FileLock lock = is.getChannel().lock(0L, Long.MAX_VALUE, true); // shared lock for reading
				try {
					t.data = (T)reader.readObject(is);
				} finally {
					lock.release();
				}
			} finally {
				try { is.close(); } catch (IOException f) { }
			}
		} catch (IOException e) {
			throw new TaskAbortException("FileArchiver could not complete pull on " + file, e, true);
		} catch (RuntimeException e) {
			throw new TaskAbortException("FileArchiver could not complete pull on " + file, e);
		}
	}

	@Override public void push(PushTask<T> t) throws TaskAbortException {
		if (random) { t.meta = java.util.UUID.randomUUID().toString(); }
		String[] s = getFileParts(t.meta);
		File file = new File(prefix + s[0] + suffix + s[1] + extension);
		try {
			FileOutputStream os = new FileOutputStream(file);
			try {
				FileLock lock = os.getChannel().lock();
				try {
					writer.writeObject(t.data, os);
				} finally {
					lock.release();
				}
			} finally {
				try { os.close(); } catch (IOException f) { }
			}
		} catch (IOException e) {
			throw new TaskAbortException("FileArchiver could not complete push on " + file, e, true);
		} catch (RuntimeException e) {
			throw new TaskAbortException("FileArchiver could not complete push on " + file, e);
		}
	}

	@Override public void pullLive(PullTask<T> t, SimpleProgress p) throws TaskAbortException {
		try {
			pull(t);
			if (testmode) { randomWait(p); }
			else { p.addTotal(0, true); }
		} catch (TaskAbortException e) {
			p.abort(e);
		}
	}

	@Override public void pushLive(PushTask<T> t, SimpleProgress p) throws TaskAbortException {
		try {
			push(t);
			if (testmode) { randomWait(p); }
			else { p.addTotal(0, true); }
		} catch (TaskAbortException e) {
			p.abort(e);
		}
	}

}