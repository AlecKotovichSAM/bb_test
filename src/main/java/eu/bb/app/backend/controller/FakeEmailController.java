package eu.bb.app.backend.controller;

import eu.bb.app.backend.service.FakeEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fake-email")
@Tag(name = "Fake Email", description = "Тестовый API для отправки email (выводит в консоль вместо реальной отправки)")
public class FakeEmailController {
    private final FakeEmailService svc;
    public FakeEmailController(FakeEmailService svc) { this.svc = svc; }
    
    @PostMapping("/send")
    @Operation(summary = "Отправить тестовое email", description = "Имитирует отправку email. В реальности выводит информацию в консоль. Используется для разработки и тестирования")
    @ApiResponse(responseCode = "200", description = "Email успешно отправлен (выведен в консоль)")
    public void send(
            @Parameter(description = "Email получателя", required = true) @RequestParam String to, 
            @Parameter(description = "Тема письма", required = true) @RequestParam String subject, 
            @Parameter(description = "Текст письма", required = true) @RequestParam String body) { 
        svc.send(to, subject, body); 
    }
}
