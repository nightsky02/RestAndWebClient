package com.accenture.externalapis.demo.controller;

import com.accenture.externalapis.demo.client.BookRestClient;
import com.accenture.externalapis.demo.client.BookWebClient;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/books")
public class BookController {

    // ***************************************
    // Since there is a global exception handler,
    // catching and processing the ClientException were skipped
    // ****************************************
    private final BookRestClient bookRestClient;
    private final BookWebClient bookWebClient;

    public BookController(BookRestClient bookRestClient, BookWebClient bookWebClient) {
        this.bookRestClient = bookRestClient;
        this.bookWebClient = bookWebClient;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBook(@PathVariable Long id) {
        BookDto book = bookRestClient.getBook(id);

        if(book == null){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No found book with the following id");
        }
        return ResponseEntity.ok(book);
    }

    @GetMapping
    public ResponseEntity<?> getAllBooks() {
        List<BookDto> books = bookRestClient.getAllBooks();

        if(books == null || books.isEmpty()){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No found books");
        }
        return ResponseEntity.ok(books);
    }

    @GetMapping("/async/{id}")
    public Mono<BookDto> getBookAsync(@PathVariable Long id) {
        return bookWebClient.getBookAsync(id);
    }

    @GetMapping("/async")
    public Flux<BookDto> getAllBooksAsync() {
        return bookWebClient.getAllBooksAsync();
    }

    @GetMapping("/async/parallel")
    public Mono<List<BookDto>> getBooksInParallel(@RequestParam Long id1,
                                                  @RequestParam Long id2) {
        return bookWebClient.getBooksInParallel(id1, id2);
    }
}