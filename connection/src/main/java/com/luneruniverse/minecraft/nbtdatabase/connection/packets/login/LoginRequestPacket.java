package com.luneruniverse.minecraft.nbtdatabase.connection.packets.login;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.KryoException;
import com.esotericsoftware.kryo.kryo5.KryoSerializable;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

public class LoginRequestPacket extends Packet implements KryoSerializable {
	
	private PublicKey publicKey;
	private byte[] challenge;
	
	public LoginRequestPacket(PublicKey publicKey, byte[] challenge) {
		this.publicKey = publicKey;
		this.challenge = challenge;
	}
	LoginRequestPacket() {
		// Deserialization
	}
	
	public PublicKey getPublicKey() {
		return publicKey;
	}
	public byte[] getChallenge() {
		return challenge;
	}
	
	@Override
	public void write(Kryo kryo, Output output) {
		byte[] publicKeyBytes = publicKey.getEncoded();
		output.writeVarInt(publicKeyBytes.length, true);
		output.writeBytes(publicKeyBytes);
		
		output.writeVarInt(challenge.length, true);
		output.writeBytes(challenge);
	}
	@Override
	public void read(Kryo kryo, Input input) {
		byte[] publicKeyBytes = new byte[input.readVarInt(true)];
		input.readBytes(publicKeyBytes);
		try {
			publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyBytes));
		} catch (GeneralSecurityException e) {
			throw new KryoException("Failed to deserialize publicKey", e);
		}
		
		challenge = new byte[input.readVarInt(true)];
		input.readBytes(challenge);
	}
	
}
