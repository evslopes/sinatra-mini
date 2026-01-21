package pro.sinatra.mini.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import pro.sinatra.mini.entity.Loja;
import pro.sinatra.mini.service.LojaService;

import java.util.List;

@RestController
@RequestMapping("/lojas")
public class LojaController {
    @Autowired
    private LojaService lojaService;

    @GetMapping
    public List<Loja> findAll() {
        return lojaService.findAll();
    }

    @GetMapping("/{id}")
    public Loja findById(@PathVariable Long id) {
        return lojaService.findById(id);
    }

    @PostMapping
    public Loja save(@RequestBody Loja loja) {
        return lojaService.save(loja);
    }

    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        lojaService.deleteById(id);
    }
}