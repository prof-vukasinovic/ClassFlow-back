package com.eidd.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.eidd.dto.ClassRoomPlan;
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;
import com.eidd.model.Groupe;
import com.eidd.model.Position;
import com.eidd.model.Table;

@Service
public class ClassRoomPlanService {
    private final Map<Long, ClassRoom> classRooms = new LinkedHashMap<>();
    private final ClassRoomService classRoomService = new ClassRoomService();
    private final GroupeService groupeService = new GroupeService();
    private final TableService tableService = new TableService();

    public ClassRoomPlanService() {
        seedSampleData();
    }

    public List<ClassRoom> getClassRooms() {
        return new ArrayList<>(classRooms.values());
    }

    public ClassRoom getClassRoom(long id) {
        return classRooms.get(id);
    }

    public ClassRoomPlan getPlan(long id) {
        ClassRoom classRoom = classRooms.get(id);
        if (classRoom == null) {
            return null;
        }
        return new ClassRoomPlan(classRoom, classRoom.getEleves().getEleves(), classRoom.getTables());
    }

    public List<Eleve> getEleves(long id) {
        ClassRoom classRoom = classRooms.get(id);
        if (classRoom == null) {
            return List.of();
        }
        return classRoom.getEleves().getEleves();
    }

    public List<Table> getTables(long id) {
        ClassRoom classRoom = classRooms.get(id);
        if (classRoom == null) {
            return List.of();
        }
        return classRoom.getTables();
    }

    private void seedSampleData() {
        ClassRoom salleA = createClassRoom("Salle A", 3, 2);
        ClassRoom salleB = createClassRoom("Salle B", 2, 2);
        classRooms.put(salleA.getId(), salleA);
        classRooms.put(salleB.getId(), salleB);
    }

    private ClassRoom createClassRoom(String name, int width, int height) {
        ClassRoom classRoom = classRoomService.creerClassRoom(name);
        Groupe groupe = new Groupe();
        List<Table> tables = new ArrayList<>();

        long eleveId = 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Table table = tableService.creerTable(new Position(x, y));
                tables.add(table);

                Eleve eleve = new Eleve(eleveId, "Eleve" + eleveId, "Prenom" + eleveId);
                eleve.setTable(table);
                groupeService.ajouterEleve(groupe, eleve);
                eleveId++;
            }
        }

        classRoom.setEleves(groupe);
        classRoom.setTables(tables);
        return classRoom;
    }
}
