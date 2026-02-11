package com.eidd.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.eidd.dto.ClassRoomPlan;
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;
import com.eidd.model.Table;
import com.eidd.service.ClassRoomPlanService;

@RestController
public class ClassRoomController {
    private final ClassRoomPlanService planService;

    public ClassRoomController(ClassRoomPlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "ok", "service", "classflow-back");
    }

    @GetMapping("/classrooms")
    public List<ClassRoom> getClassRooms() {
        return planService.getClassRooms();
    }

    @GetMapping("/classrooms/{id}")
    public ResponseEntity<ClassRoom> getClassRoom(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(classRoom);
    }

    @GetMapping("/classrooms/{id}/eleves")
    public ResponseEntity<List<Eleve>> getEleves(@PathVariable long id) {
        ClassRoom classRoom = planService.getClassRoom(id);
        if (classRoom == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(planService.getEleves(id));
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
        ClassRoomPlan plan = planService.getPlan(id);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(plan);
    }
}
