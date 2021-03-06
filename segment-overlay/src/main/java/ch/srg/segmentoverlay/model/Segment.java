package ch.srg.segmentoverlay.model;

import android.support.annotation.NonNull;

/**
 * Created by zapek on 2014-05-07.
 */
public class Segment implements Comparable<Segment> {

	/** Target mediaIdentifier of the media to be played. */
	private String mediaIdentifier;
	/** Unique identifier for this particular segment, not used for playing. */
	private String identifier;
	private String title;
	private String description;
	private String imageUrl;
	private String blockingReason;
	private long markIn;
	private long markOut;
	private long duration;
	private int progress;
	private boolean isCurrent;
	private long publishedTimestamp;
	private boolean displayable;
	private boolean fullLength;

	public Segment(String mediaIdentifier, String identifier, String title, String description, String imageUrl,
				   String blocking, long markIn, long markOut, long duration, long publishedTimestamp,
				   boolean displayable, boolean fullLength) {
		this.mediaIdentifier = mediaIdentifier;
		this.identifier = identifier;
		this.title = title;
		this.description = description;
		this.imageUrl = imageUrl;
		this.markIn = markIn;
		this.markOut = markOut;
		this.duration = duration;
		this.publishedTimestamp = publishedTimestamp;
		this.displayable = displayable;
		this.fullLength = fullLength;
		blockingReason = blocking;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public long getMarkIn() {
		return markIn;
	}

	public long getMarkOut() {
		return markOut;
	}

	public long getDuration() {
		return duration;
	}

	public void setProgress(int value) {
		progress = value;
	}

	public int getProgress() {
		return progress;
	}

	public void setIsCurrent(boolean value) {
		isCurrent = value;
	}

	public boolean isCurrent() {
		return isCurrent;
	}

	public long getPublishedTimestamp() {
		return publishedTimestamp;
	}

	public String getBlockingReason() {
		return blockingReason;
	}

	public boolean isBlocked() {
		return blockingReason != null;
	}

	public String getIdentifier() {
		return identifier;
	}

	public boolean isDisplayable() {
		return displayable;
	}

	public String getMediaIdentifier() {
		return mediaIdentifier;
	}

	@Override
	public int compareTo(@NonNull Segment another) {
		return ((int) (markIn - another.getMarkIn()));
	}

	@Override
	public String toString() {
		return "Segment{" +
				"mediaIdentifier='" + mediaIdentifier + '\'' +
				", identifier='" + identifier + '\'' +
				", title='" + title + '\'' +
				", description='" + description + '\'' +
				", imageUrl='" + imageUrl + '\'' +
				", blockingReason='" + blockingReason + '\'' +
				", markIn=" + markIn +
				", markOut=" + markOut +
				", duration=" + duration +
				", progress=" + progress +
				", isCurrent=" + isCurrent +
				", publishedTimestamp=" + publishedTimestamp +
				", displayable=" + displayable +
				", fullLength=" + fullLength +
				'}';
	}

	public boolean isFullLength() {
		return fullLength;
	}
}
