package com.jumbo.trus.controller;

import com.jumbo.trus.dto.beer.*;
import com.jumbo.trus.dto.beer.multi.BeerListDTO;
import com.jumbo.trus.dto.beer.response.get.BeerDetailedResponse;
import com.jumbo.trus.dto.beer.response.get.BeerSetupResponse;
import com.jumbo.trus.dto.beer.response.multi.BeerMultiAddResponse;
import com.jumbo.trus.entity.filter.BeerFilter;
import com.jumbo.trus.entity.filter.StatisticsFilter;
import com.jumbo.trus.service.BeerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.webjars.NotFoundException;


import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/beer")
public class BeerController {

    private final List<SseEmitter> emitters = new ArrayList<>();

    @Autowired
    BeerService beerService;

    @Secured("ROLE_ADMIN")
    @PostMapping("/add")
    public BeerDTO addBeer(@RequestBody BeerDTO beerDTO) {
        return beerService.addBeer(beerDTO);
    }

    @GetMapping("/get-all")
    public List<BeerDTO> getBeers(BeerFilter beerFilter) {
        /*ExecutorService sseMvcExecutor = Executors.newSingleThreadExecutor();
        sseMvcExecutor.execute(() -> {
            try {
                for (int i = 0; true; i++) {
                   postMessage("test" + i);
                    Thread.sleep(1000);
                }
            } catch (Exception ex) {
                System.out.println(ex);
            }
        });*/
        return beerService.getAll(beerFilter);
    }

    @GetMapping("/get-all-detailed")
    public BeerDetailedResponse getDetailedBeers(StatisticsFilter filter) {
        return beerService.getAllDetailed(filter);
    }

    @Secured("ROLE_ADMIN")
    @PostMapping("/multiple-add")
    public BeerMultiAddResponse addMultipleBeer(@RequestBody BeerListDTO beerListDTO) {
        return beerService.addMultipleBeer(beerListDTO);
    }

    @Secured("ROLE_ADMIN")
    @DeleteMapping("/{beerId}")
    public void deleteMatch(@PathVariable Long beerId) throws NotFoundException {
        beerService.deleteBeer(beerId);
    }

    @GetMapping("/setup")
    public BeerSetupResponse setupBeers(BeerFilter beerFilter) {
        return beerService.setupBeers(beerFilter);
    }

    /*@GetMapping("/listen")
    public SseEmitter getEvents() {
        SseEmitter emitter = new SseEmitter();
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        return emitter;
    }

    @PostMapping("/notify")
    public void postMessage(String message) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(message + "ahoj ");
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }
    }*/
}
