package com.eidd.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.eidd.dto.ClassRoomCreateRequest;
import com.eidd.dto.ClassRoomPlan;
import com.eidd.dto.ClassRoomRemarquesDto;
import com.eidd.dto.ClassRoomUpdateRequest;
import com.eidd.dto.EleveCreateRequest;
import com.eidd.dto.EleveRemarquesDto;
import com.eidd.dto.EleveUpdateRequest;
import com.eidd.dto.GroupeCreateRequest;
import com.eidd.dto.GroupeDto;
import com.eidd.dto.GroupeRandomCreateRequest;
import com.eidd.dto.GroupeUpdateRequest;
import com.eidd.dto.RemarqueDto;
import com.eidd.dto.TableCreateRequest;
import com.eidd.dto.TablePlanDto;
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;
import com.eidd.model.Table;
import com.eidd.service.ClassRoomPlanService;
import com.eidd.service.GroupeEntry;
import com.eidd.service.RemarqueService;

@RestController
public class ClassRoomController {
    private final ClassRoomPlanService planService;
    private final RemarqueService remarqueService;
    private final String appVersion;

    public ClassRoomController(ClassRoomPlanService planService,
            RemarqueService remarqueService,
            @Value("${app.version:unknown}") String appVersion) {
        this.planService = planService;
        this.remarqueService = remarqueService;
        this.appVersion = appVersion;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "classflow-back");
    }

    @GetMapping("/version")
    public Map<String, String> version() {
        return Map.of("version", appVersion);
    }

    @PostMapping("/classrooms")
    public ResponseEntity<?> createClassRoom(@RequestBody ClassRoomCreateRequest request) {
        if (request == null || request.nom() == null || request.nom().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "nom is required"));
        }

        ClassRoom created = planService.createNewClassRoom(request.nom());
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "unable to create classroom"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toClassRoomRemarques(created));
    }

    @GetMapping("/classrooms")
    public List<ClassRoomRemarquesDto> getClassRooms() {
        return planService.getClassRooms().stream()
            .map(this::toClassRoomRemarques)
            .toList();
    }

    @GetMapping("/classrooms/{id}")
    public ResponseEntity<ClassRoomRemarquesDto> getClassRoom(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toClassRoomRemarques(classRoom));
    }

    @PutMapping("/classrooms/{id}")
    public ResponseEntity<?> updateClassRoom(@PathVariable long id, @RequestBody ClassRoomUpdateRequest request) {
        if (request == null || request.nom() == null || request.nom().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "nom is required"));
        }

        ClassRoom updated = planService.updateClassRoom(id, request.nom());
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toClassRoomRemarques(updated));
    }

    @GetMapping("/classrooms/{id}/eleves")
    public ResponseEntity<List<EleveRemarquesDto>> getEleves(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }
        List<EleveRemarquesDto> eleves = classRoom.getEleves().getEleves().stream()
            .map(eleve -> toEleveRemarques(classRoom.getId(), eleve))
            .toList();
        return ResponseEntity.ok(eleves);
    }

    @GetMapping("/classrooms/{id}/tables")
    public ResponseEntity<List<Table>> getTables(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(planService.getTables(id));
    }

    @GetMapping("/classrooms/{id}/plan")
    public ResponseEntity<ClassRoomPlan> getPlan(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }
        List<TablePlanDto> tables = classRoom.getTables().stream()
            .map(table -> new TablePlanDto(
                table.getPosition().getX(),
                table.getPosition().getY(),
                findEleveForTable(classRoom, table)))
            .toList();
        return ResponseEntity.ok(new ClassRoomPlan(classRoom.getId(), classRoom.getNom(), tables));
    }

    @PostMapping("/classrooms/import-csv")
    public ResponseEntity<?> importClassRoomFromCsv(@RequestBody String csvContent) {
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "CSV content is required"));
        }

        ClassRoom imported = planService.importFromCsv(csvContent);
        if (imported == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid CSV format"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toClassRoomRemarques(imported));
    }

    @GetMapping("/classrooms/{id}/export-csv")
    public ResponseEntity<String> exportClassRoomToCsv(@PathVariable long id) {
        String csvContent = planService.exportToCsv(id);
        if (csvContent == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .header("Content-Type", "text/csv")
            .header("Content-Disposition", "attachment; filename=\"classroom-" + id + ".csv\"")
            .body(csvContent);
    }

    @PostMapping("/classrooms/{id}/eleves")
    public ResponseEntity<?> createEleve(@PathVariable long id, @RequestBody EleveCreateRequest request) {
        if (request == null || isBlank(request.nom()) || isBlank(request.prenom())) {
            return ResponseEntity.badRequest().body(Map.of("error", "nom and prenom are required"));
        }

        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }


        Integer tableIndex = request.tableIndex();
        if (tableIndex != null) {
            List<Table> tables = classRoom.getTables();
            if (tables == null || tableIndex < 0 || tableIndex >= tables.size()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "table not found"));
            }
        }

        Eleve eleve = planService.createEleve(id, request.nom().trim(), request.prenom().trim(), tableIndex);
        if (eleve == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid eleve"));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(toEleveRemarques(id, eleve));
    }

    @PutMapping("/classrooms/{classRoomId}/eleves/{eleveId}")
    public ResponseEntity<?> updateEleve(@PathVariable long classRoomId, 
            @PathVariable long eleveId,
            @RequestBody EleveUpdateRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "update payload is required"));
        }

        ClassRoom classRoom = planService.getClassRoom(classRoomId);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }

        Eleve updated = planService.updateEleve(classRoomId, eleveId, request.nom(), request.prenom());
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toEleveRemarques(classRoomId, updated));
    }

    @PostMapping("/classrooms/{id}/tables")
    public ResponseEntity<?> createTable(@PathVariable long id, @RequestBody TableCreateRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "table position is required"));
        }

        Table table = planService.createTable(id, request.x(), request.y());
        if (table == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(table);
    }

    @PostMapping("/classrooms/{id}/groupes/aleatoire")
    public ResponseEntity<?> createGroupesAleatoires(@PathVariable long id, @RequestBody GroupeRandomCreateRequest request) {
        if (request == null || request.groupCount() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "groupCount must be greater than 0"));
        }

        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }

        List<GroupeEntry> created = planService.createGroupesAleatoires(id, request.groupCount());
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "unable to create random groups"));
        }

        List<GroupeDto> response = created.stream()
            .map(entry -> toGroupeDto(classRoom.getId(), entry))
            .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/classrooms/{id}/groupes")
    public ResponseEntity<?> createGroupes(@PathVariable long id, @RequestBody GroupeCreateRequest request) {
        if (request == null || request.groupes() == null || request.groupes().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "groupes are required"));
        }

        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }

        List<GroupeEntry> created = planService.createGroupes(id, request.groupes());
        if (created == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "unable to create groups"));
        }

        List<GroupeDto> response = created.stream()
            .map(entry -> toGroupeDto(classRoom.getId(), entry))
            .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/classrooms/{id}/groupes")
    public ResponseEntity<List<GroupeDto>> getGroupes(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }

        List<GroupeDto> response = planService.getGroupes(id).stream()
            .map(entry -> toGroupeDto(classRoom.getId(), entry))
            .toList();
        return ResponseEntity.ok(response);
    }

    @PutMapping("/classrooms/{classRoomId}/groupes/{groupeId}")
    public ResponseEntity<?> updateGroupe(@PathVariable long classRoomId,
            @PathVariable long groupeId,
            @RequestBody GroupeUpdateRequest request) {
        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "update payload is required"));
        }

        ClassRoom classRoom = planService.getClassRoom(classRoomId);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }

        boolean groupeExists = planService.getGroupes(classRoomId).stream()
            .anyMatch(entry -> entry.id() == groupeId);
        if (!groupeExists) {
            return ResponseEntity.notFound().build();
        }

        GroupeEntry updated = planService.updateGroupe(classRoomId, groupeId, request.addEleveIds(), request.removeEleveIds());
        if (updated == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "unable to update group"));
        }

        return ResponseEntity.ok(toGroupeDto(classRoom.getId(), updated));
    }

    @DeleteMapping("/classrooms/{id}")
    public ResponseEntity<Void> deleteClassRoom(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }

        List<Long> eleveIds = classRoom.getEleves() == null
            ? List.of()
            : classRoom.getEleves().getEleves().stream().map(Eleve::getId).toList();

        planService.deleteClassRoom(id);
        remarqueService.deleteByClassRoomId(id);
        remarqueService.deleteByEleveIds(eleveIds);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/classrooms/{classRoomId}/eleves/{eleveId}")
    public ResponseEntity<Void> deleteEleve(@PathVariable long classRoomId, @PathVariable long eleveId) {
        if (!planService.deleteEleve(classRoomId, eleveId)) {
            return ResponseEntity.notFound().build();
        }
        remarqueService.deleteByEleveId(eleveId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/classrooms/{classRoomId}/tables/{tableIndex}")
    public ResponseEntity<Void> deleteTable(@PathVariable long classRoomId, @PathVariable int tableIndex) {
        if (!planService.deleteTableByIndex(classRoomId, tableIndex)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/classrooms/{classRoomId}/groupes/{groupeId}")
    public ResponseEntity<Void> deleteGroupe(@PathVariable long classRoomId, @PathVariable long groupeId) {
        if (!planService.deleteGroupe(classRoomId, groupeId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    private ClassRoomRemarquesDto toClassRoomRemarques(ClassRoom classRoom) {
        List<EleveRemarquesDto> eleves = classRoom.getEleves().getEleves().stream()
            .map(eleve -> toEleveRemarques(classRoom.getId(), eleve))
            .toList();
        return new ClassRoomRemarquesDto(classRoom.getId(), classRoom.getNom(), eleves, classRoom.getTables());
    }

    private EleveRemarquesDto toEleveRemarques(long classRoomId, Eleve eleve) {
        List<RemarqueDto> remarques = remarqueService.listByEleveId(eleve.getId()).stream()
            .filter(remarque -> remarque.classRoomId() == null || remarque.classRoomId() == classRoomId)
            .toList();
        return new EleveRemarquesDto(eleve.getId(), eleve.getNom(), eleve.getPrenom(), remarques);
    }

    private EleveRemarquesDto findEleveForTable(ClassRoom classRoom, Table table) {
        return classRoom.getEleves().getEleves().stream()
            .filter(eleve -> eleve.getTable() == table)
            .findFirst()
            .map(eleve -> toEleveRemarques(classRoom.getId(), eleve))
            .orElse(null);
    }

    private GroupeDto toGroupeDto(long classRoomId, GroupeEntry entry) {
        List<EleveRemarquesDto> eleves = entry.groupe().getEleves().stream()
            .map(eleve -> toEleveRemarques(classRoomId, eleve))
            .toList();
        return new GroupeDto(entry.id(), eleves);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}