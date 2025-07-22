package com.luneruniverse.minecraft.nbtdatabase.connection.packets;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.Registration;
import com.esotericsoftware.kryo.kryo5.Serializer;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.util.DefaultClassResolver;
import com.luneruniverse.minecraft.nbtdatabase.Config;
import com.luneruniverse.minecraft.nbtdatabase.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.NBTEntry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.TagFilter;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;

public class Packets {
	
	private static final Kryo KRYO = new Kryo(new DefaultClassResolver() {
		@SuppressWarnings("rawtypes")
		public Registration getRegistration(Class type) {
			Registration registration = super.getRegistration(type);
			if (registration != null)
				return registration;
			if (Set.class.isAssignableFrom(type))
				return super.getRegistration(Set.class);
			return null;
		}
	}, null);
	static {
		KRYO.register(byte[].class);
		
		KRYO.register(Optional.class);
		KRYO.register(Set.class).setInstantiator(HashSet::new);
		KRYO.register(UUID.class, new Serializer<UUID>() {
			@Override
			public void write(Kryo kryo, Output output, UUID object) {
				output.writeLong(object.getMostSignificantBits());
				output.writeLong(object.getLeastSignificantBits());
			}
			@Override
			public UUID read(Kryo kryo, Input input, Class<? extends UUID> type) {
				return new UUID(input.readLong(), input.readLong());
			}
		});
		
		KRYO.register(Config.class);
		KRYO.register(NBTEntry.class);
		KRYO.register(NBTEntry[].class);
		KRYO.register(EntryFilter.class);
		KRYO.register(EntryView.class);
		KRYO.register(EntryView.Order.class);
		KRYO.register(Tag.class);
		KRYO.register(Tag[].class);
		KRYO.register(TagFilter.class);
		
		// Client -> Server
		KRYO.register(GetConfigRequestPacket.class);
		KRYO.register(AddEntryRequestPacket.class);
		KRYO.register(EditEntryRequestPacket.class);
		KRYO.register(RemoveEntryRequestPacket.class);
		KRYO.register(GetEntryRequestPacket.class);
		KRYO.register(GetEntryNBTRequestPacket.class);
		KRYO.register(GetEntriesRequestPacket.class);
		KRYO.register(AddTagRequestPacket.class);
		KRYO.register(EditTagRequestPacket.class);
		KRYO.register(RemoveTagRequestPacket.class);
		KRYO.register(GetTagRequestPacket.class);
		KRYO.register(GetTagsRequestPacket.class);
		KRYO.register(AddTagToEntryRequestPacket.class);
		KRYO.register(RemoveTagFromEntryRequestPacket.class);
		
		// Server -> Client
		KRYO.register(ServerExceptionPacket.class);
		KRYO.register(SuccessPacket.class);
		KRYO.register(ConfigPacket.class);
		KRYO.register(EntriesPacket.class);
		KRYO.register(EntryIdPacket.class);
		KRYO.register(EntryNBTPacket.class);
		KRYO.register(TagsPacket.class);
	}
	
	public static Packet read(int packetType, ByteBuf data) {
		Class<?> packetClass = KRYO.getRegistration(packetType).getType();
		if (!Packet.class.isAssignableFrom(packetClass))
			throw new DecoderException("Not a packet type: " + packetClass.getName());
		return (Packet) KRYO.readObject(new Input(new ByteBufInputStream(data)), packetClass);
	}
	
	public static void write(Packet packet, ByteBuf buf) {
		buf.writeInt(KRYO.getRegistration(packet.getClass()).getId());
		ByteBuf data = Unpooled.buffer();
		try (Output output = new Output(new ByteBufOutputStream(data, true))) {
			KRYO.writeObject(output, packet);
			output.flush();
			buf.writeInt(data.readableBytes());
			buf.writeBytes(data);
		}
	}
	
}
