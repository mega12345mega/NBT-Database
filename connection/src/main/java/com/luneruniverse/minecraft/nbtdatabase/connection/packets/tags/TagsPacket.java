package com.luneruniverse.minecraft.nbtdatabase.connection.packets.tags;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.esotericsoftware.kryo.kryo5.serializers.FieldSerializer.NotNull;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class TagsPacket extends Packet {
	
	private @NotNull Tag[] tags;
	
	public TagsPacket(Tag[] tags) {
		this.tags = tags;
	}
	public TagsPacket(List<Tag> tags) {
		this(tags.toArray(new Tag[tags.size()]));
	}
	public TagsPacket(Tag tag) {
		this.tags = new Tag[tag == null ? 0 : 1];
		if (tag != null)
			tags[0] = tag;
	}
	public TagsPacket(Optional<Tag> tag) {
		this(tag.orElse(null));
	}
	TagsPacket() {
		// Deserialization
	}
	
	public Tag[] getTags() {
		return tags;
	}
	public List<Tag> getTagsList() {
		return Arrays.asList(tags);
	}
	public Tag getTagNonNull() throws IllegalStateException {
		if (tags.length != 1)
			throw new IllegalStateException("Expected exactly 1 tag");
		return tags[0];
	}
	public Tag getTagNullable() throws IllegalStateException {
		if (tags.length > 1)
			throw new IllegalStateException("Expected exactly 0 or 1 tag");
		if (tags.length == 0)
			return null;
		return tags[0];
	}
	public Optional<Tag> getTagOptional() throws IllegalStateException {
		return Optional.ofNullable(getTagNullable());
	}
	
}
