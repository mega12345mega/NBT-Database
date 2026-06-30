package com.luneruniverse.minecraft.nbtdatabase.connection.netty;

import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import com.luneruniverse.minecraft.nbtdatabase.connection.MojangAuth;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.DisconnectException;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.LoginPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.LoginPacket.User;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.LoginRequestPacket;
import com.luneruniverse.minecraft.nbtdatabase.connection.packets.Packet;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientLoginHandler extends SimpleChannelInboundHandler<Packet> {
	
	private final User user;
	private final String accessToken;
	private final WaitHandler wait;
	private boolean loginRequestReceived;
	
	public ClientLoginHandler(User user, String accessToken) {
		this.user = user;
		this.accessToken = accessToken;
		this.wait = new WaitHandler(true);
	}
	
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		ctx.pipeline().addAfter(ctx.name(), null, wait);
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet msg) throws Exception {
		if (msg instanceof LoginRequestPacket) {
			if (loginRequestReceived)
				throw new DisconnectException("Login failed: Server requested login twice");
			loginRequestReceived = true;
			
			LoginRequestPacket request = (LoginRequestPacket) msg;
			
			KeyGenerator sharedKeyGenerator = KeyGenerator.getInstance("AES");
			sharedKeyGenerator.init(128);
			SecretKey sharedKey = sharedKeyGenerator.generateKey();
			
			if (user == null) {
				login(ctx, request, sharedKey);
			} else {
				CompletableFuture<Void> joinFuture = MojangAuth.joinAsync(
						MojangAuth.generateServerId(request.getPublicKey(), sharedKey), user.getUuid(), accessToken);
				joinFuture.whenComplete((v, e) -> {
					if (e != null) {
						ctx.pipeline().fireExceptionCaught(e);
						ctx.pipeline().fireExceptionCaught(new DisconnectException("Login failed: Couldn't access Mojang"));
					} else {
						try {
							login(ctx, request, sharedKey);
						} catch (GeneralSecurityException e2) {
							ctx.pipeline().fireExceptionCaught(e2);
						}
					}
				});
			}
		} else
			throw new DisconnectException("Login failed: Server didn't request login");
	}
	
	private void login(ChannelHandlerContext ctx, LoginRequestPacket request, SecretKey sharedKey) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance(request.getPublicKey().getAlgorithm());
		cipher.init(Cipher.ENCRYPT_MODE, request.getPublicKey());
		
		ctx.writeAndFlush(new LoginPacket(Optional.ofNullable(user),
				cipher.doFinal(sharedKey.getEncoded()), cipher.doFinal(request.getChallenge())))
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
				.addListener(future -> {
					if (future.isSuccess()) {
						ctx.pipeline().addBefore("nbt#length", null, new EncryptionHandler(sharedKey));
						ctx.pipeline().remove(this);
						wait.setDiscard(false);
						ctx.pipeline().remove(wait);
					} else
						ctx.close();
				});
	}
	
}
