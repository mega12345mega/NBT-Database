package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.luneruniverse.minecraft.nbtdatabase.connection.MojangAuth;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.DisconnectPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.login.LoginPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.login.LoginRequestPacket;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ServerLoginHandler extends SimpleChannelInboundHandler<Packet> {
	
	private final KeyPair keys;
	private final Consumer<Optional<LoginPacket.User>> onConnect;
	private final byte[] challenge;
	private boolean loginReceived;
	
	public ServerLoginHandler(KeyPair keys, Consumer<Optional<LoginPacket.User>> onConnect) {
		this.keys = keys;
		this.onConnect = onConnect;
		
		challenge = new byte[4];
		new SecureRandom().nextBytes(challenge);
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		ctx.write(new LoginRequestPacket(keys.getPublic(), challenge))
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		
		if (ctx.channel().isActive())
			ctx.flush();
	}
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
		ctx.fireChannelActive();
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
		if (msg instanceof LoginPacket) {
			if (loginReceived) {
				ctx.close();
				return;
			}
			loginReceived = true;
			
			LoginPacket login = (LoginPacket) msg;
			
			Cipher cipher = Cipher.getInstance(keys.getPrivate().getAlgorithm());
			cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
			
			SecretKey sharedKey = new SecretKeySpec(cipher.doFinal(login.getEncryptedSharedKey()), "AES");
			
			ctx.pipeline().addBefore("nbt#length", null, new EncryptionHandler(sharedKey));
			
			if (!MessageDigest.isEqual(challenge, cipher.doFinal(login.getEncryptedChallenge()))) {
				ctx.writeAndFlush(new DisconnectPacket("Login failed: Invalid challenge"))
						.addListener(ChannelFutureListener.CLOSE);
				return;
			}
			
			Optional<LoginPacket.User> user = login.getUser();
			if (user.isPresent()) {
				WaitHandler wait = new WaitHandler(true);
				ctx.pipeline().addAfter(ctx.name(), null, wait);
				CompletableFuture<Boolean> hasJoinedFuture = MojangAuth.hasJoinedAsync(
						MojangAuth.generateServerId(keys.getPublic(), sharedKey), user.get().getUuid(), user.get().getUsername());
				hasJoinedFuture.whenCompleteAsync((hasJoined, e) -> {
					if (e != null) {
						ctx.pipeline().fireExceptionCaught(e);
						ctx.writeAndFlush(new DisconnectPacket("Login failed: Couldn't verify with Mojang"))
								.addListener(ChannelFutureListener.CLOSE);
					} else if (hasJoined) {
						onConnect.accept(user);
						wait.setDiscard(false);
						ctx.pipeline().remove(wait);
					} else {
						ctx.writeAndFlush(new DisconnectPacket("Login failed: Rejected by Mojang"))
								.addListener(ChannelFutureListener.CLOSE);
					}
				}, ctx.executor());
			} else
				onConnect.accept(user);
			
			ctx.pipeline().remove(this);
		} else
			ctx.close();
	}
	
}
