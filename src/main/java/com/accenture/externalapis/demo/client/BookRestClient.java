package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.dto.BookDto;
import java.util.List;

public interface BookRestClient {

    BookDto getBook(Long id);

    List<BookDto> getAllBooks();
}
