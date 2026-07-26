package com.luneruniverse.minecraft.nbtdatabase.connection.server;

import java.io.File;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.AllowAuthorizationManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.AuthorizationManager;
import com.luneruniverse.minecraft.nbtdatabase.connection.server.auth.AuthorizationManagers;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.ConfigurateUtil;

import io.netty.handler.ssl.SslContextBuilder;

public class ServerConfig {
	
	public static class Builder {
		
		private int threads;
		private boolean sslRequired;
		private SslContextBuilder sslBuilder;
		private AuthorizationManager authorizationManager;
		private boolean websiteEnabled;
		
		public Builder() {
			threads = 3;
			sslRequired = false;
			sslBuilder = null;
			authorizationManager = AllowAuthorizationManager.create();
			websiteEnabled = false;
		}
		
		public void threads(int threads) {
			this.threads = threads;
		}
		
		public void requireSsl(SslContextBuilder sslBuilder) {
			this.sslRequired = true;
			this.sslBuilder = sslBuilder;
		}
		public void optionalSsl(SslContextBuilder sslBuilder) {
			this.sslRequired = false;
			this.sslBuilder = sslBuilder;
		}
		public void noSsl() {
			this.sslRequired = false;
			this.sslBuilder = null;
		}
		
		public void authorizationManager(AuthorizationManager authorizationManager) {
			this.authorizationManager = authorizationManager;
		}
		
		public void websiteEnabled(boolean websiteEnabled) {
			this.websiteEnabled = websiteEnabled;
		}
		
		public ServerConfig build() {
			return new ServerConfig(
					threads,
					sslRequired,
					sslBuilder,
					authorizationManager,
					websiteEnabled);
		}
		
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public static ServerConfig fromConfigurationLoader(File parent, ConfigurationLoader<? extends ConfigurationNode> loader) throws ConfigurateException {
		ConfigurationNode node = loader.load(ConfigurationOptions.defaults().shouldCopyDefaults(false).implicitInitialization(false));
		
		Builder builder = builder();
		
		ConfigurationNode threadsNode = node.node("threads");
		if (!threadsNode.virtual())
			builder.threads(ConfigurateUtil.require(threadsNode, Integer.class, threads -> threads >= 0, "Expected integer >= 0"));
		
		ConfigurationNode sslNode = node.node("ssl");
		if (!sslNode.virtual()) {
			String sslMode = sslNode.node("mode").getString("");
			switch (sslMode) {
				case "required":
				case "optional":
					File chainFile = ConfigurateUtil.requireExistingFile(parent, sslNode.node("chain_file"));
					File privateKeyFile = ConfigurateUtil.requireExistingFile(parent, sslNode.node("private_key_file"));
					String privateKeyPassEnvVar = sslNode.node("private_key_pass_env_var").getString();
					
					SslContextBuilder sslBuilder;
					try {
						if (privateKeyPassEnvVar == null) {
							sslBuilder = SslContextBuilder.forServer(chainFile, privateKeyFile);
						} else {
							String privateKeyPass = System.getenv(privateKeyPassEnvVar);
							if (privateKeyPass == null)
								throw new SerializationException(sslNode.node("private_key_pass_env_var"), String.class, "Environment variable doesn't exist");
							
							sslBuilder = SslContextBuilder.forServer(chainFile, privateKeyFile, privateKeyPass);
						}
					} catch (IllegalArgumentException e) {
						throw new SerializationException(sslNode, ConfigurationNode.class, "Failed to read SSL files", e);
					}
					
					if (sslMode.equals("required"))
						builder.requireSsl(sslBuilder);
					else
						builder.optionalSsl(sslBuilder);
					break;
				case "disabled":
					builder.noSsl();
					break;
				default:
					throw new SerializationException(sslNode.node("mode"), String.class, "Expected 'disabled', 'optional', or 'required'");
			}
		}
		
		ConfigurationNode authorizationNode = node.node("authorization");
		if (!authorizationNode.virtual())
			builder.authorizationManager(AuthorizationManagers.deserialize(parent, authorizationNode));
		
		ConfigurationNode websiteEnabledNode = node.node("website_enabled");
		if (!websiteEnabledNode.virtual())
			builder.websiteEnabled(ConfigurateUtil.requireBoolean(websiteEnabledNode));
		
		return builder.build();
	}
	public static ServerConfig fromYamlFile(File file) throws ConfigurateException {
		return fromConfigurationLoader(file.getAbsoluteFile().getParentFile(), YamlConfigurationLoader.builder().file(file).build());
	}
	
	private final int threads;
	private final boolean sslRequired;
	private final SslContextBuilder sslBuilder;
	private final AuthorizationManager authorizationManager;
	private final boolean websiteEnabled;
	
	public ServerConfig(
			int threads,
			boolean sslRequired,
			SslContextBuilder sslBuilder,
			AuthorizationManager authorizationManager,
			boolean websiteEnabled) {
		if (sslRequired && sslBuilder == null)
			throw new IllegalArgumentException("Cannot require SSL when no sslBuilder is provided");
		
		this.threads = threads;
		this.sslRequired = sslRequired;
		this.sslBuilder = sslBuilder;
		this.authorizationManager = authorizationManager;
		this.websiteEnabled = websiteEnabled;
	}
	
	public int getThreads() {
		return threads;
	}
	public boolean isSslRequired() {
		return sslRequired;
	}
	public SslContextBuilder getSslBuilder() {
		return sslBuilder;
	}
	public AuthorizationManager getAuthorizationManager() {
		return authorizationManager;
	}
	public boolean isWebsiteEnabled() {
		return websiteEnabled;
	}
	
}
