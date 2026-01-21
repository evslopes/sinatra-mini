package pro.sinatra.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sinatra.mini.entity.Loja;

public interface LojaRepository extends JpaRepository<Loja, Long> {
}