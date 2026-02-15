package com.eidd.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.eidd.DTO.ClassRoomExport;
import com.eidd.dto.ClassRoomPlan;
import com.eidd.dto.ClassRoomRemarquesDto;
import com.eidd.dto.EleveCreateRequest;
import com.eidd.dto.EleveRemarquesDto;
import com.eidd.dto.RemarqueDto;
import com.eidd.dto.TableCreateRequest;
import com.eidd.dto.TablePlanDto;
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;
import com.eidd.model.Table;
import com.eidd.service.ClassRoomPlanService;
import com.eidd.service.RemarqueService;

@RestController
public class ClassRoomController {
    private final ClassRoomPlanService planService;
    private final RemarqueService remarqueService;

    public ClassRoomController(ClassRoomPlanService planService, RemarqueService remarqueService) {
        this.planService = planService;
        this.remarqueService = remarqueService;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "classflow-back");
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

    @GetMapping("/classrooms/{id}/chargement")
    public ResponseEntity<ClassRoomExport> loadClassRoom(@PathVariable long id) {
        ClassRoomExport export = planService.exportClassRoom(id);
        if (export == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(export);
    }

    @PostMapping("/classrooms/sauvegarde")
    public ResponseEntity<?> saveClassRoom(@RequestBody ClassRoomExport export) {
        if (export == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "classRoom export is required"));
        }

        boolean exists = planService.getClassRoom(export.getId()) != null;
        ClassRoom saved = planService.saveClassRoom(export);
        if (saved == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid classRoom export"));
        }

        HttpStatus status = exists ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(export);
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

        EleveRemarquesDto dto = new EleveRemarquesDto(eleve.getId(), eleve.getNom(), eleve.getPrenom(), List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}