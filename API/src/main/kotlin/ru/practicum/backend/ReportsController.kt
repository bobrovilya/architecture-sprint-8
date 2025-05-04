package ru.practicum.backend

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/reports")
@CrossOrigin(origins = ["http://localhost:3000/*"])
class ReportsController() {

    @GetMapping
    @PreAuthorize("hasRole('prothetic_user')")
    fun getReport(request: HttpServletRequest): ResponseEntity<Report> {
        return ResponseEntity.ok(generateReport())
    }

    private fun generateReport() = Report(
        pilotName = "Ivanov Ivan",
        status = "OK",
        batteryCharge = 98.0,
    )
}