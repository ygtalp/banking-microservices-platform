package com.banking.aml.controller;

import com.banking.aml.model.SanctionList;
import com.banking.aml.service.SanctionListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/sanctions")
@RequiredArgsConstructor
public class SanctionScreeningController {

    private final SanctionListService sanctionListService;

    @GetMapping
    public ResponseEntity<List<SanctionList>> getAllActiveSanctions() {
        return ResponseEntity.ok(sanctionListService.getAllActiveSanctions());
    }

    @GetMapping("/list/{listName}")
    public ResponseEntity<List<SanctionList>> getSanctionsByList(
            @PathVariable("listName") SanctionList.SanctionListName listName) {
        return ResponseEntity.ok(sanctionListService.getSanctionsByListName(listName));
    }

    @GetMapping("/search/name")
    public ResponseEntity<List<SanctionList>> searchByName(@RequestParam String name) {
        return ResponseEntity.ok(sanctionListService.searchByName(name));
    }

    @GetMapping("/screen/national-id/{nationalId}")
    public ResponseEntity<?> screenByNationalId(@PathVariable("nationalId") String nationalId) {
        return sanctionListService.screenByNationalId(nationalId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/screen/passport/{passportNumber}")
    public ResponseEntity<?> screenByPassport(@PathVariable("passportNumber") String passportNumber) {
        return sanctionListService.screenByPassportNumber(passportNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/search/advanced")
    public ResponseEntity<List<SanctionList>> advancedSearch(
            @RequestParam String name,
            @RequestParam(required = false) LocalDate dob,
            @RequestParam(required = false) String nationality) {
        return ResponseEntity.ok(sanctionListService.advancedSearch(name, dob, nationality));
    }

    @PostMapping("/import")
    public ResponseEntity<SanctionListService.ImportResult> importFromCsv(
            @RequestParam("file") MultipartFile file,
            @RequestParam("listName") SanctionList.SanctionListName listName) {
        SanctionListService.ImportResult result = sanctionListService.importFromCsv(file, listName);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Integer> refreshAllSanctions() {
        int count = sanctionListService.refreshAllSanctions();
        return ResponseEntity.ok(count);
    }

    @PostMapping("/{sanctionId}/delist")
    public ResponseEntity<Void> delistSanction(
            @PathVariable("sanctionId") String sanctionId,
            @RequestParam String reason) {
        sanctionListService.delistSanction(sanctionId, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/statistics")
    public ResponseEntity<SanctionListService.SanctionStatistics> getStatistics() {
        return ResponseEntity.ok(sanctionListService.getStatistics());
    }
}
