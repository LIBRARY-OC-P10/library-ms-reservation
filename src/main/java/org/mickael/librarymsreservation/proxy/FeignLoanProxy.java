package org.mickael.librarymsreservation.proxy;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDate;
import java.util.List;

@FeignClient(name = "library-ms-loan", url = "localhost:8200")
public interface FeignLoanProxy {

    @GetMapping("/api/loans/book/{bookId}/soon-returned")
    List<LocalDate> getSoonReturned(@PathVariable("bookId") Integer bookId);
}
