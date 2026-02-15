package com.eidd.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.eidd.DTO.ClassRoomExport;
import com.eidd.model.ClassRoom;
import com.eidd.model.Eleve;
import com.eidd.model.Groupe;
import com.eidd.model.Position;
import com.eidd.model.Table;
import com.eidd.repositories.ClassRoomRespository;

@Service
public class ClassRoomPlanService {
    private final Map<Long, ClassRoom> classRooms = new LinkedHashMap<>();
    private final ClassRoomService classRoomService = new ClassRoomService();
    private final GroupeService groupeService = new GroupeService();
    private final TableService tableService = new TableService();
    private long eleveIdCounter = 1;

    public ClassRoomPlanService() {
        seedSampleData();
    }

    public List<ClassRoom> getClassRooms() {
        return new ArrayList<>(classRooms.values());
    }

    public ClassRoom getClassRoom(long id) {
        return classRooms.get(id);
    }

    public boolean deleteClassRoom(long id) {
        return classRooms.remove(id) != null;
    }

    public boolean deleteEleve(long classRoomId, long eleveId) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null || classRoom.getEleves() == null) {
            return false;
        }
        List<Eleve> eleves = classRoom.getEleves().getEleves();
        if (eleves == null) {
            return false;
        }
        for (int i = 0; i < eleves.size(); i++) {
            Eleve eleve = eleves.get(i);
            if (eleve.getId() == eleveId) {
                eleves.remove(i);
                return true;
            }
        }
        return false;
    }

    public Eleve createEleve(long classRoomId, String nom, String prenom, Integer tableIndex) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null) {
            return null;
        }

        Groupe groupe = classRoom.getEleves();
        if (groupe == null) {
            groupe = new Groupe();
            classRoom.setEleves(groupe);
        }

        Eleve eleve = new Eleve(eleveIdCounter++, nom, prenom);
        if (tableIndex != null) {
            List<Table> tables = classRoom.getTables();
            if (tables == null || tableIndex < 0 || tableIndex >= tables.size()) {
                return null;
            }
            eleve.setTable(tables.get(tableIndex));
        }

        groupeService.ajouterEleve(groupe, eleve);
        return eleve;
    }

    public Table createTable(long classRoomId, int x, int y) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null) {
            return null;
        }

        List<Table> tables = classRoom.getTables();
        if (tables == null) {
            tables = new ArrayList<>();
            classRoom.setTables(tables);
        }

        Table table = tableService.creerTable(new Position(x, y));
        tables.add(table);
        return table;
    }

    public boolean deleteTableByIndex(long classRoomId, int tableIndex) {
        ClassRoom classRoom = classRooms.get(classRoomId);
        if (classRoom == null || classRoom.getTables() == null) {
            return false;
        }
        List<Table> tables = classRoom.getTables();
        if (tableIndex < 0 || tableIndex >= tables.size()) {
            return false;
        }
        Table removed = tables.remove(tableIndex);
        if (classRoom.getEleves() != null) {
            for (Eleve eleve : classRoom.getEleves().getEleves()) {
                if (eleve.getTable() == removed) {
                    eleve.setTable(null);
                }
            }
        }
        return true;
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

    public ClassRoomExport exportClassRoom(long id) {
        ClassRoom classRoom = classRooms.get(id);
        if (classRoom == null) {
            return null;
        }
        return new ClassRoomExport(classRoom);
    }

    public ClassRoom saveClassRoom(ClassRoomExport export) {
        if (export == null) {
            return null;
        }
        ClassRoom classRoom = new ClassRoom(export);
        classRooms.put(classRoom.getId(), classRoom);
        updateCountersFromImport(classRoom);
        return classRoom;
    }

    private void seedSampleData() {
        ClassRoomRespository.resetCounter();
        ClassRoomRespository.incrementCounter();
        eleveIdCounter = 1;
        ClassRoom salleA = createClassRoom("Salle A", 3, 2);
        ClassRoom salleB = createClassRoom("Salle B", 2, 2);
        classRooms.put(salleA.getId(), salleA);
        classRooms.put(salleB.getId(), salleB);
    }

    private ClassRoom createClassRoom(String name, int width, int height) {
        ClassRoom classRoom = classRoomService.creerClassRoom(name);
        Groupe groupe = new Groupe();
        List<Table> tables = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Table table = tableService.creerTable(new Position(x, y));
                tables.add(table);

                long eleveId = eleveIdCounter++;
                Eleve eleve = new Eleve(eleveId, "Eleve" + eleveId, "Prenom" + eleveId);
                eleve.setTable(table);
                groupeService.ajouterEleve(groupe, eleve);
            }
        }

        classRoom.setEleves(groupe);
        classRoom.setTables(tables);
        return classRoom;
    }

    private void updateCountersFromImport(ClassRoom classRoom) {
        if (classRoom == null) {
            return;
        }

        long classRoomId = classRoom.getId();
        while (ClassRoomRespository.getCounter() <= classRoomId) {
            ClassRoomRespository.incrementCounter();
        }

        long maxEleveId = classRoom.getEleves() == null ? 0
            : classRoom.getEleves().getEleves().stream()
                .mapToLong(Eleve::getId)
                .max()
                .orElse(0);
        if (maxEleveId >= eleveIdCounter) {
            eleveIdCounter = maxEleveId + 1;
        }
    }
}
