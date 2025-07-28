package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;

public class EncryptionHandler extends ChannelDuplexHandler {
	
	private static Cipher createCipher(int mode, SecretKey sharedKey) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("AES/CFB8/NoPadding");
		cipher.init(mode, sharedKey, new IvParameterSpec(sharedKey.getEncoded()));
		return cipher;
	}
	private static ByteBuf update(Cipher cipher, ByteBuf msg) {
		byte[] msgBytes = new byte[msg.readableBytes()];
		msg.readBytes(msgBytes);
		return Unpooled.wrappedBuffer(cipher.update(msgBytes));
	}
	
	private final Cipher decryptionCipher;
	private final Cipher encryptionCipher;
	
	public EncryptionHandler(SecretKey sharedKey) throws GeneralSecurityException {
		decryptionCipher = createCipher(Cipher.DECRYPT_MODE, sharedKey);
		encryptionCipher = createCipher(Cipher.ENCRYPT_MODE, sharedKey);
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (!(msg instanceof ByteBuf))
				throw new DecoderException("Cannot decrypt '" + msg.getClass().getName() + "'");
			
			ctx.fireChannelRead(update(decryptionCipher, (ByteBuf) msg));
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		try {
			if (!(msg instanceof ByteBuf))
				throw new EncoderException("Cannot encrypt '" + msg.getClass().getName() + "'");
			
			ctx.write(update(encryptionCipher, (ByteBuf) msg), promise);
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}
	
}
