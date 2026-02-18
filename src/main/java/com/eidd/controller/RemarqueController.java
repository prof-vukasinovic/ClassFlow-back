package com.eidd.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.eidd.dto.RemarqueDto;
import com.eidd.dto.RemarqueRequest;
import com.eidd.dto.RemarqueStats;
import com.eidd.dto.RemarqueType;
import com.eidd.service.ClassRoomPlanService;
import com.eidd.service.RemarqueService;

@RestController
public class RemarqueController {
    private final RemarqueService remarqueService;
    private final ClassRoomPlanService planService;

    public RemarqueController(RemarqueService remarqueService, ClassRoomPlanService planService) {
        this.remarqueService = remarqueService;
        this.planService = planService;
    }

    @GetMapping("/remarques")
    public List<RemarqueDto> listRemarques() {
        return remarqueService.listAll();
    }

    @GetMapping("/remarques/{id}")
    public ResponseEntity<RemarqueDto> getRemarque(@PathVariable long id) {
        RemarqueDto remarque = remarqueService.getById(id);
        if (remarque == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarque);
    }

    @GetMapping("/classrooms/{classRoomId}/remarques")
    public ResponseEntity<List<RemarqueDto>> getClassRoomRemarques(@PathVariable long classRoomId) {
        if (!classRoomExists(classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByClassRoomId(classRoomId));
    }

    @GetMapping("/eleves/{eleveId}/remarques")
    public ResponseEntity<List<RemarqueDto>> getEleveRemarques(@PathVariable long eleveId) {
        if (!eleveExists(eleveId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByEleveId(eleveId));
    }

    @GetMapping("/classrooms/{classRoomId}/eleves/{eleveId}/remarques")
    public ResponseEntity<List<RemarqueDto>> getClassRoomEleveRemarques(
        @PathVariable long classRoomId,
        @PathVariable long eleveId
    ) {
        if (!classRoomExists(classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        if (!eleveInClassRoom(classRoomId, eleveId)) {
            return ResponseEntity.notFound().build();
        }
        List<RemarqueDto> remarques = remarqueService.listByEleveId(eleveId).stream()
            .filter(remarque -> remarque.classRoomId() == null || remarque.classRoomId() == classRoomId)
            .toList();
        return ResponseEntity.ok(remarques);
    }

    @GetMapping("/remarques/stats")
    public RemarqueStats getStats() {
        return remarqueService.stats();
    }

    @PostMapping("/remarques")
    public ResponseEntity<?> createRemarque(@RequestBody RemarqueRequest request) {
        if (request == null || isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule is required"));
        }
        if (request.classRoomId() != null && !classRoomExists(request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto created = remarqueService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/remarques/{id}")
    public ResponseEntity<?> updateRemarque(@PathVariable long id, @RequestBody RemarqueRequest request) {
        if (request == null || (!hasUpdates(request))) {
            return ResponseEntity.badRequest().body(Map.of("error", "at least one field is required"));
        }
        if (request.intitule() != null && isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule cannot be blank"));
        }
        if (request.classRoomId() != null && !classRoomExists(request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto updated = remarqueService.update(id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/remarques/{id}")
    public ResponseEntity<Void> deleteRemarque(@PathVariable long id) {
        if (!remarqueService.delete(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ========== Endpoints spécifiques pour les devoirs non faits ==========

    @GetMapping("/devoirs-non-faits")
    public List<RemarqueDto> listDevoirsNonFaits() {
        return remarqueService.listByType(RemarqueType.DEVOIR_NON_FAIT);
    }

    @GetMapping("/classrooms/{classRoomId}/devoirs-non-faits")
    public ResponseEntity<List<RemarqueDto>> getClassRoomDevoirsNonFaits(@PathVariable long classRoomId) {
        if (!classRoomExists(classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByClassRoomIdAndType(classRoomId, RemarqueType.DEVOIR_NON_FAIT));
    }

    @GetMapping("/eleves/{eleveId}/devoirs-non-faits")
    public ResponseEntity<List<RemarqueDto>> getEleveDevoirsNonFaits(@PathVariable long eleveId) {
        if (!eleveExists(eleveId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByEleveIdAndType(eleveId, RemarqueType.DEVOIR_NON_FAIT));
    }

    @PostMapping("/devoirs-non-faits")
    public ResponseEntity<?> createDevoirNonFait(@RequestBody RemarqueRequest request) {
        if (request == null || isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule is required"));
        }
        if (request.classRoomId() != null && !classRoomExists(request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto created = remarqueService.create(request, RemarqueType.DEVOIR_NON_FAIT);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========== Endpoints spécifiques pour les bavardages ==========

    @GetMapping("/bavardages")
    public List<RemarqueDto> listBavardages() {
        return remarqueService.listByType(RemarqueType.BAVARDAGE);
    }

    @GetMapping("/classrooms/{classRoomId}/bavardages")
    public ResponseEntity<List<RemarqueDto>> getClassRoomBavardages(@PathVariable long classRoomId) {
        if (!classRoomExists(classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByClassRoomIdAndType(classRoomId, RemarqueType.BAVARDAGE));
    }

    @GetMapping("/eleves/{eleveId}/bavardages")
    public ResponseEntity<List<RemarqueDto>> getEleveBavardages(@PathVariable long eleveId) {
        if (!eleveExists(eleveId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByEleveIdAndType(eleveId, RemarqueType.BAVARDAGE));
    }

    @PostMapping("/bavardages")
    public ResponseEntity<?> createBavardage(@RequestBody RemarqueRequest request) {
        if (request == null || isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule is required"));
        }
        if (request.classRoomId() != null && !classRoomExists(request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto created = remarqueService.create(request, RemarqueType.BAVARDAGE);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========== Méthodes privées helpers ==========

    private boolean classRoomExists(long classRoomId) {
        return planService.getClassRoom(classRoomId) != null;
    }

    private boolean eleveExists(long eleveId) {
        return planService.getClassRooms().stream()
            .flatMap(cr -> cr.getEleves().getEleves().stream())
            .anyMatch(eleve -> eleve.getId() == eleveId);
    }

    private boolean eleveInClassRoom(long classRoomId, long eleveId) {
        var classRoom = planService.getClassRoom(classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return false;
        }
        return classRoom.getEleves().getEleves().stream()
            .anyMatch(eleve -> eleve.getId() == eleveId);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasUpdates(RemarqueRequest request) {
        return request.intitule() != null || request.eleveId() != null || request.classRoomId() != null || request.type() != null;
    }
}
