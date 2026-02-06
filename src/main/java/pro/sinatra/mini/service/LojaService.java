package pro.sinatra.mini.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import pro.sinatra.mini.entity.Loja;
import pro.sinatra.mini.repository.LojaRepository;

import java.util.List;

@Service
public class LojaService {
    @Autowired
    private LojaRepository lojaRepository;

    @Cacheable(value = "lojasAll")
    public List<Loja> findAll() {
        return lojaRepository.findAll();
    }

    @Cacheable(value = "lojas", key = "#id")
    public Loja findById(Long id) {
        return lojaRepository.findById(id).orElse(null);
    }

    public Loja save(Loja loja) {
        return lojaRepository.save(loja);
    }

    public void deleteById(Long id) {
        lojaRepository.deleteById(id);
    }
}