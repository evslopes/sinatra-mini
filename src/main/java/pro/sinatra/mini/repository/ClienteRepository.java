package pro.sinatra.mini.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pro.sinatra.mini.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}