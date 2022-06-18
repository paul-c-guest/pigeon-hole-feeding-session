import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;

class ClusteredList {

	private List<List<ClusteredFile>> fileClusters;
	private Integer position = null;
	private final static long FIVE_MINUTES = 300;

	public ClusteredList(File[] input) {

		List<ClusteredFile> files = new ArrayList<>();
		Metadata data;
		ExifIFD0Directory exif;
		Date date;

		for (File file : input) {
			try {
				data = JpegMetadataReader.readMetadata(file);
				exif = data.getFirstDirectoryOfType(ExifIFD0Directory.class);
				date = exif.getDate(ExifIFD0Directory.TAG_DATETIME);
				
//				System.out.println(exif.getTags().toString());

				files.add(new ClusteredFile(file, date.toInstant()));

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

			long current = files.get(i).time.getEpochSecond();
			long next = files.get(i + 1).time.getEpochSecond();

			if (next - current > FIVE_MINUTES) {
				files.get(i).clusterEnd = true;
			}
		}

		// convert list to 2d array
		fileClusters = new ArrayList<List<ClusteredFile>>();

		List<ClusteredFile> cluster = new ArrayList<ClusteredFile>();

		for (int i = 0; i < files.size(); i++) {
			ClusteredFile current = files.get(i);
			cluster.add(current);

			if (current.clusterEnd) {
				fileClusters.add(cluster);
				cluster = new ArrayList<ClusteredFile>();
			}
		}
	}

	public List<ClusteredFile> getNext() {
		return fileClusters.get(next());
	}

	public List<ClusteredFile> getPrevious() {
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

	public void printStructure() {
		for (List<ClusteredFile> cluster : fileClusters) {
			for (ClusteredFile file : cluster) {
				System.out.print(file.file.getName() + " ");
			}
			System.out.println();
		}
	}

	public String getClusterData() {
		List<ClusteredFile> cluster = fileClusters.get(position);
		String firstNumber = getNumberFromString(cluster.get(0).file.getName());
		String firstTime = processTime(cluster.get(0).time.toString());
		
		StringBuilder sb = cluster.size() == 1 
				? new StringBuilder()
					.append("[ " + firstNumber + " at " + firstTime + " ]")
				: new StringBuilder()
				.append("[ first image ")
				.append(firstNumber)
				.append(" at ")
				.append(firstTime)
				.append(" ] [ last image ")
				.append(getNumberFromString(cluster.get(cluster.size() - 1).file.getName()))
				.append(" at ")
				.append(processTime(cluster.get(cluster.size() - 1).time.toString()))
				.append(" ]");
		
		return sb.toString();
	}

	private String getNumberFromString(String name) {
		return name.replaceAll("[^0-9]+", "");
	}

	private String processTime(String time) {
		return time.substring(11, 16);
	}

	class ClusteredFile implements Comparable<ClusteredFile> {
		File file;
		Instant time;
		boolean clusterEnd = false;

		public ClusteredFile(File file, Instant time) {
			this.file = file;
			this.time = time;
		}

		@Override
		public String toString() {
			return file.getName() + (clusterEnd ? " cluster end" : "");
		}

		@Override
		public int compareTo(ClusteredFile other) {
			return (int) (this.time.getEpochSecond() - other.time.getEpochSecond());
		}
	}
}
