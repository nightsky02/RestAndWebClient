package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
public class BookWebClientImpl implements BookWebClient {

    private final WebClient webClient;


    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        // validates properties to be sure that all the properties exist and are not empty
        if(!ClientPropertiesValidator.validateProperties(properties)) {
            throw new ClientException("Cannot initialize WebClient");
        }

        this.webClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.authToken())
                .build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        Mono<BookApiResponse> result = this.webClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                     .onStatus(HttpStatusCode::is4xxClientError, (res) -> {
                         // book id will be printed to know which id caused the error in the parallel request
                         return Mono.error(new ClientException(String.format("Client HTTP error: %s book_id = %d", res.statusCode(), id)));
                     })
                     .onStatus(HttpStatusCode::is5xxServerError, (res) -> {
                         // book id will be printed to know which id caused the error in the parallel request
                         return Mono.error(new ClientException(String.format("Client HTTP error: %s book_id = %d", res.statusCode(), id)));
                     })
                    .bodyToMono(BookApiResponse.class)
                     .timeout(Duration.ofSeconds(3))
                     .onErrorResume(this::mapException);

        return result.map(this::mapResponseToDTO);
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        Flux<BookApiResponse> result = this.webClient.get()
                .uri("/books")
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (res) -> {
                    return Mono.error(new ClientException("Client HTTP error: " + res.statusCode()));
                })
                .onStatus(HttpStatusCode::is5xxServerError, (res) -> {
                    return Mono.error(new ClientException("Server HTTP error: " + res.statusCode()));
                })
                .bodyToFlux(BookApiResponse.class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(this::mapException);

        return result.map(this::mapResponseToDTO);
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        Mono<BookDto> firstBook = this.getBookAsync(id1);
        Mono<BookDto> secondBook = this.getBookAsync(id2);

        return Mono.zip(firstBook, secondBook)
                .map(tuple -> {
                    return List.of(tuple.getT1(), tuple.getT2());
                });
    }

    private BookDto mapResponseToDTO(BookApiResponse response){
        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price()
        );
    }

    private Mono<BookApiResponse> mapException(Throwable exception){
        // a simple function that maps the occurred exception
        // if it was not processed earlier in the onStatus method

        if(exception instanceof ClientException){
            return Mono.error(exception);
        }
        if(exception instanceof org.springframework.core.codec.DecodingException){
            return Mono.error(new ClientException("Error: Cannot extract your response"));
        }
        else if(exception instanceof java.util.concurrent.TimeoutException){
            return Mono.error(new ClientException("Error: Timed out"));
        }
        return Mono.error(new ClientException("Error during processing your request: " + exception.getMessage()));
    }

}
