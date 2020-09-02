package org.mickael.librarymsreservation.proxy;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "library-ms-book", url = "localhost:8100")//il faut modifier les uri avec le nom du ms à appeler
//@RibbonClient(name = "micro service à appeler")
public interface FeignBookProxy {



    @GetMapping("/api/copies/available/book/{bookId}")
     boolean checkIfCopyAvailableForBook(@PathVariable("bookId") Integer bookId);

    @GetMapping("/api/copies/available-number/book/{bookId}")
    Integer numberOfCopyAvailableForBook(@PathVariable("bookId")  Integer bookId);

/*
    @GetMapping("/api/books/{id}")
    Book retrieveBook(@PathVariable("id") Integer id);
*/



}
