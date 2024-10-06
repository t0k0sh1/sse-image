package com.t0k0sh1.sse.image.controller;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import reactor.netty.http.client.HttpClient;

@RestController
@RequestMapping("/api/v1")
public class ImageController {

    private final WebClient webClient;

    public ImageController() {
        HttpClient httpClient = HttpClient.create().resolver(DefaultAddressResolverGroup.INSTANCE);
        this.webClient = WebClient
                .builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl("https://cdn.pixabay.com").build();
    }

    @CrossOrigin(origins = "http://127.0.0.1:5500")
    @GetMapping(value="/images/updates", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamImageUpdates() {
        try {
            SseEmitter emitter = new SseEmitter();
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            scheduler.scheduleAtFixedRate(() -> {
                try {
                    String imagePath = downloadImage();

                    String imageURL = "/api/v1/images/" + imagePath;
                    emitter.send(SseEmitter.event()
                            .data(imageURL));
                } catch (IOException e) {
                    e.printStackTrace();
                    emitter.completeWithError(e);
                    scheduler.shutdown();
                }
            }, 0, 10, TimeUnit.SECONDS);


            emitter.onCompletion(scheduler::shutdown);
            emitter.onTimeout(scheduler::shutdown);
            emitter.onError(e -> scheduler.shutdown());

            return emitter;
        } catch(Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @GetMapping(value = "/images/{imageName}", produces = MediaType.IMAGE_JPEG_VALUE)
    public FileSystemResource getImage(@PathVariable String imageName) {
        return new FileSystemResource(new File(System.getProperty("java.io.tmpdir"), imageName));
    }

    private String downloadImage() throws IOException {
        try {
            byte[] imageBytes = webClient.get()
                    .uri("/user/2024/06/10/13-43-32-848_250x250.jpg")
                    .retrieve()
                    .bodyToMono(byte[].class)
                    .block();

            String imageName = "image_" + System.currentTimeMillis() + ".jpg";
            File outputFile = new File(System.getProperty("java.io.tmpdir"), imageName);
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(imageBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return imageName;
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
