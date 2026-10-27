package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Component
public class BookRestClientImpl implements BookRestClient{

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3)); // connection timeout 3 secs
        factory.setReadTimeout(Duration.ofSeconds(3)); // read timeout 3 secs

        this.restClient = builder
                .requestFactory(factory)
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.authToken()) // fake access token
                .build();
    }

    @Override
    public BookDto getBook(Long id) {
       BookApiResponse response;
       try {
           response = restClient.get().uri("/books/{id}", id).retrieve()
                   .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                       throw new ClientException(createErrorMessage(res));
                   })
                   .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                       throw new ClientException(createErrorMessage(res));
                   })
                   .body(BookApiResponse.class);
       } catch(ResourceAccessException ex){
           throw new ClientException("Cannot access the resource: " + ex.getMessage());
       } catch(RestClientException ex){
           throw new ClientException("Cannot extract the response");
       }

       return this.mapResponseToDTO(response);
    }

    @Override
    public List<BookDto> getAllBooks() {
        List<BookApiResponse> response;
        try {
            response = restClient.get().uri("/books").retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ClientException(createErrorMessage(res));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        throw new ClientException(createErrorMessage(res));
                    })
                    .body(new ParameterizedTypeReference<List<BookApiResponse>>() {});
        } catch(ResourceAccessException ex){
            throw new ClientException("Cannot access the resource: " + ex.getMessage());
        } catch(RestClientException ex){
            throw new ClientException("Cannot extract the response");
        }

        if(response == null)
            return List.of();
        return response.stream().map(this::mapResponseToDTO).toList();
    }

    private String createErrorMessage(ClientHttpResponse res) {
        try{
            if(res.getStatusCode().is5xxServerError())
            {
                return "Internal server error occurred: " + res.getStatusCode();
            }
            return "Client error occurred: " + res.getStatusCode();
        } catch (IOException exception){
            return "Some error occurred during completing your request";
        }

    }
    private BookDto mapResponseToDTO(BookApiResponse response){
        if(response == null)
            return null;

        return new BookDto(
                response.title(),
                response.author(),
                response.genre(),
                response.price()
        );
    }
}
