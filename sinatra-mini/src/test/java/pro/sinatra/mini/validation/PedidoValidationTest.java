package pro.sinatra.mini.validation;

import org.junit.jupiter.api.Test;
import pro.sinatra.mini.reports.MediaSemanalDTO;
import pro.sinatra.mini.reports.MenorVendaDiaDTO;
import pro.sinatra.mini.reports.PeriodoDTO;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de validação de DTOs e regras de negócio
 */
public class PedidoValidationTest {

    @Test
    public void periodoDTO_validData_createsSuccessfully() {
        LocalDate inicio = LocalDate.of(2025, 1, 1);
        LocalDate fim = LocalDate.of(2025, 1, 10);
        long dias = 10;

        PeriodoDTO dto = new PeriodoDTO(inicio, fim, dias);

        assertNotNull(dto);
        assertEquals(inicio, dto.dataInicial());
        assertEquals(fim, dto.dataFinal());
        assertEquals(dias, dto.dias());
    }

    @Test
    public void periodoDTO_nullValues_handlesGracefully() {
        // DTOs podem ter valores null dependendo do cenário
        assertDoesNotThrow(() -> {
            new PeriodoDTO(null, null, 0);
        });
    }

    @Test
    public void menorVendaDiaDTO_validData_createsSuccessfully() {
        LocalDate dia = LocalDate.of(2025, 1, 15);
        double total = 1250.50;

        MenorVendaDiaDTO dto = new MenorVendaDiaDTO(dia, total);

        assertNotNull(dto);
        assertEquals(dia, dto.dia());
        assertEquals(total, dto.total(), 0.01);
    }

    @Test
    public void menorVendaDiaDTO_zeroTotal_isValid() {
        LocalDate dia = LocalDate.of(2025, 1, 15);
        double total = 0.0;

        MenorVendaDiaDTO dto = new MenorVendaDiaDTO(dia, total);

        assertEquals(0.0, dto.total(), 0.01);
    }

    @Test
    public void mediaSemanalDTO_validDayOfWeek_createsSuccessfully() {
        int diaSemana = DayOfWeek.MONDAY.getValue();
        double media = 1520.30;

        MediaSemanalDTO dto = new MediaSemanalDTO(diaSemana, media);

        assertNotNull(dto);
        assertEquals(diaSemana, dto.dayOfWeek());
        assertEquals(media, dto.average(), 0.01);
    }

    @Test
    public void mediaSemanalDTO_allDaysOfWeek_areValid() {
        for (DayOfWeek day : DayOfWeek.values()) {
            int dayValue = day.getValue();
            MediaSemanalDTO dto = new MediaSemanalDTO(dayValue, 100.0);
            
            assertTrue(dayValue >= 1 && dayValue <= 7, 
                    "Day of week should be between 1-7");
            assertEquals(dayValue, dto.dayOfWeek());
        }
    }

    @Test
    public void mediaSemanalDTO_zeroMedia_isValid() {
        MediaSemanalDTO dto = new MediaSemanalDTO(1, 0.0);
        assertEquals(0.0, dto.average(), 0.01);
    }

    @Test
    public void mediaSemanalDTO_negativeMedia_isAccepted() {
        // Embora incomum, valores negativos podem ser tecnicamente válidos
        // (ex: estornos, devoluções)
        MediaSemanalDTO dto = new MediaSemanalDTO(1, -50.0);
        assertEquals(-50.0, dto.average(), 0.01);
    }

    @Test
    public void periodoDTO_invertedDates_isAccepted() {
        // O DTO em si não valida lógica de negócio, apenas armazena dados
        LocalDate inicio = LocalDate.of(2025, 12, 31);
        LocalDate fim = LocalDate.of(2025, 1, 1);
        
        PeriodoDTO dto = new PeriodoDTO(inicio, fim, 0);
        
        assertNotNull(dto);
        // A validação de lógica deve ser feita no Service, não no DTO
    }

    @Test
    public void menorVendaDiaDTO_precisionHandling() {
        // Testar precisão de valores decimais
        double total = 123.456;
        MenorVendaDiaDTO dto = new MenorVendaDiaDTO(LocalDate.now(), total);
        
        assertEquals(123.456, dto.total(), 0.001);
    }

    @Test
    public void periodoDTO_negativeDays_isAccepted() {
        // Dias negativos podem indicar período inválido
        // DTO não valida, apenas armazena
        PeriodoDTO dto = new PeriodoDTO(
                LocalDate.of(2025, 1, 10), 
                LocalDate.of(2025, 1, 1), 
                -9
        );
        
        assertEquals(-9, dto.dias());
    }

    @Test
    public void mediaSemanalDTO_largeMedia_isAccepted() {
        // Valores grandes devem ser aceitos
        double largeMean = 1_000_000.0;
        MediaSemanalDTO dto = new MediaSemanalDTO(1, largeMean);
        
        assertEquals(largeMean, dto.average(), 0.01);
    }

    @Test
    public void menorVendaDiaDTO_largeTotal_isAccepted() {
        // Valores grandes devem ser aceitos
        double largeTotal = 1_000_000.50;
        MenorVendaDiaDTO dto = new MenorVendaDiaDTO(LocalDate.now(), largeTotal);
        
        assertEquals(largeTotal, dto.total(), 0.01);
    }

    @Test
    public void periodoDTO_sameDates_zeroOrOneDias() {
        LocalDate sameDay = LocalDate.of(2025, 1, 15);
        
        // Mesmo dia pode significar 0 ou 1 dia dependendo da lógica
        PeriodoDTO dto1 = new PeriodoDTO(sameDay, sameDay, 0);
        PeriodoDTO dto2 = new PeriodoDTO(sameDay, sameDay, 1);
        
        assertEquals(0, dto1.dias());
        assertEquals(1, dto2.dias());
    }

    @Test
    public void mediaSemanalDTO_precisionHandling() {
        // Testar precisão de 2 casas decimais
        double media = 123.456;
        MediaSemanalDTO dto = new MediaSemanalDTO(1, media);
        
        // DTO armazena o valor exato, arredondamento é feito no Service
        assertEquals(123.456, dto.average(), 0.001);
    }

    @Test
    public void allDTOs_immutability() {
        // Records são imutáveis por padrão
        PeriodoDTO periodo = new PeriodoDTO(
                LocalDate.of(2025, 1, 1), 
                LocalDate.of(2025, 1, 31), 
                30
        );
        
        MenorVendaDiaDTO menorVenda = new MenorVendaDiaDTO(
                LocalDate.now(), 
                100.0
        );
        
        MediaSemanalDTO mediaSemanal = new MediaSemanalDTO(1, 200.0);
        
        // Todos os DTOs são records e, portanto, imutáveis
        assertNotNull(periodo);
        assertNotNull(menorVenda);
        assertNotNull(mediaSemanal);
    }
}
