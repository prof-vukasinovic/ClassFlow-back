package com.eidd.controller;

import java.security.Principal;
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
    public List<RemarqueDto> listRemarques(Principal principal) {
        return remarqueService.listAll(owner(principal));
    }

    @GetMapping("/remarques/{id}")
    public ResponseEntity<RemarqueDto> getRemarque(@PathVariable long id, Principal principal) {
        RemarqueDto remarque = remarqueService.getById(owner(principal), id);
        if (remarque == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarque);
    }

    @GetMapping("/classrooms/{classRoomId}/remarques")
    public ResponseEntity<List<RemarqueDto>> getClassRoomRemarques(@PathVariable long classRoomId, Principal principal) {
        if (!classRoomExists(owner(principal), classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByClassRoomId(owner(principal), classRoomId));
    }

    @GetMapping("/eleves/{eleveId}/remarques")
    public ResponseEntity<List<RemarqueDto>> getEleveRemarques(@PathVariable long eleveId, Principal principal) {
        if (!eleveExists(owner(principal), eleveId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByEleveId(owner(principal), eleveId));
    }

    @GetMapping("/classrooms/{classRoomId}/eleves/{eleveId}/remarques")
    public ResponseEntity<List<RemarqueDto>> getClassRoomEleveRemarques(
        @PathVariable long classRoomId,
        @PathVariable long eleveId,
        Principal principal
    ) {
        if (!classRoomExists(owner(principal), classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        if (!eleveInClassRoom(owner(principal), classRoomId, eleveId)) {
            return ResponseEntity.notFound().build();
        }
        List<RemarqueDto> remarques = remarqueService.listByEleveId(owner(principal), eleveId).stream()
            .filter(remarque -> remarque.classRoomId() == null || remarque.classRoomId() == classRoomId)
            .toList();
        return ResponseEntity.ok(remarques);
    }

    @GetMapping("/remarques/stats")
    public RemarqueStats getStats(Principal principal) {
        return remarqueService.stats(owner(principal));
    }

    @PostMapping("/remarques")
    public ResponseEntity<?> createRemarque(@RequestBody RemarqueRequest request, Principal principal) {
        if (request == null || isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule is required"));
        }
        if (request.classRoomId() != null && !classRoomExists(owner(principal), request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(owner(principal), request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto created = remarqueService.create(owner(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/remarques/{id}")
    public ResponseEntity<?> updateRemarque(@PathVariable long id, @RequestBody RemarqueRequest request, Principal principal) {
        if (request == null || (!hasUpdates(request))) {
            return ResponseEntity.badRequest().body(Map.of("error", "at least one field is required"));
        }
        if (request.intitule() != null && isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule cannot be blank"));
        }
        if (request.classRoomId() != null && !classRoomExists(owner(principal), request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(owner(principal), request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto updated = remarqueService.update(owner(principal), id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/remarques/{id}")
    public ResponseEntity<Void> deleteRemarque(@PathVariable long id, Principal principal) {
        if (!remarqueService.delete(owner(principal), id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    // ========== Endpoints spécifiques pour les devoirs non faits ==========

    @GetMapping("/devoirs-non-faits")
    public List<RemarqueDto> listDevoirsNonFaits(Principal principal) {
        return remarqueService.listByType(owner(principal), RemarqueType.DEVOIR_NON_FAIT);
    }

    @GetMapping("/classrooms/{classRoomId}/devoirs-non-faits")
    public ResponseEntity<List<RemarqueDto>> getClassRoomDevoirsNonFaits(@PathVariable long classRoomId, Principal principal) {
        if (!classRoomExists(owner(principal), classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByClassRoomIdAndType(owner(principal), classRoomId, RemarqueType.DEVOIR_NON_FAIT));
    }

    @GetMapping("/eleves/{eleveId}/devoirs-non-faits")
    public ResponseEntity<List<RemarqueDto>> getEleveDevoirsNonFaits(@PathVariable long eleveId, Principal principal) {
        if (!eleveExists(owner(principal), eleveId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByEleveIdAndType(owner(principal), eleveId, RemarqueType.DEVOIR_NON_FAIT));
    }

    @PostMapping("/devoirs-non-faits")
    public ResponseEntity<?> createDevoirNonFait(@RequestBody RemarqueRequest request, Principal principal) {
        if (request == null || isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule is required"));
        }
        if (request.classRoomId() != null && !classRoomExists(owner(principal), request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(owner(principal), request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto created = remarqueService.create(owner(principal), request, RemarqueType.DEVOIR_NON_FAIT);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========== Endpoints spécifiques pour les bavardages ==========

    @GetMapping("/bavardages")
    public List<RemarqueDto> listBavardages(Principal principal) {
        return remarqueService.listByType(owner(principal), RemarqueType.BAVARDAGE);
    }

    @GetMapping("/classrooms/{classRoomId}/bavardages")
    public ResponseEntity<List<RemarqueDto>> getClassRoomBavardages(@PathVariable long classRoomId, Principal principal) {
        if (!classRoomExists(owner(principal), classRoomId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByClassRoomIdAndType(owner(principal), classRoomId, RemarqueType.BAVARDAGE));
    }

    @GetMapping("/eleves/{eleveId}/bavardages")
    public ResponseEntity<List<RemarqueDto>> getEleveBavardages(@PathVariable long eleveId, Principal principal) {
        if (!eleveExists(owner(principal), eleveId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(remarqueService.listByEleveIdAndType(owner(principal), eleveId, RemarqueType.BAVARDAGE));
    }

    @PostMapping("/bavardages")
    public ResponseEntity<?> createBavardage(@RequestBody RemarqueRequest request, Principal principal) {
        if (request == null || isBlank(request.intitule())) {
            return ResponseEntity.badRequest().body(Map.of("error", "intitule is required"));
        }
        if (request.classRoomId() != null && !classRoomExists(owner(principal), request.classRoomId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "classroom not found"));
        }
        if (request.eleveId() != null && !eleveExists(owner(principal), request.eleveId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "eleve not found"));
        }

        RemarqueDto created = remarqueService.create(owner(principal), request, RemarqueType.BAVARDAGE);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ========== Méthodes privées helpers ==========

    private boolean classRoomExists(String owner, long classRoomId) {
        return planService.getClassRoom(owner, classRoomId) != null;
    }

    private boolean eleveExists(String owner, long eleveId) {
        return planService.getClassRooms(owner).stream()
            .flatMap(cr -> cr.getEleves().getEleves().stream())
            .anyMatch(eleve -> eleve.getId() == eleveId);
    }

    private boolean eleveInClassRoom(String owner, long classRoomId, long eleveId) {
        var classRoom = planService.getClassRoom(owner, classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return false;
        }
        return classRoom.getEleves().getEleves().stream()
            .anyMatch(eleve -> eleve.getId() == eleveId);
    }

    private String owner(Principal principal) {
        return principal == null ? "anonymous" : principal.getName();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean hasUpdates(RemarqueRequest request) {
        return request.intitule() != null || request.eleveId() != null || request.classRoomId() != null || request.type() != null;
    }
}
