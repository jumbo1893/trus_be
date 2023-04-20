package com.jumbo.trus.controller;

import com.jumbo.trus.dto.beer.*;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.service.BeerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.webjars.NotFoundException;

import java.util.List;

@RestController
@RequestMapping("/beer")
public class BeerController {

    @Autowired
    BeerService beerService;

    @PostMapping("/add")
    public BeerDTO addBeer(@RequestBody BeerDTO beerDTO) {
        return beerService.addBeer(beerDTO);
    }

    @GetMapping("/get-all")
    public List<BeerDTO> getBeers(BeerFilter beerFilter) {
        return beerService.getAll(beerFilter);
    }

    @GetMapping("/get-all-detailed")
    public BeerDetailedResponse getDetailedBeers(BeerFilter beerFilter) {
        return beerService.getAllDetailed(beerFilter);
    }

    @PostMapping("/multiple-add")
    public BeerMultiAddResponse addMultipleBeer(@RequestBody BeerListDTO beerListDTO) {
        return beerService.addMultipleBeer(beerListDTO);
    }

    @DeleteMapping("/{beerId}")
    public void deleteMatch(@PathVariable Long beerId) throws NotFoundException {
        beerService.deleteMatch(beerId);
    }
}
