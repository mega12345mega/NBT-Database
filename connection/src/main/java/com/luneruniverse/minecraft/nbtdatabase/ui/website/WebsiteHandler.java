package com.luneruniverse.minecraft.nbtdatabase.ui.website;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.owasp.encoder.Encode;

import com.luneruniverse.minecraft.nbtdatabase.DataVersion;
import com.luneruniverse.minecraft.nbtdatabase.Entry;
import com.luneruniverse.minecraft.nbtdatabase.Tag;
import com.luneruniverse.minecraft.nbtdatabase.connection.access.NBTDatabaseAccess;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.AuthorizationServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.IllegalRequestServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.exceptions.InternalServerException;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.FutureUtil;
import com.luneruniverse.minecraft.nbtdatabase.connection.util.IOUtil;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryFilter;
import com.luneruniverse.minecraft.nbtdatabase.request.EntryView;
import com.luneruniverse.minecraft.nbtdatabase.request.IllegalRequestException;
import com.luneruniverse.minecraft.nbtdatabase.request.TagFilter;
import com.luneruniverse.minecraft.nbtdatabase.ui.ColorInput;
import com.luneruniverse.minecraft.nbtdatabase.ui.DataVersionInput;
import com.luneruniverse.minecraft.nbtdatabase.ui.TimestampUtil;
import com.luneruniverse.minecraft.nbtdatabase.ui.UUIDInput;
import com.luneruniverse.simplecli.CommandParseException;
import com.luneruniverse.simplecli.inputs.BooleanInput;
import com.luneruniverse.simplecli.inputs.IntegerInput;
import com.luneruniverse.simplecli.inputs.LongInput;
import com.luneruniverse.simplecli.inputs.StringInput;
import com.luneruniverse.simplecli.inputs.StringKeyInput;
import com.luneruniverse.simplecli.inputs.flags.Flag;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

@Sharable
public class WebsiteHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	
	private final NBTDatabaseAccess database;
	private final String websiteHtml;
	private final Map<String, Map.Entry<String, byte[]>> resources;
	
	public WebsiteHandler(NBTDatabaseAccess database) {
		this.database = database;
		this.websiteHtml = IOUtil.readStringAndCloseOrNull(getClass().getClassLoader().getResourceAsStream("ui/website.html"));
		this.resources = new HashMap<>();
		addResource("logo_transparent_plain.svg", "image/svg+xml");
		addResource("download.svg", "image/svg+xml");
	}
	private void addResource(String name, String contentType) {
		resources.put(name, new AbstractMap.SimpleImmutableEntry<>(contentType,
				IOUtil.readAllBytesAndCloseOrNull(getClass().getClassLoader().getResourceAsStream("ui/" + name))));
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
		if (!msg.decoderResult().isSuccess()) {
			writeError(ctx, HttpResponseStatus.BAD_REQUEST, null);
			return;
		}
		
		URIBuilder uri;
		try {
			uri = new URIBuilder(msg.uri());
		} catch (URISyntaxException e) {
			writeError(ctx, HttpResponseStatus.BAD_REQUEST, null);
			return;
		}
		List<String> path = uri.getPathSegments();
		
		if (path.isEmpty() || path.size() == 1 && path.get(0).isEmpty()) {
			Map<String, String> params = new HashMap<>();
			for (NameValuePair param : uri.getQueryParams())
				params.putIfAbsent(param.getName(), param.getValue());
			writeWebsite(ctx, msg, params);
			return;
		}
		
		if (path.size() == 1 && resources.containsKey(path.get(0))) {
			Map.Entry<String, byte[]> resource = resources.get(path.get(0));
			if (resource.getValue() == null)
				writeError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
			else
				writeResource(ctx, msg, resource.getValue(), resource.getKey());
			return;
		}
		
		if (path.size() == 2 && path.get(0).equals("entry")) {
			try {
				long id = Long.parseLong(path.get(1));
				writeEntryNBT(ctx, msg, id);
				return;
			} catch (NumberFormatException e) {}
		}
		
		writeError(ctx, HttpResponseStatus.NOT_FOUND, null);
	}
	
	private void writeWebsite(ChannelHandlerContext ctx, FullHttpRequest msg, Map<String, String> params) {
		if (websiteHtml == null) {
			writeError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
			return;
		}
		
		EntryFilter filter = new EntryFilter();
		EntryView view = new EntryView();
		CompletableFuture<List<Entry>> request;
		Optional<Long> id = parseParam(params.get("id"), new LongInput());
		if (id.isPresent()) {
			request = FutureUtil.thenApply(database.getEntry(id.get()), entryOptional -> {
				if (entryOptional.isPresent())
					return Arrays.asList(entryOptional.get());
				throw new IllegalRequestException("Entry doesn't exist: " + id.get());
			});
		} else {
			parseParam(params.get("name"), new StringInput()).ifPresent(filter::filterByName);
			parseParam(params.get("nbt_length_min"), new IntegerInput().min(0)).ifPresent(filter::filterByMinNbtLength);
			parseParam(params.get("nbt_length_max"), new IntegerInput().min(0)).ifPresent(filter::filterByMaxNbtLength);
			parseParam(params.get("type"), StringKeyInput.forEnum(Entry.Type.class, true)).ifPresent(filter::filterByType);
			parseParam(params.get("data_version_min"), new DataVersionInput()).ifPresent(filter::filterByMinDataVersion);
			parseParam(params.get("data_version_max"), new DataVersionInput()).ifPresent(filter::filterByMaxDataVersion);
			parseParam(params.get("author_uuid"), new UUIDInput()).ifPresent(filter::filterByAuthorUuid);
			parseParam(params.get("author_username"), new StringInput()).ifPresent(filter::filterByAuthorUsername);
			for (Map.Entry<String, String> param : params.entrySet()) {
				if (param.getKey().startsWith("tag:")) {
					parseParam(param.getValue(), new BooleanInput()).ifPresent(tagEnabled -> {
						if (tagEnabled)
							filter.filterByTag(param.getKey().substring("tag:".length()));
					});
				}
			}
			
			parseParam(params.get("order"), StringKeyInput.forEnum(EntryView.Order.class, true)).ifPresent(view::setOrder);
			parseParam(params.get("reversed_order"), new BooleanInput()).ifPresent(view::setReversedOrder);
			parseParam(params.get("offset"), new IntegerInput().min(0)).ifPresent(view::setOffset);
			
			request = database.getEntries(filter, view);
		}
		
		List<Tag> tags = new ArrayList<>();
		List<Entry> entries = new ArrayList<>();
		Map<Entry, List<Tag>> entriesWithTags = new ConcurrentHashMap<>();
		CompletableFuture<Void> requestWithTags = FutureUtil.allOf(
				FutureUtil.thenApply(database.getTags(new TagFilter()), tags2 -> {
					tags.addAll(tags2);
					return null;
				}),
				FutureUtil.thenCompose(request, entries2 -> {
					entries.addAll(entries2);
					List<CompletableFuture<?>> tagFutures = new ArrayList<>();
					for (Entry entry : entries2) {
						tagFutures.add(FutureUtil.thenApply(database.getTags(new TagFilter().filterByEntryId(entry.getId())),
								entryTags -> {
									entriesWithTags.put(entry, entryTags);
									return null;
								}));
					}
					return FutureUtil.allOf(tagFutures.toArray(new CompletableFuture<?>[tagFutures.size()]));
				}));
		
		writeDatabaseRequest(ctx, msg, requestWithTags, v -> {
			StringBuilder ordersStr = new StringBuilder();
			for (EntryView.Order order : EntryView.Order.values()) {
				if (order == view.getOrder())
					ordersStr.append("<option selected value=\"");
				else
					ordersStr.append("<option value=\"");
				ordersStr.append(Encode.forHtmlAttribute(order.name()));
				ordersStr.append("\">");
				ordersStr.append(Encode.forHtmlContent(order.toString()));
				ordersStr.append("</option>");
			}
			
			StringBuilder typesStr = new StringBuilder("<option></option>");
			for (Entry.Type type : Entry.Type.values()) {
				if (type == filter.getType())
					typesStr.append("<option selected value=\"");
				else
					typesStr.append("<option value=\"");
				typesStr.append(Encode.forHtmlAttribute(type.name()));
				typesStr.append("\">");
				typesStr.append(Encode.forHtmlContent(type.toString()));
				typesStr.append("</option>");
			}
			
			StringBuilder tagsStr = new StringBuilder("<span>Tags:");
			if (tags.isEmpty())
				tagsStr.append(" There are no tags");
			tagsStr.append("</span>");
			for (Tag tag : tags) {
				tagsStr.append("<span>");
				writeTag(tagsStr, tag);
				tagsStr.append(" <input type=\"checkbox\" name=\"tag:");
				tagsStr.append(Encode.forHtmlAttribute(tag.getName()));
				tagsStr.append("\"");
				if (filter.getTags() != null && filter.getTags().contains(tag.getName()))
					tagsStr.append(" checked");
				tagsStr.append(">");
				tagsStr.append("</span>");
			}
			
			StringBuilder entriesStr = new StringBuilder();
			if (entries.isEmpty())
				entriesStr.append("No entries found");
			for (Entry entry : entries) {
				entriesStr.append("<div onclick=\"if (!event.target.matches('.entry_download, .entry_download *'))");
				entriesStr.append("window.location.assign('?id=");
				entriesStr.append(entry.getId());
				entriesStr.append("')\" class=\"entry");
				if (entry.isVerified())
					entriesStr.append(" entry_verified");
				entriesStr.append("\">");
				
				entriesStr.append("<span class=\"entry_topbar\">");
				
				entriesStr.append("<text class=\"entry_name\">");
				entriesStr.append(Encode.forHtmlContent(entry.getName()));
				if (entry.isVerified())
					entriesStr.append(" <text class=\"unicode_icon\">✔</text>");
				entriesStr.append("</text>");
				
				entriesStr.append("<text class=\"entry_topbar_spacer\"></text>");
				
				entriesStr.append("<a class=\"entry_download\" href=\"/entry/");
				entriesStr.append(entry.getId());
				entriesStr.append("\" download=\"");
				entriesStr.append(Encode.forHtmlAttribute(entry.getName()));
				entriesStr.append(".nbt\"><img src=\"download.svg\"></a>");
				
				entriesStr.append("</span>");
				
				entriesStr.append("<span class=\"entry_tags\">");
				for (Tag tag : entriesWithTags.get(entry))
					writeTag(entriesStr, tag);
				entriesStr.append("</span>");
				
				entriesStr.append("<text title=\"UUID: ");
				entriesStr.append(entry.getAuthorUuid().toString());
				entriesStr.append("\">Author: ");
				entriesStr.append(Encode.forHtmlContent(entry.getAuthorUsername()));
				entriesStr.append("</text>");
				
				entriesStr.append("<text>Type: ");
				entriesStr.append(entry.getType().toString());
				entriesStr.append("</text>");
				
				entriesStr.append("<text>Data Version: ");
				entriesStr.append(Encode.forHtmlContent(DataVersion.toViewableString(entry.getDataVersion())));
				entriesStr.append("</text>");
				
				entriesStr.append("<text>Bytes: ");
				entriesStr.append(String.format("%,d", entry.getNbtLength()));
				entriesStr.append("</text>");
				
				entriesStr.append("<text title=\"");
				if (entry.getCreated() == entry.getModified()) {
					entriesStr.append("Never Modified");
				} else {
					entriesStr.append("Modified: ");
					entriesStr.append(TimestampUtil.formatTimestamp(entry.getModified()));
				}
				entriesStr.append("\">Created: ");
				entriesStr.append(TimestampUtil.formatTimestamp(entry.getCreated()));
				entriesStr.append("</text>");
				
				if (id.isPresent()) {
					entriesStr.append("<text>ID: ");
					entriesStr.append(entry.getId());
					entriesStr.append("</text>");
					
					entriesStr.append("<text>Hash: ");
					entriesStr.append(Encode.forHtmlContent(entry.getHash()));
					entriesStr.append("</text>");
				}
				
				entriesStr.append("</div>");
			}
			
			return websiteHtml
					.replace("{@name}", filter.getName() == null ? "" : " value=\"" + Encode.forHtmlAttribute(filter.getName()) + "\"")
					.replace("{@orders}", ordersStr)
					.replace("{@reversed_order}", view.isReversedOrder() ? " checked" : "")
					.replace("{@nbt_length_min}", filter.getMinNbtLength() == null ? "" : " value=" + filter.getMinNbtLength())
					.replace("{@nbt_length_max}", filter.getMaxNbtLength() == null ? "" : " value=" + filter.getMaxNbtLength())
					.replace("{@types}", typesStr)
					.replace("{@data_version_min}", filter.getMinDataVersion() == null ? "" : " value=\"" + Encode.forHtmlAttribute(DataVersion.toParsableString(filter.getMinDataVersion())) + "\"")
					.replace("{@data_version_max}", filter.getMaxDataVersion() == null ? "" : " value=\"" + Encode.forHtmlAttribute(DataVersion.toParsableString(filter.getMaxDataVersion())) + "\"")
					.replace("{@author_uuid}", filter.getAuthorUuid() == null ? "" : " value=\"" + filter.getAuthorUuid() + "\"")
					.replace("{@author_username}", filter.getAuthorUsername() == null ? "" : " value=\"" + Encode.forHtmlAttribute(filter.getAuthorUsername()) + "\"")
					.replace("{@tags}", tagsStr)
					.replace("{@load_more}", id.isPresent() ? " hidden" : "")
					.replace("{@back_to_first_results}", view.getOffset() == 0 ? " hidden" : "")
					.replace("{@load_more_offset}", Integer.toString(view.getOffset() + entriesWithTags.size()))
					.replace("{@entries}", entriesStr)
					.getBytes(StandardCharsets.UTF_8);
		}, "text/html; charset=UTF-8", null);
	}
	private void writeTag(StringBuilder output, Tag tag) {
		output.append("<text class=\"tag\" style=\"color: ");
		output.append(tag.isTextColorWhite() ? "white" : "black");
		output.append("; background-color: #");
		output.append(ColorInput.toString(tag.getColor()));
		output.append(";\">");
		output.append(Encode.forHtmlContent(tag.getName()));
		output.append("</text>");
	}
	
	private void writeResource(ChannelHandlerContext ctx, FullHttpRequest msg, byte[] resource, String contentType) {
		writeSuccess(ctx, HttpUtil.isKeepAlive(msg), resource, contentType, null);
	}
	
	private void writeEntryNBT(ChannelHandlerContext ctx, FullHttpRequest msg, long id) {
		writeDatabaseRequest(ctx, msg, database.getEntryNBT(id), nbtOptional -> {
			if (!nbtOptional.isPresent())
				writeError(ctx, HttpResponseStatus.BAD_REQUEST, "Entry doesn't exist: " + id);
			return nbtOptional.orElse(null);
		}, "application/octet-stream",
				headers -> headers.set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment"));
	}
	
	private <T> void writeDatabaseRequest(ChannelHandlerContext ctx, FullHttpRequest msg,
			CompletableFuture<T> request, Function<T, byte[]> content, String contentType, Consumer<HttpHeaders> headers) {
		boolean keepAlive = HttpUtil.isKeepAlive(msg);
		
		FutureUtil.whenCompleteAsync(request, (value, e) -> {
			if (e != null) {
				if (e instanceof IllegalRequestException || e instanceof IllegalRequestServerException) {
					writeError(ctx, HttpResponseStatus.BAD_REQUEST, e.getMessage());
				} else if (e instanceof AuthorizationServerException) {
					writeError(ctx, HttpResponseStatus.FORBIDDEN, e.getMessage());
				} else if (e instanceof InternalServerException) {
					writeError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
				} else {
					e.printStackTrace();
					writeError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, null);
				}
			} else {
				byte[] content2 = content.apply(value);
				if (content2 == null)
					return;
				writeSuccess(ctx, keepAlive, content2, contentType, headers2 -> {
					headers2.set(HttpHeaderNames.CACHE_CONTROL, HttpHeaderValues.NO_STORE);
					if (headers != null)
						headers.accept(headers2);
				});
			}
		}, ctx.executor());
	}
	
	private void writeSuccess(ChannelHandlerContext ctx, boolean keepAlive,
			byte[] content, String contentType, Consumer<HttpHeaders> headers) {
		FullHttpResponse response =
				new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.copiedBuffer(content));
		response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
		response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
		if (headers != null)
			headers.accept(response.headers());
		if (keepAlive)
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		
		ChannelFuture future = ctx.writeAndFlush(response);
		future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
		if (!keepAlive)
			future.addListener(ChannelFutureListener.CLOSE);
	}
	
	private void writeError(ChannelHandlerContext ctx, HttpResponseStatus status, String msg) {
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
				msg == null ? Unpooled.EMPTY_BUFFER : Unpooled.copiedBuffer(msg, StandardCharsets.UTF_8));
		
		if (msg != null) {
			response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
			response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
		}
		
		ctx.writeAndFlush(response)
				.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE)
				.addListener(ChannelFutureListener.CLOSE);
	}
	
	private <T> Optional<T> parseParam(String value, Flag<T> parser) {
		if (value == null)
			return Optional.empty();
		try {
			return Optional.of(parser.parse(value));
		} catch (CommandParseException e) {
			return Optional.empty();
		}
	}
	
}
