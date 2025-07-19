package com.kyotom.halo.immich.client;

import com.kyotom.halo.immich.record.Album;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import com.kyotom.halo.immich.record.AddToAlbumResponse;
import com.kyotom.halo.immich.record.DeleteErrorResponse;
import com.kyotom.halo.immich.record.ImageDetail;
import com.kyotom.halo.immich.record.ShareResponse;
import com.kyotom.halo.immich.record.UploadImageResult;
import com.kyotom.halo.immich.record.UploadResponse;
import com.kyotom.halo.immich.exception.ImmichException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple3;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.kyotom.halo.immich.ImmichAttachmentHandler.handleError;

public class ImmichClient {
    // 静态变量存储单例实例
    private static Map<String,ImmichClient> instanceMap = new ConcurrentHashMap<>();
    private static final Object lock = new Object();

    protected WebClient client;
    protected String server;
    protected String apiKey;
    protected String albumId;
    protected String albumName;

    public ImmichClient(@NotNull String server, @NotNull String apiKey, @NotNull String albumName) {
        this.server = server;
        this.apiKey = apiKey;
        this.albumName = albumName;
        final String baseUrl = server + (server.endsWith("/") ? "" : "/");

        var builder = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeader("Accept", "application/json")
            .filter(errorHandler());
        if (StringUtils.hasText(apiKey)) {
            builder = builder.defaultHeader("x-api-key", apiKey);
        }
        client = builder.build();
    }

    protected Mono<String> getAlbumId(String albumName) {
        return client.get()
                .uri("api/albums")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Album>>() {
                })
                .flatMap(albumList -> {
                    if (albumList == null || albumList.isEmpty()) {
                        throw new ImmichException(HttpStatus.NOT_FOUND, "No albums found");
                    }
                    return Mono.just(albumList.stream()
                        .filter(album -> albumName.equalsIgnoreCase(album.albumName()))
                        .findFirst()
                        .map(Album::id).orElseThrow(() -> new ImmichException(HttpStatus.NOT_FOUND, "Album not found: " + albumName)));
                });
    }

    protected static ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(resp -> {
            if (resp.statusCode().is5xxServerError()) {
                return resp.bodyToMono(String.class)
                    .flatMap(errorBody ->
                        Mono.error(new ImmichException(resp.statusCode(), errorBody)));
            } else if (!resp.statusCode().is2xxSuccessful()) {
                return resp.bodyToMono(ImmichResponse.class).flatMap(body ->
                    Mono.error(new ImmichException(resp.statusCode(), body.message)));
            }
            // 2xx
            return Mono.just(resp);
        });
    }

    public Mono<UploadImageResult> upload(@NotNull Flux<DataBuffer> content, @Nullable String filename) {

        return uploadImage(content, filename)
            .flatMap(uploadResponse -> {
                Mono<?> addToAlbumId;
                if (albumId != null && !"".equalsIgnoreCase(albumId)) {
                    addToAlbumId = addToAlbumId(uploadResponse.id(), albumId);
                }
                else {
                    addToAlbumId = Mono.empty();
                }
                Mono<ShareResponse> shareResponse = sharedImage(uploadResponse.id());
                Mono<ImageDetail> imageDetail = getImageDetail(uploadResponse.id());

                return addToAlbumId.then(Mono.zip(shareResponse, imageDetail, Mono.fromSupplier(()-> uploadResponse)).flatMap(this::getUploadResult));
            });
    }

    private Mono<UploadImageResult> getUploadResult(
        Tuple3<ShareResponse, ImageDetail, UploadResponse> tuple) {
        ShareResponse shareResponse = tuple.getT1();
        ImageDetail imageDetail = tuple.getT2();
        UploadResponse uploadResponse = tuple.getT3();
        return Mono.fromSupplier(() -> {
            // 假设 d() 返回一个字符串，包含 b 和 c 的结果
            return new UploadImageResult(uploadResponse.id(), shareResponse.id(), shareResponse.assets().get(0).originalFileName(), shareResponse.key(), imageDetail.exifInfo().fileSizeInByte(), imageDetail.originalMimeType());
        });
    }

    private Mono<ImageDetail> getImageDetail(String imageId) {
        return client.get()
            .uri("/api/assets/" + imageId)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ImageDetail>() {
            })
            .single();
    }

    private Mono<UploadResponse> uploadImage(@NotNull Flux<DataBuffer> content, String filename) {
        final var bodyBuilder = new MultipartBodyBuilder();
        String uuid = UUID.randomUUID().toString();
        String deviceAssetId = ("halo-" + filename + "-" + uuid).replaceAll("\\s+", "");
        // 获取当前时间并格式化为需要的字符串格式
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withLocale(Locale.ENGLISH);
        String currentTime = formatter.format(Instant.now());

        bodyBuilder.part("deviceAssetId", deviceAssetId, MediaType.TEXT_PLAIN);
        bodyBuilder.part("deviceId", "halo", MediaType.TEXT_PLAIN);
        bodyBuilder.part("fileCreatedAt", currentTime, MediaType.TEXT_PLAIN);
        bodyBuilder.part("fileModifiedAt", currentTime, MediaType.TEXT_PLAIN);
        bodyBuilder.part("isFavorite", "false", MediaType.TEXT_PLAIN); // boolean 会转换为字符串
        final var filePartBuilder = bodyBuilder.asyncPart("assetData", content, DataBuffer.class);
        if (filename != null) {
            filePartBuilder.filename(filename);
        }
        final var t = MediaTypeFactory.getMediaType(filename);
        filePartBuilder.contentType(t.orElse(MediaType.APPLICATION_OCTET_STREAM));

        return client.post()
            .uri("/api/assets")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<UploadResponse>() {
            })
            .single();
    }

    private Mono<List<AddToAlbumResponse>> addToAlbumId(String imageId, String albumId) {
        String json = "{\"ids\":[\"" + imageId + "\"]}";
        return client.put()
            .uri("/api/albums/" + albumId + "/assets")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<List<AddToAlbumResponse>>() {
            })
            .single();
    }

    private Mono<ShareResponse> sharedImage(String id) {
        String json = "{\"type\": \"INDIVIDUAL\",\"assetIds\": [\""+ id +"\"],\"allowUpload\": false,\"description\": \"\",\"password\": \"\",\"allowDownload\": false,\"showMetadata\": false\n}";
        return client.post()
            .uri("/api/shared-links")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ShareResponse>() {
            })
            .single();
    }

    public Mono<Void> delete(@NotNull String imageId, @NotNull String sharedId) {
        return deleteShareUrl(sharedId).then(deleteImage(imageId));
    }

    public Mono<Void> deleteImage(@NotNull String key) {
        String json = "{\"force\": true,\"ids\": [\"" + key + "\"]}";
        return client
            .method(HttpMethod.DELETE) // 使用 DELETE 方法
            .uri("/api/assets") // 修正 URI，确保 "/" 符合 RESTful 约定
            .contentType(MediaType.APPLICATION_JSON) // 设置内容类型
            .bodyValue(json) // 设置请求体
            .retrieve()
            .onStatus(status -> status.value() == 204, response -> {
                // 204 No Content: 请求成功，什么都不返回
                return Mono.empty(); // 返回 Mono.empty 表示成功
            })
            .onStatus(status -> status.value() != 204, response -> {
                // 如果不是 204，则抛出异常
                return response.bodyToMono(DeleteErrorResponse.class)
                    .flatMap(errorBody -> Mono.error(new ImmichException(HttpStatusCode.valueOf(errorBody.statusCode()), errorBody.message())));
            })
            .bodyToMono(new ParameterizedTypeReference<ImmichResponse<Void>>() {})
            .then(Mono.empty()); // 完成 Mono
    }

    public Mono<Void> deleteShareUrl(@NotNull String sharedId) {
        // 假设这是另一个 API 请求，您可以使用响应进行进一步操作
        return client
            .method(HttpMethod.DELETE) // 使用 GET 方法
            .uri("/api/shared-links/" + sharedId) // 另一个 API 的 URI
            .retrieve()
            .onStatus(status -> status.value() == 200, response -> {
                return Mono.empty(); // 返回 Mono.empty 表示成功
            })
            .onStatus(status -> status.value() != 200, response -> {
                // 如果不是 204，则抛出异常
                return response.bodyToMono(DeleteErrorResponse.class)
                    .flatMap(errorBody -> Mono.error(new ImmichException(HttpStatusCode.valueOf(errorBody.statusCode()), errorBody.message())));
            })
            .bodyToMono(new ParameterizedTypeReference<ImmichResponse<Void>>() {})
            .then(Mono.empty()); // 完
    }

    /**
     * Verify that the Lsky Pro API response status is {@code true}.
     */
    <T> Mono<T> checkResponse(ImmichResponse<T> resp) {
        if (resp.status()) {
            return Mono.justOrEmpty(resp.data);
        }
        return Mono.error(
            new ImmichException(HttpStatus.OK, "status=false: " + resp.message));
    }

    public record ImmichResponse<T>(boolean status, String message, T data) {
    }

    // 提供静态工厂方法
    public static Mono<ImmichClient> create(@NotNull String displayName, @NotNull String server, @NotNull String apiKey, @Nullable String albumName) {
        // 第一次检查 - 无锁检查
        if (instanceMap.get(displayName) != null && isSameConfiguration(instanceMap.get(displayName), server, apiKey, albumName)) {
            return Mono.just(instanceMap.get(displayName));
        }

        // 同步块 - double check
        synchronized (lock) {
            // 第二次检查 - 在锁内再次检查
            if (instanceMap.get(displayName) != null && isSameConfiguration(instanceMap.get(displayName), server, apiKey, albumName)) {
                return Mono.just(instanceMap.get(displayName));
            }

            // 创建新实例
            ImmichClient client = new ImmichClient(server, apiKey, albumName);

            if (albumName == null || "".equalsIgnoreCase(albumName.trim())) {
                client.albumId = null;
                // 更新字段以反映实际的albumName
                client.albumName = albumName;
                instanceMap.put(displayName, client);
                return Mono.just(client);
            }

            return client.getAlbumId(albumName)
                    .map(albumId -> {
                        client.albumId = albumId;
                        client.albumName = albumName; // 设置albumName
                        instanceMap.put(displayName, client);
                        return client;
                    })
                    .onErrorMap(error -> {
                        if (error instanceof ImmichException) {
                            return error;
                        }
                        else {
                            return handleError(error);
                        }
                    });
        }
    }

    // 辅助方法：检查配置是否相同
    private static boolean isSameConfiguration(ImmichClient existingClient, String server, String apiKey, String albumName) {
        return Objects.equals(existingClient.server, server) &&
                Objects.equals(existingClient.apiKey, apiKey) &&
                Objects.equals(existingClient.albumName, albumName);
    }

}
