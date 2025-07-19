package com.kyotom.halo.immich;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import com.kyotom.halo.immich.client.ImmichClient;
import com.kyotom.halo.immich.exception.ImmichException;
import com.kyotom.halo.immich.record.UploadImageResult;
import com.kyotom.halo.immich.exception.FileTypeNotAllowedException;
import org.pf4j.Extension;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.halo.app.core.extension.attachment.Attachment;
import run.halo.app.core.extension.attachment.Policy;
import run.halo.app.core.extension.attachment.endpoint.AttachmentHandler;
import run.halo.app.extension.ConfigMap;
import run.halo.app.extension.Metadata;
import run.halo.app.infra.utils.JsonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.reactivestreams.Publisher;
import org.springframework.lang.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.SynchronousSink;
import run.halo.app.infra.FileCategoryMatcher;
import run.halo.app.infra.utils.FileTypeDetectUtils;

@Slf4j
@Extension
public class ImmichAttachmentHandler implements AttachmentHandler {

    public static final String IMAGE_KEY = "immich.plugin.halo.kyotom.com/image-key";
    public static final String IMAGE_SHARED_ID = "immich.plugin.halo.kyotom.com/image-shared-imageId";
    public static final String IMAGE_LINK = "immich.plugin.halo.kyotom.com/image-link";

    @Override
    public Mono<Attachment> upload(UploadContext uploadOption) {
        return Mono.just(uploadOption)
            .filter(option -> this.shouldHandle(option.policy()))
            .flatMap(option -> {

                var configMap = option.configMap();
                var setting = Optional.ofNullable(configMap)
                    .map(ConfigMap::getData)
                    .map(data -> data.get("default"))
                    .map(json -> JsonUtils.jsonToObject(json, ImmichProperties.class))
                    .orElseGet(ImmichProperties::new);

                final var file = option.file();
                return validateFile(file, setting).then(upload(uploadOption, setting)
                    .subscribeOn(Schedulers.boundedElastic())
                    .onErrorMap(ImmichAttachmentHandler::handleError)
                    .map(resp -> buildAttachment(setting, resp)));
            });
    }

    public static Throwable handleError(Throwable t) {
        if (t instanceof ImmichException e) {
            if (e.statusCode.value() == 401) {
                return new ServerWebInputException(
                    "Immich authentication failed, please check your API token.");
            } else if (e.statusCode.value() == 403) {
                return new ServerWebInputException(
                    "Immich API may have been disabled (HTTP 403).");
            } else if (e.statusCode.value() == 429) {
                return new ServerWebInputException(
                    "Immich API error: usage quota exceeded (HTTP 429).");
            }
            return new ServerWebInputException(
                "Immich API error (HTTP %d): %s".formatted(e.statusCode.value(), e.getMessage()));
        } else if (t instanceof WebClientRequestException e) {
            return new ServerWebInputException(
                "Failed to request Immich API: %s".formatted(e.getMessage()));
        }
        return t;
    }

    Attachment buildAttachment(
        ImmichProperties properties, UploadImageResult uploadImageResult) {

        final String url = properties.getImmichUrl() + "/api/assets/" + uploadImageResult.imageId() + "/thumbnail?size=preview&key=" + uploadImageResult.key();

        final var metadata = new Metadata();
        metadata.setGenerateName(UUID.randomUUID().toString());
        final var annotations = Map.of(
            IMAGE_KEY, uploadImageResult.imageId(),
            IMAGE_SHARED_ID, uploadImageResult.shardId(),
            IMAGE_LINK, url
        );
        metadata.setAnnotations(annotations);

        // Warning: due to the limitation of Lsky Pro API,
        // the file size may be wrong if you configured server side image conversion.
        var spec = new Attachment.AttachmentSpec();

        spec.setSize(uploadImageResult.size());
        spec.setDisplayName(uploadImageResult.originalFileName());
        spec.setMediaType(uploadImageResult.originalMimeType());

        final var attachment = new Attachment();
        attachment.setMetadata(metadata);
        attachment.setSpec(spec);
        log.info("Built attachment {} successfully", uploadImageResult.imageId());
        return attachment;
    }


    Mono<UploadImageResult> upload(UploadContext uploadContext,
        ImmichProperties properties) {
        return ImmichClient.create(
                properties.getDisplayName(),
                properties.getImmichUrl(),
                properties.getImmichApiKey(),
                properties.getImmichAlbumName()
            )
            .onErrorMap(ImmichAttachmentHandler::handleError)
            .flatMap(immichClient ->
                immichClient.upload(uploadContext.file().content(),
                    uploadContext.file().filename())
            )
            .onErrorMap(ImmichAttachmentHandler::handleError);
    }


    private Mono<Void> validateFile(FilePart file, ImmichProperties setting) {
        var validations = new ArrayList<Publisher<?>>(2);
        if (!CollectionUtils.isEmpty(setting.getAllowedFileTypes())) {
            var typeValidator = file.content()
                .next()
                .handle((dataBuffer, sink) -> {
                    var mimeType = detectMimeType(dataBuffer.asInputStream(), file.filename());
                    if (!FileTypeDetectUtils.isValidExtensionForMime(mimeType, file.filename())) {
                        handleFileTypeError(sink, "fileTypeNotMatch", mimeType);
                        return;
                    }
                    var isAllow = setting.getAllowedFileTypes()
                        .stream()
                        .map(FileCategoryMatcher::of)
                        .anyMatch(matcher -> matcher.match(mimeType));
                    if (isAllow) {
                        sink.next(dataBuffer);
                        return;
                    }
                    handleFileTypeError(sink, "fileTypeNotSupported", mimeType);
                });
            validations.add(typeValidator);
        }
        return Mono.when(validations);
    }

    private static void handleFileTypeError(SynchronousSink<Object> sink, String detailCode,
        String mimeType) {
        sink.error(new FileTypeNotAllowedException("File type is not allowed",
            "problemDetail.attachment.upload." + detailCode,
            new Object[] {mimeType})
        );
    }

    @NonNull
    private String detectMimeType(InputStream inputStream, String name) {
        try {
            return FileTypeDetectUtils.detectMimeType(inputStream, name);
        } catch (IOException e) {
            log.warn("Failed to detect file type", e);
            return "Unknown";
        }
    }

    @Override
    public Mono<Attachment> delete(DeleteContext deleteContext) {
        return Mono.just(deleteContext)
            .filter((ctx) -> shouldHandle(ctx.policy()))
            .flatMap((ctx) -> {
                final var key = getImageKey(ctx.attachment());
                final var sharedId = getImageSharedId(ctx.attachment());
                if (key.isEmpty()) {
                    log.warn(
                        "Cannot obtain image key from attachment {}, skip deleting from Immich.",
                        ctx.attachment().getMetadata().getName());
                    return Mono.just(ctx);
                }

                var configMap = deleteContext.configMap();
                var setting = Optional.ofNullable(configMap)
                    .map(ConfigMap::getData)
                    .map(data -> data.get("default"))
                    .map(json -> JsonUtils.jsonToObject(json, ImmichProperties.class))
                    .orElseGet(ImmichProperties::new);

                return delete(key.get(), sharedId.get(), setting)
                    .then(Mono.just(ctx))
                    .doOnSuccess(v -> log.info("Attachment {} deleted from Immich.",
                        ctx.attachment().getMetadata().getName()));
            })
            .onErrorMap(ImmichAttachmentHandler::handleError)
            .map(DeleteContext::attachment);
    }

    @Override
    public Mono<URI> getSharedURL(Attachment attachment,
        Policy policy,
        ConfigMap configMap,
        Duration ttl) {
        return getPermalink(attachment, policy, configMap);
    }

    @Override
    public Mono<URI> getPermalink(Attachment attachment, Policy policy, ConfigMap configMap) {
        if (!shouldHandle(policy)) {
            return Mono.empty();
        }
        final var link = getImageLink(attachment);
        return link.map(s -> Mono.just(URI.create(s))).orElseGet(Mono::empty);
    }

    Mono<Void> delete(String imageId, String sharedId, ImmichProperties properties) {
        return ImmichClient.create(
                properties.getDisplayName(),
                properties.getImmichUrl(),
                properties.getImmichApiKey(),
                properties.getImmichAlbumName()
            )
            .onErrorMap(error -> new RuntimeException("Failed to initialize ImmichClient", error))
            .flatMap(immichClient ->
                immichClient.delete(imageId, sharedId)
            )
            .onErrorMap(error -> new RuntimeException("Failed to delete image", error));
    }

    Optional<String> getImageLink(Attachment attachment) {
        return Optional.ofNullable(attachment.getMetadata().getAnnotations().get(IMAGE_LINK));
    }

    Optional<String> getImageKey(Attachment attachment) {
        return Optional.ofNullable(attachment.getMetadata().getAnnotations().get(IMAGE_KEY));
    }

    Optional<String> getImageSharedId(Attachment attachment) {
        return Optional.ofNullable(attachment.getMetadata().getAnnotations().get(IMAGE_SHARED_ID));
    }


    private boolean shouldHandle(Policy policy) {
        if (policy == null
            || policy.getSpec() == null
            || !StringUtils.hasText(policy.getSpec().getTemplateName())) {
            return false;
        }
        return "immich".equals(policy.getSpec().getTemplateName());
    }

}
