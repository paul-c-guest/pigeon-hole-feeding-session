import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

public class ClusteredList {

	private List<List<File>> fileClusters;
	private Integer position = null;
	private final static long FIVE_MINUTES = 300;

	public ClusteredList(File[] input) {

		List<ClusteredFile> files = new ArrayList<>();

		for (File file : input) {

			try {
				Metadata data = JpegMetadataReader.readMetadata(file);
				ExifIFD0Directory exif = data.getFirstDirectoryOfType(ExifIFD0Directory.class);
				Date date = exif.getDate(ExifIFD0Directory.TAG_DATETIME);

				files.add(new ClusteredFile(file, date.toInstant().getEpochSecond()));

			} catch (JpegProcessingException e) {
				continue;

			} catch (IOException e) {
				continue;
//				e.printStackTrace();
			}

		}

		// ClusteredFile implements Comparable
		Collections.sort(files);

		int limit = files.size();

		// set boundary flags for appropriate entries
		for (int i = 0; i < limit; i++) {

			// final entry is always a boundary
			if (i == limit - 1) {
				files.get(i).clusterEnd = true;
				continue;
			}

			long current = files.get(i).time;
			long next = files.get(i + 1).time;

			if (next - current > FIVE_MINUTES) {
				files.get(i).clusterEnd = true;
			}

		}

		// convert list to 2d array
		fileClusters = new ArrayList<List<File>>();

		List<File> cluster = new ArrayList<File>();

		for (int i = 0; i < files.size(); i++) {

			ClusteredFile current = files.get(i);
			cluster.add(current.file);

			if (current.clusterEnd) {
				fileClusters.add(cluster);
				cluster = new ArrayList<File>();
			}
		}
	}

	public List<File> getNext() {
		return fileClusters.get(next());
	}
	
	public List<File> getPrevious() {
		return fileClusters.get(previous());
	}
	
	public int size() {
		return fileClusters.size();
	}
	
	private int next() {
		if (position == null) {
			position = 0;
			return position;
		}
		position = (position + 1) % size();
		return position;
	}
	
	private int previous() {
		if (position == null) {
			position = 0;
		}
		// guard against modulo of negative numbers
		if (position - 1 < 0) {
			position += fileClusters.size();
		}
		position = (position - 1) % size();
		return position;
	}

	public void print() {

		for (List<File> cluster : fileClusters) {
			for (File file : cluster) {
				System.out.print(file.getName() + " ");
			}
			System.out.println();
		}

	}

	private class ClusteredFile implements Comparable<ClusteredFile> {
		File file;
		Long time;
		boolean clusterEnd = false;

		public ClusteredFile(File file, Long time) {
			this.file = file;
			this.time = time;
		}

		@Override
		public String toString() {
			return file.getName() + (clusterEnd ? " cluster end" : "");
		}

		@Override
		public int compareTo(ClusteredFile other) {
			return (int) (this.time - other.time);
		}
	}

}
