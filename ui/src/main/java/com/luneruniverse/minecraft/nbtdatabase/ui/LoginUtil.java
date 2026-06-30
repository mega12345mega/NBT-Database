package com.luneruniverse.minecraft.nbtdatabase.ui;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;

import net.lenni0451.commons.httpclient.HttpClient;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.MinecraftAuth.InitialXblSessionBuilder;
import net.raphimc.minecraftauth.MinecraftAuth.MsaTokenBuilder;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import net.raphimc.minecraftauth.util.MicrosoftConstants;
import net.raphimc.minecraftauth.util.logging.ILogger;

public class LoginUtil {
	
	private static final HttpClient HTTP_CLIENT = MinecraftAuth.createHttpClient();
	
	public static void setMinecraftAuthLogger() {
		MinecraftAuth.LOGGER = new ILogger() {
			@Override
			public void info(String message) {}
			@Override
			public void warn(String message) {
				System.err.println("[MinecraftAuth] " + message);
			}
			@Override
			public void error(String message) {
				System.err.println("[MinecraftAuth] " + message);
			}
		};
	}
	
	private static AbstractStep<?, StepFullJavaSession.FullJavaSession> createLoginStep(
			int timeout, Function<MsaTokenBuilder, InitialXblSessionBuilder> type) {
		return type.apply(MinecraftAuth.builder()
						.withClientId(MicrosoftConstants.JAVA_TITLE_ID).withScope(MicrosoftConstants.SCOPE_TITLE_AUTH)
						.withTimeout(timeout))
				.withDeviceToken("Win32")
				.sisuTitleAuthentication(MicrosoftConstants.JAVA_XSTS_RELYING_PARTY)
				.buildMinecraftJavaProfileStep(false);
	}
	
	public static void loginWithDeviceCode(Consumer<String> urlConsumer,
			Consumer<StepFullJavaSession.FullJavaSession> sessionConsumer, Runnable timeoutConsumer) {
		try {
			AbstractStep<?, StepFullJavaSession.FullJavaSession> step = createLoginStep(5 * 60, MsaTokenBuilder::deviceCode);
			
			sessionConsumer.accept(step.getFromInput(HTTP_CLIENT, new StepMsaDeviceCode.MsaDeviceCodeCallback(
					msaDeviceCode -> urlConsumer.accept(msaDeviceCode.getDirectVerificationUri()))));
		} catch (TimeoutException e) {
			timeoutConsumer.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
