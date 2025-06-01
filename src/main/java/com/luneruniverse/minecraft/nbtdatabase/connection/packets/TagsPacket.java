package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.simplepacketlibrary.packets.Packet;

public class TagsPacket extends Packet {
	
	private final Tag[] tags;
	
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
	public TagsPacket(DataInputStream in) throws IOException {
		this.tags = new Tag[in.readInt()];
		for (int i = 0; i < tags.length; i++) {
			String name = in.readUTF();
			int color = in.readInt();
			this.tags[i] = new Tag(name, color);
		}
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
	
	@Override
	public void write(DataOutputStream out) throws IOException {
		out.writeInt(tags.length);
		for (Tag tag : tags) {
			out.writeUTF(tag.name);
			out.writeInt(tag.color);
		}
	}
	
}
