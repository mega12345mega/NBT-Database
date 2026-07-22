package com.luneruniverse.minecraft.nbtdatabase.ui;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.luneruniverse.minecraft.nbtdatabase.connection.user.Profile;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;

import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

public class LoginUtil {
	
	private static final HttpClient HTTP_CLIENT = MinecraftAuth.createHttpClient("NBT-Database");
	
	public static void loginWithDeviceCode(
			Consumer<String> urlConsumer, BiConsumer<Profile, MinecraftToken> sessionConsumer,
			Runnable timeoutConsumer, Consumer<Exception> exceptionConsumer) {
		FutureUtil.DAEMON_EXECUTOR.execute(() -> {
			try {
				JavaAuthManager auth = JavaAuthManager.create(HTTP_CLIENT).login(DeviceCodeMsaAuthService::new,
						(Consumer<MsaDeviceCode>) code -> urlConsumer.accept(code.getDirectVerificationUri()));
				MinecraftProfile profile = auth.getMinecraftProfile().getUpToDate();
				MinecraftToken token = auth.getMinecraftToken().getUpToDate();
				sessionConsumer.accept(new Profile(profile.getId(), profile.getName()), token);
			} catch (TimeoutException e) {
				timeoutConsumer.run();
			} catch (IOException | InterruptedException e) {
				exceptionConsumer.accept(e);
			}
		});
	}
	
}
