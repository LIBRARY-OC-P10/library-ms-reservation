package org.mickael.librarymsreservation.proxy;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "library-ms-book", url = "localhost:8100")//il faut modifier les uri avec le nom du ms à appeler
//@RibbonClient(name = "micro service à appeler")
public interface FeignBookProxy {



    @GetMapping("/api/copies/available/book/{bookId}")
    boolean checkIfCopyAvailableForBook(@PathVariable("bookId") Integer bookId, @RequestHeader("Authorization") String accessToken);

    @GetMapping("/api/copies/available-number/book/{bookId}")
    Integer numberOfCopyAvailableForBook(@PathVariable("bookId") Integer bookId, @RequestHeader("Authorization") String accessToken);

    @GetMapping("/api/copies/exist-number/book/{bookId}")
    Integer numberOfCopyForBook(@PathVariable("bookId") Integer bookId, @RequestHeader("Authorization") String accessToken);

/*
    @GetMapping("/api/books/{id}")
    Book retrieveBook(@PathVariable("id") Integer id);
*/



}
